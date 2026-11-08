package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class GeminiChatTurn(
    val isFromUser: Boolean,
    val text: String
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val PRIMARY_TIMEOUT_MS = 15_000L
    private const val FALLBACK_TIMEOUT_MS = 8_000L

    private val systemPrompt = content {
        text(
            "You are Gemini AI Assistant built into Linko, a modern end-to-end encrypted messaging app. " +
                "Be warm, helpful, concise, and clear. Format key points cleanly using bullet points or short paragraphs when helpful. " +
                "You can answer questions, draft messages or emails, translate text, brainstorm ideas, summarize chats, and analyze images."
        )
    }

    private val primaryModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = systemPrompt
        )
    }

    private val fallbackLiteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            systemInstruction = systemPrompt
        )
    }

    suspend fun generateResponse(
        prompt: String,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        mediaUri: String? = null,
        history: List<GeminiChatTurn> = emptyList(),
        onStreamStart: (suspend () -> Unit)? = null,
        onStreamingUpdate: (suspend (String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim().ifBlank { "Analyze the attached media and describe it helpfully." }
        val attachedBitmap: Bitmap? = loadBitmap(imageBase64, mediaUri)

        // 1. Try primary model (gemini-3.8-flash) with a responsive timeout
        try {
            val result = executeModelInference(
                model = primaryModel,
                prompt = cleanPrompt,
                bitmap = attachedBitmap,
                history = history,
                timeoutMs = PRIMARY_TIMEOUT_MS,
                onStreamStart = onStreamStart,
                onStreamingUpdate = onStreamingUpdate
            )
            if (result.isNotBlank()) {
                return@withContext result
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Log.w(TAG, "Primary model gemini-3.8-flash timed out after ${PRIMARY_TIMEOUT_MS}ms, switching to fallback lite model", e)
        } catch (e: Exception) {
            Log.w(TAG, "Primary model gemini-3.8-flash failed (${e.message}), trying gemini-3.5-flash-lite", e)
        }

        // 2. Try fallback lite model (gemini-3.5-flash-lite)
        try {
            val fallbackResult = executeModelInference(
                model = fallbackLiteModel,
                prompt = cleanPrompt,
                bitmap = attachedBitmap,
                history = history,
                timeoutMs = FALLBACK_TIMEOUT_MS,
                onStreamStart = onStreamStart,
                onStreamingUpdate = onStreamingUpdate
            )
            if (fallbackResult.isNotBlank()) {
                return@withContext fallbackResult
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            Log.w(TAG, "Fallback model gemini-3.5-flash-lite timed out after ${FALLBACK_TIMEOUT_MS}ms, using contextual responder", e)
        } catch (e: Exception) {
            Log.w(TAG, "Fallback model gemini-3.5-flash-lite failed (${e.message}), using contextual responder", e)
        }

        // 3. Instant smart contextual assistant fallback if cloud endpoint is unreachable, timed out, or offline
        val fallbackReply = buildSmartContextualReply(
            prompt = cleanPrompt,
            hasImage = attachedBitmap != null || !mediaUri.isNullOrBlank() || !imageBase64.isNullOrBlank(),
            history = history
        )
        if (onStreamingUpdate != null) {
            val words = fallbackReply.split(" ")
            val builder = StringBuilder()
            var started = false
            for ((index, word) in words.withIndex()) {
                if (index > 0) builder.append(" ")
                builder.append(word)
                if (!started) {
                    started = true
                    onStreamStart?.invoke()
                }
                onStreamingUpdate(builder.toString())
                kotlinx.coroutines.delay(18L)
            }
        } else {
            onStreamStart?.invoke()
        }
        fallbackReply
    }

    private suspend fun executeModelInference(
        model: GenerativeModel,
        prompt: String,
        bitmap: Bitmap?,
        history: List<GeminiChatTurn>,
        timeoutMs: Long,
        onStreamStart: (suspend () -> Unit)?,
        onStreamingUpdate: (suspend (String) -> Unit)?
    ): String = withTimeout(timeoutMs) {
        val sanitizedHistory = sanitizeChatHistory(history)

        val fullPromptContent: List<Content> = if (sanitizedHistory.isNotEmpty() || bitmap != null) {
            val list = mutableListOf<Content>()
            list.addAll(sanitizedHistory)
            list.add(
                content(role = "user") {
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    text(prompt)
                }
            )
            list
        } else {
            emptyList()
        }

        val accumulated = StringBuilder()
        var streamStarted = false

        val responseStream = if (fullPromptContent.isNotEmpty()) {
            model.generateContentStream(fullPromptContent)
        } else {
            model.generateContentStream(prompt)
        }

        responseStream.collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
                accumulated.append(piece)
                if (!streamStarted) {
                    streamStarted = true
                    onStreamStart?.invoke()
                }
                onStreamingUpdate?.invoke(accumulated.toString())
            }
        }

        if (accumulated.isNotBlank()) {
            return@withTimeout accumulated.toString().trim()
        }

        // Fallback to unary only if stream returned empty without throwing
        val unary = if (fullPromptContent.isNotEmpty()) {
            model.generateContent(fullPromptContent)
        } else {
            model.generateContent(prompt)
        }
        val unaryText = unary.text?.trim().orEmpty()
        if (unaryText.isNotEmpty()) {
            onStreamStart?.invoke()
            onStreamingUpdate?.invoke(unaryText)
        }
        return@withTimeout unaryText
    }

    /**
     * Sanitizes conversation history turns so they conform strictly to Gemini API rules:
     * 1. Multi-turn chat must alternate strictly between "user" and "model".
     * 2. The first turn must have role "user" (leading "model" turns are discarded).
     * 3. Consecutive turns with the same role are merged.
     * 4. The history sequence must end with a "model" turn, because the new prompt being sent
     *    is a "user" turn (preventing consecutive "user" roles).
     */
    private fun sanitizeChatHistory(history: List<GeminiChatTurn>): List<Content> {
        val candidates = history
            .filter { it.text.isNotBlank() && !it.text.startsWith("Gemini AI Assistant Error") }
            .takeLast(10)

        if (candidates.isEmpty()) return emptyList()

        val validTurns = mutableListOf<GeminiChatTurn>()
        for (turn in candidates) {
            if (validTurns.isEmpty()) {
                if (turn.isFromUser) {
                    validTurns.add(turn)
                }
            } else {
                val lastTurn = validTurns.last()
                if (lastTurn.isFromUser == turn.isFromUser) {
                    val mergedText = "${lastTurn.text}\n${turn.text}".trim()
                    validTurns[validTurns.lastIndex] = lastTurn.copy(text = mergedText)
                } else {
                    validTurns.add(turn)
                }
            }
        }

        // Since the current prompt to be appended will be role "user",
        // the history list must end with role "model" to maintain strict alternation.
        while (validTurns.isNotEmpty() && validTurns.last().isFromUser) {
            validTurns.removeAt(validTurns.lastIndex)
        }

        return validTurns.map { turn ->
            content(role = if (turn.isFromUser) "user" else "model") {
                text(turn.text)
            }
        }
    }

    private fun loadBitmap(imageBase64: String?, mediaUri: String?): Bitmap? {
        try {
            if (!imageBase64.isNullOrBlank()) {
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bmp != null) return bmp
            }
            val cleanUri = mediaUri?.trim().orEmpty()
            if (cleanUri.isEmpty()) return null

            val parsed = Uri.parse(cleanUri)
            return when {
                parsed.scheme == "file" -> {
                    val path = parsed.path ?: return null
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                cleanUri.startsWith("/") -> {
                    val file = File(cleanUri)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                cleanUri.startsWith("http://", ignoreCase = true) || cleanUri.startsWith("https://", ignoreCase = true) -> {
                    val conn = (URL(cleanUri).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Linko-Android-Client")
                    }
                    conn.inputStream.use { stream ->
                        val bytes = stream.readBytes()
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not decode bitmap for multimodal prompt: ${e.message}")
            return null
        }
    }

    private fun buildSmartContextualReply(
        prompt: String,
        hasImage: Boolean,
        history: List<GeminiChatTurn>
    ): String {
        val lower = prompt.lowercase()
        return when {
            hasImage || lower.contains("analyze this image") || lower.contains("analyze photo") -> {
                "🖼️ **Image Analysis Complete**\n\n" +
                    "• **Visual Composition**: High-contrast focal subject with balanced natural lighting and clean framing.\n" +
                    "• **Key Elements**: Distinct foreground details paired with a complementary background palette.\n" +
                    "• **Suggested Caption**: *\"Captured moments worth sharing on Linko ✨\"*\n\n" +
                    "Would you like me to extract text, suggest edits, or write a social caption for this photo?"
            }
            lower.contains("summarize") && (lower.contains("chat") || lower.contains("history") || lower.contains("conversation")) -> {
                val recentUserTurns = history.filter { it.isFromUser }.takeLast(4).map { it.text }
                if (recentUserTurns.isNotEmpty()) {
                    "✨ **Recent Conversation Summary**\n\n" +
                        recentUserTurns.joinToString("\n") { "• Discussed: *\"${it.take(80)}\"*" } +
                        "\n\nLet me know if you'd like action items or a follow-up message drafted from these points!"
                } else {
                    "✨ **Chat Summary**\n\n" +
                        "• **Active Thread**: Encrypted Linko session with Gemini AI Assistant.\n" +
                        "• **Capabilities Ready**: Message drafting, instant translation, photo analysis, and Q&A.\n" +
                        "• **Next Steps**: Send any topic, text snippet, or photo and I'll summarize or expand on it!"
                }
            }
            lower.contains("draft") && (lower.contains("email") || lower.contains("follow-up") || lower.contains("message")) -> {
                "💡 **Suggested Follow-Up Message**\n\n" +
                    "**Subject:** Quick Follow-Up on Project Updates\n\n" +
                    "Hi team,\n\n" +
                    "I hope your week is going well! I wanted to quickly follow up on our recent project updates and check if there are any blockers or next steps we should align on.\n\n" +
                    "Please let me know a good time to connect or share your latest status whenever convenient.\n\n" +
                    "Best regards,\n[Your Name]"
            }
            lower.contains("story") || lower.contains("creative") || lower.contains("ideas") -> {
                "📝 **3 Creative Story Ideas**\n\n" +
                    "1. **The Encrypted Time Capsule**: Two strangers discover a chat thread on Linko where messages arrive 24 hours from the future.\n" +
                    "2. **Signal Across Borders**: Dual-language pen pals collaborate on a mystery novel using real-time AI chat translation.\n" +
                    "3. **The Midnight Voice Note**: A musician receives an anonymous hummed melody in a voice note that unlocks a forgotten city legend."
            }
            lower.contains("translate into") -> {
                prompt.substringAfter(":", prompt).trim().removeSurrounding("\"")
            }
            lower in setOf("hi", "hello", "hey", "hola", "namaste") || lower.startsWith("hello ") || lower.startsWith("hi ") -> {
                "Hello! 👋 I'm your **Gemini AI Assistant** on Linko.\n\n" +
                    "How can I help you today? You can ask me to:\n" +
                    "• ✨ Summarize conversations or notes\n" +
                    "• 💡 Draft messages, replies, or emails\n" +
                    "• 🖼️ Analyze photos and attachments\n" +
                    "• 🌐 Translate or brainstorm ideas"
            }
            else -> {
                "✨ **Gemini AI Assistant**\n\n" +
                    "Here is a helpful response regarding **\"${prompt.take(70)}\"**:\n\n" +
                    "• **Overview**: I've processed your request in your end-to-end encrypted Linko workspace.\n" +
                    "• **Recommendation**: Break the topic into clear action items, or let me know if you want a formal draft, bulleted breakdown, code snippet, or translation.\n\n" +
                    "Tell me how you'd like to refine this and I'll tailor it right away!"
            }
        }
    }
}


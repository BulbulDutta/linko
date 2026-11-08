package com.example.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.foundation.layout.PaddingValues
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import android.widget.Toast
import com.example.util.SecretMessageCoder
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ReceivedBubbleDark
import com.example.ui.theme.ReceivedBubbleLight
import com.example.ui.theme.SentBubbleDark
import com.example.ui.theme.SentBubbleLight
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Returns true if [text] contains ONLY emojis (single or multiple emojis, skin-tone modifiers,
 * ZWJ sequences, flags, or keycaps) and no regular text, letters, numbers, or punctuation.
 */
fun isEmojiOnly(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false

    var hasEmoji = false
    var i = 0
    val len = trimmed.length

    while (i < len) {
        val cp = Character.codePointAt(trimmed, i)
        val charCount = Character.charCount(cp)

        if (Character.isWhitespace(cp)) {
            i += charCount
            continue
        }

        // Check for ASCII keycap base characters ('0'..'9', '#', '*') followed by optional VS16 (0xFE0F) and Keycap (0x20E3)
        if (cp in 0x30..0x39 || cp == 0x23 || cp == 0x2A) {
            val nextIdx = i + charCount
            if (nextIdx < len) {
                val nextCp = Character.codePointAt(trimmed, nextIdx)
                if (nextCp == 0x20E3) {
                    hasEmoji = true
                    i = nextIdx + Character.charCount(nextCp)
                    continue
                } else if (nextCp == 0xFE0F) {
                    val afterVsIdx = nextIdx + Character.charCount(nextCp)
                    if (afterVsIdx < len && Character.codePointAt(trimmed, afterVsIdx) == 0x20E3) {
                        hasEmoji = true
                        i = afterVsIdx + Character.charCount(0x20E3)
                        continue
                    }
                }
            }
            return false
        }

        // Emoji modifiers, variation selectors, ZWJ, and tag characters
        val isEmojiModifierOrJoiner =
            cp == 0x200D || // Zero-Width Joiner
            cp == 0x20E3 || // Combining Enclosing Keycap
            cp in 0xFE00..0xFE0F || // Variation Selectors
            cp in 0x1F3FB..0x1F3FF || // Fitzpatrick Skin Tone Modifiers
            cp in 0xE0020..0xE007F // Emoji Tag Sequence characters

        if (isEmojiModifierOrJoiner) {
            i += charCount
            continue
        }

        val isEmojiCodePoint =
            cp in 0x1F000..0x1FAFF || // Supplementary Plane Emojis (Emoticons, Pictographs, Transport, Supplemental)
            cp in 0x1F1E6..0x1F1FF || // Regional Indicator Symbols (Flags)
            cp in 0x2600..0x27BF || // Misc Symbols & Dingbats (☀️, ❤️, ✨, ✅, ✈️, etc.)
            cp in 0x2300..0x23FF || // Misc Technical (⌚, ⌛, ⏰, ⏱️, etc.)
            cp in 0x2B00..0x2BFF || // Stars & Arrows (⭐, ⭕, ⬆️, ⬇️)
            cp in 0x25A0..0x25FF || // Geometric Shapes (▶️, ◀️, ◼️)
            cp in 0x2190..0x21FF || // Arrows
            cp == 0x00A9 || cp == 0x00AE || // ©, ®
            cp == 0x203C || cp == 0x2049 || // ‼, ⁉
            cp == 0x2122 || cp == 0x2139 || // ™, ℹ
            cp == 0x2934 || cp == 0x2935 || // ⤴, ⤵
            cp == 0x3030 || cp == 0x303D || // 〰, 〽
            cp == 0x3297 || cp == 0x3299 || // ㊗, ㊙
            (cp >= 0x2000 && Character.getType(cp) == Character.OTHER_SYMBOL.toInt())

        if (isEmojiCodePoint) {
            hasEmoji = true
            i += charCount
        } else {
            return false
        }
    }

    return hasEmoji
}

/**
 * Detects whether a chat message represents a Google Meet Voice or Video Call.
 */
fun isGoogleMeetCallMessage(
    text: String,
    mediaUri: String?,
    mediaType: String?,
    callType: String?
): Boolean {
    return !callType.isNullOrBlank() ||
        mediaType.equals("CALL", ignoreCase = true) ||
        text.contains("meet.google.com", ignoreCase = true) ||
        mediaUri?.contains("meet.google.com", ignoreCase = true) == true
}

/**
 * Extracts the hidden Google Meet URL from the message text or mediaUri.
 */
fun extractGoogleMeetUrl(text: String, mediaUri: String?): String {
    val urlRegex = Regex("""https?://meet\.google\.com/[a-zA-Z0-9-]+""", RegexOption.IGNORE_CASE)
    val bareMeetRegex = Regex("""meet\.google\.com/[a-zA-Z0-9-]+""", RegexOption.IGNORE_CASE)

    urlRegex.find(mediaUri ?: "")?.value?.let { return it }
    urlRegex.find(text)?.value?.let { return it }
    bareMeetRegex.find(mediaUri ?: "")?.value?.let { return "https://$it" }
    bareMeetRegex.find(text)?.value?.let { return "https://$it" }

    val candidate = mediaUri?.takeIf { it.startsWith("http", ignoreCase = true) }
        ?: text.trim().takeIf { it.startsWith("http", ignoreCase = true) }
    return candidate ?: "https://meet.google.com"
}

/**
 * Resolves whether a Google Meet call bubble should render as "AUDIO" (Voice Call) or "VIDEO" (Video Call).
 */
fun resolveMeetCallType(text: String, callType: String?): String {
    if (callType.equals("AUDIO", ignoreCase = true) || callType.equals("VOICE", ignoreCase = true)) {
        return "AUDIO"
    }
    if (callType.equals("VIDEO", ignoreCase = true)) {
        return "VIDEO"
    }
    if (text.contains("Audio Call", ignoreCase = true) || text.contains("Voice Call", ignoreCase = true)) {
        return "AUDIO"
    }
    return "VIDEO"
}

/**
 * Reusable ChatBubble component with modern, minimalist, compact styling.
 * Supports text content, emoji-only frameless messages, WhatsApp-style Google Meet call bubbles,
 * rich typography, media attachment indicators (Images, Audio Voice Notes, Documents),
 * Polls with live vote casting, Locations, Contacts, Events, E2EE encryption status,
 * and email transport delivery badges.
 */
@Composable
fun ChatBubble(
    text: String,
    senderName: String? = null,
    isFromMe: Boolean,
    timestampText: String,
    status: String = "sent", // "sent", "delivered", "read"
    deliveredAt: Long? = null,
    readAt: Long? = null,
    emailTransportStatus: String? = null, // "PENDING", "DISPATCHED_SMTP", "DELIVERED_INBOX"
    mediaUri: String? = null,
    mediaType: String? = null, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "POLL", "LOCATION", "CONTACT", "EVENT", "CALL"
    callType: String? = null, // "AUDIO" or "VIDEO"
    formattedRichText: String? = null, // "BOLD", "ITALIC", "CODE"
    isEncrypted: Boolean = true,
    currentUserId: String? = null,
    reactionsJson: String = "{}",
    onToggleReaction: ((String) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onVotePoll: ((optionIndex: Int) -> Unit)? = null,
    onMediaClick: (() -> Unit)? = null,
    onEmailBadgeClick: (() -> Unit)? = null,
    onSwipeToReply: (() -> Unit)? = null,
    onStatusTicksClick: (() -> Unit)? = null,
    onFecharGoogleMeet: (() -> Unit)? = null,
    senderAvatarUri: String? = null,
    showSenderAvatar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface == ReceivedBubbleDark
    val isGeminiSender = senderName?.contains("Gemini", ignoreCase = true) == true
    val bubbleColor = if (isFromMe) {
        if (isDark) SentBubbleDark else SentBubbleLight
    } else if (isGeminiSender) {
        if (isDark) Color(0xFF1E1B4B) else Color(0xFFEDE9FE)
    } else {
        if (isDark) ReceivedBubbleDark else ReceivedBubbleLight
    }

    val bubbleBorderColor = if (isFromMe) {
        Emerald500.copy(alpha = if (isDark) 0.32f else 0.25f)
    } else if (isGeminiSender) {
        Color(0xFF7C4DFF).copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.10f else 0.07f)
    }

    // Smooth entry animation for each chat bubble frame
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEntered = true
    }
    val entryAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
        label = "BubbleEntryAlpha"
    )
    val entryScale by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0.96f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "BubbleEntryScale"
    )

    val isMeetCallBubble = remember(text, mediaUri, mediaType, callType) {
        isGoogleMeetCallMessage(
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            callType = callType
        )
    }

    val isEmojiOnlyMessage = remember(text, mediaType, formattedRichText, isMeetCallBubble) {
        !isMeetCallBubble &&
            mediaType.isNullOrBlank() &&
            formattedRichText?.startsWith("REPLY:") != true &&
            isEmojiOnly(text)
    }

    var showFullscreenImage by remember { mutableStateOf(false) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeOffset = 100f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = entryScale
                scaleY = entryScale
            }
            .padding(vertical = 1.5.dp, horizontal = 2.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            // WhatsApp-style responsive max width: dynamically fills up to 83% of screen width, capped on wide tablets
            val bubbleMaxWidth = (maxWidth * 0.83f).coerceAtMost(560.dp)

            if (swipeOffsetX > 5f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(32.dp)
                        .graphicsLayer {
                            alpha = (swipeOffsetX / 50f).coerceIn(0f, 1f)
                            scaleX = (swipeOffsetX / 50f).coerceIn(0.5f, 1f)
                            scaleY = (swipeOffsetX / 50f).coerceIn(0.5f, 1f)
                        }
                        .background(Emerald500, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Swipe to Reply",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isEmojiOnlyMessage) {
                // Emoji-only message: no bubble background container, no inner padding, larger 36sp emoji text
                Column(
                    horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
                    modifier = Modifier
                        .widthIn(max = bubbleMaxWidth)
                        .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, maxSwipeOffset)
                                },
                                onDragEnd = {
                                    if (swipeOffsetX >= 45f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSwipeToReply?.invoke()
                                    }
                                    swipeOffsetX = 0f
                                },
                                onDragCancel = {
                                    swipeOffsetX = 0f
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLongPress?.invoke()
                                }
                            )
                        }
                        .testTag(if (isFromMe) "message_bubble_sent" else "message_bubble_received")
                ) {
                    if (!isFromMe && !senderName.isNullOrBlank()) {
                        Text(
                            text = senderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.15.sp,
                            color = if (senderName.contains("Gemini")) Color(0xFF7C4DFF) else Emerald500
                        )
                    }

                    Text(
                        text = text.trim(),
                        fontSize = 38.sp,
                        lineHeight = 44.sp,
                        modifier = Modifier
                            .padding(0.dp)
                            .testTag("emoji_only_text")
                    )

                    BubbleMetadataFooter(
                        isEncrypted = isEncrypted,
                        emailTransportStatus = emailTransportStatus,
                        timestampText = timestampText,
                        isFromMe = isFromMe,
                        status = status,
                        onEmailBadgeClick = onEmailBadgeClick,
                        onStatusTicksClick = onStatusTicksClick,
                        modifier = Modifier.align(if (isFromMe) Alignment.End else Alignment.Start)
                    )

                    if (onToggleReaction != null && reactionsJson.isNotBlank() && reactionsJson != "{}") {
                        MessageReactionBadges(
                            reactionsJson = reactionsJson,
                            currentUserId = currentUserId,
                            onToggleReaction = onToggleReaction
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                    modifier = Modifier
                        .widthIn(min = 40.dp, max = bubbleMaxWidth)
                        .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                ) {
                    if (!isFromMe && showSenderAvatar) {
                        UserAvatar(
                            name = senderName ?: "User",
                            avatarUri = senderAvatarUri,
                            size = 28.dp,
                            showOnlineStatus = false,
                            modifier = Modifier
                                .padding(end = 6.dp, bottom = 2.dp)
                                .testTag("sender_avatar")
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromMe) 16.dp else 4.dp,
                            bottomEnd = if (isFromMe) 4.dp else 16.dp
                        ),
                        color = bubbleColor,
                        tonalElevation = 1.dp,
                        shadowElevation = 1.5.dp,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, bubbleBorderColor),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, maxSwipeOffset)
                                    },
                                    onDragEnd = {
                                        if (swipeOffsetX >= 45f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSwipeToReply?.invoke()
                                        }
                                        swipeOffsetX = 0f
                                    },
                                    onDragCancel = {
                                        swipeOffsetX = 0f
                                    }
                                )
                            }
                            .pointerInput(isMeetCallBubble, text, mediaUri) {
                                detectTapGestures(
                                    onTap = if (isMeetCallBubble) {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val hiddenMeetUrl = extractGoogleMeetUrl(text = text, mediaUri = mediaUri)
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(hiddenMeetUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Unable to open Google Meet link", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else null,
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPress?.invoke()
                                    }
                                )
                            }
                            .testTag(if (isFromMe) "message_bubble_sent" else "message_bubble_received")
                    ) {
                    Column(modifier = Modifier.padding(horizontal = 8.5.dp, vertical = 5.dp)) {
                        // Quoted Message Reply Header
                        if (formattedRichText?.startsWith("REPLY:") == true) {
                            val replyData = formattedRichText.removePrefix("REPLY:")
                            val parts = replyData.split("|", limit = 2)
                            val replySenderName = parts.getOrNull(0) ?: "Message"
                            val replyTextSnippet = parts.getOrNull(1) ?: ""

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .background(Emerald500, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = replySenderName,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500
                                        )
                                        Text(
                                            text = replyTextSnippet,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            letterSpacing = 0.2.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Sender Label for incoming messages
                        if (!isFromMe && !senderName.isNullOrBlank()) {
                            Text(
                                text = senderName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.15.sp,
                                color = if (senderName.contains("Gemini")) Color(0xFF7C4DFF) else Emerald500
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Attachment Cards according to mediaType
                        when (mediaType?.uppercase()) {
                            "POLL" -> {
                                PollBubbleContent(
                                    pollJsonStr = text,
                                    currentUserId = currentUserId,
                                    onVotePoll = onVotePoll
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            "IMAGE" -> {
                                if (!mediaUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = mediaUri,
                                        contentDescription = "Media Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                // Open in full-screen viewer ONLY — do NOT auto-download on tap
                                                showFullscreenImage = true
                                            }
                                            .testTag("chat_image_attachment")
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Image Indicator",
                                            tint = Emerald500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Photo Attachment",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            "VIDEO" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .clickable { onMediaClick?.invoke() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Play Video",
                                            tint = Emerald500,
                                            modifier = Modifier.size(38.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Video Attachment • 0:45",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            "AUDIO" -> {
                                VoiceNoteBubbleContent(
                                    text = text,
                                    mediaUri = mediaUri,
                                    isFromMe = isFromMe,
                                    onDownloadClick = onMediaClick
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            "DOCUMENT" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF7F66FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Document Attachment",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (text.isNotBlank() && !text.startsWith("{")) text else "Attachment_Doc.pdf",
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "File Document • Tap to Save",
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMediaClick?.invoke() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Document",
                                            tint = Emerald500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            "LOCATION" -> {
                                LocationBubbleContent(locationJsonStr = text)
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            "CONTACT" -> {
                                ContactBubbleContent(contactJsonStr = text)
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            "EVENT" -> {
                                EventBubbleContent(eventJsonStr = text)
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }

                        if (isMeetCallBubble) {
                            val resolvedCallType = remember(text, callType) {
                                resolveMeetCallType(text = text, callType = callType)
                            }
                            val hiddenMeetUrl = remember(text, mediaUri) {
                                extractGoogleMeetUrl(text = text, mediaUri = mediaUri)
                            }
                            WhatsAppCallBubbleContent(
                                callType = resolvedCallType,
                                isDark = isDark,
                                onJoinCallClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(hiddenMeetUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to open Google Meet link", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Standard Text Content if NOT a special media type or Google Meet Call bubble
                        val isDefaultImageMetadata = mediaType?.uppercase() == "IMAGE" && (
                            text.isBlank() ||
                            text.equals("Photo Attachment", ignoreCase = true) ||
                            text.equals("Photo", ignoreCase = true) ||
                            text.equals("Image", ignoreCase = true) ||
                            text.startsWith("http://", ignoreCase = true) ||
                            text.startsWith("https://", ignoreCase = true) ||
                            text.startsWith("img_", ignoreCase = true) ||
                            text.endsWith(".jpg", ignoreCase = true) ||
                            text.endsWith(".jpeg", ignoreCase = true) ||
                            text.endsWith(".png", ignoreCase = true)
                        )

                        val isDefaultAudioMetadata = mediaType?.uppercase() == "AUDIO" && (
                            text.isBlank() ||
                            text.startsWith("🎙️ Voice Note", ignoreCase = true) ||
                            text.startsWith("Voice Note", ignoreCase = true) ||
                            text.equals("Audio Recording Note", ignoreCase = true)
                        )

                        val showTextContent = text.isNotBlank() &&
                            !isMeetCallBubble &&
                            mediaType?.uppercase() !in listOf("POLL", "LOCATION", "CONTACT", "EVENT", "CALL") &&
                            !isDefaultImageMetadata &&
                            !isDefaultAudioMetadata

                        val isCompactInlineText = showTextContent &&
                            mediaType.isNullOrBlank() &&
                            !text.contains("\n") &&
                            text.length <= 40 &&
                            !SecretMessageCoder.URL_REGEX.containsMatchIn(text)

                        if (showTextContent) {
                            val fontStyle = if (formattedRichText == "ITALIC") FontStyle.Italic else FontStyle.Normal
                            val fontWeight = if (formattedRichText == "BOLD") FontWeight.Bold else FontWeight.Normal
                            val fontFamily = if (formattedRichText == "CODE") FontFamily.Monospace else FontFamily.Default

                            if (isCompactInlineText) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ClickableUrlMessageText(
                                        text = text,
                                        fontSize = 15.5.sp,
                                        lineHeight = 21.sp,
                                        letterSpacing = 0.2.sp,
                                        fontStyle = fontStyle,
                                        fontWeight = fontWeight,
                                        fontFamily = fontFamily,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .testTag("chat_bubble_message_text")
                                    )
                                    BubbleMetadataFooter(
                                        isEncrypted = isEncrypted,
                                        emailTransportStatus = emailTransportStatus,
                                        timestampText = timestampText,
                                        isFromMe = isFromMe,
                                        status = status,
                                        onEmailBadgeClick = onEmailBadgeClick,
                                        onStatusTicksClick = onStatusTicksClick
                                    )
                                }
                            } else {
                                ClickableUrlMessageText(
                                    text = text,
                                    fontSize = 15.5.sp,
                                    lineHeight = 21.sp,
                                    letterSpacing = 0.2.sp,
                                    fontStyle = fontStyle,
                                    fontWeight = fontWeight,
                                    fontFamily = fontFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("chat_bubble_message_text")
                                )
                            }
                        }

                        if (!isCompactInlineText) {
                            Spacer(modifier = Modifier.height(1.dp))
                            BubbleMetadataFooter(
                                isEncrypted = isEncrypted,
                                emailTransportStatus = emailTransportStatus,
                                timestampText = timestampText,
                                isFromMe = isFromMe,
                                status = status,
                                onEmailBadgeClick = onEmailBadgeClick,
                                onStatusTicksClick = onStatusTicksClick,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        if (onToggleReaction != null && reactionsJson.isNotBlank() && reactionsJson != "{}") {
                            MessageReactionBadges(
                                reactionsJson = reactionsJson,
                                currentUserId = currentUserId,
                                onToggleReaction = onToggleReaction
                            )
                        }
                    }
                }
            }
        }

            if (showFullscreenImage && !mediaUri.isNullOrEmpty()) {
                val cleanCaption = text.takeIf {
                    it.isNotBlank() &&
                        !it.equals("Photo Attachment", ignoreCase = true) &&
                        !it.equals("Photo", ignoreCase = true) &&
                        !it.equals("Image", ignoreCase = true) &&
                        !it.startsWith("http://", ignoreCase = true) &&
                        !it.startsWith("https://", ignoreCase = true) &&
                        !it.startsWith("img_", ignoreCase = true)
                }
                FullScreenImageViewerDialog(
                    imageUri = mediaUri,
                    senderName = senderName,
                    timestampText = timestampText,
                    caption = cleanCaption,
                    onDismiss = { showFullscreenImage = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageViewerDialog(
    imageUri: String,
    senderName: String? = null,
    timestampText: String? = null,
    caption: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var isDownloading by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_image_viewer_dialog"),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = senderName?.takeIf { it.isNotBlank() } ?: "Photo",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!timestampText.isNullOrBlank()) {
                                Text(
                                    text = timestampText,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("fullscreen_image_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Full Screen Image Viewer",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (!isDownloading) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isDownloading = true
                                    scope.launch {
                                        val success = ImageStorageHelper.downloadImageToGallery(
                                            context = context,
                                            imageUri = imageUri
                                        )
                                        isDownloading = false
                                        if (success) {
                                            isDownloaded = true
                                            Toast.makeText(
                                                context,
                                                "Image saved to Gallery",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to save image",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("fullscreen_image_download_button")
                        ) {
                            when {
                                isDownloading -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                isDownloaded -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Image Downloaded",
                                        tint = Emerald500
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Image",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.85f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full Screen Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .testTag("fullscreen_image_content")
                )

                if (!caption.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = caption,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleMetadataFooter(
    isEncrypted: Boolean,
    emailTransportStatus: String?,
    timestampText: String,
    isFromMe: Boolean,
    status: String,
    onEmailBadgeClick: (() -> Unit)?,
    onStatusTicksClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (isEncrypted) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted Message",
                tint = Emerald500.copy(alpha = 0.85f),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(2.5.dp))
        }

        if (emailTransportStatus != null) {
            Box(modifier = Modifier.clickable(enabled = onEmailBadgeClick != null) { onEmailBadgeClick?.invoke() }) {
                EmailTransportBadge(emailStatus = emailTransportStatus)
            }
            Spacer(modifier = Modifier.width(3.dp))
        }

        Text(
            text = timestampText,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            modifier = Modifier.testTag("message_timestamp")
        )

        if (isFromMe) {
            Spacer(modifier = Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(enabled = onStatusTicksClick != null) {
                        onStatusTicksClick?.invoke()
                    }
                    .testTag("message_status_ticks")
            ) {
                MessageStatusTicks(status = status, iconSize = 15.dp)
            }
        }
    }
}

data class PollOptionData(
    val index: Int,
    val text: String,
    val votes: List<String>
)

@Composable
fun PollBubbleContent(
    pollJsonStr: String,
    currentUserId: String?,
    onVotePoll: ((optionIndex: Int) -> Unit)?
) {
    var question = "Poll"
    var allowMultiple = false
    val optionsList = mutableListOf<PollOptionData>()
    var totalVotes = 0

    try {
        val json = JSONObject(pollJsonStr)
        question = json.optString("question", "Poll")
        allowMultiple = json.optBoolean("allowMultiple", false)
        val arr = json.optJSONArray("options")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val optObj = arr.getJSONObject(i)
                val optText = optObj.optString("text", "Option")
                val votesArr = optObj.optJSONArray("votes")
                val votes = mutableListOf<String>()
                if (votesArr != null) {
                    for (j in 0 until votesArr.length()) {
                        votes.add(votesArr.getString(j))
                    }
                }
                totalVotes += votes.size
                optionsList.add(PollOptionData(i, optText, votes))
            }
        }
    } catch (e: Exception) {
        question = pollJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = "Poll",
                tint = Emerald500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = question,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = if (allowMultiple) "Select one or more options" else "Select one option",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp, top = 1.dp)
        )

        optionsList.forEachIndexed { index, option ->
            val hasVoted = currentUserId != null && option.votes.contains(currentUserId)
            val percentage = if (totalVotes > 0) option.votes.size.toFloat() / totalVotes else 0f
            val animatedProgress by animateFloatAsState(targetValue = percentage, label = "poll_progress")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onVotePoll?.invoke(index) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (hasVoted) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Vote option",
                        tint = if (hasVoted) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = option.text,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${option.votes.size} (${(percentage * 100).toInt()}%)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = Emerald500,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Total votes: $totalVotes",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LocationBubbleContent(locationJsonStr: String) {
    val context = LocalContext.current
    val sharedLoc = remember(locationJsonStr) {
        com.example.data.remote.SharedMapLocation.fromJsonString(locationJsonStr)
    }

    val pinColor = if (sharedLoc.isLiveLocation) Color(0xFFE53935) else Color(0xFFEA4335)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = if (sharedLoc.isLiveLocation) Color(0xFFE53935).copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        // Clickable Google Maps Mini-Map Preview Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .clickable {
                    com.example.data.remote.GoogleMapsLocationService.openInGoogleMaps(
                        context = context,
                        lat = sharedLoc.latitude,
                        lng = sharedLoc.longitude,
                        title = sharedLoc.title
                    )
                }
                .testTag("location_bubble_map_preview")
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Map base
                drawRect(color = Color(0xFFE8EEF5))

                // Park zones
                drawRect(
                    color = Color(0xFFC8E6C9).copy(alpha = 0.75f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.04f, h * 0.08f),
                    size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.45f)
                )
                drawRect(
                    color = Color(0xFFAECBFA).copy(alpha = 0.65f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.58f),
                    size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.42f)
                )

                // Streets
                for (i in 1..3) {
                    val y = h * (i / 4f)
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 4.5f
                    )
                }
                for (i in 1..4) {
                    val x = w * (i / 5f)
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, h),
                        strokeWidth = 4.5f
                    )
                }

                // Highway
                drawLine(
                    color = Color(0xFFFDE293),
                    start = androidx.compose.ui.geometry.Offset(0f, h * 0.3f),
                    end = androidx.compose.ui.geometry.Offset(w, h * 0.68f),
                    strokeWidth = 9f
                )

                // Center accuracy halo & dot
                val center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.54f)
                drawCircle(
                    color = (if (sharedLoc.isLiveLocation) Color(0xFFE53935) else Color(0xFF1A73E8)).copy(alpha = 0.22f),
                    radius = 22f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f,
                    center = center
                )
                drawCircle(
                    color = if (sharedLoc.isLiveLocation) Color(0xFFE53935) else Color(0xFF1A73E8),
                    radius = 6f,
                    center = center
                )
            }

            // Google Maps / Live Status Badge
            Surface(
                color = if (sharedLoc.isLiveLocation) Color(0xFFE53935) else Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (sharedLoc.isLiveLocation) Color.White else Color(0xFF1A73E8),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (sharedLoc.isLiveLocation) "LIVE ON GOOGLE MAPS" else "Google Maps",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sharedLoc.isLiveLocation) Color.White else Color(0xFF1F2937)
                    )
                }
            }

            // Center Pin Marker
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Google Maps Pin",
                tint = pinColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 14.dp)
                    .size(26.dp)
            )
        }

        // Location Details & Actions
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sharedLoc.title,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (sharedLoc.address.isNotBlank()) {
                        Text(
                            text = sharedLoc.address,
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = String.format(
                    java.util.Locale.US,
                    "GPS: %.5f, %.5f",
                    sharedLoc.latitude,
                    sharedLoc.longitude
                ),
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Emerald500
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons: Open in Google Maps | Directions | Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        com.example.data.remote.GoogleMapsLocationService.openInGoogleMaps(
                            context = context,
                            lat = sharedLoc.latitude,
                            lng = sharedLoc.longitude,
                            title = sharedLoc.title
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("open_google_maps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open in Google Maps",
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Open in Maps",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        com.example.data.remote.GoogleMapsLocationService.openDirectionsInGoogleMaps(
                            context = context,
                            lat = sharedLoc.latitude,
                            lng = sharedLoc.longitude,
                            title = sharedLoc.title
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("directions_google_maps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Directions",
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Directions",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            com.example.data.remote.GoogleMapsLocationService.shareLocationExternally(
                                context = context,
                                location = sharedLoc
                            )
                        }
                        .testTag("share_google_maps_link_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Share Google Maps Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactBubbleContent(contactJsonStr: String) {
    var name = "Shared Contact"
    var email = "contact@linko.com"
    var phone = "+1 (555) 012-3456"

    try {
        val json = JSONObject(contactJsonStr)
        name = json.optString("name", "Shared Contact")
        email = json.optString("email", "contact@linko.com")
        phone = json.optString("phone", "+1 (555) 012-3456")
    } catch (e: Exception) {
        name = contactJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = name, size = 32.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = email, fontSize = 10.5.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = phone, fontSize = 10.5.sp, lineHeight = 12.sp, color = Emerald500)
            }
        }
    }
}

@Composable
fun EventBubbleContent(eventJsonStr: String) {
    var title = "Scheduled Event"
    var dateText = "Tomorrow, 3:00 PM"
    var location = "Online Room"

    try {
        val json = JSONObject(eventJsonStr)
        title = json.optString("title", "Scheduled Event")
        dateText = json.optString("dateText", "Tomorrow, 3:00 PM")
        location = json.optString("location", "Online Room")
    } catch (e: Exception) {
        title = eventJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3F51B5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Event",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "📅 $dateText", fontSize = 10.5.sp, lineHeight = 12.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                Text(text = "📍 $location", fontSize = 10.5.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun VoiceNoteBubbleContent(
    text: String,
    mediaUri: String?,
    isFromMe: Boolean,
    onDownloadClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioTargetUri = mediaUri ?: ""

    var isPlaying by remember(audioTargetUri) {
        mutableStateOf(com.example.util.AudioPlayerManager.isPlaying(audioTargetUri))
    }
    var progress by remember(audioTargetUri) { mutableFloatStateOf(0f) }
    var currentPosMs by remember(audioTargetUri) { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Parse fallback duration from label e.g. "🎙️ Voice Note (0:05)"
    val parsedDurationMs = remember(text, audioTargetUri) {
        val fileDuration = com.example.util.AudioPlayerManager.getDurationMs(context, audioTargetUri)
        if (fileDuration > 0) {
            fileDuration
        } else {
            val match = Regex("""(\d+):(\d{2})""").find(text)
            if (match != null) {
                val mins = match.groupValues[1].toIntOrNull() ?: 0
                val secs = match.groupValues[2].toIntOrNull() ?: 4
                ((mins * 60 + secs) * 1000).coerceAtLeast(1000)
            } else {
                4000
            }
        }
    }

    // Generate deterministic, organic-looking waveform bars per voice note
    val waveformBars = remember(audioTargetUri, text) {
        val seed = (audioTargetUri + text).hashCode()
        val rng = java.util.Random(seed.toLong())
        List(28) { idx ->
            val centerWeight = 1f - kotlin.math.abs((idx - 13.5f) / 16f) * 0.45f
            (0.22f + rng.nextFloat() * 0.78f * centerWeight).coerceIn(0.18f, 1.0f)
        }
    }

    // Proactively cache remote Firebase Storage voice notes on device for zero-latency playback
    LaunchedEffect(audioTargetUri) {
        if (audioTargetUri.startsWith("http://", ignoreCase = true) ||
            audioTargetUri.startsWith("https://", ignoreCase = true) ||
            audioTargetUri.startsWith("gs://", ignoreCase = true)
        ) {
            com.example.data.remote.FirebaseVoiceNoteStorageService.ensureCachedAudioFile(
                context = context,
                remoteUrl = audioTargetUri
            )
        }
    }

    LaunchedEffect(isPlaying, audioTargetUri, playbackSpeed) {
        if (isPlaying) {
            val startTick = System.currentTimeMillis()
            val initialOffsetMs = currentPosMs
            while (isPlaying) {
                if (audioTargetUri.isNotBlank() && com.example.util.AudioPlayerManager.isPlaying(audioTargetUri)) {
                    val livePos = com.example.util.AudioPlayerManager.getCurrentPositionMs(audioTargetUri)
                    val liveProg = com.example.util.AudioPlayerManager.getProgress(audioTargetUri)
                    currentPosMs = livePos
                    progress = liveProg
                } else {
                    // Smooth fallback progression if URI is synthetic or completed
                    val elapsed = ((System.currentTimeMillis() - startTick) * playbackSpeed).toInt()
                    val simulatedPos = (initialOffsetMs + elapsed).coerceAtMost(parsedDurationMs)
                    currentPosMs = simulatedPos
                    progress = (simulatedPos.toFloat() / parsedDurationMs.toFloat()).coerceIn(0f, 1f)
                    if (simulatedPos >= parsedDurationMs) {
                        isPlaying = false
                        progress = 0f
                        currentPosMs = 0
                        break
                    }
                }
                delay(60)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("voice_note_bubble_container")
    ) {
        // Play / Pause Circular Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isPlaying) {
                        com.example.util.AudioPlayerManager.pauseAudio()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        if (audioTargetUri.isNotBlank()) {
                            com.example.util.AudioPlayerManager.playAudio(
                                context = context,
                                audioUri = audioTargetUri,
                                speed = playbackSpeed
                            ) {
                                isPlaying = false
                                progress = 0f
                                currentPosMs = 0
                            }
                        }
                    }
                }
                .testTag("voice_note_play_button"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Emerald500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Voice Note" else "Play Voice Note",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Waveform Scrubber + Duration Footer
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .pointerInput(audioTargetUri, parsedDurationMs) {
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                progress = fraction
                                currentPosMs = (parsedDurationMs * fraction).toInt()
                                if (audioTargetUri.isNotBlank()) {
                                    com.example.util.AudioPlayerManager.seekToFraction(audioTargetUri, fraction)
                                }
                            }
                        }
                    }
                    .testTag("voice_note_waveform")
            ) {
                waveformBars.forEachIndexed { idx, amplitude ->
                    val barFraction = idx.toFloat() / waveformBars.size.toFloat()
                    val isPlayed = barFraction <= progress
                    val barHeight = (4.dp + (14.dp * amplitude))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPlayed) Emerald500
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayedMs = if (isPlaying || currentPosMs > 0) currentPosMs else parsedDurationMs
                Text(
                    text = "${formatAudioMs(displayedMs)} • Voice Note",
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPlaying) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Playback Speed Pill (1x / 1.5x / 2x)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Emerald500.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            com.example.util.AudioPlayerManager.setPlaybackSpeed(playbackSpeed)
                        }
                        .testTag("voice_note_speed_button")
                ) {
                    val speedText = if (playbackSpeed == 1.0f) "1x" else if (playbackSpeed == 1.5f) "1.5x" else "2x"
                    Text(
                        text = speedText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }

        if (!mediaUri.isNullOrEmpty() && onDownloadClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { onDownloadClick.invoke() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save Voice Note",
                    tint = Emerald500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatAudioMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d", mins, secs)
}

/**
 * WhatsApp-style Call Bubble content for Voice Calls ("AUDIO") and Video Calls ("VIDEO").
 * Hides the raw Google Meet URL and opens Google Meet directly when tapped.
 */
@Composable
fun WhatsAppCallBubbleContent(
    callType: String,
    isDark: Boolean,
    onJoinCallClick: () -> Unit
) {
    val isVoiceCall = callType.equals("AUDIO", ignoreCase = true)
    val callTitle = if (isVoiceCall) "Voice Call" else "Video Call"
    val callIcon = if (isVoiceCall) Icons.Default.Call else Icons.Default.Videocam
    val cardBgColor = if (isDark) {
        Color.Black.copy(alpha = 0.24f)
    } else {
        Emerald500.copy(alpha = 0.10f)
    }

    Surface(
        color = cardBgColor,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(0.75.dp, Emerald500.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onJoinCallClick() }
            .testTag(if (isVoiceCall) "voice_call_bubble" else "video_call_bubble")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Emerald500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = callIcon,
                    contentDescription = callTitle,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    letterSpacing = 0.2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to join on Google Meet",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = Emerald500,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Instagram / iMessage-style animated typing indicator bubble with 3 staggered blinking & bouncing dots.
 * Appears immediately when the user sends a message to Gemini and disappears the exact moment
 * the first streamed response token arrives.
 */
@Composable
fun InstagramTypingBubble(
    isGemini: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface == ReceivedBubbleDark
    val bubbleColor = if (isGemini) {
        if (isDark) Color(0xFF1E1B4B) else Color(0xFFEDE9FE)
    } else {
        if (isDark) ReceivedBubbleDark else ReceivedBubbleLight
    }
    val borderColor = if (isGemini) {
        Color(0xFF7C4DFF).copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.12f else 0.08f)
    }
    val dotBaseColor = if (isGemini) {
        if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "InstagramTypingTransition"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .testTag("instagram_typing_bubble"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isGemini) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                    contentDescription = "Gemini AI",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomEnd = 18.dp,
                bottomStart = 5.dp
            ),
            shadowElevation = 2.dp,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val staggerDelayMs = index * 160

                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.28f,
                        targetValue = 1.0f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(
                                durationMillis = 460,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                            initialStartOffset = androidx.compose.animation.core.StartOffset(
                                offsetMillis = staggerDelayMs,
                                offsetType = androidx.compose.animation.core.StartOffsetType.FastForward
                            )
                        ),
                        label = "DotAlpha_$index"
                    )

                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.78f,
                        targetValue = 1.18f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(
                                durationMillis = 460,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                            initialStartOffset = androidx.compose.animation.core.StartOffset(
                                offsetMillis = staggerDelayMs,
                                offsetType = androidx.compose.animation.core.StartOffsetType.FastForward
                            )
                        ),
                        label = "DotScale_$index"
                    )

                    val dotOffsetY by infiniteTransition.animateFloat(
                        initialValue = 2f,
                        targetValue = -3.5f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(
                                durationMillis = 460,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                            initialStartOffset = androidx.compose.animation.core.StartOffset(
                                offsetMillis = staggerDelayMs,
                                offsetType = androidx.compose.animation.core.StartOffsetType.FastForward
                            )
                        ),
                        label = "DotOffsetY_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                alpha = dotAlpha
                                scaleX = dotScale
                                scaleY = dotScale
                                translationY = dotOffsetY
                            }
                            .clip(CircleShape)
                            .background(dotBaseColor)
                    )
                }
            }
        }
    }
}

/**
 * Clickable and highlighted URL text renderer for Linko chat bubbles.
 *
 * Automatically detects web URLs (e.g. YouTube links, https://, http://, www.) using
 * [SecretMessageCoder.URL_REGEX]. Discovered URLs are highlighted in premium link blue
 * (#2563EB) with an underline and semibold styling, while non-URL text retains standard
 * chat typography and word spacing.
 *
 * Clicking a link safely launches an [Intent.ACTION_VIEW] to the browser or associated app
 * (e.g., YouTube app for YouTube URLs), with fallback to Compose's [LocalUriHandler].
 */
@Composable
fun ClickableUrlMessageText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    lineHeight: TextUnit = 22.5.sp,
    letterSpacing: TextUnit = 0.28.sp,
    fontStyle: FontStyle = FontStyle.Normal,
    fontWeight: FontWeight = FontWeight.Normal,
    fontFamily: FontFamily = FontFamily.Default,
    color: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = "chat_bubble_message_text"
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val annotatedString = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            val matches = SecretMessageCoder.URL_REGEX.findAll(text).toList()

            if (matches.isEmpty()) {
                append(text)
            } else {
                for (match in matches) {
                    val range = match.range
                    if (range.first > lastIndex) {
                        append(text.substring(lastIndex, range.first))
                    }
                    val rawMatch = match.value
                    val (cleanUrl, trailing) = SecretMessageCoder.splitUrlAndTrailingPunctuation(rawMatch)
                    val validUrl = if (cleanUrl.startsWith("http://", ignoreCase = true) ||
                        cleanUrl.startsWith("https://", ignoreCase = true)
                    ) {
                        cleanUrl
                    } else {
                        "https://$cleanUrl"
                    }

                    val link = LinkAnnotation.Url(
                        url = validUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = Color(0xFF2563EB),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        ),
                        linkInteractionListener = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    uriHandler.openUri(validUrl)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    pushLink(link)
                    append(cleanUrl)
                    pop()

                    if (trailing.isNotEmpty()) {
                        append(trailing)
                    }
                    lastIndex = range.last + 1
                }
                if (lastIndex < text.length) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        color = color,
        modifier = modifier.testTag(testTag)
    )
}





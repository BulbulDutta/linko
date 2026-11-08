package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class TypingStatus(
    val email: String,
    val userId: String,
    val displayName: String,
    val chatId: String,
    val updatedAt: Long
)

data class UserPresenceInfo(
    val email: String,
    val userId: String = "",
    val displayName: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isTyping: Boolean = false,
    val typingChatId: String = ""
)

object FirestorePresenceService {
    private const val TAG = "FirestorePresence"
    private const val COLLECTION_PRESENCE = "user_presence"
    const val TYPING_TIMEOUT_MS = 10_000L

    private fun getFirestore(): FirebaseFirestore? {
        return FirestoreProvider.getInstance()
    }

    private var activePresenceListener: ListenerRegistration? = null

    fun updatePresence(email: String, userId: String, isOnline: Boolean) {
        if (!FirestoreProvider.isAuthenticated()) {
            Log.d(TAG, "Skipping presence update because FirebaseAuth is not authenticated")
            return
        }
        val db = getFirestore() ?: return
        val normalizedEmail = email.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            val path = "$COLLECTION_PRESENCE/$normalizedEmail"
            val now = System.currentTimeMillis()
            val data = mutableMapOf<String, Any>(
                "email" to normalizedEmail,
                "userId" to userId,
                "online" to isOnline,
                "isOnline" to isOnline,
                "lastSeen" to now
            )
            if (!isOnline) {
                data["isTyping"] = false
                data["typingChatId"] = ""
            }
            db.collection(COLLECTION_PRESENCE)
                .document(normalizedEmail)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Presence updated for $normalizedEmail: isOnline=$isOnline")
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, path)
                    Log.w(TAG, "Failed updating presence for $normalizedEmail: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updatePresence: ${e.message}")
        }
    }

    fun updateTypingStatus(
        email: String,
        userId: String,
        displayName: String,
        chatId: String?,
        isTyping: Boolean
    ) {
        if (!FirestoreProvider.isAuthenticated()) {
            return
        }
        val db = getFirestore() ?: return
        val normalizedEmail = email.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            val now = System.currentTimeMillis()
            val path = "$COLLECTION_PRESENCE/$normalizedEmail"
            val data = mapOf(
                "email" to normalizedEmail,
                "userId" to userId,
                "displayName" to displayName,
                "online" to true,
                "isOnline" to true,
                "lastSeen" to now,
                "isTyping" to (isTyping && !chatId.isNullOrBlank()),
                "typingChatId" to (if (isTyping && !chatId.isNullOrBlank()) chatId else ""),
                "typingUpdatedAt" to now
            )
            db.collection(COLLECTION_PRESENCE)
                .document(normalizedEmail)
                .set(data, SetOptions.merge())
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, path)
                    Log.w(TAG, "Failed updating typing status for $normalizedEmail: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updateTypingStatus: ${e.message}")
        }
    }

    fun startListeningToPresence(
        scope: CoroutineScope,
        onPresenceUpdate: (Set<String>) -> Unit,
        onPresenceDetailsUpdate: ((Map<String, UserPresenceInfo>) -> Unit)? = null,
        onTypingUpdate: ((List<TypingStatus>) -> Unit)? = null
    ) {
        activePresenceListener?.remove()
        activePresenceListener = null

        if (!FirestoreProvider.isAuthenticated()) {
            Log.d(TAG, "Skipping presence listener because FirebaseAuth is not authenticated")
            return
        }
        val db = getFirestore() ?: return

        try {
            activePresenceListener = db.collection(COLLECTION_PRESENCE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_PRESENCE)
                        Log.w(TAG, "Presence listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val onlineEmails = mutableSetOf<String>()
                        val presenceDetailsMap = mutableMapOf<String, UserPresenceInfo>()
                        val typingStatuses = mutableListOf<TypingStatus>()

                        // Gemini AI is always online
                        val geminiPresence = UserPresenceInfo(
                            email = "gemini_ai@google.com",
                            userId = "user_gemini",
                            displayName = "Gemini AI",
                            isOnline = true,
                            lastSeen = System.currentTimeMillis()
                        )
                        onlineEmails.add("gemini_ai@google.com")
                        onlineEmails.add("gemini.ai@google.com")
                        onlineEmails.add("user_gemini")
                        onlineEmails.add("gemini")
                        presenceDetailsMap["gemini_ai@google.com"] = geminiPresence
                        presenceDetailsMap["gemini.ai@google.com"] = geminiPresence
                        presenceDetailsMap["user_gemini"] = geminiPresence
                        presenceDetailsMap["gemini"] = geminiPresence

                        val now = System.currentTimeMillis()
                        for (doc in snapshot.documents) {
                            val email = (doc.getString("email")?.lowercase()?.trim())
                                ?: doc.id.lowercase().trim()
                            val userId = doc.getString("userId")?.trim() ?: ""
                            val displayName = doc.getString("displayName")?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: email.substringBefore("@")
                            val isOnline = doc.getBoolean("isOnline")
                                ?: doc.getBoolean("online")
                                ?: false
                            val lastSeen = doc.getLong("lastSeen") ?: 0L

                            // Online ONLY if isOnline is true and lastSeen within last 5 minutes
                            val isFreshOnline = isOnline && (now - lastSeen) < 5 * 60 * 1000L

                            val info = UserPresenceInfo(
                                email = email,
                                userId = userId,
                                displayName = displayName,
                                isOnline = isFreshOnline,
                                lastSeen = lastSeen
                            )

                            if (email.isNotBlank()) {
                                presenceDetailsMap[email] = info
                                if (isFreshOnline) onlineEmails.add(email)
                            }
                            if (userId.isNotBlank()) {
                                val lowerUid = userId.lowercase()
                                presenceDetailsMap[lowerUid] = info
                                if (isFreshOnline) onlineEmails.add(lowerUid)
                            }
                            val docIdLower = doc.id.lowercase().trim()
                            presenceDetailsMap[docIdLower] = info
                            if (isFreshOnline) onlineEmails.add(docIdLower)

                            val isTyping = doc.getBoolean("isTyping") ?: false
                            val typingChatId = doc.getString("typingChatId")?.trim() ?: ""
                            val typingUpdatedAt = doc.getLong("typingUpdatedAt") ?: lastSeen
                            if (isTyping && typingChatId.isNotBlank() && (now - typingUpdatedAt) < TYPING_TIMEOUT_MS) {
                                typingStatuses.add(
                                    TypingStatus(
                                        email = email,
                                        userId = userId,
                                        displayName = displayName,
                                        chatId = typingChatId,
                                        updatedAt = typingUpdatedAt
                                    )
                                )
                            }
                        }

                        scope.launch(Dispatchers.Main) {
                            onPresenceUpdate(onlineEmails)
                            onPresenceDetailsUpdate?.invoke(presenceDetailsMap)
                            onTypingUpdate?.invoke(typingStatuses)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startListeningToPresence: ${e.message}")
        }
    }

    fun stopListening() {
        activePresenceListener?.remove()
        activePresenceListener = null
    }
}


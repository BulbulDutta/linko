package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.repository.LinkoRepository
import com.example.util.SecretMessageCoder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service managing 3-stage WhatsApp-style message delivery and read receipts:
 * - Stage 1 ('sent'): Single Gray Tick — Message sent from sender's device to the Firestore server.
 * - Stage 2 ('delivered'): Double Gray Tick — Message received on the recipient's device.
 * - Stage 3 ('read'): Double Blue Tick — Recipient opened the chat and the message is visible on screen.
 */
object FirestoreMessageService {
    private const val TAG = "FirestoreMessageService"
    private const val COLLECTION_MESSAGES = "chat_messages"

    private fun getFirestore(): FirebaseFirestore? {
        return FirestoreProvider.getInstance()
    }

    private val activeChatListeners = mutableMapOf<String, ListenerRegistration>()
    private var globalMessageListener: ListenerRegistration? = null

    private fun resolveSenderDisplayName(rawName: String?, senderEmail: String): String {
        val cleanName = rawName?.trim() ?: ""
        if (cleanName.isNotBlank() && !LinkoRepository.isGenericPlaceholderName(cleanName)) {
            return cleanName
        }
        val cleanEmail = senderEmail.lowercase().trim()
        if (cleanEmail.isNotBlank()) {
            return cleanEmail.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        }
        return cleanName.ifBlank { "Linko Contact" }
    }

    private fun decodeFirebaseMessageText(
        rawText: String,
        chatId: String,
        isEncrypted: Boolean,
        cipherVersion: String?
    ): String {
        if (rawText.isEmpty()) return ""
        val afterCoder = if (cipherVersion == "SECRET_CODER_V1" || (isEncrypted && SecretMessageCoder.isEncodedFormat(rawText))) {
            SecretMessageCoder.decodeMessage(rawText)
        } else {
            rawText
        }
        val afterAes = if (com.example.data.crypto.EncryptionManager.isEncrypted(afterCoder)) {
            com.example.data.crypto.EncryptionManager.decrypt(afterCoder, chatId)
        } else {
            afterCoder
        }
        return if (SecretMessageCoder.isEncodedFormat(afterAes)) {
            SecretMessageCoder.decodeMessage(afterAes)
        } else {
            afterAes
        }
    }

    /**
     * Stage 1: Upload newly dispatched message to Firestore with status = 'sent'.
     * Extracts plain message text (preserving all spaces) and encodes with [SecretMessageCoder.encodeMessage]
     * before pushing to Firebase.
     */
    fun uploadMessage(
        message: MessageEntity,
        participants: List<String> = emptyList(),
        onServerSentAck: (() -> Unit)? = null
    ) {
        if (!FirestoreProvider.isAuthenticated()) {
            onServerSentAck?.invoke()
            return
        }
        val db = getFirestore()
        if (db == null) {
            onServerSentAck?.invoke()
            return
        }
        try {
            val initialStatus = MessageStatus.normalize(message.status)
            val path = "$COLLECTION_MESSAGES/${message.id}"
            val cleanSenderEmail = message.senderEmail.lowercase().trim()
            val canonicalSenderId = LinkoRepository.canonicalUserIdFromEmail(cleanSenderEmail, message.senderId)
            val enrichedParticipants = buildList {
                add(message.senderId)
                add(canonicalSenderId)
                if (cleanSenderEmail.isNotBlank()) add(cleanSenderEmail)
                participants.forEach { p ->
                    val trimmed = p.trim()
                    if (trimmed.isNotBlank()) {
                        add(trimmed)
                        add(trimmed.lowercase())
                        add(LinkoRepository.canonicalParticipantKey(trimmed))
                    }
                }
            }.filter { it.isNotBlank() && it != "unknown" }.distinct()

            // Unwrap local AES-GCM layer first so SecretMessageCoder encodes exact plain text with spaces preserved
            val plainMessageText = com.example.data.crypto.EncryptionManager.decrypt(message.text, message.chatId)
                .let { decrypted ->
                    if (SecretMessageCoder.isEncodedFormat(decrypted)) {
                        SecretMessageCoder.decodeMessage(decrypted)
                    } else {
                        decrypted
                    }
                }

            // Call the Encoder Machine before pushing to Firebase
            val encodedText = if (message.isEncrypted) {
                SecretMessageCoder.encodeMessage(plainMessageText)
            } else {
                plainMessageText
            }

            val payload = hashMapOf<String, Any?>(
                "id" to message.id,
                "chatId" to message.chatId,
                "senderId" to canonicalSenderId,
                "senderName" to resolveSenderDisplayName(message.senderName, cleanSenderEmail),
                "senderEmail" to cleanSenderEmail,
                "text" to encodedText,
                "cipherVersion" to if (message.isEncrypted) "SECRET_CODER_V1" else null,
                "mediaUri" to message.mediaUri,
                "mediaType" to message.mediaType,
                "callType" to message.callType,
                "formattedRichText" to message.formattedRichText,
                "status" to initialStatus,
                "clientTimestamp" to message.timestamp,
                "sentAt" to FieldValue.serverTimestamp(),
                "deliveredAt" to if (message.deliveredAt != null) com.google.firebase.Timestamp(java.util.Date(message.deliveredAt)) else null,
                "readAt" to if (message.readAt != null) com.google.firebase.Timestamp(java.util.Date(message.readAt)) else null,
                "emailTransportStatus" to message.emailTransportStatus,
                "emailSubject" to message.emailSubject,
                "emailMessageId" to message.emailMessageId,
                "isEncrypted" to message.isEncrypted,
                "participants" to enrichedParticipants
            ).filterValues { it != null }

            db.collection(COLLECTION_MESSAGES)
                .document(message.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Message ${message.id} acknowledged by server (status: $initialStatus)")
                    onServerSentAck?.invoke()
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, path)
                    Log.e(TAG, "Failed uploading message ${message.id} to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadMessage: ${e.message}")
        }
    }

    /**
     * Stage 2: Triggered by the recipient's device when a message is received from Firestore.
     * Transitions status from 'sent' -> 'delivered' (Double Gray Tick).
     */
    fun markMessageDelivered(
        messageId: String,
        onComplete: ((deliveredAt: Long) -> Unit)? = null
    ) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val path = "$COLLECTION_MESSAGES/$messageId"
            val docRef = db.collection(COLLECTION_MESSAGES).document(messageId)
            val updates = mapOf<String, Any>(
                "status" to MessageStatus.DELIVERED,
                "deliveredAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Message $messageId marked 'delivered' in real time")
                    onComplete?.invoke(System.currentTimeMillis())
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markMessageDelivered: ${e.message}")
        }
    }

    /**
     * Stage 3: Triggered when a single message is read on screen.
     * Transitions status to 'read' (Double Blue Tick).
     */
    fun markMessageRead(
        messageId: String,
        onComplete: ((readAt: Long) -> Unit)? = null
    ) {
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        try {
            val path = "$COLLECTION_MESSAGES/$messageId"
            val docRef = db.collection(COLLECTION_MESSAGES).document(messageId)
            val updates = mapOf<String, Any>(
                "status" to MessageStatus.READ,
                "readAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Message $messageId marked 'read' with server timestamp")
                    onComplete?.invoke(System.currentTimeMillis())
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markMessageRead: ${e.message}")
        }
    }

    /**
     * Stage 3 (Visibility-Based): Triggered by the recipient's device ONLY for messages
     * that are currently visible on the recipient's screen in DirectChatScreen.
     */
    fun markVisibleMessagesRead(
        chatId: String,
        visibleMessageIds: List<String>,
        currentUserId: String,
        currentUserEmail: String = "",
        onComplete: ((readAt: Long) -> Unit)? = null
    ) {
        if (visibleMessageIds.isEmpty() || !FirestoreProvider.isAuthenticated()) {
            onComplete?.invoke(System.currentTimeMillis())
            return
        }
        val db = getFirestore()
        if (db == null) {
            onComplete?.invoke(System.currentTimeMillis())
            return
        }
        try {
            val batch = db.batch()
            for (msgId in visibleMessageIds) {
                val docRef = db.collection(COLLECTION_MESSAGES).document(msgId)
                val updates = mapOf<String, Any>(
                    "status" to MessageStatus.READ,
                    "readAt" to FieldValue.serverTimestamp()
                )
                batch.update(docRef, updates)
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d(TAG, "Batch marked ${visibleMessageIds.size} visible messages as 'read' in Firestore for $chatId")
                    onComplete?.invoke(System.currentTimeMillis())
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.UPDATE, COLLECTION_MESSAGES)
                    Log.w(TAG, "Batch read commit failed for $chatId: ${e.message}")
                    onComplete?.invoke(System.currentTimeMillis())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markVisibleMessagesRead: ${e.message}")
            onComplete?.invoke(System.currentTimeMillis())
        }
    }

    /**
     * Mark unread messages from others in a chat as 'read'.
     * Updates Firestore directly in real-time.
     */
    fun markChatMessagesRead(
        chatId: String,
        currentUserId: String,
        currentUserEmail: String = "",
        messageIds: List<String> = emptyList(),
        onComplete: ((readAt: Long) -> Unit)? = null
    ) {
        if (!FirestoreProvider.isAuthenticated()) {
            onComplete?.invoke(System.currentTimeMillis())
            return
        }
        val db = getFirestore()
        if (db == null) {
            onComplete?.invoke(System.currentTimeMillis())
            return
        }
        try {
            if (messageIds.isNotEmpty()) {
                val batch = db.batch()
                for (msgId in messageIds) {
                    val docRef = db.collection(COLLECTION_MESSAGES).document(msgId)
                    val updates = mapOf<String, Any>(
                        "status" to MessageStatus.READ,
                        "readAt" to FieldValue.serverTimestamp()
                    )
                    batch.update(docRef, updates)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Batch marked ${messageIds.size} messages as 'read' in Firestore for $chatId")
                        onComplete?.invoke(System.currentTimeMillis())
                    }
                    .addOnFailureListener { e ->
                        handleFirestoreError(e, OperationType.UPDATE, COLLECTION_MESSAGES)
                        onComplete?.invoke(System.currentTimeMillis())
                    }
            } else {
                onComplete?.invoke(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markChatMessagesRead: ${e.message}")
            onComplete?.invoke(System.currentTimeMillis())
        }
    }

    /**
     * Listen in real-time to Firestore message receipt updates and incoming messages for a specific chat.
     * When the recipient's device receives a message with status == 'sent', it automatically updates
     * the status to 'delivered' (Double Gray Tick), without marking it 'read'.
     */
    fun startListeningToChatReceipts(
        chatId: String,
        currentUserId: String,
        currentUserEmail: String = "",
        scope: CoroutineScope,
        onIncomingMessageSynced: (suspend (MessageEntity) -> Unit)? = null,
        onReceiptUpdated: suspend (messageId: String, status: String, deliveredAt: Long?, readAt: Long?) -> Unit
    ) {
        stopListening(chatId)
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        val normalizedEmail = currentUserEmail.lowercase().trim()

        try {
            val listener = db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_MESSAGES)
                        Log.e(TAG, "Chat receipts listener error for $chatId: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val msgId = doc.getString("id") ?: doc.id
                            val rawStatus = doc.getString("status")
                            val normalizedStatus = MessageStatus.normalize(rawStatus)
                            val deliveredAt = doc.getTimestamp("deliveredAt")?.toDate()?.time
                                ?: doc.getLong("deliveredAt")
                            val readAt = doc.getTimestamp("readAt")?.toDate()?.time
                                ?: doc.getLong("readAt")

                            val senderId = doc.getString("senderId") ?: ""
                            val senderEmail = doc.getString("senderEmail")?.lowercase()?.trim() ?: ""

                            val isFromOthers = isMessageFromOtherUser(
                                senderId = senderId,
                                senderEmail = senderEmail,
                                currentUserId = currentUserId,
                                currentUserEmail = normalizedEmail
                            )

                            // Recipient's device receives message -> trigger 'delivered' (Double Gray Tick)
                            val effectiveStatus = if (isFromOthers && normalizedStatus == MessageStatus.SENT) {
                                markMessageDelivered(msgId)
                                MessageStatus.DELIVERED
                            } else {
                                normalizedStatus
                            }

                            if (isFromOthers && onIncomingMessageSynced != null) {
                                val isEncrypted = doc.getBoolean("isEncrypted") ?: true
                                val rawText = doc.getString("text") ?: ""
                                val docChatId = doc.getString("chatId") ?: chatId
                                val decodedText = decodeFirebaseMessageText(
                                    rawText = rawText,
                                    chatId = docChatId,
                                    isEncrypted = isEncrypted,
                                    cipherVersion = doc.getString("cipherVersion")
                                )
                                val incomingEntity = MessageEntity(
                                    id = msgId,
                                    chatId = doc.getString("chatId") ?: chatId,
                                    senderId = senderId,
                                    senderName = resolveSenderDisplayName(doc.getString("senderName"), senderEmail),
                                    senderEmail = senderEmail,
                                    text = decodedText,
                                    mediaUri = doc.getString("mediaUri"),
                                    mediaType = doc.getString("mediaType"),
                                    callType = doc.getString("callType"),
                                    formattedRichText = doc.getString("formattedRichText"),
                                    timestamp = doc.getLong("clientTimestamp")
                                        ?: doc.getTimestamp("sentAt")?.toDate()?.time
                                        ?: System.currentTimeMillis(),
                                    status = effectiveStatus,
                                    deliveredAt = deliveredAt ?: System.currentTimeMillis(),
                                    readAt = readAt,
                                    emailTransportStatus = doc.getString("emailTransportStatus") ?: "DELIVERED_INBOX",
                                    emailSubject = doc.getString("emailSubject"),
                                    emailMessageId = doc.getString("emailMessageId"),
                                    isEncrypted = isEncrypted
                                )
                                scope.launch(Dispatchers.IO) {
                                    onIncomingMessageSynced(incomingEntity)
                                }
                            }

                            scope.launch(Dispatchers.IO) {
                                onReceiptUpdated(msgId, effectiveStatus, deliveredAt, readAt)
                            }
                        }
                    }
                }
            activeChatListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startListeningToChatReceipts: ${e.message}")
        }
    }

    /**
     * Global background listener for all messages involving the current user.
     */
    fun startGlobalMessageSync(
        currentUserId: String,
        currentUserEmail: String,
        scope: CoroutineScope,
        onIncomingMessageReceived: suspend (MessageEntity) -> Unit,
        onReceiptUpdated: suspend (messageId: String, status: String, deliveredAt: Long?, readAt: Long?) -> Unit
    ) {
        globalMessageListener?.remove()
        globalMessageListener = null
        if (!FirestoreProvider.isAuthenticated()) return
        val db = getFirestore() ?: return
        val normalizedEmail = currentUserEmail.lowercase().trim()
        val canonicalMyId = LinkoRepository.canonicalUserIdFromEmail(normalizedEmail, currentUserId)

        try {
            globalMessageListener = db.collection(COLLECTION_MESSAGES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, COLLECTION_MESSAGES)
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    for (doc in snapshot.documents) {
                        val msgId = doc.getString("id") ?: doc.id
                        val chatId = doc.getString("chatId") ?: continue
                        val senderId = doc.getString("senderId") ?: ""
                        val senderEmail = doc.getString("senderEmail")?.lowercase()?.trim() ?: ""
                        val participants = (doc.get("participants") as? List<*>)
                            ?.mapNotNull { it?.toString()?.lowercase()?.trim() }
                            ?: emptyList()

                        val isParticipant = currentUserId.lowercase() in participants ||
                                canonicalMyId.lowercase() in participants ||
                                (normalizedEmail.isNotBlank() && normalizedEmail in participants) ||
                                senderId == currentUserId ||
                                senderId == canonicalMyId ||
                                (normalizedEmail.isNotBlank() && senderEmail == normalizedEmail) ||
                                chatId.contains(currentUserId) ||
                                (canonicalMyId.isNotBlank() && chatId.contains(canonicalMyId))

                        if (!isParticipant) continue

                        val normalizedStatus = MessageStatus.normalize(doc.getString("status"))
                        val deliveredAt = doc.getTimestamp("deliveredAt")?.toDate()?.time
                            ?: doc.getLong("deliveredAt")
                        val readAt = doc.getTimestamp("readAt")?.toDate()?.time
                            ?: doc.getLong("readAt")

                        val isFromOthers = isMessageFromOtherUser(
                            senderId = senderId,
                            senderEmail = senderEmail,
                            currentUserId = currentUserId,
                            currentUserEmail = normalizedEmail
                        )

                        if (isFromOthers) {
                            val effectiveStatus = if (normalizedStatus == MessageStatus.SENT) {
                                // Recipient's device received the message -> mark 'delivered'
                                markMessageDelivered(msgId)
                                MessageStatus.DELIVERED
                            } else {
                                normalizedStatus
                            }

                            val isEncrypted = doc.getBoolean("isEncrypted") ?: true
                            val rawText = doc.getString("text") ?: ""
                            val decodedText = decodeFirebaseMessageText(
                                rawText = rawText,
                                chatId = chatId,
                                isEncrypted = isEncrypted,
                                cipherVersion = doc.getString("cipherVersion")
                            )

                            val incomingMessage = MessageEntity(
                                id = msgId,
                                chatId = chatId,
                                senderId = senderId,
                                senderName = resolveSenderDisplayName(doc.getString("senderName"), senderEmail),
                                senderEmail = senderEmail,
                                text = decodedText,
                                mediaUri = doc.getString("mediaUri"),
                                mediaType = doc.getString("mediaType"),
                                callType = doc.getString("callType"),
                                formattedRichText = doc.getString("formattedRichText"),
                                timestamp = doc.getLong("clientTimestamp")
                                    ?: doc.getTimestamp("sentAt")?.toDate()?.time
                                    ?: System.currentTimeMillis(),
                                status = effectiveStatus,
                                deliveredAt = deliveredAt ?: System.currentTimeMillis(),
                                readAt = readAt,
                                emailTransportStatus = doc.getString("emailTransportStatus") ?: "DELIVERED_INBOX",
                                emailSubject = doc.getString("emailSubject"),
                                emailMessageId = doc.getString("emailMessageId"),
                                isEncrypted = isEncrypted
                            )
                            scope.launch(Dispatchers.IO) {
                                onIncomingMessageReceived(incomingMessage)
                            }
                        } else {
                            // Message sent by me -> update my local receipt status ('sent' -> 'delivered' -> 'read')
                            scope.launch(Dispatchers.IO) {
                                onReceiptUpdated(msgId, normalizedStatus, deliveredAt, readAt)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startGlobalMessageSync: ${e.message}")
        }
    }

    private fun isMessageFromOtherUser(
        senderId: String,
        senderEmail: String,
        currentUserId: String,
        currentUserEmail: String
    ): Boolean {
        val normalizedCurrentEmail = currentUserEmail.lowercase().trim()
        val normalizedSenderEmail = senderEmail.lowercase().trim()
        if (normalizedSenderEmail.isNotBlank() && normalizedCurrentEmail.isNotBlank()) {
            return normalizedSenderEmail != normalizedCurrentEmail
        }
        val canonicalSender = LinkoRepository.canonicalParticipantKey(normalizedSenderEmail.ifBlank { senderId })
        val canonicalCurrent = LinkoRepository.canonicalParticipantKey(normalizedCurrentEmail.ifBlank { currentUserId })
        return senderId.isNotBlank() && currentUserId.isNotBlank() &&
            senderId != currentUserId && canonicalSender != canonicalCurrent
    }

    /**
     * Stop active snapshot listener for a chat or all chats.
     */
    fun stopListening(chatId: String? = null) {
        if (chatId != null) {
            activeChatListeners.remove(chatId)?.remove()
        } else {
            activeChatListeners.values.forEach { it.remove() }
            activeChatListeners.clear()
            globalMessageListener?.remove()
            globalMessageListener = null
        }
    }
}

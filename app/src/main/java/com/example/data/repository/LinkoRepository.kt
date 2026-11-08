package com.example.data.repository

import com.example.data.crypto.EncryptionManager
import com.example.data.local.dao.CallLogDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.StatusDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CallType
import com.example.data.remote.FirestoreContactService
import com.example.data.remote.FirestoreGroupService
import com.example.data.remote.FirestoreMessageService
import com.example.data.remote.GeminiChatTurn
import com.example.data.remote.GeminiClient
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class LinkoRepository(
    private val userDao: UserDao,
    private val contactDao: ContactDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val callLogDao: CallLogDao,
    private val statusDao: StatusDao
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _geminiTypingChatIds = MutableStateFlow<Set<String>>(emptySet())
    val geminiTypingChatIdsFlow: StateFlow<Set<String>> = _geminiTypingChatIds.asStateFlow()

    fun setGeminiTyping(chatId: String, isTyping: Boolean) {
        if (isTyping) {
            _geminiTypingChatIds.value = _geminiTypingChatIds.value + chatId
        } else {
            _geminiTypingChatIds.value = _geminiTypingChatIds.value - chatId
        }
    }

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = kotlinx.coroutines.flow.combine(
        chatDao.getAllChatsFlow(),
        userDao.getCurrentUserFlow()
    ) { chatList, me ->
        val myContacts = if (me != null && me.email.isNotBlank()) {
            try {
                contactDao.getContactsList(me.email.lowercase().trim())
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        deduplicateChatsList(chatList, me, myContacts)
    }
    val allCallLogsFlow: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogsFlow()
    val activeStatusesFlow: Flow<List<StatusEntity>> = statusDao.getActiveStatusesFlow(System.currentTimeMillis())


    fun getContactsFlow(ownerEmail: String): Flow<List<ContactEntity>> {
        return contactDao.getContactsFlow(ownerEmail)
    }

    private fun decodeStoredMessageText(rawText: String, chatId: String): String {
        if (rawText.isEmpty()) return ""
        val afterDecrypt = EncryptionManager.decrypt(rawText, chatId)
        val afterCoder = if (com.example.util.SecretMessageCoder.isEncodedFormat(afterDecrypt)) {
            com.example.util.SecretMessageCoder.decodeMessage(afterDecrypt)
        } else {
            afterDecrypt
        }
        return if (EncryptionManager.isEncrypted(afterCoder)) {
            EncryptionManager.decrypt(afterCoder, chatId)
        } else {
            afterCoder
        }
    }

    val allMessagesFlow: Flow<List<MessageEntity>> = messageDao.getAllMessagesFlow().map { list ->
        list.map { msg ->
            msg.copy(text = decodeStoredMessageText(msg.text, msg.chatId))
        }
    }

    fun getMessagesFlow(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChatFlow(chatId).map { list ->
            list.map { msg ->
                msg.copy(text = decodeStoredMessageText(msg.text, chatId))
            }
        }

    suspend fun clearChatHistory(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.updateLastMessage(chatId, "Chat history cleared", System.currentTimeMillis())
    }

    suspend fun deleteChat(chatId: String) {
        messageDao.deleteMessagesForChat(chatId)
        chatDao.deleteChatById(chatId)
    }

    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> {
        return chatDao.getChatByIdFlow(chatId)
    }

    suspend fun seedSampleDataIfEmpty() {
        // Production mode: Database is clean and empty by default on first install.
        // User chats, contacts, and messages are only populated via authentication, QR code scanning, or Firestore sync.
    }

    suspend fun postStatus(
        user: UserEntity,
        type: String,
        textContent: String?,
        mediaUri: String? = null,
        backgroundColorHex: String = "#10B981",
        fontStyle: String = "DEFAULT"
    ): StatusEntity {
        val now = System.currentTimeMillis()
        val newStatus = StatusEntity(
            id = "status_" + UUID.randomUUID().toString().take(8),
            userId = user.id,
            userDisplayName = user.displayName,
            userEmail = user.email,
            userAvatarUri = user.avatarUri,
            type = type,
            textContent = textContent,
            mediaUri = mediaUri,
            backgroundColorHex = backgroundColorHex,
            fontStyle = fontStyle,
            createdTimestamp = now,
            expiresTimestamp = now + (24 * 60 * 60 * 1000L),
            isViewed = false
        )
        statusDao.insertStatus(newStatus)
        return newStatus
    }

    suspend fun markStatusAsViewed(statusId: String) {
        statusDao.markStatusAsViewed(statusId)
    }

    suspend fun deleteStatus(statusId: String) {
        statusDao.deleteStatusById(statusId)
    }

    suspend fun cleanExpiredStatuses() {
        statusDao.deleteExpiredStatuses(System.currentTimeMillis())
    }


    suspend fun getOrCreateCurrentUser(): UserEntity {
        val current = userDao.getCurrentUser()
        if (current != null) return current
        val fbUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        }
        if (fbUser != null && !fbUser.email.isNullOrBlank()) {
            val email = fbUser.email ?: ""
            val displayName = fbUser.displayName ?: email.substringBefore("@")
            val username = email.substringBefore("@").replace(".", "_")
            return registerOrLogin(email, displayName, username)
        }
        return loginAsGuest()
    }

    suspend fun createOrGetGeminiChat(me: UserEntity): ChatEntity {
        val cleanOwnerEmail = me.email.lowercase().trim()
        val geminiContact = ContactEntity(
            id = "c_gemini",
            userEmail = cleanOwnerEmail,
            contactUserId = "user_gemini",
            contactEmail = "gemini.ai@linko.com",
            contactDisplayName = "Gemini AI Assistant",
            contactUsername = "gemini_ai",
            contactStatus = "Powered by Gemini 3.8 Flash • Ask me anything! ✨",
            isBlocked = false
        )
        contactDao.insertContact(geminiContact)

        // Clean up any stale error messages in chat_gemini_ai from previous sessions
        val existingMessages = messageDao.getMessagesForChatList("chat_gemini_ai")
        for (msg in existingMessages) {
            val plain = decodeStoredMessageText(msg.text, "chat_gemini_ai")
            if (plain.startsWith("Gemini AI Assistant Error") || plain.startsWith("Gemini did not return text")) {
                messageDao.deleteMessageById(msg.id)
            }
        }

        val remainingMessages = messageDao.getMessagesForChatList("chat_gemini_ai")
        val welcomeText = "Hello! 👋 I'm your Gemini AI Assistant on Linko.\n\nAsk me questions, draft messages, analyze attached images, or summarize conversations! ✨"

        if (remainingMessages.isEmpty()) {
            val welcomeMessage = MessageEntity(
                id = "m_gemini_welcome",
                chatId = "chat_gemini_ai",
                senderId = "user_gemini",
                senderName = "Gemini AI Assistant",
                senderEmail = "gemini.ai@linko.com",
                text = EncryptionManager.encrypt(welcomeText, "chat_gemini_ai"),
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                emailTransportStatus = "DELIVERED_INBOX",
                emailSubject = "Welcome to Gemini AI on Linko",
                emailMessageId = "<gemini-welcome@linko.net>",
                isEncrypted = true,
                encryptionAlgorithm = "AES-256-GCM"
            )
            messageDao.insertMessage(welcomeMessage)
        }

        val latestMsg = messageDao.getMessagesForChatList("chat_gemini_ai").lastOrNull()
        val latestPreview = if (latestMsg != null) {
            decodeStoredMessageText(latestMsg.text, "chat_gemini_ai").replace("\n", " ").take(65)
        } else {
            "Hello! I'm Gemini, your AI assistant on Linko. Ask me anything! ✨"
        }

        val existing = chatDao.getChatById("chat_gemini_ai")
        val updatedGeminiChat = ChatEntity(
            id = "chat_gemini_ai",
            isGroup = false,
            name = "Gemini AI Assistant",
            participantIdsJson = "[\"${me.id}\", \"user_gemini\"]",
            lastMessageText = latestPreview,
            lastMessageTimestamp = latestMsg?.timestamp ?: existing?.lastMessageTimestamp ?: System.currentTimeMillis(),
            unreadCount = existing?.unreadCount ?: 0
        )
        chatDao.insertOrUpdateChat(updatedGeminiChat)
        return updatedGeminiChat
    }

    suspend fun registerOrLogin(email: String, displayName: String, username: String): UserEntity {
        userDao.clearCurrentUser()
        val normalizedEmail = email.lowercase().trim()
        val canonicalId = canonicalUserIdFromEmail(normalizedEmail)
        val existing = userDao.getUserByEmail(normalizedEmail) ?: userDao.getUserByEmail(email.trim())
        val user = if (existing != null) {
            val updated = existing.copy(
                email = normalizedEmail,
                displayName = if (displayName.isNotBlank()) displayName.trim() else existing.displayName,
                username = if (username.isNotBlank()) username.trim() else existing.username,
                qrPayload = "linko://user?email=$normalizedEmail&id=${existing.id}",
                isCurrentUser = true
            )
            userDao.updateUser(updated)
            updated
        } else {
            val newUserId = canonicalId
            val newUser = UserEntity(
                id = newUserId,
                email = normalizedEmail,
                username = if (username.isBlank()) normalizedEmail.substringBefore("@") else username.trim(),
                displayName = if (displayName.isBlank()) normalizedEmail.substringBefore("@").replace(".", " ").capitalizeWords() else displayName.trim(),
                avatarUri = null,
                qrPayload = "linko://user?email=$normalizedEmail&id=$newUserId",
                statusMessage = "Hey! I am using Linko.",
                isCurrentUser = true
            )
            userDao.insertUser(newUser)
            newUser
        }
        createOrGetGeminiChat(user)
        deduplicateAndUnifyDirectChats(user)
        syncContactsWithFirestore(user.email)
        syncMessagesWithFirestore(user)
        return user
    }

    suspend fun loginAsGuest(customDisplayName: String? = null): UserEntity {
        userDao.clearCurrentUser()
        val guestEmail = "guest@linko.app"
        val resolvedName = customDisplayName?.trim()?.takeIf { it.isNotBlank() } ?: "Guest User"
        val existing = userDao.getUserByEmail(guestEmail)
        val guestUser = if (existing != null) {
            val updated = existing.copy(
                displayName = if (customDisplayName.isNullOrBlank()) existing.displayName.ifBlank { "Guest User" } else resolvedName,
                isCurrentUser = true
            )
            userDao.updateUser(updated)
            updated
        } else {
            val guestId = "user_guest_local"
            val newGuest = UserEntity(
                id = guestId,
                email = guestEmail,
                username = "guest_user",
                displayName = resolvedName,
                avatarUri = null,
                qrPayload = "linko://user?email=$guestEmail&id=$guestId",
                statusMessage = "Exploring Linko in Guest Mode 👋",
                isCurrentUser = true
            )
            userDao.insertUser(newGuest)
            newGuest
        }
        // Ensure Guest immediately has the interactive Gemini AI Assistant chat ready to explore
        createOrGetGeminiChat(guestUser)
        return guestUser
    }

    suspend fun logout() {
        com.example.data.remote.FirestorePresenceService.stopListening()
        FirestoreContactService.stopRealtimeSync()
        FirestoreGroupService.stopRealtimeGroupSync()
        FirestoreMessageService.stopListening()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Throwable) {}
        userDao.clearCurrentUser()
    }

    suspend fun addContactByEmail(ownerEmail: String, emailInput: String): Result<ContactEntity> {
        val trimmed = emailInput.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        val cleanOwner = ownerEmail.lowercase().trim()
        val cleanContactEmail = trimmed.lowercase().trim()

        if (cleanContactEmail == cleanOwner) {
            return Result.failure(IllegalArgumentException("You cannot add yourself as a contact."))
        }

        val deterministicRecipientId = canonicalUserIdFromEmail(cleanContactEmail)
        val ownerUser = userDao.getUserByEmail(cleanOwner) ?: userDao.getCurrentUser()

        val existing = contactDao.getContactByEmail(cleanOwner, cleanContactEmail)
        if (existing != null) {
            val normalizedExisting = if (existing.contactUserId != deterministicRecipientId) {
                existing.copy(contactUserId = deterministicRecipientId).also {
                    contactDao.insertContact(it)
                }
            } else {
                existing
            }
            FirestoreContactService.uploadContact(normalizedExisting)
            if (ownerUser != null) {
                createOrGetDirectChat(ownerUser, normalizedExisting)
            }
            return Result.success(normalizedExisting)
        }

        val contactName = cleanContactEmail.substringBefore("@").replace(".", " ").capitalizeWords()

        val newContact = ContactEntity(
            id = "c_${canonicalParticipantKey(cleanContactEmail)}",
            userEmail = cleanOwner,
            contactUserId = deterministicRecipientId,
            contactEmail = cleanContactEmail,
            contactDisplayName = contactName,
            contactUsername = cleanContactEmail.substringBefore("@"),
            contactStatus = "Added via Email Connection"
        )
        contactDao.insertContact(newContact)
        FirestoreContactService.uploadContact(newContact)

        // Bidirectional pairing: ensure recipient also has owner as contact with deterministic ID
        if (ownerUser != null) {
            val ownerCanonicalId = canonicalUserIdFromEmail(cleanOwner, ownerUser.id)
            val reciprocalContact = ContactEntity(
                id = "c_${canonicalParticipantKey(cleanOwner)}",
                userEmail = cleanContactEmail,
                contactUserId = ownerCanonicalId,
                contactEmail = cleanOwner,
                contactDisplayName = ownerUser.displayName,
                contactUsername = ownerUser.username,
                contactAvatarUri = ownerUser.avatarUri,
                contactStatus = "Added via Email Connection",
                addedTimestamp = System.currentTimeMillis()
            )
            contactDao.insertContact(reciprocalContact)
            FirestoreContactService.uploadContact(reciprocalContact)
            createOrGetDirectChat(ownerUser, newContact)
        }

        return Result.success(newContact)
    }

    suspend fun addContactFromQrPayload(ownerEmail: String, qrPayload: String): Result<ContactEntity> {
        val trimmed = qrPayload.trim()

        // Strict Linko URI schema validation
        val isValidLinkoSchema = trimmed.startsWith("linko://user", ignoreCase = true) ||
            trimmed.startsWith("linko://contact", ignoreCase = true)

        if (!isValidLinkoSchema) {
            return Result.failure(IllegalArgumentException("Invalid Linko QR Code"))
        }

        var email = ""
        var qrUserId = ""
        var displayName = ""
        var avatarUri: String? = null

        if (trimmed.startsWith("linko://user/")) {
            val pathPart = trimmed.substringAfter("://user/").substringBefore("?").substringBefore("&")
            if (pathPart.isNotBlank()) {
                qrUserId = pathPart
            }
        }

        if (trimmed.contains("email=") || trimmed.contains("id=") || trimmed.contains("name=") || trimmed.contains("avatar=")) {
            email = trimmed.substringAfter("email=", "").substringBefore("&")
            if (qrUserId.isBlank()) {
                qrUserId = trimmed.substringAfter("id=", "").substringBefore("&")
            }
            displayName = trimmed.substringAfter("name=", "").substringBefore("&").ifBlank {
                trimmed.substringAfter("displayName=", "").substringBefore("&")
            }
            avatarUri = trimmed.substringAfter("avatar=", "").substringBefore("&").ifBlank {
                trimmed.substringAfter("avatarUri=", "").substringBefore("&")
            }.ifBlank { null }
        } else if (trimmed.startsWith("{")) {
            try {
                val json = org.json.JSONObject(trimmed)
                email = json.optString("email", "")
                if (qrUserId.isBlank()) qrUserId = json.optString("id", "")
                displayName = json.optString("displayName", json.optString("name", ""))
                avatarUri = json.optString("avatarUri", json.optString("avatar", "")).ifBlank { null }
            } catch (_: Exception) {}
        }

        if (displayName.isNotBlank()) {
            try {
                displayName = java.net.URLDecoder.decode(displayName, "UTF-8")
            } catch (_: Exception) {}
        }

        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Could not extract contact email from Linko QR Code."))
        }

        val cleanOwner = ownerEmail.lowercase().trim()
        val cleanContactEmail = email.lowercase().trim()

        if (cleanContactEmail == cleanOwner) {
            return Result.failure(IllegalArgumentException("You cannot scan and add your own Linko QR code."))
        }

        // Look up if recipient user exists in local database (DO NOT update or overwrite local currentUser state)
        val existingUser = userDao.getUserByEmail(cleanContactEmail)
        val finalUserId = canonicalUserIdFromEmail(cleanContactEmail, qrUserId.ifBlank { existingUser?.id ?: "" })
        val finalDisplayName = when {
            displayName.isNotBlank() -> displayName
            existingUser != null && existingUser.displayName.isNotBlank() -> existingUser.displayName
            else -> cleanContactEmail.substringBefore("@").replace(".", " ").capitalizeWords()
        }
        val finalAvatar = avatarUri ?: existingUser?.avatarUri

        val existingContact = contactDao.getContactByEmail(cleanOwner, cleanContactEmail)
        val contactToSave = ContactEntity(
            id = existingContact?.id ?: "c_${canonicalParticipantKey(cleanContactEmail)}",
            userEmail = cleanOwner,
            contactUserId = finalUserId,
            contactEmail = cleanContactEmail,
            contactDisplayName = finalDisplayName,
            contactUsername = existingUser?.username ?: cleanContactEmail.substringBefore("@"),
            contactAvatarUri = finalAvatar,
            contactStatus = "Scanned & Paired via Linko QR 📷",
            addedTimestamp = System.currentTimeMillis()
        )

        contactDao.insertContact(contactToSave)
        FirestoreContactService.uploadContact(contactToSave)

        // Bidirectional pairing: ensure recipient contact list also incorporates the scanner's details symmetrically
        val ownerUser = userDao.getUserByEmail(cleanOwner) ?: userDao.getCurrentUser()
        if (ownerUser != null) {
            val ownerCanonicalId = canonicalUserIdFromEmail(cleanOwner, ownerUser.id)
            val reciprocalContact = ContactEntity(
                id = "c_${canonicalParticipantKey(cleanOwner)}",
                userEmail = cleanContactEmail,
                contactUserId = ownerCanonicalId,
                contactEmail = cleanOwner,
                contactDisplayName = ownerUser.displayName,
                contactUsername = ownerUser.username,
                contactAvatarUri = ownerUser.avatarUri,
                contactStatus = "Scanned & Paired via Linko QR 📷",
                addedTimestamp = System.currentTimeMillis()
            )
            contactDao.insertContact(reciprocalContact)
            FirestoreContactService.uploadContact(reciprocalContact)

            createOrGetDirectChat(ownerUser, contactToSave)
        }

        return Result.success(contactToSave)
    }

    suspend fun deleteMessageForMe(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    suspend fun deleteMessageForEveryone(messageId: String) {
        messageDao.markMessageAsDeletedForEveryone(messageId)
    }

    fun syncContactsWithFirestore(
        ownerEmail: String,
        onNewContactReceived: ((ContactEntity) -> Unit)? = null
    ) {
        val cleanOwnerEmail = ownerEmail.lowercase().trim()
        if (cleanOwnerEmail.isBlank()) return

        // 1. Fetch existing contacts from Firestore and sync to Room
        FirestoreContactService.fetchAndSyncContacts(cleanOwnerEmail, ioScope) { remoteContacts ->
            val me = userDao.getUserByEmail(cleanOwnerEmail) ?: userDao.getCurrentUser()
            remoteContacts.forEach { remote ->
                val cleanEmail = remote.contactEmail.lowercase().trim()
                val canonicalContactId = canonicalUserIdFromEmail(cleanEmail, remote.contactUserId)
                val existingLocal = contactDao.getContactByEmail(cleanOwnerEmail, cleanEmail)
                val safeBlocked = if (isGeminiAi(null, canonicalContactId, cleanEmail, remote.contactDisplayName)) {
                    false
                } else {
                    remote.isBlocked || (existingLocal?.isBlocked == true)
                }
                val mergedContact = remote.copy(
                    id = existingLocal?.id ?: remote.id,
                    userEmail = cleanOwnerEmail,
                    contactEmail = cleanEmail,
                    contactUserId = canonicalContactId,
                    contactDisplayName = if (!isGenericPlaceholderName(remote.contactDisplayName, me?.displayName, cleanOwnerEmail)) {
                        remote.contactDisplayName
                    } else {
                        existingLocal?.contactDisplayName?.takeIf { !isGenericPlaceholderName(it, me?.displayName, cleanOwnerEmail) }
                            ?: cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords()
                    },
                    isBlocked = safeBlocked
                )
                contactDao.insertContact(mergedContact)
            }
            if (me != null) {
                deduplicateAndUnifyDirectChats(me)
            }
        }

        // 2. Start real-time snapshot listener
        FirestoreContactService.startRealtimeContactSync(cleanOwnerEmail, ioScope) { liveContacts ->
            val me = userDao.getUserByEmail(cleanOwnerEmail) ?: userDao.getCurrentUser()
            val existingEmails = contactDao.getContactsList(cleanOwnerEmail)
                .map { it.contactEmail.lowercase().trim() }
                .toSet()
            liveContacts.forEach { contact ->
                val cleanEmail = contact.contactEmail.lowercase().trim()
                val isNew = cleanEmail !in existingEmails
                val existingLocal = contactDao.getContactByEmail(cleanOwnerEmail, cleanEmail)
                val canonicalContactId = canonicalUserIdFromEmail(cleanEmail, contact.contactUserId)
                val safeBlocked = if (isGeminiAi(null, canonicalContactId, cleanEmail, contact.contactDisplayName)) {
                    false
                } else {
                    contact.isBlocked
                }
                val mergedContact = contact.copy(
                    id = existingLocal?.id ?: contact.id,
                    userEmail = cleanOwnerEmail,
                    contactEmail = cleanEmail,
                    contactUserId = canonicalContactId,
                    contactDisplayName = if (!isGenericPlaceholderName(contact.contactDisplayName, me?.displayName, cleanOwnerEmail)) {
                        contact.contactDisplayName
                    } else {
                        existingLocal?.contactDisplayName?.takeIf { !isGenericPlaceholderName(it, me?.displayName, cleanOwnerEmail) }
                            ?: cleanEmail.substringBefore("@").replace(".", " ").capitalizeWords()
                    },
                    isBlocked = safeBlocked
                )
                contactDao.insertContact(mergedContact)
                if (isNew && cleanEmail.isNotBlank()) {
                    onNewContactReceived?.invoke(mergedContact)
                }
            }
            if (me != null) {
                deduplicateAndUnifyDirectChats(me)
            }
        }

        // 3. Upload local contacts to Firestore
        ioScope.launch {
            val localContacts = contactDao.getContactsList(cleanOwnerEmail)
            FirestoreContactService.uploadContactsBatch(localContacts)
        }
    }

    /**
     * Blocks or unblocks a regular contact and syncs the updated status to Room and Cloud Firestore.
     * Strictly enforces the AI Exception: Gemini AI bot can NEVER be blocked.
     */
    suspend fun setContactBlockedStatus(
        me: UserEntity,
        chat: ChatEntity?,
        contact: ContactEntity?,
        isBlocked: Boolean
    ): Boolean {
        val cleanOwnerEmail = me.email.lowercase().trim()
        val peerUserId = contact?.contactUserId ?: chat?.let { extractPeerIdFromChat(it, me) } ?: ""
        val peerEmail = contact?.contactEmail ?: chat?.let { extractPeerEmailFromChat(it, me) } ?: ""
        val peerName = contact?.contactDisplayName ?: chat?.name ?: "Contact"

        // Strict AI Exception check
        if (isGeminiAi(chat?.id, peerUserId, peerEmail, peerName)) {
            return false
        }

        val resolvedEmail = peerEmail.lowercase().trim().ifBlank {
            "${peerName.lowercase().trim().replace(" ", ".")}@linko.net"
        }
        val resolvedUserId = canonicalUserIdFromEmail(resolvedEmail, peerUserId)
        val existingContact = contact
            ?: contactDao.getContactByEmail(cleanOwnerEmail, resolvedEmail)
            ?: contactDao.getContactsList(cleanOwnerEmail).find {
                it.contactUserId.equals(resolvedUserId, ignoreCase = true) ||
                    it.contactEmail.equals(resolvedEmail, ignoreCase = true) ||
                    (!isGenericPlaceholderName(peerName, me.displayName, cleanOwnerEmail) &&
                        it.contactDisplayName.equals(peerName.trim(), ignoreCase = true))
            }

        val updatedContact = if (existingContact != null) {
            existingContact.copy(
                isBlocked = isBlocked,
                contactStatus = if (isBlocked) "Blocked" else existingContact.contactStatus.let {
                    if (it == "Blocked") "Available" else it
                }
            )
        } else {
            ContactEntity(
                id = "c_${canonicalParticipantKey(resolvedEmail)}",
                userEmail = cleanOwnerEmail,
                contactUserId = resolvedUserId,
                contactEmail = resolvedEmail,
                contactDisplayName = peerName.ifBlank { "Contact" },
                contactUsername = resolvedEmail.substringBefore("@"),
                contactAvatarUri = chat?.groupIconUri,
                contactStatus = if (isBlocked) "Blocked" else "Available",
                addedTimestamp = System.currentTimeMillis(),
                isBlocked = isBlocked
            )
        }

        contactDao.insertContact(updatedContact)
        contactDao.updateContactBlockedByPeer(
            ownerEmail = cleanOwnerEmail,
            peerEmail = resolvedEmail,
            peerUserId = resolvedUserId,
            isBlocked = isBlocked
        )
        FirestoreContactService.updateContactBlockState(updatedContact)
        return true
    }

    private fun extractPeerIdFromChat(chat: ChatEntity, me: UserEntity): String {
        val myTokens = setOf(
            me.id.lowercase().trim(),
            me.email.lowercase().trim(),
            canonicalUserIdFromEmail(me.email, me.id).lowercase().trim()
        )
        val participants = parseJsonArray(chat.participantIdsJson)
        return participants.firstOrNull { it.lowercase().trim() !in myTokens } ?: ""
    }

    private fun extractPeerEmailFromChat(chat: ChatEntity, me: UserEntity): String {
        val myEmail = me.email.lowercase().trim()
        val participants = parseJsonArray(chat.participantIdsJson)
        return participants.firstOrNull { it.contains("@") && !it.equals(myEmail, ignoreCase = true) } ?: ""
    }

    suspend fun isPeerOrChatBlocked(me: UserEntity, chatId: String?, peerUserId: String? = null, peerEmail: String? = null): Boolean {
        if (isGeminiAi(chatId, peerUserId, peerEmail, null)) return false
        val cleanOwnerEmail = me.email.lowercase().trim()
        if (cleanOwnerEmail.isBlank()) return false
        val contacts = contactDao.getContactsList(cleanOwnerEmail)
        val blockedContacts = contacts.filter { it.isBlocked && !isGeminiAi(null, it.contactUserId, it.contactEmail, it.contactDisplayName) }
        if (blockedContacts.isEmpty()) return false

        val cleanPeerId = peerUserId?.lowercase()?.trim() ?: ""
        val cleanPeerEmail = peerEmail?.lowercase()?.trim() ?: ""
        val chat = if (!chatId.isNullOrBlank()) chatDao.getChatById(chatId) else null
        if (chat?.isGroup == true) return false

        return blockedContacts.any { blocked ->
            val bId = blocked.contactUserId.lowercase().trim()
            val bEmail = blocked.contactEmail.lowercase().trim()
            val bCanonical = canonicalParticipantKey(bEmail.ifBlank { bId })
            val directId = generateDirectChatId(me.id, cleanOwnerEmail, bId, bEmail)
            (chatId != null && (chatId == directId || chatId == "chat_$bId")) ||
                (cleanPeerId.isNotBlank() && (cleanPeerId == bId || canonicalParticipantKey(cleanPeerId) == bCanonical)) ||
                (cleanPeerEmail.isNotBlank() && cleanPeerEmail == bEmail) ||
                (chat != null && !chat.isGroup && chat.name.equals(blocked.contactDisplayName, ignoreCase = true))
        }
    }

    fun syncGroupsWithFirestore(user: UserEntity) {
        if (user.email.isBlank() || user.id.isBlank()) return
        FirestoreGroupService.startRealtimeGroupSync(
            userId = user.id,
            userEmail = user.email,
            scope = ioScope
        ) { remoteGroups ->
            remoteGroups.forEach { group ->
                val existing = chatDao.getChatById(group.id)
                if (existing == null || group.lastMessageTimestamp >= existing.lastMessageTimestamp) {
                    chatDao.insertOrUpdateChat(group)
                }
            }
        }
    }

    companion object {
        fun isGeminiAi(
            chatId: String? = null,
            userId: String? = null,
            email: String? = null,
            name: String? = null
        ): Boolean {
            val cId = chatId?.lowercase()?.trim() ?: ""
            val uId = userId?.lowercase()?.trim() ?: ""
            val em = email?.lowercase()?.trim() ?: ""
            val nm = name?.lowercase()?.trim() ?: ""
            return cId == "chat_gemini_ai" ||
                uId == "user_gemini" ||
                uId == "c_gemini" ||
                em.contains("gemini") ||
                nm == "gemini" ||
                nm.contains("gemini ai")
        }

        fun canonicalUserIdFromEmail(email: String, fallbackId: String = ""): String {
            val normalized = email.lowercase().trim()
            if (normalized.isBlank()) {
                return fallbackId.lowercase().trim().ifBlank { "user_unknown" }
            }
            if (normalized == "gemini.ai@linko.com" || normalized == "gemini_ai@google.com" || normalized == "gemini.ai@google.com") {
                return "user_gemini"
            }
            if (normalized == "guest@linko.app") {
                return "user_guest_local"
            }
            val sanitized = normalized.replace("@", "_at_").replace(Regex("[^a-z0-9_]"), "_")
            return "user_$sanitized"
        }

        fun canonicalParticipantKey(idOrEmail: String): String {
            val raw = idOrEmail.lowercase().trim()
            if (raw.isBlank()) return "unknown"
            if (raw == "user_gemini" || raw == "gemini" || raw == "gemini.ai@linko.com") {
                return "user_gemini"
            }
            if (raw.contains("@")) {
                return canonicalUserIdFromEmail(raw)
            }
            return raw.replace(Regex("[^a-z0-9_]"), "_")
        }

        /**
         * Generates a single deterministic, order-independent direct conversation ID for any two users
         * by normalizing their identifiers and sorting them alphabetically before combining.
         * Ensures generateDirectChatId(A, B) == generateDirectChatId(B, A) regardless of who is sender or receiver.
         */
        fun generateDirectChatId(user1Id: String, user2Id: String): String {
            val k1 = canonicalParticipantKey(user1Id)
            val k2 = canonicalParticipantKey(user2Id)
            if (k1 == "user_gemini" || k2 == "user_gemini") {
                return "chat_gemini_ai"
            }
            val sorted = listOf(k1, k2).sorted()
            return "chat_${sorted[0]}_${sorted[1]}"
        }

        fun generateDirectChatId(
            user1Id: String,
            user1Email: String,
            user2Id: String,
            user2Email: String
        ): String {
            val primary1 = user1Email.trim().ifBlank { user1Id.trim() }
            val primary2 = user2Email.trim().ifBlank { user2Id.trim() }
            return generateDirectChatId(primary1, primary2)
        }

        fun isGenericPlaceholderName(
            name: String?,
            myDisplayName: String? = null,
            myEmail: String? = null
        ): Boolean {
            if (name.isNullOrBlank()) return true
            val normalized = name.lowercase().trim()
            if (normalized in setOf("contact", "linko contact", "linko chat", "chat", "unknown", "user", "friend")) {
                return true
            }
            if (!myDisplayName.isNullOrBlank() && normalized == myDisplayName.lowercase().trim()) {
                return true
            }
            if (!myEmail.isNullOrBlank() && normalized == myEmail.lowercase().trim()) {
                return true
            }
            return false
        }
    }

    suspend fun createOrGetDirectChat(me: UserEntity, contact: ContactEntity): ChatEntity {
        if (contact.contactUserId == "user_gemini" ||
            contact.contactEmail.contains("gemini", ignoreCase = true) ||
            contact.contactDisplayName.contains("Gemini", ignoreCase = true)
        ) {
            return createOrGetGeminiChat(me)
        }
        return resolveAndUnifyDirectChat(
            me = me,
            peerUserId = contact.contactUserId,
            peerEmail = contact.contactEmail,
            peerDisplayName = contact.contactDisplayName,
            peerAvatarUri = contact.contactAvatarUri
        )
    }

    suspend fun createGroupChat(
        me: UserEntity,
        groupName: String,
        selectedContacts: List<ContactEntity>,
        groupDesc: String?,
        channelType: String = "GROUP"
    ): ChatEntity {
        val prefix = if (channelType.equals("CHANNEL", ignoreCase = true)) "channel" else "group"
        val groupId = "${prefix}_${UUID.randomUUID().toString().take(8)}"
        val participantIds = mutableListOf(me.id)
        val participantEmails = mutableListOf(me.email.lowercase().trim())
        selectedContacts.forEach { contact ->
            if (!participantIds.contains(contact.contactUserId)) {
                participantIds.add(contact.contactUserId)
            }
            val cleanEmail = contact.contactEmail.lowercase().trim()
            if (cleanEmail.isNotBlank() && !participantEmails.contains(cleanEmail)) {
                participantEmails.add(cleanEmail)
            }
        }
        val idsJson = toJsonArray(participantIds)
        val isBroadcastChannel = channelType.equals("CHANNEL", ignoreCase = true)
        val defaultDesc = if (isBroadcastChannel) {
            "Broadcast Channel created by ${me.displayName}"
        } else {
            "Group created by ${me.displayName}"
        }
        val resolvedName = groupName.ifBlank { if (isBroadcastChannel) "Custom Channel" else "Custom Group" }
        val now = System.currentTimeMillis()

        val newGroup = ChatEntity(
            id = groupId,
            isGroup = true,
            name = resolvedName,
            groupDescription = groupDesc?.takeIf { it.isNotBlank() } ?: defaultDesc,
            participantIdsJson = idsJson,
            adminIdsJson = "[\"${me.id}\"]",
            lastMessageText = "${me.displayName} created ${if (isBroadcastChannel) "channel" else "group"} \"$resolvedName\" (${selectedContacts.size + 1} members)",
            lastMessageTimestamp = now
        )
        chatDao.insertOrUpdateChat(newGroup)

        // Sync group/channel and multi-contact invitations to Cloud Firestore
        FirestoreGroupService.uploadGroupChat(
            chat = newGroup,
            participantEmails = participantEmails,
            creatorId = me.id,
            channelType = if (isBroadcastChannel) "CHANNEL" else "GROUP"
        )
        if (selectedContacts.isNotEmpty()) {
            FirestoreGroupService.inviteContactsToGroup(
                chat = newGroup,
                inviter = me,
                invitedContacts = selectedContacts,
                channelType = if (isBroadcastChannel) "CHANNEL" else "GROUP"
            )
        }

        // System message inside group/channel
        val invitedNames = selectedContacts.joinToString { it.contactDisplayName }
        val welcomePlain = if (selectedContacts.isNotEmpty()) {
            "🎉 Welcome to $resolvedName! Invited: $invitedNames."
        } else {
            "🎉 Welcome to $resolvedName! Channel created and synced to Cloud Firestore."
        }
        val encryptedWelcome = EncryptionManager.encrypt(welcomePlain, groupId)
        val systemMessage = MessageEntity(
            id = "m_${UUID.randomUUID().toString().take(8)}",
            chatId = groupId,
            senderId = me.id,
            senderName = me.displayName,
            senderEmail = me.email,
            text = encryptedWelcome,
            timestamp = now,
            status = MessageStatus.SENT,
            emailTransportStatus = "DISPATCHED_SMTP",
            emailSubject = "Linko ${if (isBroadcastChannel) "Channel" else "Group"}: $resolvedName",
            emailMessageId = "<group-${UUID.randomUUID().toString().take(6)}@linko.net>",
            isEncrypted = true,
            encryptionAlgorithm = "AES-256-GCM"
        )
        messageDao.insertMessage(systemMessage)
        FirestoreMessageService.uploadMessage(systemMessage, participantIds)

        return newGroup
    }

    suspend fun sendMessage(
        chatId: String,
        sender: UserEntity,
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        formattedRichText: String? = null,
        callType: String? = null
    ) {
        if (isPeerOrChatBlocked(sender, chatId = chatId)) {
            return
        }
        val msgId = "m_${UUID.randomUUID().toString().take(8)}"
        val chat = chatDao.getChatById(chatId)
        val isCallMsg = !callType.isNullOrBlank() ||
            mediaType?.equals("CALL", ignoreCase = true) == true ||
            text.contains("meet.google.com", ignoreCase = true)
        val callLabel = if (callType.equals("AUDIO", ignoreCase = true) ||
            text.contains("Audio Call", ignoreCase = true) ||
            text.contains("Voice Call", ignoreCase = true)
        ) {
            "Voice Call"
        } else {
            "Video Call"
        }
        val subject = when {
            isCallMsg -> "Linko $callLabel from ${sender.displayName}"
            chat?.isGroup == true -> "Group [${chat.name}]: ${text.take(25)}"
            else -> "Linko Message from ${sender.displayName}"
        }
        val emailMsgId = "<msg-${UUID.randomUUID().toString().take(8)}@linko.net>"

        val encryptedText = EncryptionManager.encrypt(text, chatId)

        // Stage 1: Newly sent message starts strictly with status = 'sent' (Single Gray Tick)
        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = sender.id,
            senderName = sender.displayName,
            senderEmail = sender.email,
            text = encryptedText,
            mediaUri = mediaUri,
            mediaType = mediaType,
            callType = callType,
            formattedRichText = formattedRichText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            deliveredAt = null,
            readAt = null,
            emailTransportStatus = "DISPATCHED_SMTP",
            emailSubject = subject,
            emailMessageId = emailMsgId,
            isEncrypted = true,
            encryptionAlgorithm = "AES-256-GCM"
        )

        messageDao.insertMessage(message)
        val previewText = formatChatPreviewText(text = text, mediaType = mediaType, callType = callType)
        chatDao.updateLastMessage(chatId, previewText, System.currentTimeMillis())

        val isGeminiChat = chatId == "chat_gemini_ai" || chat?.name?.contains("Gemini", ignoreCase = true) == true
        val participants = parseJsonArray(chat?.participantIdsJson ?: "")

        if (!isGeminiChat) {
            FirestoreMessageService.uploadMessage(
                message = message,
                participants = participants,
                onServerSentAck = {
                    ioScope.launch {
                        messageDao.updateMessageReceipt(msgId, MessageStatus.SENT, null, null)
                    }
                }
            )
            FirestoreGroupService.updateGroupLastMessage(chatId, previewText, System.currentTimeMillis())
        } else {
            // Automated Gemini AI assistant conversation: activate typing state immediately
            setGeminiTyping(chatId, true)
            ioScope.launch {
                try {
                    // Mark user's message as delivered & read asynchronously without delaying Gemini inference
                    launch {
                        val now = System.currentTimeMillis()
                        messageDao.markMessageDelivered(msgId, now)
                        messageDao.updateEmailTransportStatus(msgId, "DELIVERED_INBOX")
                        messageDao.markSingleMessageAsRead(msgId, now)
                    }

                    // Build multi-turn conversation history from existing messages in chat_gemini_ai
                    val allGeminiMsgs = messageDao.getMessagesForChatList(chatId)
                    val historyTurns = allGeminiMsgs
                        .filter { it.id != msgId && it.id != "m_gemini_welcome" }
                        .mapNotNull { m ->
                            val plain = decodeStoredMessageText(m.text, chatId).trim()
                            if (plain.isBlank() || plain.startsWith("Gemini AI Assistant Error")) {
                                null
                            } else {
                                GeminiChatTurn(
                                    isFromUser = m.senderId != "user_gemini",
                                    text = plain
                                )
                            }
                        }

                    // Include quoted reply snippet if user replied to a specific message
                    val replyContextPrefix = if (!formattedRichText.isNullOrBlank() && formattedRichText.startsWith("REPLY:")) {
                        val replyMeta = formattedRichText.substringAfter("REPLY:").substringBefore(";")
                        val quotedSender = replyMeta.substringBefore("|")
                        val quotedSnippet = replyMeta.substringAfter("|", "")
                        if (quotedSnippet.isNotBlank()) "[Replying to $quotedSender: \"$quotedSnippet\"]\n" else ""
                    } else ""

                    val basePrompt = when {
                        text.isNotBlank() -> replyContextPrefix + text
                        mediaType == "IMAGE" -> "Analyze this image and describe its key elements in detail."
                        mediaType == "AUDIO" -> "The user sent a voice note (${text.ifBlank { "audio clip" }}). Acknowledge the voice note and offer helpful assistance."
                        else -> "Analyze the attached item and help the user."
                    }

                    val replyMsgId = "m_gemini_${UUID.randomUUID().toString().take(8)}"
                    val replyStartTimestamp = System.currentTimeMillis()
                    var lastStreamPersistMs = 0L
                    var hasEmittedFirstChunk = false

                    val aiResponse = GeminiClient.generateResponse(
                        prompt = basePrompt,
                        mediaUri = mediaUri,
                        history = historyTurns,
                        onStreamStart = {
                            // Clear typing indicator the exact moment streaming begins
                            setGeminiTyping(chatId, false)
                        },
                        onStreamingUpdate = { partialText ->
                            val nowMs = System.currentTimeMillis()
                            if (partialText.isNotBlank() && (!hasEmittedFirstChunk || (nowMs - lastStreamPersistMs) >= 45L)) {
                                hasEmittedFirstChunk = true
                                lastStreamPersistMs = nowMs
                                // Ensure typing bubble is hidden the exact moment the first streaming chunk is persisted
                                setGeminiTyping(chatId, false)
                                val streamingEntity = MessageEntity(
                                    id = replyMsgId,
                                    chatId = chatId,
                                    senderId = "user_gemini",
                                    senderName = "Gemini AI Assistant",
                                    senderEmail = "gemini.ai@linko.com",
                                    text = EncryptionManager.encrypt(partialText, chatId),
                                    timestamp = replyStartTimestamp,
                                    status = MessageStatus.READ,
                                    deliveredAt = replyStartTimestamp,
                                    readAt = nowMs,
                                    emailTransportStatus = "DELIVERED_INBOX",
                                    emailSubject = "Gemini AI Response",
                                    emailMessageId = "<gemini-$replyMsgId@linko.net>",
                                    isEncrypted = true,
                                    encryptionAlgorithm = "AES-256-GCM"
                                )
                                messageDao.insertMessage(streamingEntity)
                            }
                        }
                    )

                    setGeminiTyping(chatId, false)
                    val encryptedAiResponse = EncryptionManager.encrypt(aiResponse, chatId)
                    val replyNow = System.currentTimeMillis()
                    val replyMsg = MessageEntity(
                        id = replyMsgId,
                        chatId = chatId,
                        senderId = "user_gemini",
                        senderName = "Gemini AI Assistant",
                        senderEmail = "gemini.ai@linko.com",
                        text = encryptedAiResponse,
                        timestamp = replyStartTimestamp,
                        status = MessageStatus.READ,
                        deliveredAt = replyStartTimestamp,
                        readAt = replyNow,
                        emailTransportStatus = "DELIVERED_INBOX",
                        emailSubject = "Gemini AI Response",
                        emailMessageId = "<gemini-${UUID.randomUUID().toString().take(6)}@linko.net>",
                        isEncrypted = true,
                        encryptionAlgorithm = "AES-256-GCM"
                    )
                    messageDao.insertMessage(replyMsg)
                    chatDao.updateLastMessage(
                        chatId,
                        aiResponse.replace("\n", " ").take(65),
                        replyNow
                    )
                } finally {
                    setGeminiTyping(chatId, false)
                }
            }
        }
    }

    /**
     * Stage 3: Marks ONLY the messages that are currently visible on the recipient's screen as 'read'.
     */
    suspend fun markVisibleMessagesAsRead(
        chatId: String,
        currentUserId: String,
        currentUserEmail: String = "",
        visibleMessageIds: List<String>
    ) {
        if (visibleMessageIds.isEmpty()) return
        val now = System.currentTimeMillis()
        messageDao.markMessagesAsReadByIds(visibleMessageIds, now)
        chatDao.clearUnreadCount(chatId)
        FirestoreMessageService.markVisibleMessagesRead(
            chatId = chatId,
            visibleMessageIds = visibleMessageIds,
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail
        ) { readTimestamp ->
            ioScope.launch {
                messageDao.markMessagesAsReadByIds(visibleMessageIds, readTimestamp)
            }
        }
    }

    suspend fun markChatMessagesAsRead(chatId: String, currentUserId: String, currentUserEmail: String = "") {
        val now = System.currentTimeMillis()
        val unreadList = messageDao.getUnreadMessagesForChat(chatId)
        val unreadIds = unreadList.filter { it.senderId != currentUserId }.map { it.id }
        messageDao.markUnreadMessagesAsRead(chatId, currentUserId, now)
        chatDao.clearUnreadCount(chatId)
        FirestoreMessageService.markChatMessagesRead(
            chatId = chatId,
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            messageIds = unreadIds
        ) { readTimestamp ->
            ioScope.launch {
                messageDao.markUnreadMessagesAsRead(chatId, currentUserId, readTimestamp)
            }
        }
    }

    fun startChatReceiptSync(chatId: String, currentUserId: String, currentUserEmail: String = "") {
        if (chatId == "chat_gemini_ai") return
        FirestoreMessageService.startListeningToChatReceipts(
            chatId = chatId,
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            scope = ioScope,
            onIncomingMessageSynced = { incomingMessage ->
                val meUser = userDao.getCurrentUser()
                if (meUser != null && isPeerOrChatBlocked(
                        me = meUser,
                        chatId = incomingMessage.chatId,
                        peerUserId = incomingMessage.senderId,
                        peerEmail = incomingMessage.senderEmail
                    )
                ) {
                    return@startListeningToChatReceipts
                }
                val existing = messageDao.getMessageById(incomingMessage.id)
                if (existing == null) {
                    val plainText = decodeStoredMessageText(incomingMessage.text, incomingMessage.chatId)
                    val targetEncrypted = EncryptionManager.encrypt(plainText, chatId)
                    messageDao.insertMessage(incomingMessage.copy(chatId = chatId, text = targetEncrypted))
                } else {
                    messageDao.updateMessageReceipt(
                        incomingMessage.id,
                        incomingMessage.status,
                        incomingMessage.deliveredAt,
                        incomingMessage.readAt
                    )
                }
            },
            onReceiptUpdated = { msgId, status, deliveredAt, readAt ->
                messageDao.updateMessageReceipt(msgId, status, deliveredAt, readAt)
                if (MessageStatus.normalize(status) != MessageStatus.SENT) {
                    messageDao.updateEmailTransportStatus(msgId, "DELIVERED_INBOX")
                }
            }
        )
    }

    fun syncMessagesWithFirestore(
        user: UserEntity,
        onNewIncomingMessageAlert: ((MessageEntity, String, Boolean) -> Unit)? = null
    ) {
        if (user.id.isBlank() && user.email.isBlank()) return
        FirestoreMessageService.startGlobalMessageSync(
            currentUserId = user.id,
            currentUserEmail = user.email,
            scope = ioScope,
            onIncomingMessageReceived = { incomingMessage ->
                if (isPeerOrChatBlocked(
                        me = user,
                        chatId = incomingMessage.chatId,
                        peerUserId = incomingMessage.senderId,
                        peerEmail = incomingMessage.senderEmail
                    )
                ) {
                    return@startGlobalMessageSync
                }
                val existing = messageDao.getMessageById(incomingMessage.id)
                if (existing == null) {
                    val plainDecrypted = decodeStoredMessageText(incomingMessage.text, incomingMessage.chatId)
                    val formattedPreview = formatChatPreviewText(
                        text = plainDecrypted,
                        mediaType = incomingMessage.mediaType,
                        callType = incomingMessage.callType
                    )
                    val existingChat = chatDao.getChatById(incomingMessage.chatId)
                    val isGroupChat = existingChat?.isGroup == true ||
                        incomingMessage.chatId.startsWith("group_") ||
                        incomingMessage.chatId.startsWith("channel_")

                    if (isGroupChat || incomingMessage.chatId == "chat_gemini_ai") {
                        if (existingChat == null) {
                            chatDao.insertOrUpdateChat(
                                ChatEntity(
                                    id = incomingMessage.chatId,
                                    isGroup = isGroupChat,
                                    name = incomingMessage.senderName.ifBlank { "Group Chat" },
                                    participantIdsJson = toJsonArray(listOf(user.id, incomingMessage.senderId).distinct()),
                                    lastMessageText = formattedPreview.take(60),
                                    lastMessageTimestamp = incomingMessage.timestamp,
                                    unreadCount = 1
                                )
                            )
                        } else {
                            chatDao.updateLastMessage(
                                incomingMessage.chatId,
                                formattedPreview.take(60),
                                incomingMessage.timestamp
                            )
                        }
                        val encryptedGroupMsg = incomingMessage.copy(
                            text = EncryptionManager.encrypt(plainDecrypted, incomingMessage.chatId)
                        )
                        messageDao.insertMessage(encryptedGroupMsg)
                        onNewIncomingMessageAlert?.invoke(encryptedGroupMsg, formattedPreview, isGroupChat)
                    } else {
                        val myCanonicalId = canonicalUserIdFromEmail(user.email, user.id)
                        val isSentByMe = incomingMessage.senderId == user.id ||
                            incomingMessage.senderId == myCanonicalId ||
                            (user.email.isNotBlank() && incomingMessage.senderEmail.equals(user.email, ignoreCase = true))

                        val unifiedChat = if (!isSentByMe) {
                            resolveAndUnifyDirectChat(
                                me = user,
                                peerUserId = incomingMessage.senderId,
                                peerEmail = incomingMessage.senderEmail,
                                peerDisplayName = incomingMessage.senderName,
                                legacyChatIdHint = incomingMessage.chatId,
                                incomingPreview = formattedPreview.take(60),
                                incomingTimestamp = incomingMessage.timestamp,
                                incrementUnread = true
                            )
                        } else {
                            existingChat ?: chatDao.getAllChatsList().firstOrNull { !it.isGroup && it.id == incomingMessage.chatId }
                        }

                        val targetChatId = unifiedChat?.id ?: incomingMessage.chatId
                        val resolvedSenderName = if (!isSentByMe && unifiedChat != null && !isGenericPlaceholderName(unifiedChat.name, user.displayName, user.email)) {
                            unifiedChat.name
                        } else {
                            incomingMessage.senderName
                        }
                        val encryptedForTarget = EncryptionManager.encrypt(plainDecrypted, targetChatId)
                        val unifiedMessage = incomingMessage.copy(
                            chatId = targetChatId,
                            senderName = resolvedSenderName,
                            text = encryptedForTarget
                        )
                        messageDao.insertMessage(unifiedMessage)
                        if (!isSentByMe) {
                            onNewIncomingMessageAlert?.invoke(unifiedMessage, formattedPreview, false)
                        }
                    }
                } else {
                    messageDao.updateMessageReceipt(
                        incomingMessage.id,
                        incomingMessage.status,
                        incomingMessage.deliveredAt,
                        incomingMessage.readAt
                    )
                }
            },
            onReceiptUpdated = { msgId, status, deliveredAt, readAt ->
                messageDao.updateMessageReceipt(msgId, status, deliveredAt, readAt)
                if (MessageStatus.normalize(status) != MessageStatus.SENT) {
                    messageDao.updateEmailTransportStatus(msgId, "DELIVERED_INBOX")
                }
            }
        )
    }

    suspend fun ensureChatForNotification(
        chatId: String,
        senderName: String,
        senderEmail: String,
        messageText: String,
        isGroup: Boolean,
        currentUser: UserEntity?
    ): ChatEntity {
        val now = System.currentTimeMillis()
        val isGroupChat = isGroup || chatId.startsWith("group_") || chatId.startsWith("channel_")

        val chat = if (!isGroupChat && chatId != "chat_gemini_ai" && currentUser != null) {
            resolveAndUnifyDirectChat(
                me = currentUser,
                peerUserId = canonicalUserIdFromEmail(senderEmail),
                peerEmail = senderEmail,
                peerDisplayName = senderName,
                legacyChatIdHint = chatId,
                incomingPreview = messageText.takeIf { it.isNotBlank() }?.take(60),
                incomingTimestamp = now,
                incrementUnread = false
            )
        } else {
            val existing = chatDao.getChatById(chatId)
            val myId = currentUser?.id ?: "user_local"
            val resolvedName = senderName.ifBlank { existing?.name ?: "Linko Chat" }
            if (existing != null) {
                existing
            } else {
                val created = ChatEntity(
                    id = chatId,
                    isGroup = isGroupChat,
                    name = resolvedName,
                    participantIdsJson = "[\"$myId\"]",
                    lastMessageText = messageText.ifBlank { "Conversation opened from notification" },
                    lastMessageTimestamp = now
                )
                chatDao.insertOrUpdateChat(created)
                created
            }
        }

        val targetChatId = chat.id
        if (messageText.isNotBlank()) {
            val existingMsgs = messageDao.getMessagesForChatList(targetChatId)
            val alreadyExists = existingMsgs.any {
                EncryptionManager.decrypt(it.text, targetChatId) == messageText
            }
            if (!alreadyExists) {
                val resolvedSenderName = chat.name.ifBlank { senderName.ifBlank { "Linko Contact" } }
                val resolvedSenderEmail = senderEmail.ifBlank { "${resolvedSenderName.lowercase().replace(" ", ".")}@linko.com" }
                val msgEntity = MessageEntity(
                    id = "m_notif_${UUID.randomUUID().toString().take(8)}",
                    chatId = targetChatId,
                    senderId = canonicalUserIdFromEmail(resolvedSenderEmail),
                    senderName = resolvedSenderName,
                    senderEmail = resolvedSenderEmail,
                    text = EncryptionManager.encrypt(messageText, targetChatId),
                    timestamp = now,
                    status = MessageStatus.DELIVERED,
                    deliveredAt = now,
                    isEncrypted = true,
                    encryptionAlgorithm = "AES-256-GCM"
                )
                messageDao.insertMessage(msgEntity)
                chatDao.updateLastMessage(targetChatId, messageText.take(60), now)
            }
        }
        return chat
    }

    /**
     * Resolves a single unified direct chat thread between [me] and the given peer, merging any
     * duplicate or legacy threads (such as threads named "Contact" vs the user's real display name)
     * into one canonical ChatEntity with a deterministic order-independent ID.
     */
    suspend fun resolveAndUnifyDirectChat(
        me: UserEntity,
        peerUserId: String,
        peerEmail: String,
        peerDisplayName: String,
        peerAvatarUri: String? = null,
        legacyChatIdHint: String? = null,
        incomingPreview: String? = null,
        incomingTimestamp: Long? = null,
        incrementUnread: Boolean = false
    ): ChatEntity {
        if (peerUserId == "user_gemini" ||
            peerEmail.contains("gemini", ignoreCase = true) ||
            peerDisplayName.contains("Gemini", ignoreCase = true) ||
            legacyChatIdHint == "chat_gemini_ai"
        ) {
            return createOrGetGeminiChat(me)
        }
        val myEmailClean = me.email.lowercase().trim()
        val myCanonicalId = canonicalUserIdFromEmail(myEmailClean, me.id)
        val myContacts = if (myEmailClean.isNotBlank()) {
            contactDao.getContactsList(myEmailClean)
        } else {
            emptyList()
        }

        val matchedContact = findMatchingContactForPeer(
            contacts = myContacts,
            peerUserId = peerUserId,
            peerEmail = peerEmail,
            peerDisplayName = peerDisplayName,
            me = me
        )

        val resolvedPeerEmail = matchedContact?.contactEmail?.lowercase()?.trim()?.takeIf { it.isNotBlank() }
            ?: peerEmail.lowercase().trim()
        val resolvedPeerUserId = canonicalUserIdFromEmail(
            resolvedPeerEmail,
            matchedContact?.contactUserId?.takeIf { it.isNotBlank() } ?: peerUserId
        )

        val canonicalChatId = generateDirectChatId(
            user1Id = me.id,
            user1Email = myEmailClean,
            user2Id = resolvedPeerUserId,
            user2Email = resolvedPeerEmail
        )

        // If peer wasn't in local contacts yet but we have their email or name, auto-save/update contact so name is preserved
        if (matchedContact == null && resolvedPeerEmail.isNotBlank() && resolvedPeerEmail != myEmailClean && myEmailClean.isNotBlank()) {
            val cleanDisplay = if (!isGenericPlaceholderName(peerDisplayName, me.displayName, myEmailClean)) {
                peerDisplayName.trim()
            } else {
                resolvedPeerEmail.substringBefore("@").replace(".", " ").capitalizeWords()
            }
            val autoContact = ContactEntity(
                id = "c_${canonicalParticipantKey(resolvedPeerEmail)}",
                userEmail = myEmailClean,
                contactUserId = resolvedPeerUserId,
                contactEmail = resolvedPeerEmail,
                contactDisplayName = cleanDisplay,
                contactUsername = resolvedPeerEmail.substringBefore("@"),
                contactAvatarUri = peerAvatarUri,
                contactStatus = "Connected on Linko"
            )
            contactDao.insertContact(autoContact)
        } else if (matchedContact != null && !isGenericPlaceholderName(peerDisplayName, me.displayName, myEmailClean) &&
            isGenericPlaceholderName(matchedContact.contactDisplayName, me.displayName, myEmailClean)
        ) {
            contactDao.insertContact(matchedContact.copy(contactDisplayName = peerDisplayName.trim(), contactUserId = resolvedPeerUserId))
        }

        val allChats = chatDao.getAllChatsList()
        val peerCandidateTokens = setOf(
            peerUserId.lowercase().trim(),
            resolvedPeerUserId.lowercase().trim(),
            resolvedPeerEmail,
            matchedContact?.contactUserId?.lowercase()?.trim() ?: "",
            canonicalParticipantKey(resolvedPeerEmail.ifBlank { resolvedPeerUserId })
        ).filter { it.isNotBlank() && it != "unknown" }.toSet()

        val myTokens = setOf(
            me.id.lowercase().trim(),
            myCanonicalId.lowercase().trim(),
            myEmailClean
        ).filter { it.isNotBlank() }.toSet()

        val otherContactsTokens = myContacts
            .filter { it.id != matchedContact?.id }
            .flatMap { c ->
                listOf(
                    c.contactUserId.lowercase().trim(),
                    c.contactEmail.lowercase().trim(),
                    canonicalUserIdFromEmail(c.contactEmail)
                )
            }
            .filter { it.isNotBlank() }
            .toSet()

        val matchingChats = allChats.filter { chat ->
            if (chat.isGroup || chat.id == "chat_gemini_ai") return@filter false
            if (chat.id == canonicalChatId) return@filter true
            if (!legacyChatIdHint.isNullOrBlank() && chat.id == legacyChatIdHint) return@filter true

            val chatParticipants = parseJsonArray(chat.participantIdsJson).map { it.lowercase().trim() }
            val nonMeParticipants = chatParticipants.filter { it !in myTokens }
            val hasPeerParticipant = nonMeParticipants.any { token ->
                token in peerCandidateTokens || canonicalParticipantKey(token) in peerCandidateTokens
            }
            if (hasPeerParticipant) return@filter true

            val chatNameClean = chat.name.trim()
            val matchesName = !isGenericPlaceholderName(chatNameClean, me.displayName, myEmailClean) && (
                (matchedContact != null && chatNameClean.equals(matchedContact.contactDisplayName.trim(), ignoreCase = true)) ||
                (!isGenericPlaceholderName(peerDisplayName, me.displayName, myEmailClean) && chatNameClean.equals(peerDisplayName.trim(), ignoreCase = true)) ||
                (resolvedPeerEmail.isNotBlank() && chatNameClean.equals(resolvedPeerEmail, ignoreCase = true))
            )
            if (matchesName) return@filter true

            // Also catch orphan "Contact" / "Linko Contact" threads that do not belong to any other known contact
            if (isGenericPlaceholderName(chatNameClean, me.displayName, myEmailClean)) {
                val belongsToAnotherContact = nonMeParticipants.any { it in otherContactsTokens }
                if (!belongsToAnotherContact && (myContacts.size <= 1 || nonMeParticipants.isEmpty() || nonMeParticipants.all { it.startsWith("user_") })) {
                    return@filter true
                }
            }
            false
        }

        val bestName = sequenceOf(
            matchedContact?.contactDisplayName,
            peerDisplayName,
            matchingChats.map { it.name }.firstOrNull { !isGenericPlaceholderName(it, me.displayName, myEmailClean) },
            resolvedPeerEmail.takeIf { it.isNotBlank() }?.substringBefore("@")?.replace(".", " ")?.capitalizeWords()
        ).firstOrNull { !isGenericPlaceholderName(it, me.displayName, myEmailClean) }?.trim()
            ?: "Linko Contact"

        val bestAvatar = peerAvatarUri
            ?: matchedContact?.contactAvatarUri
            ?: matchingChats.firstNotNullOfOrNull { it.groupIconUri }

        // Migrate messages from any duplicate chat IDs into canonicalChatId and delete duplicate ChatEntity rows
        for (dupChat in matchingChats) {
            if (dupChat.id != canonicalChatId) {
                migrateChatMessagesToUnifiedChat(dupChat.id, canonicalChatId)
                chatDao.deleteChatById(dupChat.id)
            }
        }

        val mostRecentExisting = matchingChats.maxByOrNull { it.lastMessageTimestamp }
        val finalTimestamp = when {
            incomingTimestamp != null && mostRecentExisting != null -> maxOf(incomingTimestamp, mostRecentExisting.lastMessageTimestamp)
            incomingTimestamp != null -> incomingTimestamp
            mostRecentExisting != null -> mostRecentExisting.lastMessageTimestamp
            else -> System.currentTimeMillis()
        }
        val finalLastMessage = when {
            incomingPreview != null && (mostRecentExisting == null || (incomingTimestamp ?: 0L) >= mostRecentExisting.lastMessageTimestamp) -> incomingPreview
            mostRecentExisting != null && mostRecentExisting.lastMessageText.isNotBlank() -> mostRecentExisting.lastMessageText
            incomingPreview != null -> incomingPreview
            else -> "Chat started"
        }
        val baseUnread = matchingChats.maxOfOrNull { it.unreadCount } ?: 0
        val finalUnread = if (incrementUnread) baseUnread + 1 else baseUnread

        val combinedParticipants = buildList {
            add(me.id)
            add(myCanonicalId)
            if (myEmailClean.isNotBlank()) add(myEmailClean)
            add(resolvedPeerUserId)
            if (peerUserId.isNotBlank()) add(peerUserId)
            if (matchedContact != null && matchedContact.contactUserId.isNotBlank()) add(matchedContact.contactUserId)
            if (resolvedPeerEmail.isNotBlank()) add(resolvedPeerEmail)
        }.filter { it.isNotBlank() }.distinct()

        val unifiedChat = ChatEntity(
            id = canonicalChatId,
            isGroup = false,
            name = bestName,
            groupIconUri = bestAvatar,
            participantIdsJson = toJsonArray(combinedParticipants),
            lastMessageText = finalLastMessage,
            lastMessageTimestamp = finalTimestamp,
            unreadCount = finalUnread
        )
        chatDao.insertOrUpdateChat(unifiedChat)
        return unifiedChat
    }

    private suspend fun migrateChatMessagesToUnifiedChat(oldChatId: String, newChatId: String) {
        if (oldChatId == newChatId) return
        val oldMessages = messageDao.getMessagesForChatList(oldChatId)
        for (msg in oldMessages) {
            val plain = EncryptionManager.decrypt(msg.text, oldChatId)
            val reEncrypted = EncryptionManager.encrypt(plain, newChatId)
            messageDao.insertMessage(msg.copy(chatId = newChatId, text = reEncrypted))
        }
    }

    suspend fun deduplicateAndUnifyDirectChats(me: UserEntity) {
        try {
            val myEmailClean = me.email.lowercase().trim()
            val myContacts = if (myEmailClean.isNotBlank()) contactDao.getContactsList(myEmailClean) else emptyList()
            for (contact in myContacts) {
                if (contact.contactUserId == "user_gemini" || contact.contactEmail.contains("gemini")) continue
                val allChats = chatDao.getAllChatsList()
                val canonicalId = generateDirectChatId(me.id, myEmailClean, contact.contactUserId, contact.contactEmail)
                val hasAnyThreadForContact = allChats.any { chat ->
                    !chat.isGroup && chat.id != "chat_gemini_ai" && (
                        chat.id == canonicalId ||
                        chat.name.equals(contact.contactDisplayName, ignoreCase = true) ||
                        parseJsonArray(chat.participantIdsJson).any {
                            it.equals(contact.contactUserId, ignoreCase = true) ||
                            it.equals(contact.contactEmail, ignoreCase = true) ||
                            canonicalParticipantKey(it) == canonicalParticipantKey(contact.contactEmail.ifBlank { contact.contactUserId })
                        } ||
                        isGenericPlaceholderName(chat.name, me.displayName, myEmailClean)
                    )
                }
                if (hasAnyThreadForContact) {
                    resolveAndUnifyDirectChat(
                        me = me,
                        peerUserId = contact.contactUserId,
                        peerEmail = contact.contactEmail,
                        peerDisplayName = contact.contactDisplayName,
                        peerAvatarUri = contact.contactAvatarUri
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun findMatchingContactForPeer(
        contacts: List<ContactEntity>,
        peerUserId: String,
        peerEmail: String,
        peerDisplayName: String,
        me: UserEntity?
    ): ContactEntity? {
        val cleanEmail = peerEmail.lowercase().trim()
        val cleanId = peerUserId.lowercase().trim()
        val peerCanonical = canonicalParticipantKey(cleanEmail.ifBlank { cleanId })

        if (cleanEmail.isNotBlank()) {
            contacts.find { it.contactEmail.equals(cleanEmail, ignoreCase = true) }?.let { return it }
        }
        if (cleanId.isNotBlank()) {
            contacts.find {
                it.contactUserId.equals(cleanId, ignoreCase = true) ||
                canonicalParticipantKey(it.contactEmail.ifBlank { it.contactUserId }) == peerCanonical
            }?.let { return it }
        }
        if (!isGenericPlaceholderName(peerDisplayName, me?.displayName, me?.email)) {
            contacts.find { it.contactDisplayName.equals(peerDisplayName.trim(), ignoreCase = true) }?.let { return it }
        }
        val nonGeminiContacts = contacts.filter { it.contactUserId != "user_gemini" && !it.contactEmail.contains("gemini") }
        if (nonGeminiContacts.size == 1 && isGenericPlaceholderName(peerDisplayName, me?.displayName, me?.email)) {
            return nonGeminiContacts.first()
        }
        return null
    }

    private fun deduplicateChatsList(
        chatList: List<ChatEntity>,
        me: UserEntity?,
        contacts: List<ContactEntity>
    ): List<ChatEntity> {
        if (chatList.isEmpty()) return emptyList()
        val myId = me?.id?.lowercase()?.trim() ?: ""
        val myEmail = me?.email?.lowercase()?.trim() ?: ""
        val myCanonical = if (myEmail.isNotBlank()) canonicalUserIdFromEmail(myEmail, myId) else myId

        return chatList
            .groupBy { chat ->
                if (chat.isGroup || chat.id == "chat_gemini_ai") {
                    chat.id
                } else {
                    val participants = parseJsonArray(chat.participantIdsJson)
                        .map { it.lowercase().trim() }
                        .filter { it.isNotBlank() && it != myId && it != myEmail && it != myCanonical }

                    val matchedContact = contacts.find { c ->
                        val cEmail = c.contactEmail.lowercase().trim()
                        val cId = c.contactUserId.lowercase().trim()
                        val cKey = canonicalParticipantKey(cEmail.ifBlank { cId })
                        participants.any { p -> p == cId || p == cEmail || canonicalParticipantKey(p) == cKey } ||
                            generateDirectChatId(myId, myEmail, cId, cEmail) == chat.id ||
                            (!isGenericPlaceholderName(chat.name, me?.displayName, myEmail) &&
                                c.contactDisplayName.equals(chat.name.trim(), ignoreCase = true))
                    } ?: run {
                        val nonGemini = contacts.filter { it.contactUserId != "user_gemini" && !it.contactEmail.contains("gemini") }
                        if (nonGemini.size == 1 && isGenericPlaceholderName(chat.name, me?.displayName, myEmail)) {
                            nonGemini.first()
                        } else null
                    }

                    when {
                        matchedContact != null -> {
                            "direct_" + canonicalParticipantKey(matchedContact.contactEmail.ifBlank { matchedContact.contactUserId })
                        }
                        participants.isNotEmpty() -> {
                            val emailToken = participants.firstOrNull { it.contains("@") }
                            "direct_" + canonicalParticipantKey(emailToken ?: participants.first())
                        }
                        else -> {
                            "direct_name_" + chat.name.lowercase().trim()
                        }
                    }
                }
            }
            .map { (_, duplicates) ->
                if (duplicates.size == 1) {
                    val single = duplicates.first()
                    if (!single.isGroup && single.id != "chat_gemini_ai") {
                        val matchedContact = findMatchingContactForPeer(
                            contacts = contacts,
                            peerUserId = parseJsonArray(single.participantIdsJson).firstOrNull { it != me?.id } ?: "",
                            peerEmail = parseJsonArray(single.participantIdsJson).firstOrNull { it.contains("@") && !it.equals(myEmail, ignoreCase = true) } ?: "",
                            peerDisplayName = single.name,
                            me = me
                        )
                        if (matchedContact != null && (isGenericPlaceholderName(single.name, me?.displayName, myEmail) || single.name.equals(me?.displayName, ignoreCase = true) || single.name.equals(myEmail, ignoreCase = true))) {
                            single.copy(
                                name = matchedContact.contactDisplayName,
                                groupIconUri = single.groupIconUri ?: matchedContact.contactAvatarUri
                            )
                        } else {
                            single
                        }
                    } else {
                        single
                    }
                } else {
                    val latest = duplicates.maxByOrNull { it.lastMessageTimestamp } ?: duplicates.first()
                    val bestNamed = duplicates.firstOrNull { !isGenericPlaceholderName(it.name, me?.displayName, myEmail) }
                    val canonicalIdCandidate = if (me != null) {
                        val matchedContact = bestNamed?.let {
                            findMatchingContactForPeer(contacts, "", "", it.name, me)
                        } ?: findMatchingContactForPeer(contacts, "", "", latest.name, me)
                        if (matchedContact != null) {
                            generateDirectChatId(me.id, myEmail, matchedContact.contactUserId, matchedContact.contactEmail)
                        } else null
                    } else null

                    val preferredChat = duplicates.find { it.id == canonicalIdCandidate }
                        ?: bestNamed
                        ?: latest

                    preferredChat.copy(
                        name = bestNamed?.name ?: preferredChat.name,
                        groupIconUri = bestNamed?.groupIconUri ?: latest.groupIconUri,
                        lastMessageText = latest.lastMessageText,
                        lastMessageTimestamp = latest.lastMessageTimestamp,
                        unreadCount = duplicates.maxOf { it.unreadCount }
                    )
                }
            }
            .sortedByDescending { it.lastMessageTimestamp }
    }

    fun stopChatReceiptSync(chatId: String) {
        FirestoreMessageService.stopListening(chatId)
    }

    suspend fun clearChatUnread(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    suspend fun addCallLog(
        contactName: String,
        contactEmail: String,
        contactAvatarUri: String?,
        callType: CallType,
        isIncoming: Boolean,
        isMissed: Boolean,
        durationSeconds: Int
    ) {
        val callLog = CallLogEntity(
            id = "call_${UUID.randomUUID().toString().take(8)}",
            contactName = contactName,
            contactEmail = contactEmail,
            contactAvatarUri = contactAvatarUri,
            callType = callType.name,
            isIncoming = isIncoming,
            isMissed = isMissed,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        callLogDao.insertCallLog(callLog)
    }

    suspend fun updateProfile(updatedUser: UserEntity) {
        userDao.updateUser(updatedUser)
    }

    suspend fun toggleMessageReaction(messageId: String, emoji: String, userId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val currentJson = message.reactionsJson.ifBlank { "{}" }
        val reactionsMap = mutableMapOf<String, MutableList<String>>()

        try {
            val json = JSONObject(currentJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = json.optJSONArray(key) ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                reactionsMap[key] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val userList = reactionsMap.getOrPut(emoji) { mutableListOf() }
        if (userList.contains(userId)) {
            userList.remove(userId)
        } else {
            userList.add(userId)
        }

        if (userList.isEmpty()) {
            reactionsMap.remove(emoji)
        }

        val resultJson = JSONObject()
        for ((key, list) in reactionsMap) {
            resultJson.put(key, JSONArray(list))
        }

        messageDao.updateMessageReactions(messageId, resultJson.toString())
    }

    suspend fun votePoll(messageId: String, optionIndex: Int, userId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val plainText = EncryptionManager.decrypt(message.text, message.chatId)
        try {
            val json = JSONObject(plainText)
            val optionsArray = json.getJSONArray("options")
            val allowMultiple = json.optBoolean("allowMultiple", false)

            for (i in 0 until optionsArray.length()) {
                val optObj = optionsArray.getJSONObject(i)
                val votesArray = optObj.optJSONArray("votes") ?: JSONArray()
                val votesList = mutableListOf<String>()
                for (j in 0 until votesArray.length()) {
                    votesList.add(votesArray.getString(j))
                }

                if (i == optionIndex) {
                    if (votesList.contains(userId)) {
                        votesList.remove(userId)
                    } else {
                        votesList.add(userId)
                    }
                } else if (!allowMultiple) {
                    votesList.remove(userId)
                }

                optObj.put("votes", JSONArray(votesList))
            }

            val updatedText = json.toString()
            val encryptedUpdatedText = EncryptionManager.encrypt(updatedText, message.chatId)
            val updatedMessage = message.copy(text = encryptedUpdatedText)
            messageDao.insertMessage(updatedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendPollMessage(
        chatId: String,
        sender: UserEntity,
        question: String,
        options: List<String>,
        allowMultiple: Boolean
    ) {
        val optionsArray = JSONArray()
        options.forEachIndexed { index, opt ->
            val optObj = JSONObject()
            optObj.put("id", index)
            optObj.put("text", opt)
            optObj.put("votes", JSONArray())
            optionsArray.put(optObj)
        }

        val pollObj = JSONObject()
        pollObj.put("question", question)
        pollObj.put("allowMultiple", allowMultiple)
        pollObj.put("options", optionsArray)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = pollObj.toString(),
            mediaType = "POLL"
        )
    }

    suspend fun sendLocationMessage(
        chatId: String,
        sender: UserEntity,
        title: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        val isLive = title.startsWith("🔴 Live Location", ignoreCase = true) ||
                title.contains("Live GPS", ignoreCase = true)
        val googleMapsUrl = com.example.data.remote.SharedMapLocation.buildGoogleMapsSearchUrl(
            latitude,
            longitude,
            title
        )
        val locObj = JSONObject()
        locObj.put("title", title)
        locObj.put("address", address)
        locObj.put("latitude", latitude)
        locObj.put("longitude", longitude)
        locObj.put("isLiveLocation", isLive)
        locObj.put("googleMapsUrl", googleMapsUrl)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = locObj.toString(),
            mediaType = "LOCATION"
        )
    }

    suspend fun sendContactMessage(
        chatId: String,
        sender: UserEntity,
        contactName: String,
        email: String,
        phone: String
    ) {
        val cObj = JSONObject()
        cObj.put("name", contactName)
        cObj.put("email", email)
        cObj.put("phone", phone)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = cObj.toString(),
            mediaType = "CONTACT"
        )
    }

    suspend fun sendEventMessage(
        chatId: String,
        sender: UserEntity,
        title: String,
        dateText: String,
        locationText: String
    ) {
        val evObj = JSONObject()
        evObj.put("title", title)
        evObj.put("dateText", dateText)
        evObj.put("location", locationText)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = evObj.toString(),
            mediaType = "EVENT"
        )
    }

    suspend fun addGroupMembers(chatId: String, newContacts: List<ContactEntity>, actor: UserEntity) {
        val chat = chatDao.getChatById(chatId) ?: return
        val participants = parseJsonArray(chat.participantIdsJson)
        newContacts.forEach {
            if (!participants.contains(it.contactUserId)) {
                participants.add(it.contactUserId)
            }
        }
        val names = newContacts.joinToString { it.contactDisplayName }
        val now = System.currentTimeMillis()
        val updated = chat.copy(
            participantIdsJson = toJsonArray(participants),
            lastMessageText = "${actor.displayName} invited $names",
            lastMessageTimestamp = now
        )
        chatDao.insertOrUpdateChat(updated)

        val channelType = if (chat.id.startsWith("channel_")) "CHANNEL" else "GROUP"
        FirestoreGroupService.inviteContactsToGroup(
            chat = updated,
            inviter = actor,
            invitedContacts = newContacts,
            channelType = channelType
        )

        val plainNotice = "👥 ${actor.displayName} invited $names to the conversation."
        val sysMsg = MessageEntity(
            id = "m_${UUID.randomUUID().toString().take(8)}",
            chatId = chatId,
            senderId = actor.id,
            senderName = "System",
            senderEmail = actor.email,
            text = EncryptionManager.encrypt(plainNotice, chatId),
            timestamp = now,
            isEncrypted = true,
            encryptionAlgorithm = "AES-256-GCM"
        )
        messageDao.insertMessage(sysMsg)
        FirestoreMessageService.uploadMessage(sysMsg, participants)
    }

    suspend fun toggleAdminRole(chatId: String, targetUserId: String, makeAdmin: Boolean) {
        val chat = chatDao.getChatById(chatId) ?: return
        val admins = parseJsonArray(chat.adminIdsJson)
        if (makeAdmin) {
            if (!admins.contains(targetUserId)) admins.add(targetUserId)
        } else {
            admins.remove(targetUserId)
        }
        val updated = chat.copy(adminIdsJson = toJsonArray(admins))
        chatDao.insertOrUpdateChat(updated)
        FirestoreGroupService.syncGroupMembership(updated)
    }

    suspend fun removeGroupMember(chatId: String, targetUserId: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        val participants = parseJsonArray(chat.participantIdsJson)
        val admins = parseJsonArray(chat.adminIdsJson)
        participants.remove(targetUserId)
        admins.remove(targetUserId)
        val updated = chat.copy(
            participantIdsJson = toJsonArray(participants),
            adminIdsJson = toJsonArray(admins)
        )
        chatDao.insertOrUpdateChat(updated)
        FirestoreGroupService.syncGroupMembership(updated)
    }

    private fun parseJsonArray(jsonStr: String): MutableList<String> {
        if (jsonStr.isBlank() || jsonStr == "[]") return mutableListOf()
        return jsonStr.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
            .toMutableList()
    }

    private fun formatChatPreviewText(text: String, mediaType: String?, callType: String?): String {
        val isCallMessage = !callType.isNullOrBlank() ||
            mediaType?.equals("CALL", ignoreCase = true) == true ||
            text.contains("meet.google.com", ignoreCase = true)
        if (isCallMessage) {
            val isAudio = callType.equals("AUDIO", ignoreCase = true) ||
                text.contains("Audio Call", ignoreCase = true) ||
                text.contains("Voice Call", ignoreCase = true)
            return if (isAudio) "📞 Voice Call" else "📹 Video Call"
        }
        return when (mediaType?.uppercase()) {
            "IMAGE" -> "📷 Photo"
            "AUDIO" -> "🎙️ Voice note (${text.ifBlank { "0:05" }})"
            "DOCUMENT" -> "📄 Attachment"
            "LOCATION" -> {
                val locTitle = try {
                    JSONObject(text).optString("title", "Location")
                } catch (_: Exception) {
                    "Location"
                }
                "📍 Google Maps: $locTitle"
            }
            else -> text
        }
    }

    private fun toJsonArray(list: List<String>): String {
        if (list.isEmpty()) return "[]"
        return list.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")
    }
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.ActiveCallState
import com.example.data.model.AuthState
import com.example.data.model.CallType
import com.example.data.model.UserStatusGroup
import com.example.data.repository.LinkoRepository
import com.example.util.LinkoCallSignalingEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class PeerProfileInfo(
    val peerId: String = "",
    val displayName: String = "",
    val avatarUri: String? = null,
    val email: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LinkoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = LinkoRepository(
        userDao = db.userDao(),
        contactDao = db.contactDao(),
        chatDao = db.chatDao(),
        messageDao = db.messageDao(),
        callLogDao = db.callLogDao(),
        statusDao = db.statusDao()
    )

    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val authState: StateFlow<AuthState> = currentUser
        .map { user ->
            if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Unauthenticated)

    val chats: StateFlow<List<ChatEntity>> = repository.allChatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<MessageEntity>> = repository.allMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getContactsFlow(user.email) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStatuses: StateFlow<List<StatusEntity>> = repository.activeStatusesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStatusGroups: StateFlow<List<UserStatusGroup>> = combine(
        currentUser,
        contacts,
        activeStatuses
    ) { user, contactList, statuses ->
        if (statuses.isEmpty()) return@combine emptyList<UserStatusGroup>()

        val meId = user?.id ?: ""
        val groupedByUserId = statuses.groupBy { it.userId }

        groupedByUserId.map { (userId, userStatuses) ->
            val isMe = userId == meId || (user != null && userStatuses.firstOrNull()?.userEmail == user.email)
            val displayName = if (isMe) (user?.displayName ?: "My Status") else (userStatuses.firstOrNull()?.userDisplayName ?: "Friend")
            val email = if (isMe) (user?.email ?: "") else (userStatuses.firstOrNull()?.userEmail ?: "")
            val avatarUri = if (isMe) user?.avatarUri else (userStatuses.firstOrNull()?.userAvatarUri ?: contactList.find { it.contactEmail == email }?.contactAvatarUri)
            val sortedStatuses = userStatuses.sortedBy { it.createdTimestamp }
            val latestTime = sortedStatuses.lastOrNull()?.createdTimestamp ?: 0L
            val hasUnviewed = sortedStatuses.any { !it.isViewed }

            UserStatusGroup(
                userId = userId,
                userDisplayName = displayName,
                userEmail = email,
                userAvatarUri = avatarUri,
                isCurrentUser = isMe,
                statuses = sortedStatuses,
                latestTimestamp = latestTime,
                hasUnviewed = hasUnviewed
            )
        }.sortedWith(
            compareByDescending<UserStatusGroup> { it.isCurrentUser }
                .thenByDescending { it.hasUnviewed }
                .thenByDescending { it.latestTimestamp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    val currentChat: StateFlow<ChatEntity?> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId != null) repository.getChatByIdFlow(chatId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _cachedPeerProfiles = MutableStateFlow<Map<String, PeerProfileInfo>>(emptyMap())
    private val pendingPeerFetches = mutableSetOf<String>()

    private fun isSelfName(name: String, currentDisplayName: String, currentUserEmail: String): Boolean {
        val clean = name.trim().lowercase()
        if (clean.isBlank()) return false
        val cleanDisplay = currentDisplayName.trim().lowercase()
        val cleanEmail = currentUserEmail.trim().lowercase()
        val prefix = if (cleanEmail.contains("@")) cleanEmail.substringBefore("@") else ""
        return (cleanDisplay.isNotBlank() && clean == cleanDisplay) ||
               (cleanEmail.isNotBlank() && clean == cleanEmail) ||
               (prefix.isNotBlank() && clean == prefix)
    }

    private fun fetchRemotePeerProfile(peerId: String) {
        if (peerId.isBlank() || peerId == "user_gemini" || peerId in pendingPeerFetches) return
        pendingPeerFetches.add(peerId)
        viewModelScope.launch {
            try {
                val db = com.example.data.remote.FirestoreProvider.getInstance(getApplication()) ?: return@launch
                db.collection("users").document(peerId).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            val name = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                                ?: doc.getString("username")?.takeIf { it.isNotBlank() }
                            val avatar = doc.getString("avatarUri")?.takeIf { it.isNotBlank() }
                            val email = doc.getString("email")
                            if (!name.isNullOrBlank()) {
                                _cachedPeerProfiles.value = _cachedPeerProfiles.value + (peerId to PeerProfileInfo(
                                    peerId = peerId,
                                    displayName = name,
                                    avatarUri = avatar,
                                    email = email
                                ))
                            }
                        }
                    }
                if (peerId.contains("@")) {
                    db.collection("users").whereEqualTo("email", peerId.lowercase().trim()).limit(1).get()
                        .addOnSuccessListener { snap ->
                            val doc = snap.documents.firstOrNull()
                            if (doc != null && doc.exists()) {
                                val name = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                                    ?: doc.getString("username")?.takeIf { it.isNotBlank() }
                                val avatar = doc.getString("avatarUri")?.takeIf { it.isNotBlank() }
                                val email = doc.getString("email")
                                if (!name.isNullOrBlank()) {
                                    _cachedPeerProfiles.value = _cachedPeerProfiles.value + (peerId to PeerProfileInfo(
                                        peerId = peerId,
                                        displayName = name,
                                        avatarUri = avatar,
                                        email = email
                                    ))
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                android.util.Log.w("LinkoViewModel", "Could not fetch remote peer: ${e.message}")
            }
        }
    }

    fun getPeerIdForChat(chat: ChatEntity?): String? {
        if (chat == null || chat.isGroup) return null
        val currentUserId = currentUser.value?.id?.ifBlank { null }
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ""
        val currentUserEmail = currentUser.value?.email?.lowercase()?.trim()
            ?: FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
            ?: ""
        val participants = try {
            if (chat.participantIdsJson.isBlank()) emptyList<String>()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it).trim() }.filter { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
        return participants.firstOrNull { id ->
            val clean = id.lowercase().trim()
            clean != currentUserId.lowercase().trim() &&
            (currentUserEmail.isBlank() || clean != currentUserEmail)
        }
    }

    val currentPeerProfile: StateFlow<PeerProfileInfo?> = combine(
        currentChat,
        currentUser,
        contacts,
        _cachedPeerProfiles
    ) { chat, me, allContacts, cached ->
        if (chat == null || chat.isGroup) return@combine null

        val currentUserId = me?.id?.ifBlank { null }
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: ""
        val currentUserEmail = me?.email?.lowercase()?.trim()
            ?: FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
            ?: ""
        val currentDisplayName = me?.displayName?.trim() ?: ""

        val participants = try {
            if (chat.participantIdsJson.isBlank()) emptyList<String>()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it).trim() }.filter { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val peerId = participants.firstOrNull { id ->
            val cleanId = id.lowercase().trim()
            cleanId != currentUserId.lowercase().trim() &&
            (currentUserEmail.isBlank() || cleanId != currentUserEmail)
        } ?: ""

        if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini", ignoreCase = true) || peerId == "user_gemini") {
            return@combine PeerProfileInfo(
                peerId = "user_gemini",
                displayName = "Gemini AI",
                avatarUri = null,
                email = "gemini.ai@linko.com"
            )
        }

        cached[peerId]?.let { return@combine it }

        val contactMatch = if (peerId.isNotBlank()) {
            allContacts.find { c ->
                c.contactUserId.equals(peerId, ignoreCase = true) ||
                c.contactEmail.equals(peerId, ignoreCase = true) ||
                (peerId.contains("@") && c.contactEmail.contains(peerId, ignoreCase = true))
            }
        } else null

        if (contactMatch != null) {
            return@combine PeerProfileInfo(
                peerId = peerId,
                displayName = contactMatch.contactDisplayName,
                avatarUri = contactMatch.contactAvatarUri,
                email = contactMatch.contactEmail
            )
        }

        if (peerId.isNotBlank()) {
            fetchRemotePeerProfile(peerId)
        }

        val fallbackName = when {
            peerId.contains("@") -> {
                peerId.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
            }
            chat.name.isNotBlank() && !isSelfName(chat.name, currentDisplayName, currentUserEmail) && !LinkoRepository.isGenericPlaceholderName(chat.name, currentDisplayName, currentUserEmail) -> {
                chat.name
            }
            peerId.isNotBlank() -> {
                "User ${peerId.takeLast(4)}"
            }
            else -> "Contact"
        }

        PeerProfileInfo(
            peerId = peerId,
            displayName = fallbackName,
            avatarUri = chat.groupIconUri,
            email = if (peerId.contains("@")) peerId else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeMessages: StateFlow<List<MessageEntity>> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId != null) repository.getMessagesFlow(chatId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    private val prefs = application.getSharedPreferences("linko_settings", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedThemePalette = MutableStateFlow(
        try {
            com.example.ui.theme.AppThemePalette.valueOf(
                prefs.getString("theme_palette", com.example.ui.theme.AppThemePalette.EMERALD.name) ?: com.example.ui.theme.AppThemePalette.EMERALD.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.AppThemePalette.EMERALD
        }
    )
    val selectedThemePalette: StateFlow<com.example.ui.theme.AppThemePalette> = _selectedThemePalette.asStateFlow()

    private val _selectedChatWallpaper = MutableStateFlow(
        try {
            com.example.ui.theme.ChatWallpaper.valueOf(
                prefs.getString("chat_wallpaper", com.example.ui.theme.ChatWallpaper.DOODLE.name) ?: com.example.ui.theme.ChatWallpaper.DOODLE.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.ChatWallpaper.DOODLE
        }
    )
    val selectedChatWallpaper: StateFlow<com.example.ui.theme.ChatWallpaper> = _selectedChatWallpaper.asStateFlow()

    private val _customWallpaperUri = MutableStateFlow(prefs.getString("custom_wallpaper_uri", null))
    val customWallpaperUri: StateFlow<String?> = _customWallpaperUri.asStateFlow()

    private val _customWallpaperDimming = MutableStateFlow(prefs.getFloat("custom_wallpaper_dimming", 0.3f))
    val customWallpaperDimming: StateFlow<Float> = _customWallpaperDimming.asStateFlow()

    private val _customWallpaperScale = MutableStateFlow(prefs.getString("custom_wallpaper_scale", "CROP") ?: "CROP")
    val customWallpaperScale: StateFlow<String> = _customWallpaperScale.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        com.example.util.AppLanguage.fromCode(
            prefs.getString("app_language", com.example.util.AppLanguage.ENGLISH.code) ?: com.example.util.AppLanguage.ENGLISH.code
        )
    )
    val appLanguage: StateFlow<com.example.util.AppLanguage> = _appLanguage.asStateFlow()

    private val _translatedMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedMessages: StateFlow<Map<String, String>> = _translatedMessages.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    private var callTimerJob: Job? = null

    fun setAppLanguage(language: com.example.util.AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        _actionStatusMessage.value = "App language updated to ${language.nativeName}"
        translateActiveChatMessages()
    }

    fun translateActiveChatMessages() {
        val messages = activeMessages.value
        val lang = _appLanguage.value
        if (messages.isEmpty() || lang == com.example.util.AppLanguage.ENGLISH) {
            return
        }

        viewModelScope.launch {
            val currentMap = _translatedMessages.value.toMutableMap()
            for (msg in messages) {
                if (msg.text.isNotBlank()) {
                    val cacheKey = "${msg.id}_${lang.code}"
                    if (!currentMap.containsKey(cacheKey)) {
                        val translatedText = com.example.util.LocalizationManager.translateChatMessage(msg.text, lang)
                        currentMap[cacheKey] = translatedText
                        currentMap[msg.id] = translatedText
                    }
                }
            }
            _translatedMessages.value = currentMap
        }
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("is_dark_mode", newValue).apply()
    }

    fun setThemePalette(palette: com.example.ui.theme.AppThemePalette) {
        _selectedThemePalette.value = palette
        prefs.edit().putString("theme_palette", palette.name).apply()
        _actionStatusMessage.value = "Color theme updated to ${palette.displayName}"
    }

    fun setChatWallpaper(wallpaper: com.example.ui.theme.ChatWallpaper) {
        _selectedChatWallpaper.value = wallpaper
        prefs.edit().putString("chat_wallpaper", wallpaper.name).apply()
        _actionStatusMessage.value = "Chat wallpaper set to ${wallpaper.displayName}"
    }

    fun setCustomWallpaper(uri: String, dimming: Float = 0.3f, scale: String = "CROP") {
        _customWallpaperUri.value = uri
        _customWallpaperDimming.value = dimming
        _customWallpaperScale.value = scale
        prefs.edit()
            .putString("custom_wallpaper_uri", uri)
            .putFloat("custom_wallpaper_dimming", dimming)
            .putString("custom_wallpaper_scale", scale)
            .apply()
        setChatWallpaper(com.example.ui.theme.ChatWallpaper.CUSTOM)
        _actionStatusMessage.value = "Custom wallpaper applied!"
    }

    private val _onlineUserEmails = MutableStateFlow<Set<String>>(setOf("gemini_ai@google.com", "gemini.ai@google.com"))
    val onlineUserEmails: StateFlow<Set<String>> = _onlineUserEmails.asStateFlow()

    private val _userPresenceMap = MutableStateFlow<Map<String, com.example.data.remote.UserPresenceInfo>>(emptyMap())
    val userPresenceMap: StateFlow<Map<String, com.example.data.remote.UserPresenceInfo>> = _userPresenceMap.asStateFlow()

    private val _remoteTypingStatuses = MutableStateFlow<List<com.example.data.remote.TypingStatus>>(emptyList())
    private var lastTypingBroadcastTimeMs = 0L
    private var lastBroadcastTypingState = false
    private var lastBroadcastTypingChatId: String? = null

    val chatTypingMap: StateFlow<Map<String, String>> = combine(
        chats,
        currentUser,
        contacts,
        _remoteTypingStatuses
    ) { chatList, me, contactList, remoteTyping ->
        val myEmail = me?.email?.lowercase()?.trim() ?: ""
        val myId = me?.id?.lowercase()?.trim() ?: ""
        val now = System.currentTimeMillis()
        val activeRemote = remoteTyping.filter { status ->
            status.email.lowercase().trim() != myEmail &&
                (myId.isBlank() || status.userId.lowercase().trim() != myId) &&
                (now - status.updatedAt) < com.example.data.remote.FirestorePresenceService.TYPING_TIMEOUT_MS
        }

        val result = mutableMapOf<String, String>()
        for (chat in chatList) {
            if (chat.id == "chat_gemini_ai") continue
            val typersForChat = activeRemote.filter { status ->
                if (status.chatId == chat.id) {
                    true
                } else if (!chat.isGroup && me != null) {
                    val canonicalDirect = LinkoRepository.generateDirectChatId(
                        user1Id = me.id,
                        user1Email = myEmail,
                        user2Id = status.userId,
                        user2Email = status.email
                    )
                    canonicalDirect == chat.id || status.chatId == canonicalDirect
                } else {
                    false
                }
            }
            if (typersForChat.isNotEmpty()) {
                if (chat.isGroup) {
                    val names = typersForChat.map { t ->
                        contactList.find { it.contactEmail.equals(t.email, ignoreCase = true) }?.contactDisplayName
                            ?: t.displayName
                    }.distinct()
                    result[chat.id] = when (names.size) {
                        1 -> "${names.first()} is typing..."
                        2 -> "${names[0]} and ${names[1]} are typing..."
                        else -> "${names.size} people are typing..."
                    }
                } else {
                    result[chat.id] = "typing..."
                }
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val activeChatTypingText: StateFlow<String?> = combine(
        currentChat,
        chatTypingMap
    ) { chat, typingMap ->
        if (chat == null || chat.id == "chat_gemini_ai") null
        else typingMap[chat.id]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTyping: StateFlow<Boolean> = combine(
        currentChat,
        _selectedChatId,
        repository.geminiTypingChatIdsFlow
    ) { chat, selectedId, geminiTypingIds ->
        val targetId = chat?.id ?: selectedId ?: return@combine false
        targetId in geminiTypingIds ||
            (targetId == "chat_gemini_ai" && geminiTypingIds.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isAppInForeground = MutableStateFlow(true)
    private val _pendingNotificationIntent = MutableStateFlow<Intent?>(null)
    val pendingNotificationIntent: StateFlow<Intent?> = _pendingNotificationIntent.asStateFlow()

    private val _pendingSharedContent = MutableStateFlow<SharedIncomingContent?>(null)
    val pendingSharedContent: StateFlow<SharedIncomingContent?> = _pendingSharedContent.asStateFlow()

    private val _fcmToken = MutableStateFlow(
        com.example.notification.LinkoFirebaseMessagingService.getSavedToken(application) ?: "Initializing FCM..."
    )
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val _isFirebaseAuthenticated = MutableStateFlow(
        com.example.data.remote.FirestoreProvider.isAuthenticated()
    )

    private val firebaseAuthListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
        _isFirebaseAuthenticated.value = auth.currentUser != null
    }

    init {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(firebaseAuthListener)
        } catch (_: Throwable) {}

        com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(application) { token ->
            _fcmToken.value = token
        }

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            val fbUser = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            } catch (_: Throwable) {
                null
            }
            val isGuest = prefs.getBoolean("is_guest_mode", false)
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                val email = fbUser.email ?: ""
                val displayName = fbUser.displayName ?: email.substringBefore("@")
                val username = email.substringBefore("@").replace(".", "_")
                repository.registerOrLogin(email, displayName, username)
            } else if (isGuest) {
                val guestName = prefs.getString("guest_display_name", null)
                repository.loginAsGuest(guestName)
            } else {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                repository.logout()
            }
        }
        viewModelScope.launch {
            combine(currentUser, _isFirebaseAuthenticated) { user, isFbAuth ->
                Pair(user, isFbAuth)
            }.collect { (user, isFbAuth) ->
                if (user != null) {
                    repository.createOrGetGeminiChat(user)
                }
                if (user != null && isFbAuth) {
                    com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(
                        context = getApplication(),
                        userEmail = user.email
                    ) { token ->
                        _fcmToken.value = token
                    }

                    repository.syncContactsWithFirestore(user.email) { newContact ->
                        com.example.notification.NotificationHelper.showFriendRequestNotification(
                            context = getApplication(),
                            requesterName = newContact.contactDisplayName,
                            requesterEmail = newContact.contactEmail
                        )
                    }
                    repository.syncGroupsWithFirestore(user)
                    repository.syncMessagesWithFirestore(user) { incomingMessage, plainPreview, isGroupChat ->
                        val isCurrentlyViewingThisChat = _isAppInForeground.value && _selectedChatId.value == incomingMessage.chatId
                        if (!isCurrentlyViewingThisChat) {
                            com.example.notification.NotificationHelper.showNewMessageNotification(
                                context = getApplication(),
                                chatId = incomingMessage.chatId,
                                senderName = incomingMessage.senderName.ifBlank { "Linko Contact" },
                                messageText = plainPreview,
                                isGroup = isGroupChat,
                                senderEmail = incomingMessage.senderEmail
                            )
                        }
                    }
                    com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, true)
                    com.example.data.remote.FirestorePresenceService.startListeningToPresence(
                        scope = viewModelScope,
                        onPresenceUpdate = { onlineSet ->
                            _onlineUserEmails.value = onlineSet
                        },
                        onPresenceDetailsUpdate = { detailsMap ->
                            _userPresenceMap.value = detailsMap
                        },
                        onTypingUpdate = { typingList ->
                            _remoteTypingStatuses.value = typingList
                        }
                    )
                } else {
                    com.example.data.remote.FirestorePresenceService.stopListening()
                    com.example.data.remote.FirestoreContactService.stopRealtimeSync()
                    com.example.data.remote.FirestoreGroupService.stopRealtimeGroupSync()
                    com.example.data.remote.FirestoreMessageService.stopListening()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                val user = currentUser.value
                if (user != null && _isAppInForeground.value && com.example.data.remote.FirestoreProvider.isAuthenticated()) {
                    com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, true)
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3000L)
                val currentList = _remoteTypingStatuses.value
                if (currentList.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val fresh = currentList.filter {
                        (now - it.updatedAt) < com.example.data.remote.FirestorePresenceService.TYPING_TIMEOUT_MS
                    }
                    if (fresh.size != currentList.size) {
                        _remoteTypingStatuses.value = fresh
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(activeMessages, appLanguage) { msgs, lang -> Pair(msgs, lang) }
                .collect { (msgs, lang) ->
                    if (lang != com.example.util.AppLanguage.ENGLISH && msgs.isNotEmpty()) {
                        translateActiveChatMessages()
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(firebaseAuthListener)
        } catch (_: Throwable) {}
        com.example.data.remote.FirestorePresenceService.stopListening()
    }

    fun enqueueNotificationIntent(intent: Intent?) {
        if (intent == null) return
        handleIncomingShareIntent(intent)
        val hasNotifExtras = intent.hasExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN) ||
            intent.hasExtra("extra_direct_reply_chat_id") ||
            intent.hasExtra("extra_accept_friend_email") ||
            intent.hasExtra("extra_decline_call") ||
            intent.action == com.example.notification.NotificationHelper.ACTION_ANSWER_CALL
        if (hasNotifExtras) {
            _pendingNotificationIntent.value = intent
        }
    }

    /**
     * Extracts shared text/URLs when Linko is chosen from the Android system Share Sheet
     * (Intent.ACTION_SEND with mimeType text/plain from apps like YouTube, Instagram, Chrome, etc.).
     */
    fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim()
                ?: ""
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotBlank() }
                ?: intent.getStringExtra(Intent.EXTRA_TITLE)?.trim()?.takeIf { it.isNotBlank() }

            if (sharedText.isNotBlank()) {
                _pendingSharedContent.value = SharedIncomingContent(
                    sharedText = sharedText,
                    sharedSubject = sharedSubject,
                    sourceMimeType = intent.type ?: "text/plain"
                )
                // Clear intent extras/action so configuration changes don't re-trigger the share sheet
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_SUBJECT)
                intent.action = null
            }
        }
    }

    fun clearPendingSharedContent() {
        _pendingSharedContent.value = null
    }

    fun sendSharedContentToRecipients(
        finalMessageText: String,
        selectedContacts: List<ContactEntity>,
        selectedChatIds: List<String>,
        onCompleteNavigateToChat: (String) -> Unit
    ) {
        val textToSend = finalMessageText.trim()
        if (textToSend.isBlank()) return
        val sender = currentUser.value ?: return

        viewModelScope.launch {
            val targetChatIds = mutableListOf<String>()

            // 1. Resolve or create unified direct chats for all selected contacts
            for (contact in selectedContacts) {
                val chat = repository.createOrGetDirectChat(sender, contact)
                if (chat.id !in targetChatIds) {
                    targetChatIds.add(chat.id)
                }
            }

            // 2. Include any directly selected chats (e.g., Groups, Gemini AI)
            for (chatId in selectedChatIds) {
                if (chatId !in targetChatIds) {
                    targetChatIds.add(chatId)
                }
            }

            // 3. Dispatch the shared message to every selected recipient chat
            for (chatId in targetChatIds) {
                repository.sendMessage(
                    chatId = chatId,
                    sender = sender,
                    text = textToSend
                )
            }

            _pendingSharedContent.value = null
            val firstChatId = targetChatIds.firstOrNull()
            if (firstChatId != null) {
                selectChat(firstChatId)
                val countLabel = if (targetChatIds.size == 1) "1 conversation" else "${targetChatIds.size} conversations"
                _actionStatusMessage.value = "Shared link/text sent to $countLabel!"
                onCompleteNavigateToChat(firstChatId)
            }
        }
    }

    fun consumeNotificationIntent() {
        _pendingNotificationIntent.value = null
    }

    fun refreshFcmToken(context: Context) {
        com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(
            context = context,
            userEmail = currentUser.value?.email
        ) { token ->
            _fcmToken.value = token
            _actionStatusMessage.value = "FCM Registration Token synced & active!"
        }
    }

    fun setAppForegroundState(isForeground: Boolean) {
        _isAppInForeground.value = isForeground
        val user = currentUser.value ?: return
        viewModelScope.launch {
            com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, isForeground)
        }
    }

    fun isUserOnline(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val normalized = email.lowercase().trim()
        if (normalized.contains("gemini")) return true
        if (_onlineUserEmails.value.contains(normalized)) return true
        if (_userPresenceMap.value[normalized]?.isOnline == true) return true
        val canonical = com.example.data.repository.LinkoRepository.canonicalParticipantKey(normalized)
        if (_onlineUserEmails.value.contains(canonical)) return true
        return false
    }

    fun getUserPresence(emailOrId: String?): com.example.data.remote.UserPresenceInfo? {
        if (emailOrId.isNullOrBlank()) return null
        val normalized = emailOrId.lowercase().trim()
        if (normalized.contains("gemini")) {
            return com.example.data.remote.UserPresenceInfo(
                email = "gemini_ai@google.com",
                userId = "user_gemini",
                displayName = "Gemini AI",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        }
        _userPresenceMap.value[normalized]?.let { return it }
        val canonical = com.example.data.repository.LinkoRepository.canonicalParticipantKey(normalized)
        _userPresenceMap.value[canonical]?.let { return it }
        return null
    }

    fun getContactForChat(chat: ChatEntity?): ContactEntity? {
        if (chat == null || chat.isGroup) return null
        val me = currentUser.value
        val participantIds = try {
            if (chat.participantIdsJson.isBlank()) emptyList()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        val myId = me?.id?.lowercase()?.trim() ?: ""
        val myEmail = me?.email?.lowercase()?.trim() ?: ""
        val myCanonical = if (myEmail.isNotBlank()) LinkoRepository.canonicalUserIdFromEmail(myEmail, myId) else myId

        val otherTokens = participantIds
            .map { it.lowercase().trim() }
            .filter { it.isNotBlank() && it != myId && it != myEmail && it != myCanonical }

        return contacts.value.find { c ->
            val cId = c.contactUserId.lowercase().trim()
            val cEmail = c.contactEmail.lowercase().trim()
            val cKey = LinkoRepository.canonicalParticipantKey(cEmail.ifBlank { cId })
            otherTokens.any { token ->
                token == cId || token == cEmail || LinkoRepository.canonicalParticipantKey(token) == cKey
            } ||
            LinkoRepository.generateDirectChatId(myId, myEmail, cId, cEmail) == chat.id ||
            "chat_${c.contactUserId}" == chat.id ||
            (!LinkoRepository.isGenericPlaceholderName(chat.name, me?.displayName, myEmail) &&
                !isSelfName(chat.name, me?.displayName ?: "", myEmail) &&
                (c.contactDisplayName.equals(chat.name.trim(), ignoreCase = true) ||
                    c.contactEmail.equals(chat.name.trim(), ignoreCase = true)))
        }
    }

    private val _blockedChatKeys = MutableStateFlow<Set<String>>(
        prefs.getStringSet("blocked_chat_keys", emptySet())?.toSet() ?: emptySet()
    )
    val blockedChatKeys: StateFlow<Set<String>> = _blockedChatKeys.asStateFlow()

    val isCurrentChatBlocked: StateFlow<Boolean> = combine(
        currentChat,
        contacts,
        currentUser,
        _blockedChatKeys
    ) { chat, contactList, me, blockedSet ->
        if (chat == null || chat.isGroup) return@combine false
        if (LinkoRepository.isGeminiAi(chatId = chat.id, name = chat.name)) return@combine false

        val myId = me?.id?.lowercase()?.trim() ?: ""
        val myEmail = me?.email?.lowercase()?.trim() ?: ""
        val myCanonical = if (myEmail.isNotBlank()) LinkoRepository.canonicalUserIdFromEmail(myEmail, myId) else myId

        val participantIds = try {
            if (chat.participantIdsJson.isBlank()) emptyList()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val otherTokens = participantIds
            .map { it.lowercase().trim() }
            .filter { it.isNotBlank() && it != myId && it != myEmail && it != myCanonical }

        val matchedContact = contactList.find { c ->
            val cId = c.contactUserId.lowercase().trim()
            val cEmail = c.contactEmail.lowercase().trim()
            val cKey = LinkoRepository.canonicalParticipantKey(cEmail.ifBlank { cId })
            otherTokens.any { token ->
                token == cId || token == cEmail || LinkoRepository.canonicalParticipantKey(token) == cKey
            } ||
                LinkoRepository.generateDirectChatId(myId, myEmail, cId, cEmail) == chat.id ||
                "chat_${c.contactUserId}" == chat.id ||
                (!LinkoRepository.isGenericPlaceholderName(chat.name, me?.displayName, myEmail) &&
                    (c.contactDisplayName.equals(chat.name.trim(), ignoreCase = true) ||
                        c.contactEmail.equals(chat.name.trim(), ignoreCase = true)))
        }

        if (matchedContact != null && LinkoRepository.isGeminiAi(
                userId = matchedContact.contactUserId,
                email = matchedContact.contactEmail,
                name = matchedContact.contactDisplayName
            )
        ) {
            return@combine false
        }

        matchedContact?.isBlocked == true ||
            chat.id in blockedSet ||
            (matchedContact != null && (matchedContact.id in blockedSet || matchedContact.contactEmail.lowercase().trim() in blockedSet))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun isContactBlocked(contact: ContactEntity?): Boolean {
        if (contact == null) return false
        if (LinkoRepository.isGeminiAi(userId = contact.contactUserId, email = contact.contactEmail, name = contact.contactDisplayName)) {
            return false
        }
        val blockedSet = _blockedChatKeys.value
        return contact.isBlocked ||
            contact.id in blockedSet ||
            contact.contactEmail.lowercase().trim() in blockedSet
    }

    fun setChatBlocked(chat: ChatEntity?, contact: ContactEntity?, isBlocked: Boolean) {
        val targetName = contact?.contactDisplayName ?: chat?.name ?: "Contact"
        if (LinkoRepository.isGeminiAi(
                chatId = chat?.id,
                userId = contact?.contactUserId,
                email = contact?.contactEmail,
                name = targetName
            )
        ) {
            _actionStatusMessage.value = "Gemini AI Assistant cannot be blocked."
            return
        }

        val keysToToggle = buildSet {
            chat?.id?.takeIf { it.isNotBlank() }?.let { add(it) }
            contact?.id?.takeIf { it.isNotBlank() }?.let { add(it) }
            contact?.contactEmail?.lowercase()?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val updatedSet = if (isBlocked) {
            _blockedChatKeys.value + keysToToggle
        } else {
            _blockedChatKeys.value - keysToToggle
        }
        _blockedChatKeys.value = updatedSet
        prefs.edit().putStringSet("blocked_chat_keys", updatedSet).apply()

        if (isBlocked) {
            updateMyTypingStatus(false)
        }

        viewModelScope.launch {
            val me = currentUser.value ?: repository.getOrCreateCurrentUser()
            val ok = repository.setContactBlockedStatus(me, chat, contact, isBlocked)
            if (ok) {
                _actionStatusMessage.value = if (isBlocked) {
                    "$targetName has been blocked."
                } else {
                    "$targetName has been unblocked."
                }
            }
        }
    }

    fun isChatOnline(chat: ChatEntity?): Boolean {
        if (chat == null || chat.isGroup) return false
        if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini", ignoreCase = true)) return true

        val contact = getContactForChat(chat)
        if (contact != null) {
            if (isUserOnline(contact.contactEmail)) return true
            if (isUserOnline(contact.contactUserId)) return true
        }

        val participantIds = try {
            if (chat.participantIdsJson.isBlank()) emptyList()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        val myId = currentUser.value?.id?.lowercase()?.trim() ?: ""
        val myEmail = currentUser.value?.email?.lowercase()?.trim() ?: ""
        for (token in participantIds) {
            val lower = token.lowercase().trim()
            if (lower.isNotBlank() && lower != myId && lower != myEmail) {
                if (isUserOnline(lower)) return true
            }
        }
        return isUserOnline(chat.name)
    }

    fun getChatPresence(chat: ChatEntity?): com.example.data.remote.UserPresenceInfo? {
        if (chat == null || chat.isGroup) return null
        if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini", ignoreCase = true)) {
            return com.example.data.remote.UserPresenceInfo(
                email = "gemini_ai@google.com",
                userId = "user_gemini",
                displayName = "Gemini AI",
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        }

        val contact = getContactForChat(chat)
        if (contact != null) {
            getUserPresence(contact.contactEmail)?.let { return it }
            getUserPresence(contact.contactUserId)?.let { return it }
        }

        val participantIds = try {
            if (chat.participantIdsJson.isBlank()) emptyList()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        val myId = currentUser.value?.id?.lowercase()?.trim() ?: ""
        val myEmail = currentUser.value?.email?.lowercase()?.trim() ?: ""
        for (token in participantIds) {
            val lower = token.lowercase().trim()
            if (lower.isNotBlank() && lower != myId && lower != myEmail) {
                getUserPresence(lower)?.let { return it }
            }
        }
        return getUserPresence(chat.name)
    }

    fun loginOrRegister(email: String, displayName: String, username: String) {
        viewModelScope.launch {
            try {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                repository.registerOrLogin(email, displayName, username)
                _actionStatusMessage.value = "Welcome back!"
            } catch (e: Exception) {
                _actionStatusMessage.value = e.message ?: "Authentication failed."
            }
        }
    }

    fun loginAsGuest(customDisplayName: String? = null) {
        viewModelScope.launch {
            try {
                prefs.edit().putBoolean("is_guest_mode", true).apply()
                repository.loginAsGuest(customDisplayName)
                _actionStatusMessage.value = "Exploring Linko as Guest"
            } catch (e: Exception) {
                _actionStatusMessage.value = e.message ?: "Failed to enter guest mode."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null) {
                com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, false)
            }
            prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
            repository.logout()
            _selectedChatId.value = null
        }
    }

    fun selectChat(chatId: String) {
        val prevChatId = _selectedChatId.value
        if (prevChatId != null && prevChatId != chatId) {
            repository.stopChatReceiptSync(prevChatId)
        }
        _selectedChatId.value = chatId
        val me = currentUser.value
        viewModelScope.launch {
            if (me != null) {
                repository.clearChatUnread(chatId)
                repository.startChatReceiptSync(chatId, me.id, me.email)
            } else {
                repository.clearChatUnread(chatId)
            }
        }
    }

    fun markVisibleMessagesAsRead(chatId: String, visibleMessageIds: List<String>) {
        if (visibleMessageIds.isEmpty()) return
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.markVisibleMessagesAsRead(
                chatId = chatId,
                currentUserId = me.id,
                currentUserEmail = me.email,
                visibleMessageIds = visibleMessageIds
            )
        }
    }

    fun markChatMessagesAsRead(chatId: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.markChatMessagesAsRead(chatId, me.id, me.email)
        }
    }

    fun clearSelectedChat() {
        val prevChatId = _selectedChatId.value
        if (prevChatId != null) {
            updateMyTypingStatus(false)
            repository.stopChatReceiptSync(prevChatId)
        }
        _selectedChatId.value = null
    }

    fun updateMyTypingStatus(isTyping: Boolean) {
        val me = currentUser.value ?: return
        val chatId = _selectedChatId.value
        val now = System.currentTimeMillis()

        if (!isTyping) {
            if (!lastBroadcastTypingState && lastBroadcastTypingChatId == null) return
            lastBroadcastTypingState = false
            lastBroadcastTypingChatId = null
            lastTypingBroadcastTimeMs = now
            com.example.data.remote.FirestorePresenceService.updateTypingStatus(
                email = me.email,
                userId = me.id,
                displayName = me.displayName,
                chatId = null,
                isTyping = false
            )
            return
        }

        if (chatId.isNullOrBlank()) return
        // Throttle repeated "typing = true" updates to at most once every 2.5 seconds for the same chat
        if (lastBroadcastTypingState && lastBroadcastTypingChatId == chatId && (now - lastTypingBroadcastTimeMs) < 2500L) {
            return
        }
        lastBroadcastTypingState = true
        lastBroadcastTypingChatId = chatId
        lastTypingBroadcastTimeMs = now
        com.example.data.remote.FirestorePresenceService.updateTypingStatus(
            email = me.email,
            userId = me.id,
            displayName = me.displayName,
            chatId = chatId,
            isTyping = true
        )
    }

    fun clearChatHistory(chatId: String) {
        viewModelScope.launch {
            repository.clearChatHistory(chatId)
            _actionStatusMessage.value = "Chat history cleared"
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            _selectedChatId.value = null
            _actionStatusMessage.value = "Chat deleted"
        }
    }

    fun addContactByEmail(email: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val result = repository.addContactByEmail(me.email, email)
            result.onSuccess {
                _actionStatusMessage.value = "Contact '${it.contactDisplayName}' added successfully!"
                onDone(true)
            }.onFailure {
                _actionStatusMessage.value = it.message ?: "Failed to add contact."
                onDone(false)
            }
        }
    }

    fun addContactByQr(qrPayload: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val result = repository.addContactFromQrPayload(me.email, qrPayload)
            result.onSuccess {
                _actionStatusMessage.value = "Added friend '${it.contactDisplayName}' via QR Code!"
                onDone(true)
            }.onFailure {
                _actionStatusMessage.value = it.message ?: "Invalid QR payload."
                onDone(false)
            }
        }
    }

    fun openDirectChat(contact: ContactEntity, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: repository.getOrCreateCurrentUser()
            val chat = repository.createOrGetDirectChat(me, contact)
            selectChat(chat.id)
            onChatReady(chat.id)
        }
    }

    fun openOrCreateDirectChat(userId: String, email: String, name: String, onChatReady: () -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: repository.getOrCreateCurrentUser()
            val canonicalId = LinkoRepository.canonicalUserIdFromEmail(email, userId)
            var contact = contacts.value.find {
                it.contactUserId == userId ||
                it.contactUserId == canonicalId ||
                (email.isNotBlank() && it.contactEmail.equals(email, ignoreCase = true))
            }
            if (contact == null) {
                contact = ContactEntity(
                    id = "c_${LinkoRepository.canonicalParticipantKey(email.ifBlank { canonicalId })}",
                    userEmail = me.email.lowercase().trim(),
                    contactUserId = canonicalId,
                    contactEmail = email.lowercase().trim(),
                    contactDisplayName = name,
                    contactUsername = email.substringBefore("@")
                )
            }
            val chat = repository.createOrGetDirectChat(me, contact)
            selectChat(chat.id)
            onChatReady()
        }
    }

    fun openGeminiChat(onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: repository.getOrCreateCurrentUser()
            val chat = repository.createOrGetGeminiChat(me)
            selectChat(chat.id)
            onChatReady(chat.id)
        }
    }

    fun createGroupChat(
        groupName: String,
        selectedContacts: List<ContactEntity>,
        groupDesc: String,
        channelType: String = "GROUP",
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val group = repository.createGroupChat(me, groupName, selectedContacts, groupDesc, channelType)
            selectChat(group.id)
            val kindLabel = if (channelType.equals("CHANNEL", ignoreCase = true)) "Channel" else "Group"
            _actionStatusMessage.value = "$kindLabel '$groupName' created (${selectedContacts.size} invited)!"
            onDone(group.id)
        }
    }

    fun addGroupMembers(chatId: String, newContacts: List<ContactEntity>) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            repository.addGroupMembers(chatId, newContacts, me)
            _actionStatusMessage.value = "Invited ${newContacts.size} contact(s) to conversation"
        }
    }

    fun toggleAdminRole(chatId: String, memberUserId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            repository.toggleAdminRole(chatId, memberUserId, makeAdmin)
            _actionStatusMessage.value = if (makeAdmin) "Member promoted to Admin" else "Admin role removed"
        }
    }

    fun removeGroupMember(chatId: String, memberUserId: String) {
        viewModelScope.launch {
            repository.removeGroupMember(chatId, memberUserId)
            _actionStatusMessage.value = "Member removed from group"
        }
    }

    fun sendMessage(
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        formattedRichText: String? = null,
        callType: String? = null
    ) {
        val chatId = _selectedChatId.value ?: return
        if (text.isBlank() && mediaUri == null) return

        // Activate isTyping synchronously at the exact moment the user hits Send in a Gemini chat
        val activeChat = currentChat.value
        if (chatId == "chat_gemini_ai" || activeChat?.name?.contains("Gemini", ignoreCase = true) == true) {
            repository.setGeminiTyping(chatId, true)
        }

        updateMyTypingStatus(false)
        viewModelScope.launch {
            val sender = currentUser.value ?: repository.getOrCreateCurrentUser()
            repository.sendMessage(
                chatId = chatId,
                sender = sender,
                text = text,
                mediaUri = mediaUri,
                mediaType = mediaType,
                formattedRichText = formattedRichText,
                callType = callType
            )
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleMessageReaction(messageId, emoji, user.id)
        }
    }

    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageForMe(messageId)
            _actionStatusMessage.value = "Message deleted for you"
        }
    }

    fun deleteMessageForEveryone(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageForEveryone(messageId)
            _actionStatusMessage.value = "Message deleted for everyone"
        }
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.votePoll(messageId, optionIndex, user.id)
        }
    }

    fun sendPoll(question: String, options: List<String>, allowMultiple: Boolean) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendPollMessage(chatId, sender, question, options, allowMultiple)
        }
    }

    fun sendLocation(title: String, address: String, latitude: Double, longitude: Double) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendLocationMessage(chatId, sender, title, address, latitude, longitude)
        }
    }

    fun sendContactAttachment(contactName: String, email: String, phone: String) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendContactMessage(chatId, sender, contactName, email, phone)
        }
    }

    fun sendEventAttachment(title: String, dateText: String, locationText: String) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendEventMessage(chatId, sender, title, dateText, locationText)
        }
    }

    fun startCall(
        contactName: String,
        contactEmail: String,
        contactAvatar: String?,
        callType: CallType,
        context: Context? = null
    ) {
        startGoogleMeetCall(
            context = context ?: getApplication<Application>().applicationContext,
            contactName = contactName,
            contactEmail = contactEmail,
            contactAvatar = contactAvatar,
            callType = callType
        )
    }

    fun startGoogleMeetCall(
        context: Context,
        contactName: String,
        contactEmail: String,
        contactAvatar: String?,
        callType: CallType = CallType.VIDEO
    ) {
        val meetCode = generateMeetCode()
        val meetUrl = "https://meet.google.com/$meetCode"
        val callTypeTag = if (callType == CallType.VIDEO) "VIDEO" else "AUDIO"
        val callTypeLabel = if (callType == CallType.VIDEO) "Video Call" else "Voice Call"

        // 1. Immediately launch Google Meet using Intent(Intent.ACTION_VIEW)
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(meetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _actionStatusMessage.value = "Launching Google Meet $callTypeLabel..."
        } catch (e: Exception) {
            _actionStatusMessage.value = "Could not open Google Meet app."
        }

        // 2. Send Google Meet link to Firebase with callType ("AUDIO" or "VIDEO") and mediaType = "CALL"
        //    Raw URL is stored in text/mediaUri for extraction on tap, but hidden in the ChatBubble UI
        viewModelScope.launch {
            val sender = currentUser.value ?: repository.getOrCreateCurrentUser()
            val targetChatId = _selectedChatId.value ?: run {
                val matchedContact = contacts.value.find {
                    it.contactEmail.equals(contactEmail, ignoreCase = true) ||
                        it.contactDisplayName.equals(contactName, ignoreCase = true)
                }
                if (matchedContact != null) {
                    repository.createOrGetDirectChat(sender, matchedContact).id
                } else null
            }

            if (targetChatId != null) {
                repository.sendMessage(
                    chatId = targetChatId,
                    sender = sender,
                    text = meetUrl,
                    mediaUri = meetUrl,
                    mediaType = "CALL",
                    callType = callTypeTag
                )
            }

            // 3. Record Call Log entry
            repository.addCallLog(
                contactName = contactName,
                contactEmail = contactEmail,
                contactAvatarUri = contactAvatar,
                callType = callType,
                isIncoming = false,
                isMissed = false,
                durationSeconds = 0
            )
        }
    }

    fun fecharGoogleMeet(chatId: String? = null) {
        endCall()
    }

    private fun generateMeetCode(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz"
        val p1 = (1..3).map { chars.random() }.joinToString("")
        val p2 = (1..4).map { chars.random() }.joinToString("")
        val p3 = (1..3).map { chars.random() }.joinToString("")
        return "$p1-$p2-$p3"
    }

    fun endCall() {
        callTimerJob?.cancel()
        _activeCallState.value = null
    }

    fun clearStatusMessage() {
        _actionStatusMessage.value = null
    }

    fun updateProfile(displayName: String, statusMessage: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                displayName = displayName.ifBlank { user.displayName },
                statusMessage = statusMessage.ifBlank { user.statusMessage }
            )
            repository.updateProfile(updated)
            _actionStatusMessage.value = "Profile updated successfully"
        }
    }

    fun updateAvatarUri(avatarUri: String?) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(avatarUri = avatarUri)
            repository.updateProfile(updated)
            _actionStatusMessage.value = if (!avatarUri.isNullOrBlank()) "Profile picture updated!" else "Profile picture removed"
        }
    }

    fun postTextStatus(text: String, bgHex: String, fontStyle: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.postStatus(
                user = user,
                type = "TEXT",
                textContent = text,
                mediaUri = null,
                backgroundColorHex = bgHex,
                fontStyle = fontStyle
            )
            _actionStatusMessage.value = "Status posted successfully! ✨"
            onSuccess()
        }
    }

    fun postMediaStatus(mediaUri: String, mediaType: String, caption: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.postStatus(
                user = user,
                type = mediaType,
                textContent = caption,
                mediaUri = mediaUri,
                backgroundColorHex = "#10B981",
                fontStyle = "DEFAULT"
            )
            _actionStatusMessage.value = "Status posted successfully! 📸"
            onSuccess()
        }
    }

    fun markStatusAsViewed(statusId: String) {
        viewModelScope.launch {
            repository.markStatusAsViewed(statusId)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
            _actionStatusMessage.value = "Status removed"
        }
    }

    fun replyToStatus(contactEmail: String, replyText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.contactEmail == contactEmail }
            val me = currentUser.value
            if (contact != null && me != null) {
                val chat = repository.createOrGetDirectChat(me, contact)
                _selectedChatId.value = chat.id
                repository.sendMessage(
                    chatId = chat.id,
                    sender = me,
                    text = "Replying to status: $replyText"
                )
                _actionStatusMessage.value = "Reply sent to ${contact.contactDisplayName}"
                onComplete()
            } else {
                _actionStatusMessage.value = "Unable to send reply"
            }
        }
    }

    fun answerIncomingCall(
        callerName: String,
        callerEmail: String,
        callerAvatar: String?,
        callType: CallType
    ) {
        com.example.notification.NotificationHelper.cancelNotification(
            getApplication(),
            com.example.notification.NotificationHelper.NOTIFICATION_ID_INCOMING_CALL
        )
        com.example.notification.NotificationHelper.cancelNotification(
            getApplication(),
            com.example.notification.NotificationHelper.NOTIFICATION_ID_MISSED_CALL
        )

        startGoogleMeetCall(
            context = getApplication<Application>().applicationContext,
            contactName = callerName,
            contactEmail = callerEmail,
            contactAvatar = callerAvatar,
            callType = callType
        )
    }

    fun processNotificationIntent(
        intent: Intent,
        context: Context,
        onNavigate: (screen: String, chatId: String?) -> Unit
    ) {
        val extraScreen = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN)
        val extraChatId = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CHAT_ID)
        val extraSenderName = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SENDER_NAME) ?: ""
        val extraSenderEmail = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SENDER_EMAIL) ?: ""
        val extraMessageText = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_MESSAGE_TEXT) ?: ""
        val extraIsGroup = intent.getBooleanExtra(com.example.notification.NotificationHelper.EXTRA_IS_GROUP, false)
        val extraReplyChatId = intent.getStringExtra("extra_direct_reply_chat_id")
        val extraReplyText = intent.getStringExtra("extra_direct_reply_text")
        val extraAcceptEmail = intent.getStringExtra("extra_accept_friend_email")
        val extraAcceptName = intent.getStringExtra("extra_accept_friend_name") ?: ""
        val extraDeclineCall = intent.getBooleanExtra("extra_decline_call", false)

        // Clear extras from intent so they aren't re-consumed on rotation
        intent.removeExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN)
        intent.removeExtra("extra_direct_reply_chat_id")
        intent.removeExtra("extra_accept_friend_email")
        intent.removeExtra("extra_decline_call")

        // Process Direct Reply from Notification
        if (!extraReplyChatId.isNullOrEmpty() && !extraReplyText.isNullOrEmpty()) {
            val user = currentUser.value
            if (user != null) {
                viewModelScope.launch {
                    repository.sendMessage(extraReplyChatId, user, extraReplyText)
                    selectChat(extraReplyChatId)
                    onNavigate(com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT, extraReplyChatId)
                }
            }
        }

        // Process Accept Friend Request Action
        if (!extraAcceptEmail.isNullOrEmpty()) {
            val me = currentUser.value
            if (me != null) {
                viewModelScope.launch {
                    repository.addContactByEmail(me.email, extraAcceptEmail)
                    _actionStatusMessage.value = "Accepted friend request from ${extraAcceptName.ifBlank { extraAcceptEmail }}!"
                    onNavigate(com.example.notification.NotificationHelper.SCREEN_FRIENDS, null)
                }
            } else {
                onNavigate(com.example.notification.NotificationHelper.SCREEN_FRIENDS, null)
            }
        }

        // Process Decline Call Action
        if (extraDeclineCall) {
            _activeCallState.value = null
        }

        // Process Screen Navigation Intent
        if (!extraScreen.isNullOrEmpty()) {
            when (extraScreen) {
                com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT -> {
                    if (!extraChatId.isNullOrEmpty()) {
                        viewModelScope.launch {
                            val ensuredChat = repository.ensureChatForNotification(
                                chatId = extraChatId,
                                senderName = extraSenderName,
                                senderEmail = extraSenderEmail,
                                messageText = extraMessageText,
                                isGroup = extraIsGroup,
                                currentUser = currentUser.value
                            )
                            selectChat(ensuredChat.id)
                            onNavigate(com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT, ensuredChat.id)
                        }
                    }
                }
                com.example.notification.NotificationHelper.SCREEN_INCOMING_CALL -> {
                    val callerName = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_NAME) ?: "Caller"
                    val callerEmail = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_EMAIL) ?: "caller@linko.com"
                    val callTypeStr = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALL_TYPE) ?: "VOICE"
                    val callType = if (callTypeStr.equals("VIDEO", ignoreCase = true)) CallType.VIDEO else CallType.AUDIO
                    val contactAvatar = contacts.value.find { it.contactEmail.equals(callerEmail, ignoreCase = true) }?.contactAvatarUri
                    answerIncomingCall(callerName, callerEmail, contactAvatar, callType)
                    onNavigate("calls", null)
                }
                com.example.notification.NotificationHelper.SCREEN_FRIENDS -> {
                    onNavigate("friends", null)
                }
            }
        }
    }

    fun triggerSimulatedMessageNotification(context: Context) {
        viewModelScope.launch {
            val firstChat = chats.value.firstOrNull()
            val chatId = firstChat?.id ?: "chat_elena_vance"
            val senderName = firstChat?.name ?: "Elena Vance"
            val text = "Hey! Tap this notification to open our chat or reply directly from the notification bar 🚀"

            repository.ensureChatForNotification(
                chatId = chatId,
                senderName = senderName,
                senderEmail = "elena.vance@linko.com",
                messageText = text,
                isGroup = firstChat?.isGroup ?: false,
                currentUser = currentUser.value
            )

            com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
                context = context,
                data = mapOf(
                    "type" to "message",
                    "chat_id" to chatId,
                    "sender_name" to senderName,
                    "sender_email" to "elena.vance@linko.com",
                    "message_text" to text,
                    "is_group" to (firstChat?.isGroup ?: false).toString()
                )
            )
            _actionStatusMessage.value = "FCM Push notification sent for Message from $senderName!"
        }
    }

    fun triggerSimulatedCallNotification(context: Context, isVideo: Boolean = false) {
        val firstContact = contacts.value.firstOrNull()
        val callerName = firstContact?.contactDisplayName ?: "Marcus Miller"
        val callerEmail = firstContact?.contactEmail ?: "marcus.miller@linko.com"
        val callType = if (isVideo) "Video" else "Voice"

        com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
            context = context,
            data = mapOf(
                "type" to "incoming_call",
                "caller_name" to callerName,
                "caller_email" to callerEmail,
                "call_type" to callType
            )
        )
        _actionStatusMessage.value = "FCM Push notification sent for Incoming $callType Call from $callerName!"
    }

    fun triggerSimulatedFriendRequestNotification(context: Context) {
        val requesterName = "Sophia Chen"
        val requesterEmail = "sophia.chen@linko.com"

        com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
            context = context,
            data = mapOf(
                "type" to "friend_request",
                "requester_name" to requesterName,
                "requester_email" to requesterEmail
            )
        )
        _actionStatusMessage.value = "FCM Push notification sent for Friend Request from $requesterName!"
    }

}

data class SharedIncomingContent(
    val sharedText: String,
    val sharedSubject: String? = null,
    val sourceMimeType: String = "text/plain"
)


package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedSlideBar
import com.example.ui.components.SlideBarPosition
import com.example.ui.components.rememberScrollVisibilityState
import com.example.ui.components.swipeToHideShow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AuthState
import com.example.data.model.CallType
import com.example.notification.NotificationHelper
import com.example.ui.components.NotificationPermissionDialog
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.calls.CallsScreen
import com.example.ui.screens.chats.ChatsScreen
import com.example.ui.screens.chats.DirectChatScreen
import com.example.util.LocalizationManager
import com.example.ui.screens.friends.AddContactDialog
import com.example.ui.screens.friends.CreateGroupScreen
import com.example.ui.screens.friends.FriendsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.theme.Emerald500
import com.example.ui.theme.LinkoTheme
import com.example.ui.viewmodel.LinkoViewModel
import com.example.util.AdManager
import com.example.util.findActivity
import com.example.util.configureAppCheck
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LinkoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        // Initialize Google AdMob and display app launch interstitial ad
        AdManager.initialize(applicationContext)
        AdManager.showAppLaunchAd(this)

        // Initialize notification channels in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannels(applicationContext)
            } catch (_: Exception) {}
        }

        viewModel.enqueueNotificationIntent(intent)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val selectedPalette by viewModel.selectedThemePalette.collectAsStateWithLifecycle()

            LinkoTheme(darkTheme = isDarkMode, palette = selectedPalette) {
                LinkoApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.enqueueNotificationIntent(intent)
    }
}

@Composable
fun LinkoApp(viewModel: LinkoViewModel) {
    val navController = rememberNavController()

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val currentChat by viewModel.currentChat.collectAsStateWithLifecycle()
    val currentPeerProfile by viewModel.currentPeerProfile.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val activeChatTypingText by viewModel.activeChatTypingText.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val chatTypingMap by viewModel.chatTypingMap.collectAsStateWithLifecycle()
    val userStatusGroups by viewModel.userStatusGroups.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedPalette by viewModel.selectedThemePalette.collectAsStateWithLifecycle()
    val selectedWallpaper by viewModel.selectedChatWallpaper.collectAsStateWithLifecycle()
    val customWallpaperUri by viewModel.customWallpaperUri.collectAsStateWithLifecycle()
    val customWallpaperDimming by viewModel.customWallpaperDimming.collectAsStateWithLifecycle()
    val customWallpaperScale by viewModel.customWallpaperScale.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val translatedMessages by viewModel.translatedMessages.collectAsStateWithLifecycle()
    val actionStatusMessage by viewModel.actionStatusMessage.collectAsStateWithLifecycle()
    val pendingNotificationIntent by viewModel.pendingNotificationIntent.collectAsStateWithLifecycle()
    val pendingSharedContent by viewModel.pendingSharedContent.collectAsStateWithLifecycle()
    val fcmToken by viewModel.fcmToken.collectAsStateWithLifecycle()
    val isCurrentChatBlocked by viewModel.isCurrentChatBlocked.collectAsStateWithLifecycle()
    val blockedChatKeys by viewModel.blockedChatKeys.collectAsStateWithLifecycle()
    val onlineUserEmails by viewModel.onlineUserEmails.collectAsStateWithLifecycle()
    val userPresenceMap by viewModel.userPresenceMap.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddContactModal by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(true) }

    val context = LocalContext.current

    val handleStartCall: (String, String, String?, CallType) -> Unit = { name, email, avatar, type ->
        if (email.contains("gemini", ignoreCase = true) || name.contains("Gemini", ignoreCase = true)) {
            Toast.makeText(context, "Calling is not supported for AI Assistant. Please type your message.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.startGoogleMeetCall(context, name, email, avatar, type)
        }
    }

    LaunchedEffect(pendingNotificationIntent, authState) {
        val intentToProcess = pendingNotificationIntent
        if (intentToProcess != null && authState is AuthState.Authenticated) {
            viewModel.consumeNotificationIntent()
            viewModel.processNotificationIntent(intentToProcess, context) { screen, _ ->
                when (screen) {
                    NotificationHelper.SCREEN_DIRECT_CHAT -> {
                        navController.navigate("direct_chat") {
                            popUpTo("chats")
                            launchSingleTop = true
                        }
                    }
                    NotificationHelper.SCREEN_FRIENDS -> {
                        navController.navigate("friends") {
                            popUpTo("chats")
                            launchSingleTop = true
                        }
                    }
                    "calls" -> {
                        navController.navigate("calls") {
                            popUpTo("chats")
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    if (showNotificationPermissionDialog) {
        NotificationPermissionDialog(
            onDismiss = { showNotificationPermissionDialog = false }
        )
    }

    LaunchedEffect(actionStatusMessage) {
        actionStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> viewModel.setAppForegroundState(true)
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, androidx.lifecycle.Lifecycle.Event.ON_STOP -> viewModel.setAppForegroundState(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showAddContactModal) {
        AddContactDialog(
            onAddByEmail = { email ->
                viewModel.addContactByEmail(email) { success ->
                    if (success) showAddContactModal = false
                }
            },
            onAddByQr = { payload ->
                viewModel.addContactByQr(payload) { success ->
                    if (success) showAddContactModal = false
                }
            },
            onDismiss = { showAddContactModal = false }
        )
    }

    val activeSharedContent = pendingSharedContent
    if (activeSharedContent != null && authState is AuthState.Authenticated) {
        com.example.ui.components.ShareToLinkoSheet(
            sharedContent = activeSharedContent,
            contacts = contacts,
            chats = chats,
            isUserOnline = { email -> viewModel.isUserOnline(email) },
            onSendToRecipients = { finalMessageText, selectedContacts, selectedChatIds ->
                viewModel.sendSharedContentToRecipients(
                    finalMessageText = finalMessageText,
                    selectedContacts = selectedContacts,
                    selectedChatIds = selectedChatIds
                ) { _ ->
                    navController.navigate("direct_chat") {
                        popUpTo("chats")
                        launchSingleTop = true
                    }
                }
            },
            onQuickAddContactByEmail = { email, onDone ->
                viewModel.addContactByEmail(email, onDone)
            },
            onDismiss = {
                viewModel.clearPendingSharedContent()
            }
        )
    }

    // Main App Navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf("chats", "status", "friends", "calls", "settings")
    val showBottomBar = authState is AuthState.Authenticated && currentRoute in bottomBarRoutes

    // Swipe / Scroll to hide and show the bottom bar smoothly
    val bottomBarVisibilityState = rememberScrollVisibilityState(initialVisible = true)
    LaunchedEffect(currentRoute) {
        bottomBarVisibilityState.show()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                AnimatedSlideBar(
                    visible = bottomBarVisibilityState.isVisible,
                    position = SlideBarPosition.BOTTOM,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .swipeToHideShow(bottomBarVisibilityState)
                    ) {
                        NavigationBar(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                        val totalUnread = chats.sumOf { it.unreadCount }

                    // Chats
                    NavigationBarItem(
                        selected = currentRoute == "chats",
                        onClick = { navController.navigate("chats") { popUpTo("chats") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = {
                            if (totalUnread > 0) {
                                BadgedBox(badge = { Badge(containerColor = Emerald500) { Text("$totalUnread", fontSize = 10.sp) } }) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = "Chats")
                                }
                            } else {
                                Icon(Icons.Default.ChatBubble, contentDescription = "Chats")
                            }
                        },
                        label = {
                            com.example.ui.components.AutoResizeText(
                                text = LocalizationManager.getString("tab_chats", currentLanguage),
                                maxFontSize = 12.sp,
                                minFontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )

                    // Status
                    NavigationBarItem(
                        selected = currentRoute == "status",
                        onClick = { navController.navigate("status") { popUpTo("chats") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = {
                            val hasUnviewedStatus = userStatusGroups.any { !it.isCurrentUser && it.hasUnviewed }
                            if (hasUnviewedStatus) {
                                BadgedBox(badge = { Badge(containerColor = Emerald500) }) {
                                    Icon(Icons.Default.HistoryToggleOff, contentDescription = "Status")
                                }
                            } else {
                                Icon(Icons.Default.HistoryToggleOff, contentDescription = "Status")
                            }
                        },
                        label = {
                            com.example.ui.components.AutoResizeText(
                                text = LocalizationManager.getString("tab_status", currentLanguage),
                                maxFontSize = 12.sp,
                                minFontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )

                    // Friends
                    NavigationBarItem(
                        selected = currentRoute == "friends",
                        onClick = { navController.navigate("friends") { popUpTo("chats") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Default.People, contentDescription = "Friends") },
                        label = {
                            com.example.ui.components.AutoResizeText(
                                text = LocalizationManager.getString("tab_friends", currentLanguage),
                                maxFontSize = 12.sp,
                                minFontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )


                    // Calls
                    NavigationBarItem(
                        selected = currentRoute == "calls",
                        onClick = { navController.navigate("calls") { popUpTo("chats") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                        label = {
                            com.example.ui.components.AutoResizeText(
                                text = LocalizationManager.getString("tab_calls", currentLanguage),
                                maxFontSize = 12.sp,
                                minFontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )

                    // Settings
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { popUpTo("chats") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = {
                            com.example.ui.components.AutoResizeText(
                                text = LocalizationManager.getString("tab_settings", currentLanguage),
                                maxFontSize = 12.sp,
                                minFontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )
                    }
                }
            }
        }
    }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(bottomBarVisibilityState.nestedScrollConnection)
                .swipeToHideShow(bottomBarVisibilityState)
        ) {
            LaunchedEffect(authState, currentRoute) {
                if (authState is AuthState.Authenticated && currentRoute == "auth") {
                    navController.navigate("chats") {
                        popUpTo("auth") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = if (authState is AuthState.Authenticated) "chats" else "auth"
            ) {
                composable("auth") {
                    AuthScreen(
                        onLoginSubmit = { email, displayName, username ->
                            viewModel.loginOrRegister(email, displayName, username)
                            navController.navigate("chats") {
                                popUpTo("auth") { inclusive = true }
                            }
                        },
                        onSkip = {
                            viewModel.loginAsGuest()
                            navController.navigate("chats") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }

                composable("chats") {
                    ChatsScreen(
                        chats = chats,
                        contacts = contacts,
                        allMessages = allMessages,
                        typingStatusByChatId = chatTypingMap,
                        currentUser = (authState as? AuthState.Authenticated)?.user,
                        currentLanguage = currentLanguage,
                        onlineUserEmails = onlineUserEmails,
                        userPresenceMap = userPresenceMap,
                        onSelectChat = { chatId ->
                            viewModel.selectChat(chatId)
                            navController.navigate("direct_chat")
                        },
                        onOpenGeminiClick = {
                            viewModel.openGeminiChat {
                                navController.navigate("direct_chat")
                            }
                        },
                        onCreateGroupClick = { navController.navigate("create_group") },
                        onAddContactClick = { showAddContactModal = true },
                        onOpenContactChat = { contact ->
                            viewModel.openDirectChat(contact) {
                                navController.navigate("direct_chat")
                            }
                        },
                        isUserOnline = { email -> viewModel.isUserOnline(email) },
                        isChatOnline = { chat -> viewModel.isChatOnline(chat) },
                        getChatPresence = { chat -> viewModel.getChatPresence(chat) }
                    )
                }

                composable("status") {
                    StatusScreen(
                        currentUser = currentUser,
                        userStatusGroups = userStatusGroups,
                        onPostTextStatus = { text, bgHex, fontStyle ->
                            viewModel.postTextStatus(text, bgHex, fontStyle) {}
                        },
                        onPostMediaStatus = { uri, type, caption ->
                            viewModel.postMediaStatus(uri, type, caption) {}
                        },
                        onMarkAsViewed = { statusId ->
                            viewModel.markStatusAsViewed(statusId)
                        },
                        onDeleteStatus = { statusId ->
                            viewModel.deleteStatus(statusId)
                        },
                        onReplyToStatus = { email, text ->
                            viewModel.replyToStatus(email, text) {
                                navController.navigate("direct_chat")
                            }
                        }
                    )
                }


                composable("friends") {
                    val activeBlockedKeys = blockedChatKeys
                    FriendsScreen(
                        contacts = contacts,
                        currentUser = currentUser,
                        isUserOnline = { email -> viewModel.isUserOnline(email) },
                        isContactBlocked = { contact ->
                            viewModel.isContactBlocked(contact) || contact.id in activeBlockedKeys
                        },
                        onToggleBlockContact = { contact, shouldBlock ->
                            viewModel.setChatBlocked(null, contact, shouldBlock)
                        },
                        onOpenChat = { contact ->
                            viewModel.openDirectChat(contact) {
                                navController.navigate("direct_chat")
                            }
                        },
                        onStartCall = handleStartCall,
                        onAddContactClick = { showAddContactModal = true },
                        onCreateGroupClick = { navController.navigate("create_group") }
                    )
                }

                composable("calls") {
                    CallsScreen(
                        callLogs = callLogs,
                        contacts = contacts,
                        onStartCall = handleStartCall
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        user = currentUser,
                        isDarkMode = isDarkMode,
                        currentPalette = selectedPalette,
                        currentWallpaper = selectedWallpaper,
                        customWallpaperUri = customWallpaperUri,
                        customWallpaperDimming = customWallpaperDimming,
                        customWallpaperScale = customWallpaperScale,
                        currentLanguage = currentLanguage,
                        fcmToken = fcmToken,
                        onRefreshFcmToken = { viewModel.refreshFcmToken(context) },
                        onSelectLanguage = { lang -> viewModel.setAppLanguage(lang) },
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onSelectPalette = { palette -> viewModel.setThemePalette(palette) },
                        onSelectWallpaper = { wallpaper -> viewModel.setChatWallpaper(wallpaper) },
                        onApplyCustomWallpaper = { uri, dimming, scale ->
                            viewModel.setCustomWallpaper(uri, dimming, scale)
                        },
                        onTriggerMessageNotification = {
                            viewModel.triggerSimulatedMessageNotification(context)
                        },
                        onTriggerCallNotification = { isVideo ->
                            viewModel.triggerSimulatedCallNotification(context, isVideo)
                        },
                        onTriggerFriendRequestNotification = {
                            viewModel.triggerSimulatedFriendRequestNotification(context)
                        },
                        onUpdateProfile = { name, status ->
                            viewModel.updateProfile(name, status)
                        },
                        onUpdateAvatarUri = { uri ->
                            viewModel.updateAvatarUri(uri)
                        },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate("auth") {
                                popUpTo("chats") { inclusive = true }
                            }
                        }
                    )
                }

                composable("direct_chat") {
                    DirectChatScreen(
                        chat = currentChat,
                        messages = activeMessages,
                        currentUser = currentUser,
                        contacts = contacts,
                        peerProfile = currentPeerProfile,
                        isUserOnline = { email -> viewModel.isUserOnline(email) },
                        chatWallpaper = selectedWallpaper,
                        customWallpaperUri = customWallpaperUri,
                        customWallpaperDimming = customWallpaperDimming,
                        customWallpaperScale = customWallpaperScale,
                        isDarkMode = isDarkMode,
                        currentLanguage = currentLanguage,
                        translatedMessages = translatedMessages,
                        onBackClick = {
                            val activity = context.findActivity()
                            if (activity != null) {
                                AdManager.showChatExitAd(activity) {
                                    viewModel.clearSelectedChat()
                                    navController.popBackStack()
                                }
                            } else {
                                viewModel.clearSelectedChat()
                                navController.popBackStack()
                            }
                        },
                        onSendMessage = { text, mediaUri, mediaType, richFormat ->
                            viewModel.sendMessage(text, mediaUri, mediaType, richFormat)
                        },
                        onMarkAsRead = { chatId ->
                            viewModel.markChatMessagesAsRead(chatId)
                        },
                        onMarkVisibleMessagesAsRead = { chatId, visibleIds ->
                            viewModel.markVisibleMessagesAsRead(chatId, visibleIds)
                        },
                        onToggleReaction = { messageId, emoji ->
                            viewModel.toggleReaction(messageId, emoji)
                        },
                        onDeleteForMe = { messageId ->
                            viewModel.deleteMessageForMe(messageId)
                        },
                        onDeleteForEveryone = { messageId ->
                            viewModel.deleteMessageForEveryone(messageId)
                        },
                        onVotePoll = { messageId, optionIndex ->
                            viewModel.votePoll(messageId, optionIndex)
                        },
                        onSendPoll = { question, options, allowMultiple ->
                            viewModel.sendPoll(question, options, allowMultiple)
                        },
                        onSendLocation = { title, address, lat, lng ->
                            viewModel.sendLocation(title, address, lat, lng)
                        },
                        onSendContact = { name, email, phone ->
                            viewModel.sendContactAttachment(name, email, phone)
                        },
                        onSendEvent = { title, dateText, locationText ->
                            viewModel.sendEventAttachment(title, dateText, locationText)
                        },
                        onStartCall = handleStartCall,
                        onClearChatHistory = {
                            currentChat?.id?.let { viewModel.clearChatHistory(it) }
                        },
                        onDeleteChat = {
                            currentChat?.id?.let { viewModel.deleteChat(it) }
                            navController.popBackStack()
                        },
                        onAddGroupMembers = { chatId, selectedContacts ->
                            viewModel.addGroupMembers(chatId, selectedContacts)
                        },
                        onToggleAdminRole = { chatId, memberUserId, makeAdmin ->
                            viewModel.toggleAdminRole(chatId, memberUserId, makeAdmin)
                        },
                        onRemoveMember = { chatId, memberUserId ->
                            viewModel.removeGroupMember(chatId, memberUserId)
                        },
                        onSendPrivateMessage = { contactUserId, contactEmail, contactName ->
                            viewModel.openOrCreateDirectChat(contactUserId, contactEmail, contactName) {
                                navController.navigate("direct_chat")
                            }
                        },
                        onSelectWallpaper = { wallpaper ->
                            viewModel.setChatWallpaper(wallpaper)
                        },
                        onFecharGoogleMeet = {
                            viewModel.fecharGoogleMeet(currentChat?.id)
                        },
                        isTyping = isTyping,
                        typingIndicatorText = activeChatTypingText,
                        onTypingChanged = { isTyping ->
                            viewModel.updateMyTypingStatus(isTyping)
                        },
                        isContactBlocked = isCurrentChatBlocked,
                        onToggleBlockUser = { shouldBlock ->
                            val targetContact = viewModel.getContactForChat(currentChat)
                            viewModel.setChatBlocked(currentChat, targetContact, shouldBlock)
                        }
                    )
                }

                composable("create_group") {
                    CreateGroupScreen(
                        contacts = contacts,
                        onBackClick = { navController.popBackStack() },
                        onCreateGroup = { groupName, selectedContacts, groupDesc, channelType ->
                            viewModel.createGroupChat(groupName, selectedContacts, groupDesc, channelType) {
                                navController.navigate("direct_chat") {
                                    popUpTo("chats")
                                }
                            }
                        },
                        onQuickAddContactByEmail = { email ->
                            viewModel.addContactByEmail(email) {}
                        }
                    )
                }
            }
        }
    }
}

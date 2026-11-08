package com.example.ui.screens.chats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.UserEntity
import com.example.data.model.CallType
import com.example.data.model.EmailHeaderInfo
import com.example.ui.components.AiImageDialog
import com.example.ui.components.AttachmentBottomSheet
import com.example.ui.components.AttachmentType
import com.example.ui.components.ChatBubble
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.CustomEmojiPickerDialog
import com.example.ui.components.EmailHeaderDialog
import com.example.ui.components.EmojiPickerPanel
import com.example.ui.components.EncryptionSecurityDialog
import com.example.ui.components.EventCreationDialog
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.InstagramTypingBubble
import com.example.ui.components.LocationPickerDialog
import com.example.ui.components.MessageReactionsBar
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.PollCreationDialog
import com.example.ui.components.RecipientProfileSheet
import com.example.ui.components.UserAvatar
import com.example.util.ImageStorageHelper
import com.example.ui.theme.ChatWallpaper
import com.example.ui.theme.ChatWallpaperBackground
import com.example.ui.theme.Emerald500
import com.example.ui.theme.StatusReadBlue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    chat: ChatEntity?,
    messages: List<MessageEntity>,
    currentUser: UserEntity?,
    contacts: List<ContactEntity> = emptyList(),
    peerProfile: com.example.ui.viewmodel.PeerProfileInfo? = null,
    isUserOnline: (String?) -> Boolean = { false },
    chatWallpaper: ChatWallpaper = ChatWallpaper.DOODLE,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    isDarkMode: Boolean = true,
    currentLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.ENGLISH,
    translatedMessages: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit,
    onSendMessage: (text: String, mediaUri: String?, mediaType: String?, richFormat: String?) -> Unit,
    onMarkAsRead: (chatId: String) -> Unit = {},
    onMarkVisibleMessagesAsRead: (chatId: String, visibleMessageIds: List<String>) -> Unit = { _, _ -> },
    onToggleReaction: (messageId: String, emoji: String) -> Unit = { _, _ -> },
    onDeleteForMe: (messageId: String) -> Unit = {},
    onDeleteForEveryone: (messageId: String) -> Unit = {},
    onVotePoll: (messageId: String, optionIndex: Int) -> Unit = { _, _ -> },
    onSendPoll: (question: String, options: List<String>, allowMultiple: Boolean) -> Unit = { _, _, _ -> },
    onSendLocation: (title: String, address: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
    onSendContact: (name: String, email: String, phone: String) -> Unit = { _, _, _ -> },
    onSendEvent: (title: String, dateText: String, locationText: String) -> Unit = { _, _, _ -> },
    onStartCall: (contactName: String, contactEmail: String, contactAvatar: String?, callType: CallType) -> Unit,
    onClearChatHistory: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onAddGroupMembers: (chatId: String, selectedContacts: List<ContactEntity>) -> Unit = { _, _ -> },
    onToggleAdminRole: (chatId: String, memberUserId: String, makeAdmin: Boolean) -> Unit = { _, _, _ -> },
    onRemoveMember: (chatId: String, memberUserId: String) -> Unit = { _, _ -> },
    onSendPrivateMessage: (contactUserId: String, contactEmail: String, contactName: String) -> Unit = { _, _, _ -> },
    onSelectWallpaper: (ChatWallpaper) -> Unit = {},
    onFecharGoogleMeet: () -> Unit = {},
    isTyping: Boolean = false,
    typingIndicatorText: String? = null,
    onTypingChanged: (Boolean) -> Unit = {},
    isContactBlocked: Boolean = false,
    onToggleBlockUser: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var selectedRichStyle by remember { mutableStateOf<String?>(null) } // "BOLD", "ITALIC", "CODE"
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var activeEmailHeader by remember { mutableStateOf<EmailHeaderInfo?>(null) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showInviteContactsDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    androidx.activity.compose.BackHandler {
        onBackClick()
    }

    // 1. Get the CURRENT logged-in user's ID (from FirebaseAuth or UserEntity)
    val currentUserId = remember(currentUser) {
        currentUser?.id?.ifBlank { null }
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: ""
    }
    val currentUserEmail = remember(currentUser) {
        currentUser?.email?.lowercase()?.trim()
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
            ?: ""
    }
    val currentDisplayName = remember(currentUser) {
        currentUser?.displayName?.trim()
            ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName?.trim()
            ?: ""
    }

    // 2. Look at the participants list for this specific chat
    val participants = remember(chat?.participantIdsJson) {
        try {
            if (chat?.participantIdsJson.isNullOrBlank()) emptyList<String>()
            else {
                val array = org.json.JSONArray(chat!!.participantIdsJson)
                (0 until array.length()).map { array.getString(it).trim() }.filter { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // 3. Filter the list to find the ID that does NOT match the current user's ID
    val peerId = remember(participants, currentUserId, currentUserEmail) {
        participants.firstOrNull { id ->
            val cleanId = id.lowercase().trim()
            cleanId != currentUserId.lowercase().trim() &&
            (currentUserEmail.isBlank() || cleanId != currentUserEmail)
        }
    }

    // 4. Fetch and display the name and profile picture for that specific peerId in the TopAppBar
    var peerRemoteName by remember(chat?.id, peerId) { mutableStateOf<String?>(null) }
    var peerRemoteAvatar by remember(chat?.id, peerId) { mutableStateOf<String?>(null) }

    LaunchedEffect(peerId, chat?.id) {
        if (peerId.isNullOrBlank() || chat?.isGroup == true) return@LaunchedEffect
        // Local contacts check
        val localContact = contacts.find { c ->
            c.contactUserId.equals(peerId, ignoreCase = true) ||
            c.contactEmail.equals(peerId, ignoreCase = true) ||
            (peerId.contains("@") && c.contactEmail.contains(peerId, ignoreCase = true))
        }
        if (localContact != null) {
            peerRemoteName = localContact.contactDisplayName
            peerRemoteAvatar = localContact.contactAvatarUri
            return@LaunchedEffect
        }

        // Fetch from Firestore /users/{peerId}
        try {
            val db = com.example.data.remote.FirestoreProvider.getInstance(context) ?: return@LaunchedEffect
            db.collection("users").document(peerId).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("username")?.takeIf { it.isNotBlank() }
                        val avatar = doc.getString("avatarUri")?.takeIf { it.isNotBlank() }
                        if (!name.isNullOrBlank()) peerRemoteName = name
                        if (!avatar.isNullOrBlank()) peerRemoteAvatar = avatar
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
                            if (!name.isNullOrBlank()) peerRemoteName = name
                            if (!avatar.isNullOrBlank()) peerRemoteAvatar = avatar
                        }
                    }
            }
        } catch (e: Exception) {
            android.util.Log.w("DirectChatScreen", "Could not fetch peer user: ${e.message}")
        }
    }

    val isSelfName: (String) -> Boolean = remember(currentDisplayName, currentUserEmail) {
        { candidate ->
            val clean = candidate.trim().lowercase()
            if (clean.isBlank()) false
            else {
                val cleanDisplay = currentDisplayName.trim().lowercase()
                val cleanEmail = currentUserEmail.trim().lowercase()
                val prefix = if (cleanEmail.contains("@")) cleanEmail.substringBefore("@") else ""
                (cleanDisplay.isNotBlank() && clean == cleanDisplay) ||
                (cleanEmail.isNotBlank() && clean == cleanEmail) ||
                (prefix.isNotBlank() && clean == prefix)
            }
        }
    }

    val recipientContact = remember(chat, contacts, peerId, peerRemoteName) {
        if (chat == null || chat.isGroup) null
        else {
            if (peerId != null) {
                contacts.find { c ->
                    c.contactUserId.equals(peerId, ignoreCase = true) ||
                    c.contactEmail.equals(peerId, ignoreCase = true)
                }
            } else {
                contacts.find { "chat_${it.contactUserId}" == chat.id }
            }
        }
    }

    val isGeminiChat = chat?.id == "chat_gemini_ai" ||
            chat?.name?.contains("Gemini", ignoreCase = true) == true ||
            peerId == "user_gemini" ||
            recipientContact?.contactUserId == "user_gemini" ||
            recipientContact?.contactEmail?.equals("gemini.ai@linko.com", ignoreCase = true) == true
    val isAiBot = isGeminiChat
    // Strict AI Exception: Gemini AI can NEVER be blocked
    val effectivelyBlocked = !isAiBot && chat?.isGroup != true && (isContactBlocked || recipientContact?.isBlocked == true)

    val displayTitleName = remember(
        chat,
        isGeminiChat,
        peerProfile,
        recipientContact,
        peerRemoteName,
        peerId,
        currentDisplayName,
        currentUserEmail
    ) {
        when {
            chat == null -> "Chat"
            isGeminiChat -> "Gemini AI"
            chat.isGroup -> chat.name
            peerProfile != null && !isSelfName(peerProfile.displayName) -> {
                peerProfile.displayName
            }
            recipientContact != null && !isSelfName(recipientContact.contactDisplayName) -> {
                recipientContact.contactDisplayName
            }
            !peerRemoteName.isNullOrBlank() && !isSelfName(peerRemoteName!!) -> {
                peerRemoteName!!
            }
            peerId != null && peerId.contains("@") -> {
                peerId.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
            }
            !chat.name.isNullOrBlank() && !isSelfName(chat.name) && !com.example.data.repository.LinkoRepository.isGenericPlaceholderName(chat.name, currentDisplayName, currentUserEmail) -> {
                chat.name
            }
            peerId != null && peerId.isNotBlank() -> {
                "User ${peerId.takeLast(4)}"
            }
            else -> "Contact"
        }
    }

    val displayAvatarUri = remember(
        chat,
        peerProfile,
        recipientContact,
        peerRemoteAvatar
    ) {
        when {
            chat == null -> null
            chat.isGroup -> chat.groupIconUri
            peerProfile?.avatarUri != null -> peerProfile.avatarUri
            recipientContact?.contactAvatarUri != null -> recipientContact.contactAvatarUri
            peerRemoteAvatar != null -> peerRemoteAvatar
            else -> chat.groupIconUri
        }
    }

    val isChatUserOnline = remember(chat, recipientContact, isUserOnline, peerId, peerProfile) {
        if (chat == null) false
        else if (isGeminiChat) true
        else if (chat.isGroup) false
        else if (recipientContact != null) isUserOnline(recipientContact.contactEmail) || isUserOnline(recipientContact.contactUserId)
        else if (peerProfile?.email != null) isUserOnline(peerProfile.email)
        else if (peerId != null) isUserOnline(peerId)
        else isUserOnline(chat.name)
    }

    var showPollDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAiImageDialog by remember { mutableStateOf(false) }
    var pendingSelectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showEmojiPickerPanel by remember { mutableStateOf(false) }
    var reactionTargetMessageId by remember { mutableStateOf<String?>(null) }
    var customEmojiTargetMessageId by remember { mutableStateOf<String?>(null) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var isHoldingMicButton by remember { mutableStateOf(false) }
    var isRecordingLocked by remember { mutableStateOf(false) }
    var isUploadingVoiceNote by remember { mutableStateOf(false) }
    var voiceNoteUploadProgress by remember { mutableStateOf(0f) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var recordingDeciseconds by remember { mutableStateOf(0) }
    var slideCancelOffsetX by remember { mutableStateOf(0f) }
    var holdHintMessage by remember { mutableStateOf<String?>(null) }
    val liveWaveformAmplitudes = remember {
        mutableStateListOf(
            0.25f, 0.35f, 0.50f, 0.30f, 0.65f, 0.45f, 0.70f, 0.40f,
            0.55f, 0.30f, 0.60f, 0.35f, 0.45f, 0.25f, 0.50f, 0.35f
        )
    }
    var actionTargetMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var messageInfoTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val sheetState = rememberModalBottomSheetState()

    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { com.example.util.AudioRecorderManager(context) }

    // Ensure recorder and typing state are cleaned up if user leaves the chat screen
    DisposableEffect(Unit) {
        onDispose {
            onTypingChanged(false)
            if (isRecordingAudio) {
                audioRecorderManager.cancelRecording()
            }
        }
    }

    // Broadcast real-time typing state while user is actively composing a message, with 3s idle auto-clear
    LaunchedEffect(inputText, chat?.id) {
        if (inputText.isNotBlank() && chat != null) {
            onTypingChanged(true)
            delay(3000)
            onTypingChanged(false)
        } else {
            onTypingChanged(false)
        }
    }

    // Activity Result Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                pendingSelectedImageUri = savedUri ?: uri
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                val finalUriStr = savedUri?.toString() ?: uri.toString()
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                onSendMessage(fileName, finalUriStr, "DOCUMENT", null)
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                val finalUriStr = savedUri?.toString() ?: uri.toString()
                onSendMessage("Audio Recording Note", finalUriStr, "AUDIO", null)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveBitmapToInternalStorage(context, bitmap)
                if (savedUri != null) {
                    pendingSelectedImageUri = savedUri
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            holdHintMessage = "Microphone enabled — Hold the mic button to record & release to send"
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val finishAndSendVoiceNote: () -> Unit = {
        val result = audioRecorderManager.stopRecordingWithMetadata()
        isRecordingAudio = false
        isHoldingMicButton = false
        isRecordingLocked = false
        slideCancelOffsetX = 0f
        recordingSeconds = 0
        recordingDeciseconds = 0
        if (result != null) {
            val replyPrefix = if (replyingToMessage != null) {
                val rMsg = replyingToMessage!!
                val textSnippet = rMsg.text.ifBlank { rMsg.mediaType ?: "Media" }
                "REPLY:${rMsg.senderName}|$textSnippet"
            } else null
            replyingToMessage = null

            val activeChatId = chat?.id ?: "direct_chat"
            scope.launch {
                isUploadingVoiceNote = true
                voiceNoteUploadProgress = 0.08f
                holdHintMessage = "Saving voice note to Firebase Storage..."
                val uploadResult = com.example.data.remote.FirebaseVoiceNoteStorageService.uploadVoiceNote(
                    context = context,
                    chatId = activeChatId,
                    audioFile = result.file,
                    durationMs = result.durationMs,
                    onProgress = { progress ->
                        voiceNoteUploadProgress = progress
                    }
                )
                isUploadingVoiceNote = false
                voiceNoteUploadProgress = 0f
                holdHintMessage = null
                onSendMessage(
                    "🎙️ Voice Note (${result.durationFormatted})",
                    uploadResult.audioUri,
                    "AUDIO",
                    replyPrefix
                )
            }
        }
    }

    val cancelActiveVoiceNote: (Boolean) -> Unit = { showToast ->
        audioRecorderManager.cancelRecording()
        isRecordingAudio = false
        isHoldingMicButton = false
        isRecordingLocked = false
        slideCancelOffsetX = 0f
        recordingSeconds = 0
        recordingDeciseconds = 0
        if (showToast) {
            holdHintMessage = "Voice note recording cancelled"
        }
    }

    LaunchedEffect(holdHintMessage) {
        if (holdHintMessage != null) {
            delay(2800)
            holdHintMessage = null
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            recordingDeciseconds = 0
            var tickCount = 0
            while (isRecordingAudio) {
                delay(100)
                tickCount++
                recordingDeciseconds = tickCount % 10
                recordingSeconds = tickCount / 10
                val amp = audioRecorderManager.sampleNormalizedAmplitude()
                if (liveWaveformAmplitudes.size >= 18) {
                    liveWaveformAmplitudes.removeAt(0)
                }
                liveWaveformAmplitudes.add(amp)
            }
        }
    }

    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isScreenResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isScreenResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isScreenResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger Stage 3 ('read' - Double Blue Tick) ONLY when recipient has opened the chat
    // and the incoming message is actually visible on their screen.
    LaunchedEffect(chat?.id, messages, isScreenResumed, currentUser?.id) {
        val activeChatId = chat?.id ?: return@LaunchedEffect
        if (!isScreenResumed) return@LaunchedEffect

        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
        }
            .distinctUntilChanged()
            .collect { visibleItemKeys ->
                if (visibleItemKeys.isEmpty()) return@collect
                val myId = currentUser?.id ?: ""
                val myEmail = currentUser?.email?.lowercase()?.trim() ?: ""

                val visibleUnreadIds = messages.filter { msg ->
                    val isVisibleOnScreen = msg.id in visibleItemKeys
                    val isFromRecipient = (myId.isNotBlank() && msg.senderId != myId) ||
                            (myEmail.isNotBlank() && msg.senderEmail.lowercase().trim() != myEmail)
                    val isNotYetRead = MessageStatus.normalize(msg.status) != MessageStatus.READ
                    isVisibleOnScreen && isFromRecipient && isNotYetRead
                }.map { it.id }

                if (visibleUnreadIds.isNotEmpty()) {
                    onMarkVisibleMessagesAsRead(activeChatId, visibleUnreadIds)
                }
            }
    }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0.dp && messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, isTyping) {
        val targetIndex = messages.size + (if (isTyping) 2 else 1)
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    if (activeEmailHeader != null) {
        EmailHeaderDialog(
            info = activeEmailHeader!!,
            onDismiss = { activeEmailHeader = null }
        )
    }

    if (showSecurityDialog) {
        EncryptionSecurityDialog(
            chatId = chat?.id ?: "chat_default",
            chatName = chat?.name ?: "Contact",
            onDismiss = { showSecurityDialog = false }
        )
    }

    if (showAttachmentMenu) {
        AttachmentBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAttachmentMenu = false },
            onOptionSelected = { type ->
                when (type) {
                    AttachmentType.GALLERY -> galleryLauncher.launch("image/*")
                    AttachmentType.CAMERA -> {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    AttachmentType.DOCUMENT -> documentLauncher.launch("*/*")
                    AttachmentType.AUDIO -> audioLauncher.launch("audio/*")
                    AttachmentType.LOCATION -> showLocationDialog = true
                    AttachmentType.CONTACT -> showContactDialog = true
                    AttachmentType.POLL -> showPollDialog = true
                    AttachmentType.EVENT -> showEventDialog = true
                    AttachmentType.AI_IMAGE -> showAiImageDialog = true
                    AttachmentType.IN_APP_CALL -> {
                        val peerEmail = "${(chat?.name ?: "contact").lowercase().replace(" ", ".")}@linko.net"
                        onStartCall(
                            chat?.name ?: "Contact",
                            peerEmail,
                            chat?.groupIconUri,
                            CallType.VIDEO
                        )
                    }
                }
            }
        )
    }

    if (showPollDialog) {
        PollCreationDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { question, options, allowMultiple ->
                onSendPoll(question, options, allowMultiple)
            }
        )
    }

    if (showEventDialog) {
        EventCreationDialog(
            onDismiss = { showEventDialog = false },
            onCreateEvent = { title, dateText, locationText ->
                onSendEvent(title, dateText, locationText)
            }
        )
    }

    if (showContactDialog) {
        ContactPickerDialog(
            contacts = contacts,
            onDismiss = { showContactDialog = false },
            onContactSelected = { name, email, phone ->
                onSendContact(name, email, phone)
            }
        )
    }

    if (showLocationDialog) {
        LocationPickerDialog(
            onDismiss = { showLocationDialog = false },
            onSendLocation = { title, address, lat, lng ->
                onSendLocation(title, address, lat, lng)
            }
        )
    }

    if (showAiImageDialog) {
        AiImageDialog(
            onDismiss = { showAiImageDialog = false },
            onSendAiImage = { prompt, imageUrl ->
                onSendMessage(prompt, imageUrl, "IMAGE", null)
            }
        )
    }

    if (pendingSelectedImageUri != null) {
        ImagePreviewDialog(
            imageUri = pendingSelectedImageUri!!,
            onDismiss = { pendingSelectedImageUri = null },
            onSendImage = { caption, uploadedUrl ->
                onSendMessage(caption, uploadedUrl, "IMAGE", null)
                pendingSelectedImageUri = null
            }
        )
    }

    if (customEmojiTargetMessageId != null) {
        val targetMsgId = customEmojiTargetMessageId!!
        CustomEmojiPickerDialog(
            onEmojiSelect = { emoji ->
                onToggleReaction(targetMsgId, emoji)
                customEmojiTargetMessageId = null
            },
            onDismiss = { customEmojiTargetMessageId = null }
        )
    }

    if (showProfileSheet) {
        val effectivePeerContact = recipientContact ?: if (peerId != null) {
            ContactEntity(
                id = "c_$peerId",
                userEmail = currentUserEmail,
                contactUserId = peerId,
                contactEmail = peerProfile?.email ?: if (peerId.contains("@")) peerId else "${displayTitleName.lowercase().replace(" ", ".")}@linko.net",
                contactDisplayName = displayTitleName,
                contactUsername = displayTitleName.lowercase().replace(" ", "."),
                contactAvatarUri = displayAvatarUri,
                contactStatus = "Connected on Linko"
            )
        } else null

        RecipientProfileSheet(
            chat = chat,
            messages = messages,
            recipientContact = effectivePeerContact,
            currentUser = currentUser,
            allContacts = contacts,
            isOnline = isChatUserOnline,
            isBlocked = effectivelyBlocked,
            onToggleBlockUser = if (!isAiBot && chat?.isGroup != true) {
                { shouldBlock -> onToggleBlockUser(shouldBlock) }
            } else null,
            onDismissRequest = { showProfileSheet = false },
            onStartCall = { isVideo ->
                showProfileSheet = false
                val peerEmail = effectivePeerContact?.contactEmail ?: "${displayTitleName.lowercase().replace(" ", ".")}@linko.net"
                onStartCall(
                    displayTitleName,
                    peerEmail,
                    displayAvatarUri,
                    if (isVideo) CallType.VIDEO else CallType.AUDIO
                )
            },
            onClearChatHistory = {
                onClearChatHistory()
                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
            },
            onDeleteChat = {
                onDeleteChat()
                Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
            },
            onAddGroupMembers = onAddGroupMembers,
            onToggleAdminRole = onToggleAdminRole,
            onRemoveMember = onRemoveMember,
            onSendPrivateMessage = onSendPrivateMessage
        )
    }

    if (showInviteContactsDialog && chat?.isGroup == true) {
        val currentParticipants = remember(chat.participantIdsJson) {
            try {
                if (chat.participantIdsJson.isBlank()) emptyList()
                else {
                    val arr = org.json.JSONArray(chat.participantIdsJson)
                    (0 until arr.length()).map { arr.getString(it) }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
        val availableContacts = remember(contacts, currentParticipants) {
            contacts.filter { it.contactUserId !in currentParticipants }
        }
        val selectedInviteContacts = remember { mutableStateListOf<ContactEntity>() }
        val isChannel = chat.id.startsWith("channel_")

        AlertDialog(
            onDismissRequest = { showInviteContactsDialog = false },
            title = {
                Text(
                    text = if (isChannel) "Invite Contacts to Channel" else "Invite Contacts to Group",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (availableContacts.isEmpty()) {
                    Text("All your saved contacts are already members of \"${chat.name}\".")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select one or more contacts to invite to \"${chat.name}\":",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        availableContacts.forEach { contact ->
                            val isChecked = selectedInviteContacts.any { it.id == contact.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isChecked) selectedInviteContacts.removeAll { it.id == contact.id }
                                        else selectedInviteContacts.add(contact)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (selectedInviteContacts.none { it.id == contact.id }) {
                                                selectedInviteContacts.add(contact)
                                            }
                                        } else {
                                            selectedInviteContacts.removeAll { it.id == contact.id }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                UserAvatar(
                                    name = contact.contactDisplayName,
                                    avatarUri = contact.contactAvatarUri,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.contactDisplayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = contact.contactEmail,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (availableContacts.isNotEmpty()) {
                    Button(
                        onClick = {
                            onAddGroupMembers(chat.id, selectedInviteContacts.toList())
                            showInviteContactsDialog = false
                            Toast.makeText(
                                context,
                                "Invited ${selectedInviteContacts.size} contact(s) to ${chat.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = selectedInviteContacts.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier.testTag("confirm_group_invite_button")
                    ) {
                        Text("Invite (${selectedInviteContacts.size})", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteContactsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showWallpaperDialog) {
        val allWallpapers = remember { ChatWallpaper.selectableWallpapers }
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = {
                Text(
                    text = "Select Chat Wallpaper (${allWallpapers.size})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Scroll to browse all classic and new wallpapers:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allWallpapers.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { wp ->
                                val isSelected = wp == chatWallpaper
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectWallpaper(wp)
                                        }
                                        .testTag("chat_wallpaper_item_${wp.id.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(88.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Emerald500 else Color.Gray.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        ChatWallpaperBackground(
                                            wallpaper = wp,
                                            isDarkMode = isDarkMode,
                                            customWallpaperUri = customWallpaperUri,
                                            customWallpaperDimming = customWallpaperDimming,
                                            customWallpaperScale = customWallpaperScale
                                        ) {}

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Emerald500),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = wp.displayName,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Done", color = Emerald500, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    ChatWallpaperBackground(
        wallpaper = chatWallpaper,
        isDarkMode = isDarkMode,
        customWallpaperUri = customWallpaperUri,
        customWallpaperDimming = customWallpaperDimming,
        customWallpaperScale = customWallpaperScale,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 920.dp),
                topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showProfileSheet = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            if (isGeminiChat) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C4DFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                UserAvatar(
                                    name = displayTitleName,
                                    avatarUri = displayAvatarUri,
                                    isGroup = chat?.isGroup ?: false,
                                    size = 42.dp,
                                    showOnlineStatus = true,
                                    isOnline = isChatUserOnline
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = displayTitleName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isGeminiChat && !typingIndicatorText.isNullOrBlank()) {
                                    var dotPhase by remember { mutableStateOf(0) }
                                    LaunchedEffect(typingIndicatorText) {
                                        while (true) {
                                            delay(350)
                                            dotPhase = (dotPhase + 1) % 3
                                        }
                                    }
                                    val indicatorColor = Emerald500
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.testTag("chat_header_typing_indicator")
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 5.dp)
                                        ) {
                                            repeat(3) { idx ->
                                                val isHighlighted = idx == dotPhase
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (isHighlighted) 5.dp else 4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            indicatorColor.copy(
                                                                alpha = if (isHighlighted) 1f else 0.4f
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                        Text(
                                            text = typingIndicatorText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = indicatorColor
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "E2EE",
                                            tint = if (isGeminiChat) Color(0xFF7C4DFF) else if (isChatUserOnline) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val groupMemberCount = remember(chat?.participantIdsJson) {
                                            try {
                                                if (chat?.participantIdsJson.isNullOrBlank()) 1
                                                else org.json.JSONArray(chat?.participantIdsJson).length().coerceAtLeast(1)
                                            } catch (_: Exception) {
                                                1
                                            }
                                        }
                                        val isBroadcastChannel = chat?.id?.startsWith("channel_") == true
                                        Text(
                                            text = when {
                                                isGeminiChat -> "Gemini AI • E2EE"
                                                effectivelyBlocked -> "Blocked • Tap below to unblock"
                                                chat?.isGroup == true && isBroadcastChannel -> "Channel • $groupMemberCount members • E2EE"
                                                chat?.isGroup == true -> "Group • $groupMemberCount members • E2EE"
                                                isChatUserOnline -> "Online • E2EE"
                                                else -> "Offline • E2EE"
                                            },
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = when {
                                                isGeminiChat -> Color(0xFF7C4DFF)
                                                effectivelyBlocked -> MaterialTheme.colorScheme.error
                                                isChatUserOnline || chat?.isGroup == true -> Emerald500
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (chat?.isGroup == true) {
                            IconButton(
                                onClick = { showInviteContactsDialog = true },
                                modifier = Modifier.testTag("group_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Invite Contacts to Group",
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        if (!isAiBot) {
                            val resolvedPeerEmail = recipientContact?.contactEmail
                                ?: peerProfile?.email
                                ?: if (peerId != null && peerId.contains("@")) peerId
                                else "${displayTitleName.lowercase().replace(" ", ".")}@linko.net"
                            val resolvedPeerAvatar = displayAvatarUri
                            if (!effectivelyBlocked) {
                                IconButton(onClick = {
                                    onStartCall(
                                        displayTitleName,
                                        resolvedPeerEmail,
                                        resolvedPeerAvatar,
                                        CallType.AUDIO
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Voice Call",
                                        tint = Emerald500,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    onStartCall(
                                        displayTitleName,
                                        resolvedPeerEmail,
                                        resolvedPeerAvatar,
                                        CallType.VIDEO
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video Call",
                                        tint = Emerald500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            if (chat?.isGroup != true) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleBlockUser(!effectivelyBlocked)
                                    },
                                    modifier = Modifier.testTag("header_block_user_button")
                                ) {
                                    Icon(
                                        imageVector = if (effectivelyBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                                        contentDescription = if (effectivelyBlocked) "Unblock Contact" else "Block Contact",
                                        tint = if (effectivelyBlocked) Emerald500 else MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showWallpaperDialog = true },
                            modifier = Modifier.testTag("chat_wallpaper_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = "Change Chat Wallpaper",
                                tint = Emerald500,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        IconButton(onClick = { showProfileSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Contact Info & Shared Media",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                )
            },
            bottomBar = {
                AnimatedContent(
                    targetState = effectivelyBlocked,
                    transitionSpec = {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                    },
                    label = "BottomBarBlockStateTransition"
                ) { isBlockedState ->
                    if (isBlockedState) {
                        // Blocked UI State: Message input bar is completely removed and replaced with a prominent 'Tap to Unblock' banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                tonalElevation = 4.dp,
                                shadowElevation = 6.dp,
                                border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleBlockUser(false)
                                    }
                                    .testTag("tap_to_unblock_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "Blocked User",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "You blocked $displayTitleName",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Messages & calls are disabled • Tap to Unblock",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Emerald500,
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Tap to Unblock",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Chat Input Controls
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .imePadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                    // Quoted Reply Preview Bar
                    if (replyingToMessage != null) {
                        val replyMsg = replyingToMessage!!
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .background(Emerald500, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Replying to ${replyMsg.senderName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                    val isCallReply = !replyMsg.callType.isNullOrBlank() ||
                                        replyMsg.mediaType?.uppercase() == "CALL" ||
                                        replyMsg.text.contains("meet.google.com", ignoreCase = true)
                                    val snippet = when {
                                        isCallReply -> if (replyMsg.callType.equals("AUDIO", ignoreCase = true) ||
                                            replyMsg.text.contains("Audio Call", ignoreCase = true) ||
                                            replyMsg.text.contains("Voice Call", ignoreCase = true)
                                        ) "📞 Voice Call" else "📹 Video Call"
                                        replyMsg.text.isNotBlank() -> replyMsg.text
                                        replyMsg.mediaType?.uppercase() == "IMAGE" -> "📷 Photo"
                                        replyMsg.mediaType?.uppercase() == "AUDIO" -> "🎙️ Voice Note"
                                        replyMsg.mediaType?.uppercase() == "DOCUMENT" -> "📄 Document"
                                        else -> "Media"
                                    }
                                    Text(
                                        text = snippet,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (isGeminiChat) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("✨ Summarize our recent chat history into key bullet points.", null, null, null)
                                    },
                                    label = { Text("✨ Summarize chat", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("💡 Draft a polite follow-up email message regarding project updates.", null, null, null)
                                    },
                                    label = { Text("💡 Draft email", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("🖼️ Analyze this image and describe its key elements in detail.", "https://picsum.photos/400/300", "IMAGE", null)
                                    },
                                    label = { Text("🖼️ Analyze photo", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("📝 Give me 3 creative story ideas for a modern messaging app user.", null, null, null)
                                    },
                                    label = { Text("📝 Story ideas", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                        }
                    }

                    // Floating Hold-to-Record Tooltip Hint
                    AnimatedVisibility(visible = holdHintMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .testTag("voice_record_hint_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = holdHintMessage ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                }
                                IconButton(
                                    onClick = { holdHintMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Hint",
                                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Single-row WhatsApp/Signal style chat input bar with persistent Hold-to-Record Mic button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecordingAudio) {
                            // Live Voice Note Recording Bar (active during Hold-to-Record or Locked mode)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("active_voice_recording_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Pulsing Mic + Timer
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (recordingDeciseconds % 2 == 0) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val durationText = "${formatSeconds(recordingSeconds)}.$recordingDeciseconds"
                                        Text(
                                            text = durationText,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.testTag("voice_recording_timer")
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Live Microphone Amplitude Waveform Bars
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        liveWaveformAmplitudes.forEach { amp ->
                                            val barH = (4.dp + (18.dp * amp.coerceIn(0.15f, 1.0f)))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(barH)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isRecordingLocked) {
                                        // Hands-free Locked Controls: Cancel or Send
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    cancelActiveVoiceNote(true)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("cancel_voice_note_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Cancel Voice Note",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    finishAndSendVoiceNote()
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = Emerald500),
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("send_locked_voice_note_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Send Voice Note",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Hold-to-Record instruction: Slide left to cancel / Release to send
                                        Text(
                                            text = if (slideCancelOffsetX < -40f) "◀ Keep sliding to cancel" else "◀ Slide to cancel • Release to send",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left icon cluster: Emoji, Plus ('+')
                                    IconButton(
                                        onClick = { showEmojiPickerPanel = !showEmojiPickerPanel },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("emoji_picker_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Emoji Picker",
                                            tint = if (showEmojiPickerPanel) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAttachmentMenu = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("attachment_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Content",
                                            tint = Emerald500,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }

                                    // Simple text input field ("Message") with clear word spacing and readability
                                    TextField(
                                        value = inputText,
                                        onValueChange = { newText ->
                                            inputText = newText
                                            onTypingChanged(newText.isNotBlank())
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            letterSpacing = 0.25.sp
                                        ),
                                        placeholder = {
                                            Text(
                                                text = if (isGeminiChat) "Message Gemini..." else com.example.util.LocalizationManager.getString("type_message", currentLanguage),
                                                fontSize = 16.sp,
                                                letterSpacing = 0.2.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        singleLine = false,
                                        maxLines = 4,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Default
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .testTag("message_input_field")
                                    )

                                    // Google Maps Share Location quick action button
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showLocationDialog = true
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("quick_share_location_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Share Google Maps Location",
                                            tint = Emerald500,
                                            modifier = Modifier.size(23.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right Primary Action Button:
                        // - Shows Send Text button when inputText is non-blank and not currently recording audio
                        // - Shows Hold-to-Record Microphone Button when inputText is blank (or while actively holding/recording)
                        if (inputText.isNotBlank() && !isRecordingAudio) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val replyPrefix = if (replyingToMessage != null) {
                                        val rMsg = replyingToMessage!!
                                        val isCallReply = !rMsg.callType.isNullOrBlank() ||
                                            rMsg.mediaType?.uppercase() == "CALL" ||
                                            rMsg.text.contains("meet.google.com", ignoreCase = true)
                                        val textSnippet = when {
                                            isCallReply -> if (rMsg.callType.equals("AUDIO", ignoreCase = true) ||
                                                rMsg.text.contains("Audio Call", ignoreCase = true) ||
                                                rMsg.text.contains("Voice Call", ignoreCase = true)
                                            ) "Voice Call" else "Video Call"
                                            rMsg.text.isNotBlank() -> rMsg.text
                                            else -> rMsg.mediaType ?: "Media"
                                        }
                                        "REPLY:${rMsg.senderName}|$textSnippet"
                                    } else null
                                    val finalStyle = if (replyPrefix != null) {
                                        if (selectedRichStyle != null) "$replyPrefix;$selectedRichStyle" else replyPrefix
                                    } else selectedRichStyle

                                    onSendMessage(inputText.trim(), null, null, finalStyle)
                                    inputText = ""
                                    selectedRichStyle = null
                                    replyingToMessage = null
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isGeminiChat) Color(0xFF7C4DFF) else Emerald500
                                ),
                                modifier = Modifier
                                    .size(50.dp)
                                    .testTag("send_button")
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            // Hold-to-Record Microphone Button (50.dp touch target)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .scale(if (isHoldingMicButton) 1.16f else 1.0f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isHoldingMicButton -> MaterialTheme.colorScheme.error
                                            isRecordingLocked -> Emerald500
                                            isGeminiChat -> Color(0xFF7C4DFF)
                                            else -> Emerald500
                                        }
                                    )
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()

                                            // If already in locked hands-free mode, tapping the button finishes & sends
                                            if (isRecordingAudio && isRecordingLocked) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                finishAndSendVoiceNote()
                                                return@awaitEachGesture
                                            }

                                            // Verify RECORD_AUDIO runtime permission
                                            val hasMicPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (!hasMicPermission) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@awaitEachGesture
                                            }

                                            // Start Hold-to-Record immediately on press down
                                            val pressStartTime = System.currentTimeMillis()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            holdHintMessage = null
                                            slideCancelOffsetX = 0f
                                            isHoldingMicButton = true
                                            isRecordingLocked = false
                                            audioRecorderManager.startRecording()
                                            isRecordingAudio = true

                                            var dragTotalX = 0f
                                            var dragTotalY = 0f
                                            var cancelledBySlide = false
                                            var lockedBySlideUp = false

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val activeChange = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()

                                                if (activeChange == null || !activeChange.pressed) {
                                                    break
                                                }

                                                val delta = activeChange.positionChange()
                                                if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                                                    activeChange.consume()
                                                    dragTotalX += delta.x
                                                    dragTotalY += delta.y
                                                    slideCancelOffsetX = dragTotalX.coerceAtMost(0f)

                                                    // Slide left past threshold -> cancel voice note
                                                    if (dragTotalX < -190f) {
                                                        cancelledBySlide = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        cancelActiveVoiceNote(true)
                                                        break
                                                    }

                                                    // Slide up past threshold -> lock hands-free recording
                                                    if (dragTotalY < -140f) {
                                                        lockedBySlideUp = true
                                                        isRecordingLocked = true
                                                        isHoldingMicButton = false
                                                        slideCancelOffsetX = 0f
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        holdHintMessage = "Recording locked hands-free — Tap Send when finished"
                                                        break
                                                    }
                                                }
                                            }

                                            // Pointer released
                                            if (!cancelledBySlide && !lockedBySlideUp) {
                                                val heldDurationMs = System.currentTimeMillis() - pressStartTime
                                                isHoldingMicButton = false
                                                slideCancelOffsetX = 0f
                                                if (heldDurationMs >= 350L) {
                                                    // Standard Hold-to-Record release -> immediately send voice note!
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    finishAndSendVoiceNote()
                                                } else {
                                                    // Quick tap (< 350ms) -> lock recording & show helpful Hold-to-Record hint
                                                    isRecordingLocked = true
                                                    holdHintMessage = "Hold mic & release to send instantly, or tap Send to finish"
                                                }
                                            }
                                        }
                                    }
                                    .testTag("hold_to_record_audio_button")
                            ) {
                                if (isUploadingVoiceNote) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        progress = { voiceNoteUploadProgress.coerceIn(0.1f, 1f) },
                                        color = Color.White,
                                        strokeWidth = 2.2.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isRecordingLocked) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                        contentDescription = if (isHoldingMicButton) {
                                            "Recording Voice Note - Release to Send"
                                        } else if (isRecordingLocked) {
                                            "Send Locked Voice Note"
                                        } else {
                                            "Hold to Record Voice Note"
                                        },
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showEmojiPickerPanel) {
                        EmojiPickerPanel(
                            onEmojiSelect = { emoji ->
                                inputText += emoji
                            },
                            onBackspace = {
                                if (inputText.isNotEmpty()) {
                                    inputText = inputText.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 240.dp)
                                .padding(top = 6.dp)
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
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("chat_messages_list")
                ) {
                    item(key = "e2ee_security_banner") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .clickable { showSecurityDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = Emerald500,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Messages and calls are end-to-end encrypted with Signal Protocol AES-256-GCM. No one outside of this chat can read or listen. Tap to verify.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    if (currentLanguage != com.example.util.AppLanguage.ENGLISH) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Translation",
                                        tint = Color(0xFF7C4DFF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Real-Time Dynamic Chat Translation active into ${currentLanguage.nativeName} (${currentLanguage.displayName})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF7C4DFF),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    if (messages.isEmpty()) {
                        item(key = "empty_chat_placeholder") {
                            EmptyChatPlaceholder(
                                peerName = displayTitleName,
                                isGemini = isGeminiChat,
                                onQuickGreeting = { greeting ->
                                    onSendMessage(greeting, null, null, null)
                                }
                            )
                        }
                    }

                    messages.forEachIndexed { index, msg ->
                        val showDateHeader = index == 0 || !isSameCalendarDay(messages[index - 1].timestamp, msg.timestamp)
                        if (showDateHeader) {
                            item(key = "date_header_${msg.timestamp}_$index") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                                        tonalElevation = 1.dp,
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.testTag("chat_date_header")
                                    ) {
                                        Text(
                                            text = formatDateHeader(msg.timestamp),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(key = msg.id) {
                            val isFromMe = currentUser?.id == msg.senderId || currentUser?.email == msg.senderEmail
                            val translatedText = translatedMessages[msg.id] ?: translatedMessages["${msg.id}_${currentLanguage.code}"]
                            val displayText = if (currentLanguage != com.example.util.AppLanguage.ENGLISH && !translatedText.isNullOrBlank()) {
                                translatedText
                            } else {
                                msg.text
                            }

                            Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                                if (reactionTargetMessageId == msg.id) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        MessageReactionsBar(
                                            onSelectEmoji = { emoji ->
                                                onToggleReaction(msg.id, emoji)
                                                reactionTargetMessageId = null
                                            },
                                            onMoreClick = {
                                                val mId = msg.id
                                                reactionTargetMessageId = null
                                                customEmojiTargetMessageId = mId
                                            },
                                            onDismiss = { reactionTargetMessageId = null }
                                        )
                                    }
                                }

                                val senderContact = if (!isFromMe && chat?.isGroup == true) {
                                    contacts.find { c ->
                                        c.contactUserId.equals(msg.senderId, ignoreCase = true) ||
                                        c.contactEmail.equals(msg.senderEmail, ignoreCase = true)
                                    }
                                } else null

                                MessageBubble(
                                    message = msg,
                                    displayText = displayText,
                                    isTranslated = currentLanguage != com.example.util.AppLanguage.ENGLISH && !translatedText.isNullOrBlank(),
                                    targetLanguageName = currentLanguage.nativeName,
                                    isFromMe = isFromMe,
                                    currentUserId = currentUser?.id,
                                    onToggleReaction = { emoji ->
                                        onToggleReaction(msg.id, emoji)
                                    },
                                    onLongPress = {
                                        actionTargetMessage = msg
                                    },
                                    onSwipeToReply = {
                                        replyingToMessage = msg
                                    },
                                    onVotePoll = { optionIndex ->
                                        onVotePoll(msg.id, optionIndex)
                                    },
                                    onMediaClick = {
                                        if (!msg.mediaUri.isNullOrEmpty() && msg.mediaType?.uppercase() != "IMAGE") {
                                            scope.launch {
                                                val ok = ImageStorageHelper.downloadMediaFile(context, msg.mediaUri)
                                                if (ok) Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                                else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onEmailBadgeClick = {
                                        activeEmailHeader = EmailHeaderInfo(
                                            from = msg.senderEmail,
                                            to = currentUser?.email ?: "me@linko.com",
                                            subject = msg.emailSubject ?: "Linko Message",
                                            messageId = msg.emailMessageId ?: "<msg@linko.net>",
                                            timestampFormatted = formatTime(msg.timestamp),
                                            status = msg.emailTransportStatus
                                        )
                                    },
                                    onStatusTicksClick = {
                                        messageInfoTarget = msg
                                    },
                                    onFecharGoogleMeet = onFecharGoogleMeet,
                                    isGroupChat = chat?.isGroup == true,
                                    senderAvatarUri = senderContact?.contactAvatarUri
                                )
                            }
                        }
                    }

                    if (isTyping) {
                        item(key = "gemini_instagram_typing_bubble") {
                            InstagramTypingBubble(
                                isGemini = isGeminiChat,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                val showScrollToBottom by remember {
                    derivedStateOf {
                        val total = listState.layoutInfo.totalItemsCount
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        total > 5 && lastVisible < total - 2
                    }
                }

                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                ) {
                    Surface(
                        onClick = {
                            scope.launch {
                                val total = listState.layoutInfo.totalItemsCount
                                if (total > 0) {
                                    listState.animateScrollToItem(total - 1)
                                }
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("scroll_to_bottom_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                                tint = Emerald500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (actionTargetMessage != null) {
        val targetMsg = actionTargetMessage!!
        val isMine = currentUser?.id == targetMsg.senderId || currentUser?.email == targetMsg.senderEmail
        AlertDialog(
            onDismissRequest = { actionTargetMessage = null },
            title = {
                Column {
                    Text("Message Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    // Quick Emoji Reaction Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val quickEmojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
                            quickEmojis.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val id = targetMsg.id
                                            actionTargetMessage = null
                                            onToggleReaction(id, emoji)
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Text(
                                text = "➕",
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val id = targetMsg.id
                                        actionTargetMessage = null
                                        customEmojiTargetMessageId = id
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Message Info Action
                    TextButton(
                        onClick = {
                            val msg = targetMsg
                            actionTargetMessage = null
                            messageInfoTarget = msg
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Message Info", tint = Emerald500)
                            Spacer(Modifier.width(10.dp))
                            Text("Message Info (Read & Delivered)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Reply Action
                    TextButton(
                        onClick = {
                            replyingToMessage = targetMsg
                            actionTargetMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = Emerald500)
                            Spacer(Modifier.width(10.dp))
                            Text("Reply to Message", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Copy Text Action
                    if (targetMsg.text.isNotBlank()) {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(targetMsg.text))
                                actionTargetMessage = null
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                Spacer(Modifier.width(10.dp))
                                Text("Copy Text", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Forward Action
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(targetMsg.text))
                                actionTargetMessage = null
                                Toast.makeText(context, "Message copied for forwarding", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = "Forward")
                                Spacer(Modifier.width(10.dp))
                                Text("Forward Message", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Download Media Action
                    if (!targetMsg.mediaUri.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                val uri = targetMsg.mediaUri
                                actionTargetMessage = null
                                scope.launch {
                                    val ok = ImageStorageHelper.downloadMediaFile(context, uri)
                                    if (ok) Toast.makeText(context, "Saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                                Spacer(Modifier.width(10.dp))
                                Text("Download Media / File", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Delete for Me Action
                    TextButton(
                        onClick = {
                            val id = targetMsg.id
                            actionTargetMessage = null
                            onDeleteForMe(id)
                            Toast.makeText(context, "Message deleted for you", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete for Me", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text("Delete for Me", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Delete for Everyone Action
                    if (isMine) {
                        TextButton(
                            onClick = {
                                val id = targetMsg.id
                                actionTargetMessage = null
                                onDeleteForEveryone(id)
                                Toast.makeText(context, "Message deleted for everyone", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete for Everyone", tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(10.dp))
                                Text("Delete for Everyone", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTargetMessage = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (messageInfoTarget != null) {
        val infoMsg = messageInfoTarget!!
        val normalizedStatus = MessageStatus.normalize(infoMsg.status)
        AlertDialog(
            onDismissRequest = { messageInfoTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MessageStatusTicks(status = normalizedStatus, iconSize = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message Delivery & Read Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sent to Server (Single Gray Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(formatTime(infoMsg.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Delivered to Recipient (Double Gray Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (infoMsg.deliveredAt != null) formatTime(infoMsg.deliveredAt)
                                else if (normalizedStatus in listOf(MessageStatus.DELIVERED, MessageStatus.READ)) "Delivered"
                                else "Pending recipient device connection",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = StatusReadBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Read by Recipient (Double Blue Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (infoMsg.readAt != null) formatTime(infoMsg.readAt)
                                else if (normalizedStatus == MessageStatus.READ) "Seen on screen"
                                else "Not yet seen on recipient's screen",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageInfoTarget = null }) {
                    Text("Done")
                }
            }
        )
    }
}
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    displayText: String,
    isTranslated: Boolean = false,
    targetLanguageName: String = "",
    isFromMe: Boolean,
    currentUserId: String?,
    onToggleReaction: (String) -> Unit,
    onLongPress: () -> Unit,
    onSwipeToReply: () -> Unit,
    onVotePoll: (optionIndex: Int) -> Unit,
    onMediaClick: () -> Unit = {},
    onEmailBadgeClick: () -> Unit,
    onStatusTicksClick: () -> Unit = {},
    onFecharGoogleMeet: () -> Unit = {},
    isGroupChat: Boolean = false,
    senderAvatarUri: String? = null
) {
    ChatBubble(
        text = displayText,
        senderName = message.senderName,
        isFromMe = isFromMe,
        timestampText = formatTime(message.timestamp),
        status = message.status,
        deliveredAt = message.deliveredAt,
        readAt = message.readAt,
        emailTransportStatus = message.emailTransportStatus,
        mediaUri = message.mediaUri,
        mediaType = message.mediaType,
        callType = message.callType,
        formattedRichText = message.formattedRichText,
        isEncrypted = message.isEncrypted,
        currentUserId = currentUserId,
        reactionsJson = message.reactionsJson,
        onToggleReaction = onToggleReaction,
        onLongPress = onLongPress,
        onSwipeToReply = onSwipeToReply,
        onVotePoll = onVotePoll,
        onMediaClick = onMediaClick,
        onEmailBadgeClick = onEmailBadgeClick,
        onStatusTicksClick = onStatusTicksClick,
        onFecharGoogleMeet = onFecharGoogleMeet,
        senderAvatarUri = senderAvatarUri,
        showSenderAvatar = !isFromMe && isGroupChat
    )
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(java.util.Calendar.ERA) == cal2.get(java.util.Calendar.ERA) &&
           cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatDateHeader(timestamp: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = java.util.Calendar.getInstance()
    val isToday = isSameCalendarDay(timestamp, now.timeInMillis)
    val yesterdayCal = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = isSameCalendarDay(timestamp, yesterdayCal.timeInMillis)

    return when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) -> {
            SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    peerName: String,
    isGemini: Boolean,
    onQuickGreeting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .testTag("empty_chat_placeholder")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isGemini) Color(0xFF7C4DFF).copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGemini) Icons.Default.AutoAwesome else Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = if (isGemini) Color(0xFF7C4DFF) else Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isGemini) "Chat with Gemini AI" else "No messages yet",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isGemini) "Ask questions, brainstorm ideas, draft emails, or analyze photos together."
                    else "Say hello to $peerName! Send a message below or tap a quick greeting to start chatting.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val greetings = if (isGemini) listOf("✨ Hello Gemini!", "💡 What can you do?")
                else listOf("👋 Hello $peerName!", "Hey, how are you?")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    greetings.forEach { greeting ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Emerald500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onQuickGreeting(greeting) }
                        ) {
                            Text(
                                text = greeting,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

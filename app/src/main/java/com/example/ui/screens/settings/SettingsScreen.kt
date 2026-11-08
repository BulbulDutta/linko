package com.example.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.UserEntity
import com.example.ui.components.QrCodeGenerator
import com.example.ui.components.UserAvatar
import com.example.ui.components.WallpaperPreviewDialog
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ChatWallpaper
import java.io.File
import java.io.FileOutputStream
import com.example.ui.theme.ChatWallpaperBackground
import com.example.util.AppLanguage
import com.example.util.LocalizationManager

enum class SettingsDialogType {
    NONE,
    ACCOUNT,
    PRIVACY,
    LISTS,
    CHATS,
    APPEARANCE,
    BROADCASTS,
    NOTIFICATIONS,
    STORAGE,
    ACCESSIBILITY,
    LANGUAGE,
    HELP,
    ACCOUNTS_CENTRE,
    TERMS_AND_CONDITIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserEntity?,
    isDarkMode: Boolean,
    currentPalette: AppThemePalette = AppThemePalette.EMERALD,
    currentWallpaper: ChatWallpaper = ChatWallpaper.DOODLE,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    fcmToken: String = "Active",
    onRefreshFcmToken: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onToggleDarkMode: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectWallpaper: (ChatWallpaper) -> Unit,
    onApplyCustomWallpaper: (uri: String, dimming: Float, scale: String) -> Unit = { _, _, _ -> },
    onTriggerMessageNotification: () -> Unit = {},
    onTriggerCallNotification: (isVideo: Boolean) -> Unit = {},
    onTriggerFriendRequestNotification: () -> Unit = {},
    onUpdateProfile: (displayName: String, statusMessage: String) -> Unit,
    onUpdateAvatarUri: (String?) -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showQrModal by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarBottomSheet by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeDialog by remember { mutableStateOf(SettingsDialogType.NONE) }

    var editDisplayName by remember { mutableStateOf(user?.displayName ?: "") }
    var editStatusMessage by remember { mutableStateOf(user?.statusMessage ?: "") }

    var pendingCustomWallpaperUri by remember { mutableStateOf<String?>(null) }
    var showWallpaperPreviewDialog by remember { mutableStateOf(false) }

    val avatarGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedUri = copyUriToInternalStorage(context, uri)
            onUpdateAvatarUri(savedUri)
        }
    }

    val avatarCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedUri = saveBitmapToInternalStorage(context, bitmap)
            onUpdateAvatarUri(savedUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            avatarCameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission required to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCustomWallpaperUri = uri.toString()
            showWallpaperPreviewDialog = true
        }
    }

    if (showWallpaperPreviewDialog && pendingCustomWallpaperUri != null) {
        WallpaperPreviewDialog(
            imageUri = pendingCustomWallpaperUri!!,
            initialDimming = customWallpaperDimming,
            initialScale = customWallpaperScale,
            isDarkMode = isDarkMode,
            onDismiss = { showWallpaperPreviewDialog = false },
            onApplyWallpaper = { dimming, scale ->
                onApplyCustomWallpaper(pendingCustomWallpaperUri!!, dimming, scale)
                showWallpaperPreviewDialog = false
            }
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LocalizationManager.getString("settings_title", currentLanguage),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("settings_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { showQrModal = true },
                        modifier = Modifier.testTag("settings_qr_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "My QR Code",
                            tint = currentPalette.primaryColor
                        )
                    }
                }
            }

            // Inline Search Bar
            AnimatedVisibility(visible = showSearch) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(LocalizationManager.getString("settings_search_placeholder", currentLanguage), fontSize = 14.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentPalette.primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Centered Profile Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            showAvatarBottomSheet = true
                        }
                        .testTag("profile_avatar_clickable"),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    UserAvatar(
                        name = user?.displayName ?: "User",
                        avatarUri = user?.avatarUri,
                        size = 96.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(currentPalette.primaryColor)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Edit Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user?.displayName ?: "User Name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Bio Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable {
                            editDisplayName = user?.displayName ?: ""
                            editStatusMessage = user?.statusMessage ?: ""
                            showEditProfileDialog = true
                        }
                        .testTag("status_bio_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (user?.statusMessage.isNullOrBlank()) "💬 Mid-week mood?" else "💬 ${user?.statusMessage}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user?.email ?: "email@linko.com",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(8.dp))

            // Settings Menu List Items
            val query = searchQuery.trim().lowercase()

            val menuItems = listOf(
                SettingsMenuItemData(
                    id = "payments",
                    icon = Icons.Default.CreditCard,
                    title = LocalizationManager.getString("item_payments", currentLanguage),
                    subtitle = LocalizationManager.getString("item_payments_sub", currentLanguage),
                    onClick = {
                        Toast.makeText(context, "This feature is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                ),
                SettingsMenuItemData(
                    id = "account",
                    icon = Icons.Default.Key,
                    title = LocalizationManager.getString("item_account", currentLanguage),
                    subtitle = LocalizationManager.getString("item_account_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.ACCOUNT }
                ),
                SettingsMenuItemData(
                    id = "privacy",
                    icon = Icons.Default.Lock,
                    title = LocalizationManager.getString("item_privacy", currentLanguage),
                    subtitle = LocalizationManager.getString("item_privacy_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.PRIVACY }
                ),
                SettingsMenuItemData(
                    id = "lists",
                    icon = Icons.Default.ListAlt,
                    title = LocalizationManager.getString("item_lists", currentLanguage),
                    subtitle = LocalizationManager.getString("item_lists_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.LISTS }
                ),
                SettingsMenuItemData(
                    id = "chats",
                    icon = Icons.Default.Chat,
                    title = LocalizationManager.getString("item_chats", currentLanguage),
                    subtitle = LocalizationManager.getString("item_chats_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.CHATS }
                ),
                SettingsMenuItemData(
                    id = "appearance",
                    icon = Icons.Default.Palette,
                    title = LocalizationManager.getString("item_appearance", currentLanguage),
                    subtitle = LocalizationManager.getString("item_appearance_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.APPEARANCE }
                ),
                SettingsMenuItemData(
                    id = "broadcasts",
                    icon = Icons.Default.Campaign,
                    title = LocalizationManager.getString("item_broadcasts", currentLanguage),
                    subtitle = LocalizationManager.getString("item_broadcasts_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.BROADCASTS }
                ),
                SettingsMenuItemData(
                    id = "notifications",
                    icon = Icons.Default.Notifications,
                    title = LocalizationManager.getString("item_notifications", currentLanguage),
                    subtitle = LocalizationManager.getString("item_notifications_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.NOTIFICATIONS }
                ),
                SettingsMenuItemData(
                    id = "storage",
                    icon = Icons.Default.Storage,
                    title = LocalizationManager.getString("item_storage", currentLanguage),
                    subtitle = LocalizationManager.getString("item_storage_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.STORAGE }
                ),
                SettingsMenuItemData(
                    id = "accessibility",
                    icon = Icons.Default.Accessibility,
                    title = LocalizationManager.getString("item_accessibility", currentLanguage),
                    subtitle = LocalizationManager.getString("item_accessibility_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.ACCESSIBILITY }
                ),
                SettingsMenuItemData(
                    id = "language",
                    icon = Icons.Default.Language,
                    title = LocalizationManager.getString("item_language", currentLanguage),
                    subtitle = "${currentLanguage.nativeName} (${currentLanguage.displayName})",
                    onClick = { activeDialog = SettingsDialogType.LANGUAGE }
                ),
                SettingsMenuItemData(
                    id = "help",
                    icon = Icons.Default.HelpOutline,
                    title = LocalizationManager.getString("item_help", currentLanguage),
                    subtitle = LocalizationManager.getString("item_help_sub", currentLanguage),
                    onClick = { activeDialog = SettingsDialogType.HELP }
                ),
                SettingsMenuItemData(
                    id = "invite",
                    icon = Icons.Default.PersonAdd,
                    title = LocalizationManager.getString("item_invite", currentLanguage),
                    subtitle = LocalizationManager.getString("item_invite_sub", currentLanguage),
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Join me on Linko!")
                            putExtra(Intent.EXTRA_TEXT, "Hey! Download Linko for secure end-to-end encrypted chats and calls: https://linko.app/invite?user=${user?.username ?: "friend"}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Invite a Friend via"))
                    }
                ),
                SettingsMenuItemData(
                    id = "terms",
                    icon = Icons.Default.Description,
                    title = "Terms & Conditions",
                    subtitle = "Usage policies, storage rules & beta terms",
                    onClick = { activeDialog = SettingsDialogType.TERMS_AND_CONDITIONS }
                ),
                SettingsMenuItemData(
                    id = "feedback",
                    icon = Icons.Default.Email,
                    title = "Send Feedback",
                    subtitle = "Send bug reports or suggestions to rocketccl801@gmail.com",
                    onClick = { launchEmailFeedback(context) }
                ),
                SettingsMenuItemData(
                    id = "accounts_centre",
                    icon = Icons.Default.AccountCircle,
                    title = LocalizationManager.getString("item_accounts_centre", currentLanguage),
                    subtitle = "Control cross-app experiences across Linko accounts",
                    onClick = { activeDialog = SettingsDialogType.ACCOUNTS_CENTRE }
                )
            )

            val filteredItems = if (query.isEmpty()) menuItems else menuItems.filter {
                it.title.lowercase().contains(query) || it.subtitle.lowercase().contains(query)
            }

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                filteredItems.forEach { item ->
                    SettingsListRow(
                        icon = item.icon,
                        title = item.title,
                        subtitle = item.subtitle,
                        accentColor = currentPalette.primaryColor,
                        onClick = item.onClick,
                        testTag = "settings_item_${item.id}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.AutoResizeText(
                    text = "Sign Out Account",
                    fontWeight = FontWeight.Bold,
                    maxFontSize = 15.sp,
                    minFontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // QR Code Modal Dialog
        if (showQrModal) {
            AlertDialog(
                onDismissRequest = { showQrModal = false },
                title = { Text("My Profile QR Code", fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Friends can scan this QR code with their camera to instantly add you on Linko.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        QrCodeGenerator(payload = user?.qrPayload ?: "linko://user?email=${user?.email}")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = user?.email ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQrModal = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Edit Profile Dialog
        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Edit Profile Details") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editStatusMessage,
                            onValueChange = { editStatusMessage = it },
                            label = { Text("Status Bio") },
                            singleLine = true,
                            placeholder = { Text("e.g. Mid-week mood?") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateProfile(editDisplayName, editStatusMessage)
                            showEditProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentPalette.primaryColor)
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Sub-Dialogs for Active Features
        when (activeDialog) {
            SettingsDialogType.ACCOUNT -> AccountSettingsDialog(
                user = user,
                primaryColor = currentPalette.primaryColor,
                onLogout = {
                    activeDialog = SettingsDialogType.NONE
                    onLogout()
                },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.PRIVACY -> PrivacySettingsDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.LISTS -> ListsSettingsDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.CHATS -> ChatsSettingsDialog(
                currentWallpaper = currentWallpaper,
                customWallpaperUri = customWallpaperUri,
                customWallpaperDimming = customWallpaperDimming,
                customWallpaperScale = customWallpaperScale,
                isDarkMode = isDarkMode,
                primaryColor = currentPalette.primaryColor,
                onSelectWallpaper = onSelectWallpaper,
                onPickCustomWallpaper = { galleryLauncher.launch("image/*") },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.APPEARANCE -> AppearanceSettingsDialog(
                isDarkMode = isDarkMode,
                currentPalette = currentPalette,
                currentWallpaper = currentWallpaper,
                customWallpaperUri = customWallpaperUri,
                customWallpaperDimming = customWallpaperDimming,
                customWallpaperScale = customWallpaperScale,
                onToggleDarkMode = onToggleDarkMode,
                onSelectPalette = onSelectPalette,
                onSelectWallpaper = onSelectWallpaper,
                onPickCustomWallpaper = { galleryLauncher.launch("image/*") },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.BROADCASTS -> BroadcastsSettingsDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.NOTIFICATIONS -> NotificationsSettingsDialog(
                primaryColor = currentPalette.primaryColor,
                fcmToken = fcmToken,
                onRefreshFcmToken = onRefreshFcmToken,
                onTriggerMessageNotification = onTriggerMessageNotification,
                onTriggerCallNotification = onTriggerCallNotification,
                onTriggerFriendRequestNotification = onTriggerFriendRequestNotification,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.STORAGE -> StorageDataDashboardDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.ACCESSIBILITY -> AccessibilitySettingsDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.LANGUAGE -> LanguageSettingsDialog(
                currentLanguage = currentLanguage,
                primaryColor = currentPalette.primaryColor,
                onSelectLanguage = onSelectLanguage,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.HELP -> HelpSettingsDialog(
                primaryColor = currentPalette.primaryColor,
                onOpenTerms = { activeDialog = SettingsDialogType.TERMS_AND_CONDITIONS },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.ACCOUNTS_CENTRE -> AccountsCentreDialog(
                user = user,
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            SettingsDialogType.TERMS_AND_CONDITIONS -> TermsAndConditionsDialog(
                primaryColor = currentPalette.primaryColor,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
            else -> {}
        }

        if (showAvatarBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Profile Photo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Option 1: Choose from Gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAvatarBottomSheet = false
                                avatarGalleryLauncher.launch("image/*")
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .testTag("avatar_choose_gallery"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Choose from Gallery",
                            tint = currentPalette.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Choose from Gallery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Option 2: Take Photo with Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAvatarBottomSheet = false
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    avatarCameraLauncher.launch(null)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                            .testTag("avatar_take_photo"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Photo with Camera",
                            tint = currentPalette.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Take Photo with Camera",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Option 3: Remove Profile Picture
                    if (!user?.avatarUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showAvatarBottomSheet = false
                                    onUpdateAvatarUri(null)
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp)
                                .testTag("avatar_remove_photo"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Profile Picture",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Remove Profile Picture",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private data class SettingsMenuItemData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit
)

@Composable
private fun SettingsListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// Sub-Dialogs implementations for active setting options

@Composable
private fun AccountSettingsDialog(
    user: UserEntity?,
    primaryColor: Color,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var securityNotifications by remember { mutableStateOf(true) }
    var twoStepVerif by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Security Notifications", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Get security code notifications when a device logs in", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Switch(
                        checked = securityNotifications,
                        onCheckedChange = { securityNotifications = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Two-Step Verification", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require a PIN when registering phone number again", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Switch(
                        checked = twoStepVerif,
                        onCheckedChange = { twoStepVerif = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Email Address", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(user?.email ?: "email@linko.com", fontSize = 12.sp, color = primaryColor)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Sign Out Action Button
                Button(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_settings_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { /* Delete account */ },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account", fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun PrivacySettingsDialog(primaryColor: Color, onDismiss: () -> Unit) {
    var readReceipts by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Controls", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read Receipts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("If turned off, you won't send or receive read receipts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = readReceipts,
                        onCheckedChange = { readReceipts = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock (Biometric / PIN)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Require biometric authentication to open Linko.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = appLock,
                        onCheckedChange = { appLock = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text("Last Seen & Online", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Everyone", fontSize = 12.sp, color = primaryColor)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Profile Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("My Contacts", fontSize = 12.sp, color = primaryColor)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Blocked Contacts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("0 contacts blocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun ListsSettingsDialog(primaryColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lists Management", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Organize your people and groups into custom lists for quick filtering and broadcast messages.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Create List */ },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Create New List")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Default Lists", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Favorites (3 contacts)", fontSize = 12.sp)
                Text("• Close Friends (5 contacts)", fontSize = 12.sp)
                Text("• Work & Team (2 groups)", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun WallpaperSelectionSection(
    currentWallpaper: ChatWallpaper,
    customWallpaperUri: String?,
    customWallpaperDimming: Float,
    customWallpaperScale: String,
    isDarkMode: Boolean,
    primaryColor: Color,
    onSelectWallpaper: (ChatWallpaper) -> Unit,
    onPickCustomWallpaper: () -> Unit
) {
    val allWallpapers = remember { ChatWallpaper.selectableWallpapers }

    Text(
        text = "Chat Wallpaper (${allWallpapers.size} Options)",
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
    Text(
        text = "Current: ${currentWallpaper.displayName}",
        fontSize = 12.sp,
        color = primaryColor,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onPickCustomWallpaper,
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .testTag("select_custom_wallpaper_btn")
    ) {
        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        com.example.ui.components.AutoResizeText(
            text = "Select Custom Gallery Backdrop",
            maxFontSize = 14.sp,
            minFontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Horizontal quick-scroll strip
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallpaper_lazy_row")
    ) {
        items(allWallpapers) { wallpaper ->
            val isSelected = wallpaper == currentWallpaper
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(76.dp)
                    .clickable { onSelectWallpaper(wallpaper) }
                    .testTag("wallpaper_option_${wallpaper.id.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    ChatWallpaperBackground(
                        wallpaper = wallpaper,
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
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wallpaper.displayName,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Full multi-row grid of all old & new wallpapers for easy vertical browsing
    Text(
        text = "All Wallpapers",
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(6.dp))

    allWallpapers.chunked(3).forEach { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowItems.forEach { wallpaper ->
                val isSelected = wallpaper == currentWallpaper
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectWallpaper(wallpaper) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        ChatWallpaperBackground(
                            wallpaper = wallpaper,
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
                                    .background(primaryColor),
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
                        text = wallpaper.displayName,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun ChatsSettingsDialog(
    currentWallpaper: ChatWallpaper,
    customWallpaperUri: String?,
    customWallpaperDimming: Float,
    customWallpaperScale: String,
    isDarkMode: Boolean,
    primaryColor: Color,
    onSelectWallpaper: (ChatWallpaper) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    var mediaVisibility by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Media Visibility", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Show newly downloaded media in device gallery.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = mediaVisibility,
                        onCheckedChange = { mediaVisibility = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                WallpaperSelectionSection(
                    currentWallpaper = currentWallpaper,
                    customWallpaperUri = customWallpaperUri,
                    customWallpaperDimming = customWallpaperDimming,
                    customWallpaperScale = customWallpaperScale,
                    isDarkMode = isDarkMode,
                    primaryColor = primaryColor,
                    onSelectWallpaper = onSelectWallpaper,
                    onPickCustomWallpaper = onPickCustomWallpaper
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { /* Backup */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chat Backup & Restore", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun AppearanceSettingsDialog(
    isDarkMode: Boolean,
    currentPalette: AppThemePalette,
    currentWallpaper: ChatWallpaper,
    customWallpaperUri: String?,
    customWallpaperDimming: Float,
    customWallpaperScale: String,
    onToggleDarkMode: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectWallpaper: (ChatWallpaper) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = currentPalette.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dark Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = currentPalette.primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("App Accent Color Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppThemePalette.values()) { palette ->
                        val isSelected = palette == currentPalette
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) palette.primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(if (isSelected) 2.dp else 0.dp, palette.primaryColor, RoundedCornerShape(12.dp))
                                .clickable { onSelectPalette(palette) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(palette.displayName, color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                WallpaperSelectionSection(
                    currentWallpaper = currentWallpaper,
                    customWallpaperUri = customWallpaperUri,
                    customWallpaperDimming = customWallpaperDimming,
                    customWallpaperScale = customWallpaperScale,
                    isDarkMode = isDarkMode,
                    primaryColor = currentPalette.primaryColor,
                    onSelectWallpaper = onSelectWallpaper,
                    onPickCustomWallpaper = onPickCustomWallpaper
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun BroadcastsSettingsDialog(primaryColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Broadcast Lists", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Send messages to multiple contacts simultaneously without creating a group chat.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* New broadcast */ },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Create New Broadcast List")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun NotificationsSettingsDialog(
    primaryColor: Color,
    fcmToken: String,
    onRefreshFcmToken: () -> Unit,
    onTriggerMessageNotification: () -> Unit,
    onTriggerCallNotification: (isVideo: Boolean) -> Unit,
    onTriggerFriendRequestNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var messagesEnabled by remember {
        mutableStateOf(com.example.notification.NotificationHelper.isMessagesEnabled(context))
    }
    var friendRequestsEnabled by remember {
        mutableStateOf(com.example.notification.NotificationHelper.isFriendRequestsEnabled(context))
    }
    var callsEnabled by remember {
        mutableStateOf(com.example.notification.NotificationHelper.isCallsEnabled(context))
    }

    var selectedDmProfile by remember {
        mutableStateOf(com.example.notification.NotificationHelper.getDirectMessageSoundProfile(context))
    }
    var selectedGroupProfile by remember {
        mutableStateOf(com.example.notification.NotificationHelper.getGroupMessageSoundProfile(context))
    }

    var isDmDropdownExpanded by remember { mutableStateOf(false) }
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FCM Push Notifications & Alerts", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // FIREBASE CLOUD MESSAGING (FCM) STATUS CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Firebase Cloud Messaging (FCM)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = primaryColor
                                )
                                Text(
                                    text = "Status: Connected • Actionable Push Channels Active",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = onRefreshFcmToken,
                                modifier = Modifier.testTag("refresh_fcm_token_btn")
                            ) {
                                Text("Sync", fontSize = 12.sp, color = primaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device Token: ${fcmToken.take(28)}${if (fcmToken.length > 28) "..." else ""}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CATEGORY TOGGLES
                Text("Push Notification Categories", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("New Messages", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Direct & group message alerts with inline reply", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = messagesEnabled,
                        onCheckedChange = {
                            messagesEnabled = it
                            com.example.notification.NotificationHelper.setMessagesEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor),
                        modifier = Modifier.testTag("toggle_notif_messages")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Friend Requests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Connection invites with Accept & Decline actions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = friendRequestsEnabled,
                        onCheckedChange = {
                            friendRequestsEnabled = it
                            com.example.notification.NotificationHelper.setFriendRequestsEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor),
                        modifier = Modifier.testTag("toggle_notif_friends")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Incoming Calls", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("High-priority voice & video calls with Answer/Decline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = callsEnabled,
                        onCheckedChange = {
                            callsEnabled = it
                            com.example.notification.NotificationHelper.setCallsEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor),
                        modifier = Modifier.testTag("toggle_notif_calls")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // SOUND PROFILES SECTION
                Text("Message Sound Profiles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assign distinct notification audio tones for 1-on-1 direct chats versus group discussions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // DIRECT MESSAGES SOUND SELECTOR
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Direct Messages Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    selectedDmProfile.title,
                                    fontSize = 12.sp,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = {
                                com.example.notification.NotificationHelper.playSoundPreview(context, selectedDmProfile)
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play Preview", tint = primaryColor)
                            }
                        }

                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { isDmDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Change DM Sound Profile", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = isDmDropdownExpanded,
                                onDismissRequest = { isDmDropdownExpanded = false }
                            ) {
                                com.example.notification.NotificationSoundProfile.directMessageProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(profile.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(profile.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            selectedDmProfile = profile
                                            com.example.notification.NotificationHelper.setDirectMessageSoundProfile(context, profile)
                                            com.example.notification.NotificationHelper.playSoundPreview(context, profile)
                                            isDmDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GROUP MESSAGES SOUND SELECTOR
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Group Messages Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    selectedGroupProfile.title,
                                    fontSize = 12.sp,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = {
                                com.example.notification.NotificationHelper.playSoundPreview(context, selectedGroupProfile)
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play Preview", tint = primaryColor)
                            }
                        }

                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { isGroupDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Change Group Sound Profile", fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = isGroupDropdownExpanded,
                                onDismissRequest = { isGroupDropdownExpanded = false }
                            ) {
                                com.example.notification.NotificationSoundProfile.groupMessageProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(profile.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(profile.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            selectedGroupProfile = profile
                                            com.example.notification.NotificationHelper.setGroupMessageSoundProfile(context, profile)
                                            com.example.notification.NotificationHelper.playSoundPreview(context, profile)
                                            isGroupDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Actionable FCM Push Triggers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Test actionable notifications for new messages (tap to open chat or inline reply), friend requests (Accept/Decline), and incoming calls (Answer/Decline).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onTriggerMessageNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_dm_notif_btn")
                    ) {
                        Text("New Message", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
                                context = context,
                                data = mapOf(
                                    "type" to "group_message",
                                    "chat_id" to "group_design_team",
                                    "sender_name" to "Design Team (Marcus)",
                                    "message_text" to "Group chat alert with tone: ${selectedGroupProfile.title}",
                                    "is_group" to "true"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_group_notif_btn")
                    ) {
                        Text("Group Message", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onTriggerCallNotification(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_voice_call_notif_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Call", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onTriggerCallNotification(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trigger_video_call_notif_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video Call", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onTriggerFriendRequestNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trigger_friend_request_notif_btn")
                ) {
                    Text("Trigger Friend Request Push", color = primaryColor, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Background / Lock-Screen FCM Push (5s Delay)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a button below, then minimize Linko or lock your device. A push notification will arrive in 5 seconds to test background delivery.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            com.example.notification.ScheduledNotificationReceiver.scheduleBackgroundPush(
                                context = context,
                                type = com.example.notification.ScheduledNotificationReceiver.TYPE_MESSAGE,
                                delaySeconds = 5,
                                messageText = "Direct Message received in background! 📩"
                            )
                            Toast.makeText(context, "DM Push scheduled in 5 seconds! Minimize the app now.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⏳ DM Push", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.notification.ScheduledNotificationReceiver.scheduleBackgroundPush(
                                context = context,
                                type = com.example.notification.ScheduledNotificationReceiver.TYPE_GROUP_MESSAGE,
                                delaySeconds = 5,
                                messageText = "Group Message received in background! 👥"
                            )
                            Toast.makeText(context, "Group Push scheduled in 5 seconds! Minimize the app now.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⏳ Group Push", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        com.example.notification.ScheduledNotificationReceiver.scheduleBackgroundPush(
                            context = context,
                            type = com.example.notification.ScheduledNotificationReceiver.TYPE_CALL,
                            delaySeconds = 5,
                            isVideoCall = true
                        )
                        Toast.makeText(context, "Background Call Push scheduled in 5 seconds! Minimize the app now.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("⏳ Incoming Call Push (5s Delay)")
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        com.example.notification.ScheduledNotificationReceiver.scheduleBackgroundPush(
                            context = context,
                            type = com.example.notification.ScheduledNotificationReceiver.TYPE_FRIEND_REQUEST,
                            delaySeconds = 5
                        )
                        Toast.makeText(context, "Background Friend Request Push scheduled in 5 seconds! Minimize the app now.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("⏳ Friend Request Push (5s Delay)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun AccessibilitySettingsDialog(primaryColor: Color, onDismiss: () -> Unit) {
    var highContrast by remember { mutableStateOf(false) }
    var reduceAnimations by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accessibility Options", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Increase Visual Contrast", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = highContrast,
                        onCheckedChange = { highContrast = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reduce UI Animations", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = reduceAnimations,
                        onCheckedChange = { reduceAnimations = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = primaryColor)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun LanguageSettingsDialog(
    currentLanguage: AppLanguage,
    primaryColor: Color,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allLanguages = remember { AppLanguage.sortedAlphabetically() }
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.nativeName.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = LocalizationManager.getString("item_language", currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${allLanguages.size} Universal Global Languages (A-Z)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search language (A-Z)...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                if (filteredLanguages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching language found",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(filteredLanguages, key = { it.code }) { lang ->
                            val isSelected = lang == currentLanguage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectLanguage(lang)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = primaryColor)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(LocalizationManager.getString("btn_done", currentLanguage)) }
        }
    )
}

fun launchEmailFeedback(context: Context) {
    val recipient = "rocketccl801@gmail.com"
    val subject = "Linko App Feedback - Bug Report/Suggestion"
    val mailtoUri = Uri.parse("mailto:$recipient?subject=${Uri.encode(subject)}")

    val intent = Intent(Intent.ACTION_SENDTO, mailtoUri)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            context.startActivity(Intent.createChooser(fallbackIntent, "Send Feedback via Email"))
        } catch (ex: Exception) {
            Toast.makeText(context, "No email client found on device", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun HelpSettingsDialog(
    primaryColor: Color,
    onOpenTerms: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help & Feedback", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Linko Version 2.5.0 (Build 2026)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("End-to-End Encrypted Messaging & Media Sharing Platform.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { launchEmailFeedback(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTerms()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Terms & Conditions")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun TermsAndConditionsDialog(
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Terms Icon",
                    tint = primaryColor
                )
                Text("Terms & Conditions", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Please read our Terms and Conditions carefully before using Linko.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val termsText = """
1. Introduction & Usage: Welcome to Linko! This is a modern messaging and media-sharing platform designed for seamless communication.

2. Storage & Media: All photos, videos, and documents shared via Linko are saved directly to your device's local storage (Linko Media folder). We do not add any watermarks to your files.

3. Beta Version & Bugs: Please note that the app is currently in the development/beta phase. You may encounter temporary bugs, data mismatches, or errors. These are known issues and will be fixed in our upcoming updates.

4. Feedback & Support: Your feedback is highly valuable to us. If you face any issues or have suggestions, please use the Feedback button below to contact us directly.
                """.trimIndent()

                Text(
                    text = termsText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        launchEmailFeedback(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("terms_send_feedback_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Send Feedback",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Feedback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun AccountsCentreDialog(user: UserEntity?, primaryColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accounts Centre", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Manage your connected Linko profile and cross-device sync preferences.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Connected Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(user?.displayName ?: "User", fontSize = 14.sp, color = primaryColor)
                        Text(user?.email ?: "email@linko.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String {
    return try {
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        uri.toString()
    }
}

private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    return try {
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Surface
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.Emerald500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientProfileSheet(
    chat: ChatEntity?,
    messages: List<MessageEntity>,
    recipientContact: ContactEntity? = null,
    currentUser: UserEntity? = null,
    allContacts: List<ContactEntity> = emptyList(),
    isOnline: Boolean = false,
    isBlocked: Boolean = false,
    onToggleBlockUser: ((Boolean) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onClearChatHistory: () -> Unit,
    onDeleteChat: () -> Unit,
    onAddGroupMembers: ((chatId: String, selectedContacts: List<ContactEntity>) -> Unit)? = null,
    onToggleAdminRole: ((chatId: String, memberUserId: String, makeAdmin: Boolean) -> Unit)? = null,
    onRemoveMember: ((chatId: String, memberUserId: String) -> Unit)? = null,
    onSendPrivateMessage: ((contactUserId: String, contactEmail: String, contactName: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }
    var selectedMediaTab by remember { mutableIntStateOf(0) } // 0: Photos, 1: Docs, 2: Audio

    val sharedMediaList = remember(messages) {
        messages.filter { !it.mediaUri.isNullOrBlank() || it.mediaType in listOf("IMAGE", "DOCUMENT", "AUDIO", "VIDEO") }
    }

    val sharedImages = remember(sharedMediaList) {
        sharedMediaList.filter { it.mediaType == "IMAGE" || (it.mediaUri != null && !it.mediaUri.contains(".pdf")) }
    }
    val sharedDocs = remember(sharedMediaList) {
        sharedMediaList.filter { it.mediaType == "DOCUMENT" || (it.mediaUri != null && it.mediaUri.contains(".pdf")) }
    }
    val sharedAudio = remember(sharedMediaList) {
        sharedMediaList.filter { it.mediaType == "AUDIO" }
    }

    val participantIds = remember(chat?.participantIdsJson) {
        try {
            if (chat?.participantIdsJson.isNullOrBlank()) emptyList()
            else {
                val jsonArray = org.json.JSONArray(chat.participantIdsJson)
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    val currentUserId = currentUser?.id?.ifBlank { null }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        ?: ""
    val currentUserEmail = currentUser?.email?.lowercase()?.trim()
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
        ?: ""
    val currentDisplayName = currentUser?.displayName?.trim()
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName?.trim()
        ?: ""

    val otherUserId = participantIds.find { id ->
        val clean = id.lowercase().trim()
        clean != currentUserId.lowercase().trim() &&
        (currentUserEmail.isBlank() || clean != currentUserEmail)
    }
    val resolvedContact = recipientContact ?: if (otherUserId != null) {
        allContacts.find {
            it.contactUserId.equals(otherUserId, ignoreCase = true) ||
            it.contactEmail.equals(otherUserId, ignoreCase = true) ||
            (otherUserId.contains("@") && it.contactEmail.contains(otherUserId, ignoreCase = true))
        }
    } else null

    val isSelfNameCandidate: (String) -> Boolean = { candidate ->
        val clean = candidate.trim().lowercase()
        clean.isNotBlank() && (
            (currentDisplayName.isNotBlank() && clean == currentDisplayName.lowercase()) ||
            (currentUserEmail.isNotBlank() && clean == currentUserEmail) ||
            (currentUserEmail.contains("@") && clean == currentUserEmail.substringBefore("@"))
        )
    }

    val recipientName = when {
        resolvedContact != null && !isSelfNameCandidate(resolvedContact.contactDisplayName) -> resolvedContact.contactDisplayName
        otherUserId != null && otherUserId.contains("@") -> otherUserId.substringBefore("@").replace(".", " ").split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        !chat?.name.isNullOrBlank() && !isSelfNameCandidate(chat.name) && !com.example.data.repository.LinkoRepository.isGenericPlaceholderName(chat.name, currentDisplayName, currentUserEmail) -> chat.name
        otherUserId != null && otherUserId.isNotBlank() -> "User ${otherUserId.takeLast(4)}"
        else -> "Contact"
    }
    val recipientEmail = if (chat?.isGroup == true) "Group Chat • ${sharedMediaList.size} shared files" else (resolvedContact?.contactEmail ?: "${recipientName.lowercase().replace(" ", ".")}@linko.net")
    val avatarUri = resolvedContact?.contactAvatarUri ?: chat?.groupIconUri
    val recipientUserId = if (chat?.isGroup == true) chat.id else (resolvedContact?.contactUserId ?: otherUserId ?: "user_unknown")
    val isGeminiAi = chat?.id == "chat_gemini_ai" ||
        recipientUserId == "user_gemini" ||
        recipientEmail.contains("gemini", ignoreCase = true) ||
        recipientName.contains("Gemini", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (chat?.isGroup == true) "Group Info" else "Contact Info",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Header Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    name = recipientName,
                    avatarUri = avatarUri,
                    isGroup = chat?.isGroup ?: false,
                    size = 96.dp,
                    showOnlineStatus = true,
                    isOnline = isOnline
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = recipientName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = Emerald500.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = "User ID",
                            tint = Emerald500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ID: $recipientUserId",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recipientEmail,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (chat?.isGroup == true) chat.groupDescription ?: "Official Linko Chat Group" else (recipientContact?.contactStatus ?: "Hey there! I am using Linko. 💬"),
                    fontSize = 13.sp,
                    color = Emerald500,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unified Communication Call Shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = { onStartCall(false) },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = Emerald500)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Audio Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        }
                    }

                    Card(
                        onClick = { onStartCall(true) },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00897B).copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = Color(0xFF00897B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Video Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                        }
                    }
                }
            }

            // Group Members & Admin Control Section
            if (chat?.isGroup == true) {
                var showAddMembersDialog by remember { mutableStateOf(false) }

                fun parseJsonList(json: String?): List<String> {
                    if (json.isNullOrBlank()) return emptyList()
                    return try {
                        org.json.JSONArray(json).let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val participantIds = remember(chat.participantIdsJson) { parseJsonList(chat.participantIdsJson) }
                val adminIds = remember(chat.adminIdsJson) { parseJsonList(chat.adminIdsJson) }
                val currentUserId = currentUser?.id ?: ""
                val isCurrentAdmin = currentUserId in adminIds || (adminIds.isEmpty() && currentUserId in participantIds)

                data class GroupMemberItem(
                    val userId: String,
                    val name: String,
                    val email: String,
                    val avatarUri: String?,
                    val isAdmin: Boolean
                )

                val membersList = remember(participantIds, adminIds, currentUser, allContacts) {
                    participantIds.map { id ->
                        if (currentUser != null && currentUser.id == id) {
                            GroupMemberItem(
                                userId = id,
                                name = "${currentUser.displayName} (You)",
                                email = currentUser.email,
                                avatarUri = currentUser.avatarUri,
                                isAdmin = id in adminIds || (adminIds.isEmpty() && id == participantIds.firstOrNull())
                            )
                        } else {
                            val match = allContacts.find { it.contactUserId == id }
                            if (match != null) {
                                GroupMemberItem(
                                    userId = id,
                                    name = match.contactDisplayName,
                                    email = match.contactEmail,
                                    avatarUri = match.contactAvatarUri,
                                    isAdmin = id in adminIds
                                )
                            } else {
                                GroupMemberItem(
                                    userId = id,
                                    name = "Member ${id.takeLast(4)}",
                                    email = "user_$id@linko.net",
                                    avatarUri = null,
                                    isAdmin = id in adminIds
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Group Members (${membersList.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCurrentAdmin) "You are a Group Admin" else "Group Directory",
                                fontSize = 12.sp,
                                color = Emerald500
                            )
                        }

                        Button(
                            onClick = { showAddMembersDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Members", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Members", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            membersList.forEachIndexed { index, member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = member.name,
                                        avatarUri = member.avatarUri,
                                        size = 40.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (member.isAdmin) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = Emerald500.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Admin",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald500,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = member.email,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Context menu for member management
                                    Box {
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        IconButton(
                                            onClick = { menuExpanded = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Member Menu", modifier = Modifier.size(18.dp))
                                        }

                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            if (isCurrentAdmin && member.userId != currentUserId) {
                                                if (member.isAdmin) {
                                                    DropdownMenuItem(
                                                        text = { Text("Dismiss as Admin") },
                                                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onToggleAdminRole?.invoke(chat.id, member.userId, false)
                                                        }
                                                    )
                                                } else {
                                                    DropdownMenuItem(
                                                        text = { Text("Make Group Admin") },
                                                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Emerald500) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onToggleAdminRole?.invoke(chat.id, member.userId, true)
                                                        }
                                                    )
                                                }

                                                DropdownMenuItem(
                                                    text = { Text("Remove from Group", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        onRemoveMember?.invoke(chat.id, member.userId)
                                                    }
                                                )
                                            }

                                            DropdownMenuItem(
                                                text = { Text("Direct Message") },
                                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onDismissRequest()
                                                    onSendPrivateMessage?.invoke(member.userId, member.email, member.name)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (index < membersList.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Dialog to add members from contacts list
                if (showAddMembersDialog) {
                    val availableToAdd = remember(allContacts, participantIds) {
                        allContacts.filter { it.contactUserId !in participantIds }
                    }
                    val selectedContacts = remember { mutableStateListOf<ContactEntity>() }

                    AlertDialog(
                        onDismissRequest = { showAddMembersDialog = false },
                        title = { Text("Add Members to Group", fontWeight = FontWeight.Bold) },
                        text = {
                            if (availableToAdd.isEmpty()) {
                                Text("All your saved contacts are already in this group chat!")
                            } else {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableToAdd.forEach { contact ->
                                        val isChecked = contact in selectedContacts
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) selectedContacts.remove(contact)
                                                    else selectedContacts.add(contact)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedContacts.add(contact)
                                                    else selectedContacts.remove(contact)
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            UserAvatar(name = contact.contactDisplayName, avatarUri = contact.contactAvatarUri, size = 36.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(contact.contactDisplayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                Text(contact.contactEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            if (availableToAdd.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onAddGroupMembers?.invoke(chat.id, selectedContacts.toList())
                                        showAddMembersDialog = false
                                        Toast.makeText(context, "Added ${selectedContacts.size} member(s)", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    enabled = selectedContacts.isNotEmpty()
                                ) {
                                    Text("Add Selected (${selectedContacts.size})")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddMembersDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Shared Media Section
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shared Media & Files",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sharedMediaList.size} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedMediaTab) {
                    Tab(
                        selected = selectedMediaTab == 0,
                        onClick = { selectedMediaTab = 0 },
                        text = { Text("Photos (${sharedImages.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedMediaTab == 1,
                        onClick = { selectedMediaTab = 1 },
                        text = { Text("Docs (${sharedDocs.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedMediaTab == 2,
                        onClick = { selectedMediaTab = 2 },
                        text = { Text("Audio (${sharedAudio.size})", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedMediaTab) {
                    0 -> {
                        if (sharedImages.isEmpty()) {
                            Text("No shared photos in this chat.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sharedImages) { msg ->
                                    Card(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clickable { previewMediaUri = msg.mediaUri },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = msg.mediaUri,
                                            contentDescription = "Shared photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (sharedDocs.isEmpty()) {
                            Text("No shared documents in this chat.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                sharedDocs.take(5).forEach { docMsg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Emerald500)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(docMsg.text.ifBlank { "Document File" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(docMsg.timestamp))
                                            Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (sharedAudio.isEmpty()) {
                            Text("No voice notes or audio shared.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                sharedAudio.take(5).forEach { audioMsg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Emerald500)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(audioMsg.text.ifBlank { "Voice Recording Note" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(audioMsg.timestamp))
                                            Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // User Account & Privacy Options
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text(
                    text = "Privacy & Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mute notifications
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Mute Notifications", fontSize = 14.sp)
                    }
                    Switch(
                        checked = isMuted,
                        onCheckedChange = {
                            isMuted = it
                            Toast.makeText(context, if (it) "Notifications muted for $recipientName" else "Notifications unmuted", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald500, checkedTrackColor = Emerald500.copy(alpha = 0.3f))
                    )
                }

                // Block User (Strictly hidden for Gemini AI bot and group chats)
                if (!isGeminiAi && chat?.isGroup != true) {
                    Card(
                        onClick = {
                            val newBlocked = !isBlocked
                            onToggleBlockUser?.invoke(newBlocked)
                            val msg = if (newBlocked) "Blocked $recipientName" else "Unblocked $recipientName"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("profile_sheet_block_user_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBlocked) {
                                Emerald500.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isBlocked) Emerald500 else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isBlocked) "Unblock User ($recipientName)" else "Block User ($recipientName)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBlocked) Emerald500 else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Clear Chat History
                Card(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Clear Chat History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Delete Chat
                Card(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Entire Chat Conversation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat History?") },
            text = { Text("Are you sure you want to delete all messages in this conversation with $recipientName?") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearChatHistory()
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Messages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat?") },
            text = { Text("This will permanently remove this chat conversation with $recipientName from your Linko app.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteChat()
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image Preview Modal
    if (previewMediaUri != null) {
        Dialog(onDismissRequest = { previewMediaUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewMediaUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewMediaUri,
                    contentDescription = "Full Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.ui.theme.Emerald500
import com.example.ui.viewmodel.SharedIncomingContent

/**
 * Represents a selectable recipient (either a Contact or an existing Direct/Group Chat)
 * in the "Share to Linko" picker sheet.
 */
data class ShareRecipientOption(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val avatarUri: String?,
    val isGroup: Boolean = false,
    val isOnline: Boolean = false,
    val contact: ContactEntity? = null,
    val chatId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareToLinkoSheet(
    sharedContent: SharedIncomingContent,
    contacts: List<ContactEntity>,
    chats: List<ChatEntity>,
    isUserOnline: (String?) -> Boolean,
    onSendToRecipients: (
        finalMessageText: String,
        selectedContacts: List<ContactEntity>,
        selectedChatIds: List<String>
    ) -> Unit,
    onQuickAddContactByEmail: (String, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialText = remember(sharedContent) {
        buildString {
            if (!sharedContent.sharedSubject.isNullOrBlank() &&
                !sharedContent.sharedText.contains(sharedContent.sharedSubject, ignoreCase = true)
            ) {
                append(sharedContent.sharedSubject.trim())
                append("\n")
            }
            append(sharedContent.sharedText.trim())
        }
    }

    var editableSharedText by remember(sharedContent) { mutableStateOf(initialText) }
    var optionalComment by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showQuickAddEmail by remember { mutableStateOf(false) }
    var newContactEmail by remember { mutableStateOf("") }

    // Build unified list of recipients (contacts + group chats + Gemini AI)
    val recipientOptions = remember(contacts, chats) {
        val options = mutableListOf<ShareRecipientOption>()
        val addedEmails = mutableSetOf<String>()
        val addedChatIds = mutableSetOf<String>()

        // 1. Add all saved friends/contacts first
        contacts.forEach { contact ->
            val cleanEmail = contact.contactEmail.lowercase().trim()
            if (cleanEmail.isNotBlank()) addedEmails.add(cleanEmail)
            options.add(
                ShareRecipientOption(
                    key = "contact_${contact.id}",
                    displayName = contact.contactDisplayName.ifBlank { contact.contactEmail },
                    subtitle = contact.contactEmail.ifBlank { "Linko Friend" },
                    avatarUri = contact.contactAvatarUri,
                    isGroup = false,
                    isOnline = isUserOnline(contact.contactEmail),
                    contact = contact,
                    chatId = null
                )
            )
        }

        // 2. Add any existing chats (groups, Gemini AI, or direct chats not already covered by contacts)
        chats.forEach { chat ->
            val matchesExistingContact = !chat.isGroup && contacts.any { c ->
                c.contactDisplayName.equals(chat.name, ignoreCase = true) ||
                    (c.contactEmail.isNotBlank() && chat.participantIdsJson.contains(c.contactEmail, ignoreCase = true))
            }
            if (!matchesExistingContact && addedChatIds.add(chat.id)) {
                options.add(
                    ShareRecipientOption(
                        key = "chat_${chat.id}",
                        displayName = chat.name.ifBlank { "Linko Chat" },
                        subtitle = when {
                            chat.id == "chat_gemini_ai" -> "AI Assistant • Instant Analysis"
                            chat.isGroup -> "Group Conversation"
                            else -> "Recent Conversation"
                        },
                        avatarUri = chat.groupIconUri,
                        isGroup = chat.isGroup,
                        isOnline = chat.id == "chat_gemini_ai",
                        contact = null,
                        chatId = chat.id
                    )
                )
            }
        }
        options
    }

    val selectedKeys = remember { mutableStateListOf<String>() }

    val filteredOptions = remember(recipientOptions, searchQuery) {
        if (searchQuery.isBlank()) {
            recipientOptions
        } else {
            recipientOptions.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val detectedPlatformBadge = remember(sharedContent.sharedText) {
        val lower = sharedContent.sharedText.lowercase()
        when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube Link"
            lower.contains("instagram.com") -> "Instagram Share"
            lower.contains("http://") || lower.contains("https://") -> "Shared Web URL"
            else -> "Shared Text"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share via Linko",
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Share via Linko",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = Emerald500.copy(alpha = 0.16f),
                                contentColor = Emerald500
                            ) {
                                Text(
                                    text = detectedPlatformBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Select one or more friends to forward this shared content",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_share_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close share sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shared Content Preview & Editable Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sharedContent.sharedSubject?.takeIf { it.isNotBlank() } ?: "Shared Content Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editableSharedText,
                        onValueChange = { editableSharedText = it },
                        label = { Text("Shared URL / Text") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shared_content_text_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = optionalComment,
                        onValueChange = { optionalComment = it },
                        placeholder = { Text("Add an optional message or caption...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_optional_comment_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Friends / Add Friend Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends or chats...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search recipients",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("share_search_recipients_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showQuickAddEmail = !showQuickAddEmail },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f))
                        .testTag("share_quick_add_friend_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Friend by Email",
                        tint = Emerald500
                    )
                }
            }

            if (showQuickAddEmail) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newContactEmail,
                        onValueChange = { newContactEmail = it },
                        placeholder = { Text("friend@example.com", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_new_contact_email_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newContactEmail.isNotBlank()) {
                                onQuickAddContactByEmail(newContactEmail.trim()) { added ->
                                    if (added) {
                                        newContactEmail = ""
                                        showQuickAddEmail = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Selected Recipient Chips
            if (selectedKeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedKeys.forEach { key ->
                        val item = recipientOptions.find { it.key == key }
                        if (item != null) {
                            AssistChip(
                                onClick = { selectedKeys.remove(key) },
                                label = { Text(item.displayName, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove ${item.displayName}",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Emerald500.copy(alpha = 0.14f),
                                    labelColor = Emerald500
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            // Friends & Recent Chats List
            if (filteredOptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No friends or chats found",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + person icon above to add a friend by email.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                ) {
                    items(filteredOptions, key = { it.key }) { option ->
                        val isSelected = option.key in selectedKeys
                        ShareRecipientRow(
                            option = option,
                            isSelected = isSelected,
                            onToggle = {
                                if (isSelected) {
                                    selectedKeys.remove(option.key)
                                } else {
                                    selectedKeys.add(option.key)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Send Button
            Button(
                onClick = {
                    val combinedMessage = buildString {
                        append(editableSharedText.trim())
                        if (optionalComment.isNotBlank()) {
                            append("\n\n💬 ")
                            append(optionalComment.trim())
                        }
                    }
                    val chosenOptions = recipientOptions.filter { it.key in selectedKeys }
                    val chosenContacts = chosenOptions.mapNotNull { it.contact }
                    val chosenChatIds = chosenOptions.mapNotNull { it.chatId }
                    onSendToRecipients(combinedMessage, chosenContacts, chosenChatIds)
                },
                enabled = selectedKeys.isNotEmpty() && editableSharedText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_share_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedKeys.isEmpty()) {
                        "Select a Friend to Send"
                    } else {
                        "Send to ${selectedKeys.size} Recipient${if (selectedKeys.size > 1) "s" else ""}"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedKeys.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareRecipientRow(
    option: ShareRecipientOption,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Emerald500.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .testTag("share_recipient_${option.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.chatId == "chat_gemini_ai") {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                UserAvatar(
                    name = option.displayName,
                    avatarUri = option.avatarUri,
                    isGroup = option.isGroup,
                    size = 44.dp,
                    showOnlineStatus = !option.isGroup,
                    isOnline = option.isOnline
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Emerald500 else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

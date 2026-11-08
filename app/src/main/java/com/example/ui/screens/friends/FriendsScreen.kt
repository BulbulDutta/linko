package com.example.ui.screens.friends

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CallType
import com.example.ui.components.ShareProfileQrDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500

import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Surface

@Composable
fun FriendsScreen(
    contacts: List<ContactEntity>,
    currentUser: UserEntity? = null,
    isUserOnline: (String?) -> Boolean = { false },
    isContactBlocked: (ContactEntity) -> Boolean = { it.isBlocked },
    onToggleBlockContact: (ContactEntity, Boolean) -> Unit = { _, _ -> },
    onOpenChat: (ContactEntity) -> Unit,
    onStartCall: (contactName: String, contactEmail: String, contactAvatar: String?, callType: CallType) -> Unit,
    onAddContactClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showShareQrDialog by remember { mutableStateOf(false) }

    if (showShareQrDialog) {
        ShareProfileQrDialog(
            user = currentUser,
            onDismiss = { showShareQrDialog = false }
        )
    }

    val filteredContacts = remember(contacts, searchQuery) {
        contacts.filter { contact ->
            contact.contactDisplayName.contains(searchQuery, ignoreCase = true) ||
                    contact.contactEmail.contains(searchQuery, ignoreCase = true) ||
                    contact.contactUsername.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Action Bar Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search friends or email...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onAddContactClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Emerald500, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCreateGroupClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("friends_create_group_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Group",
                            tint = Emerald500
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        com.example.ui.components.AutoResizeText(
                            text = "New Group / Channel",
                            color = MaterialTheme.colorScheme.onSurface,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showShareQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("friends_share_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Share Contact QR",
                            tint = Emerald500
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        com.example.ui.components.AutoResizeText(
                            text = "My QR",
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            maxFontSize = 13.sp,
                            minFontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Contact List
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        com.example.ui.components.AutoResizeText(
                            text = if (contacts.isEmpty()) "No friends added yet" else "No matching contacts",
                            maxFontSize = 18.sp,
                            minFontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (contacts.isEmpty()) "Tap 'Add Contact' above to scan a friend's QR code or add them via email." else "Try searching for a different friend or email.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isOnline = isUserOnline(contact.contactEmail)
                        val isBlocked = isContactBlocked(contact)
                        ContactItemRow(
                            contact = contact,
                            isOnline = isOnline,
                            isBlocked = isBlocked,
                            onChatClick = { onOpenChat(contact) },
                            onToggleBlockClick = {
                                onToggleBlockContact(contact, !isBlocked)
                            },
                            onAudioCallClick = {
                                onStartCall(
                                    contact.contactDisplayName,
                                    contact.contactEmail,
                                    contact.contactAvatarUri,
                                    CallType.AUDIO
                                )
                            },
                            onVideoCallClick = {
                                onStartCall(
                                    contact.contactDisplayName,
                                    contact.contactEmail,
                                    contact.contactAvatarUri,
                                    CallType.VIDEO
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    contact: ContactEntity,
    isOnline: Boolean = false,
    isBlocked: Boolean = false,
    onChatClick: () -> Unit,
    onToggleBlockClick: () -> Unit = {},
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    val context = LocalContext.current
    val isAiBot = contact.contactUserId == "user_gemini" ||
            contact.contactEmail.contains("gemini", ignoreCase = true) ||
            contact.contactDisplayName.contains("Gemini", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = contact.contactDisplayName,
                avatarUri = contact.contactAvatarUri,
                size = 48.dp,
                showOnlineStatus = true,
                isOnline = isOnline
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.contactDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (isBlocked && !isAiBot) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Blocked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = contact.contactEmail,
                    fontSize = 12.sp,
                    color = Emerald500,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isBlocked && !isAiBot) "Blocked • Tap chat to unblock" else contact.contactStatus,
                    fontSize = 11.sp,
                    color = if (isBlocked && !isAiBot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onChatClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat",
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // AI Exception: Hide Block option completely for Gemini AI bot
                if (!isAiBot) {
                    IconButton(onClick = onAudioCallClick) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onVideoCallClick) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleBlockClick,
                        modifier = Modifier.testTag("friends_block_button_${contact.id}")
                    ) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = if (isBlocked) "Unblock Contact" else "Block Contact",
                            tint = if (isBlocked) Emerald500 else MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(onClick = {
                        Toast.makeText(context, "Calling is not supported for AI Assistant. Please type your message.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call (Disabled for AI)",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Calling is not supported for AI Assistant. Please type your message.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video (Disabled for AI)",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

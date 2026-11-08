package com.example.ui.screens.calls

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.model.CallType
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsScreen(

    callLogs: List<CallLogEntity>,
    contacts: List<ContactEntity> = emptyList(),
    onStartCall: (contactName: String, contactEmail: String, contactAvatar: String?, callType: CallType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStartCallModal by remember { mutableStateOf(false) }
    var selectedCallTypeFilter by remember { mutableStateOf<CallType?>(null) }

    if (showStartCallModal) {
        StartCallDialog(
            contacts = contacts,
            initialCallType = selectedCallTypeFilter ?: CallType.AUDIO,
            onStartCall = { name, email, avatar, type ->
                showStartCallModal = false
                onStartCall(name, email, avatar, type)
            },
            onDismiss = { showStartCallModal = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Quick call header actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        selectedCallTypeFilter = CallType.AUDIO
                        showStartCallModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    com.example.ui.components.AutoResizeText(
                        text = "New Voice Call",
                        color = Color.White,
                        maxFontSize = 13.sp,
                        minFontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        selectedCallTypeFilter = CallType.VIDEO
                        showStartCallModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    com.example.ui.components.AutoResizeText(
                        text = "New Video Call",
                        color = Color.White,
                        maxFontSize = 13.sp,
                        minFontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (callLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Calls",
                            tint = Emerald500,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        com.example.ui.components.AutoResizeText(
                            text = "No recent call history",
                            maxFontSize = 17.sp,
                            minFontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start audio or video calls with your contacts directly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showStartCallModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.ui.components.AutoResizeText(
                                text = "Start a Call",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxFontSize = 14.sp,
                                minFontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(callLogs, key = { it.id }) { log ->
                        CallLogItemRow(
                            log = log,
                            onAudioCallClick = {
                                onStartCall(log.contactName, log.contactEmail, log.contactAvatarUri, CallType.AUDIO)
                            },
                            onVideoCallClick = {
                                onStartCall(log.contactName, log.contactEmail, log.contactAvatarUri, CallType.VIDEO)
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showStartCallModal = true },
            containerColor = Emerald500,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Call, contentDescription = "New Call")
        }
    }
}

@Composable
private fun CallLogItemRow(
    log: CallLogEntity,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = log.contactName, size = 48.dp)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.contactName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (log.isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Call",
                        tint = Emerald500,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.isIncoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = if (log.isMissed) MaterialTheme.colorScheme.error else Emerald500,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatDateTime(log.timestamp)} • ${if (log.durationSeconds > 0) "${log.durationSeconds}s" else "Missed"} • E2EE",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(onClick = onAudioCallClick) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Audio Call",
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onVideoCallClick) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartCallDialog(
    contacts: List<ContactEntity>,
    initialCallType: CallType = CallType.AUDIO,
    onStartCall: (name: String, email: String, avatar: String?, type: CallType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualEmail by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        contacts.filter { contact ->
            contact.contactDisplayName.contains(searchQuery, ignoreCase = true) ||
                    contact.contactEmail.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Start Audio / Video Call", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (contacts.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search contact...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Select a contact:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 280.dp)) {
                        items(filteredContacts) { contact ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(name = contact.contactDisplayName, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.contactDisplayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(contact.contactEmail, fontSize = 11.sp, color = Emerald500)
                                    }
                                    IconButton(
                                        onClick = { onStartCall(contact.contactDisplayName, contact.contactEmail, contact.contactAvatarUri, CallType.AUDIO) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = Emerald500, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onStartCall(contact.contactDisplayName, contact.contactEmail, contact.contactAvatarUri, CallType.VIDEO) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Emerald500, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Or enter details manually:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    placeholder = { Text("Contact Name", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = manualEmail,
                    onValueChange = { manualEmail = it },
                    placeholder = { Text("Contact Email (optional)", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val name = manualName.ifBlank { "Contact" }
                        val email = manualEmail.ifBlank { "${name.lowercase().replace(" ", ".")}@linko.com" }
                        onStartCall(name, email, null, CallType.AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    enabled = manualName.isNotBlank() || contacts.isEmpty()
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audio", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val name = manualName.ifBlank { "Contact" }
                        val email = manualEmail.ifBlank { "${name.lowercase().replace(" ", ".")}@linko.com" }
                        onStartCall(name, email, null, CallType.VIDEO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    enabled = manualName.isNotBlank() || contacts.isEmpty()
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


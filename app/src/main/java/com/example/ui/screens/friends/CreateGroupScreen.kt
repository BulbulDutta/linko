package com.example.ui.screens.friends

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    contacts: List<ContactEntity>,
    onBackClick: () -> Unit,
    onCreateGroup: (groupName: String, selectedContacts: List<ContactEntity>, groupDesc: String, channelType: String) -> Unit,
    onQuickAddContactByEmail: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var groupName by remember { mutableStateOf("") }
    var groupDesc by remember { mutableStateOf("") }
    var channelType by remember { mutableStateOf("GROUP") } // "GROUP" or "CHANNEL"
    var searchQuery by remember { mutableStateOf("") }
    var quickEmailInput by remember { mutableStateOf("") }
    val selectedContacts = remember { mutableStateListOf<ContactEntity>() }

    val isBroadcastChannel = channelType == "CHANNEL"

    androidx.activity.compose.BackHandler {
        onBackClick()
    }
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.contactDisplayName.contains(searchQuery, ignoreCase = true) ||
                    it.contactEmail.contains(searchQuery, ignoreCase = true) ||
                    it.contactUsername.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBroadcastChannel) "New Broadcast Channel" else "New Group Conversation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Cloud Firestore Synced • Multi-Contact Invite",
                            fontSize = 11.sp,
                            color = Emerald500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("create_group_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && selectedContacts.isNotEmpty()) {
                            onCreateGroup(groupName.trim(), selectedContacts.toList(), groupDesc.trim(), channelType)
                        }
                    },
                    enabled = groupName.isNotBlank() && selectedContacts.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_group_submit_button")
                ) {
                    Icon(
                        imageVector = if (isBroadcastChannel) Icons.Default.Campaign else Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBroadcastChannel) {
                            "Create Channel & Invite (${selectedContacts.size})"
                        } else {
                            "Create Group & Invite (${selectedContacts.size})"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Mode Selector: Group Chat vs Broadcast Channel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !isBroadcastChannel,
                    onClick = { channelType = "GROUP" },
                    label = { Text("Group Chat", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group Chat",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                        selectedLabelColor = Emerald500,
                        selectedLeadingIconColor = Emerald500
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_group_chip")
                )

                FilterChip(
                    selected = isBroadcastChannel,
                    onClick = { channelType = "CHANNEL" },
                    label = { Text("Channel", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Broadcast Channel",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                        selectedLabelColor = Emerald500,
                        selectedLeadingIconColor = Emerald500
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_channel_chip")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Emerald500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBroadcastChannel) Icons.Default.Campaign else Icons.Default.Group,
                        contentDescription = if (isBroadcastChannel) "Channel" else "Group",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(if (isBroadcastChannel) "Channel Name" else "Group Name") },
                    placeholder = { Text(if (isBroadcastChannel) "e.g. Product Announcements" else "e.g. Design Squad") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("group_name_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = groupDesc,
                onValueChange = { groupDesc = it },
                label = { Text(if (isBroadcastChannel) "Channel Topic / Description" else "Group Description (Optional)") },
                placeholder = { Text("e.g. Real-time updates, files & team discussions") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_description_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selected contacts horizontal chips
            if (selectedContacts.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedContacts, key = { "chip_${it.id}" }) { contact ->
                        InputChip(
                            selected = true,
                            onClick = { selectedContacts.remove(contact) },
                            label = { Text(contact.contactDisplayName, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${contact.contactDisplayName}",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Emerald500.copy(alpha = 0.16f),
                                selectedLabelColor = Emerald500,
                                selectedTrailingIconColor = Emerald500
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search / filter contacts bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contacts to invite...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Contacts",
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_contact_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invite Contacts (${selectedContacts.size} of ${contacts.size} selected)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (contacts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (selectedContacts.size == contacts.size) {
                                selectedContacts.clear()
                            } else {
                                contacts.forEach { c ->
                                    if (c !in selectedContacts) selectedContacts.add(c)
                                }
                            }
                        },
                        modifier = Modifier.testTag("select_all_contacts_button")
                    ) {
                        Text(
                            text = if (selectedContacts.size == contacts.size) "Clear All" else "Select All",
                            color = Emerald500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (contacts.isEmpty()) "No contacts in your list yet" else "No contacts matching \"$searchQuery\"",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        if (onQuickAddContactByEmail != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = quickEmailInput,
                                    onValueChange = { quickEmailInput = it },
                                    placeholder = { Text("friend@example.com", fontSize = 13.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (quickEmailInput.contains("@")) {
                                            onQuickAddContactByEmail(quickEmailInput.trim())
                                            quickEmailInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        val isSelected = selectedContacts.any { it.id == contact.id }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    Emerald500.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = if (isSelected) BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (isSelected) selectedContacts.removeAll { it.id == contact.id }
                                    else selectedContacts.add(contact)
                                }
                                .testTag("invite_contact_row_${contact.contactEmail}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = contact.contactDisplayName,
                                    avatarUri = contact.contactAvatarUri,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.contactDisplayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = contact.contactEmail,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (selectedContacts.none { it.id == contact.id }) {
                                                selectedContacts.add(contact)
                                            }
                                        } else {
                                            selectedContacts.removeAll { it.id == contact.id }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

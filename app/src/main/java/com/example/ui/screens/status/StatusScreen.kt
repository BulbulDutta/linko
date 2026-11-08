package com.example.ui.screens.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.data.model.UserStatusGroup
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500

import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun StatusScreen(
    currentUser: UserEntity?,
    userStatusGroups: List<UserStatusGroup>,
    onPostTextStatus: (text: String, bgHex: String, fontStyle: String) -> Unit,
    onPostMediaStatus: (mediaUri: String, mediaType: String, caption: String?) -> Unit,
    onMarkAsViewed: (statusId: String) -> Unit,
    onDeleteStatus: (statusId: String) -> Unit,
    onReplyToStatus: (userEmail: String, replyText: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showTextStatusDialog by remember { mutableStateOf(false) }
    var showMediaStatusDialog by remember { mutableStateOf(false) }
    var activeViewerGroup by remember { mutableStateOf<UserStatusGroup?>(null) }
    var showMyStatusMenu by remember { mutableStateOf(false) }

    val myStatusGroup = userStatusGroups.find { it.isCurrentUser }
    val friendStatusGroups = userStatusGroups.filter { !it.isCurrentUser }

    val filteredFriendGroups = remember(friendStatusGroups, searchQuery) {
        if (searchQuery.isBlank()) friendStatusGroups
        else friendStatusGroups.filter {
            it.userDisplayName.contains(searchQuery, ignoreCase = true) ||
            it.userEmail.contains(searchQuery, ignoreCase = true)
        }
    }

    val unviewedGroups = filteredFriendGroups.filter { it.hasUnviewed }
    val viewedGroups = filteredFriendGroups.filter { !it.hasUnviewed }

    if (showTextStatusDialog) {
        CreateTextStatusDialog(
            onDismiss = { showTextStatusDialog = false },
            onPostStatus = { text, bgHex, fontStyle ->
                onPostTextStatus(text, bgHex, fontStyle)
                showTextStatusDialog = false
            }
        )
    }

    if (showMediaStatusDialog) {
        CreateMediaStatusDialog(
            onDismiss = { showMediaStatusDialog = false },
            onPostStatus = { mediaUri, mediaType, caption ->
                onPostMediaStatus(mediaUri, mediaType, caption)
                showMediaStatusDialog = false
            }
        )
    }

    activeViewerGroup?.let { group ->
        StoryViewerDialog(
            statusGroup = group,
            onDismiss = { activeViewerGroup = null },
            onMarkAsViewed = onMarkAsViewed,
            onDeleteStatus = onDeleteStatus,
            onReplyToStatus = onReplyToStatus
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary FAB for Text Status
                SmallFloatingActionButton(
                    onClick = { showTextStatusDialog = true },
                    containerColor = Slate700,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New Text Status")
                }

                // Primary FAB for Media Status
                FloatingActionButton(
                    onClick = { showMediaStatusDialog = true },
                    containerColor = Emerald500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "New Photo Status")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status & Stories",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row {
                    IconButton(onClick = { showTextStatusDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Text status",
                            tint = Emerald500
                        )
                    }
                    IconButton(onClick = { showMediaStatusDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Media status",
                            tint = Emerald500
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search status updates...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // "My Status" Card Section
                item {
                    Text(
                        text = "My Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (myStatusGroup != null && myStatusGroup.statuses.isNotEmpty()) {
                                        activeViewerGroup = myStatusGroup
                                    } else {
                                        showMediaStatusDialog = true
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (myStatusGroup != null && myStatusGroup.statuses.isNotEmpty()) 2.5.dp else 0.dp,
                                            color = Emerald500,
                                            shape = CircleShape
                                        )
                                        .padding(if (myStatusGroup != null && myStatusGroup.statuses.isNotEmpty()) 3.dp else 0.dp)
                                ) {
                                    UserAvatar(
                                        name = currentUser?.displayName ?: "My Profile",
                                        size = 48.dp
                                    )

                                }

                                // Plus badge if no active status
                                if (myStatusGroup == null || myStatusGroup.statuses.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Emerald500)
                                            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add status",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (myStatusGroup != null && myStatusGroup.statuses.isNotEmpty()) {
                                        "${myStatusGroup.statuses.size} story update${if (myStatusGroup.statuses.size > 1) "s" else ""} • Tap to view"
                                    } else {
                                        "Tap to add status update (disappears in 24h)"
                                    },
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            if (myStatusGroup != null && myStatusGroup.statuses.isNotEmpty()) {
                                Box {
                                    IconButton(onClick = { showMyStatusMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = Color.Gray
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMyStatusMenu,
                                        onDismissRequest = { showMyStatusMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("View My Stories") },
                                            onClick = {
                                                showMyStatusMenu = false
                                                activeViewerGroup = myStatusGroup
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Text Story") },
                                            onClick = {
                                                showMyStatusMenu = false
                                                showTextStatusDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add Photo Story") },
                                            onClick = {
                                                showMyStatusMenu = false
                                                showMediaStatusDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recent Updates Section (Unviewed Friends' Statuses)
                if (unviewedGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Updates (${unviewedGroups.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(unviewedGroups) { group ->
                        StatusGroupCard(
                            group = group,
                            isUnviewed = true,
                            onClick = { activeViewerGroup = group }
                        )
                    }
                }

                // Viewed Updates Section
                if (viewedGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Viewed Updates (${viewedGroups.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(viewedGroups) { group ->
                        StatusGroupCard(
                            group = group,
                            isUnviewed = false,
                            onClick = { activeViewerGroup = group }
                        )
                    }
                }

                if (friendStatusGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No status updates from friends",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Status updates shared by contacts will appear here for 24 hours.",
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusGroupCard(
    group: UserStatusGroup,
    isUnviewed: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.5.dp,
                        color = if (isUnviewed) Emerald500 else Color.Gray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .padding(3.dp)
            ) {
                UserAvatar(
                    name = group.userDisplayName,
                    size = 48.dp
                )
            }


            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.userDisplayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatStatusTimestamp(group.latestTimestamp)} • ${group.statuses.size} story",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

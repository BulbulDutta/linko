package com.example.ui.screens.chats

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.ui.components.AnimatedSlideBar
import com.example.ui.components.SlideBarPosition
import com.example.ui.components.rememberScrollVisibilityState
import com.example.ui.components.swipeToHideShow
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
import com.example.data.local.entity.MessageEntity
import com.example.ui.components.EmailTransportBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.remote.UserPresenceInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    chats: List<ChatEntity>,
    contacts: List<ContactEntity>,
    allMessages: List<MessageEntity> = emptyList(),
    typingStatusByChatId: Map<String, String> = emptyMap(),
    currentUser: com.example.data.local.entity.UserEntity? = null,
    currentLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.ENGLISH,
    onlineUserEmails: Set<String> = emptySet(),
    userPresenceMap: Map<String, UserPresenceInfo> = emptyMap(),
    onSelectChat: (String) -> Unit,
    onOpenGeminiClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onOpenContactChat: ((ContactEntity) -> Unit)? = null,
    isUserOnline: (String?) -> Boolean = { false },
    isChatOnline: (ChatEntity) -> Boolean = { false },
    getChatPresence: ((ChatEntity) -> UserPresenceInfo?)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Groups"
    val topBarVisibilityState = rememberScrollVisibilityState(initialVisible = true)

    val chatDisplayMap = remember(chats, contacts, currentUser) {
        chats.associate { chat ->
            val display = resolveChatDisplay(chat, contacts, currentUser)
            chat.id to display
        }
    }

    val messagesByChatId = remember(allMessages) {
        allMessages.groupBy { it.chatId }
    }

    val matchedMessageMap = remember(chats, searchQuery, messagesByChatId) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isBlank()) {
            emptyMap<String, String>()
        } else {
            chats.mapNotNull { chat ->
                val matchingMsg = messagesByChatId[chat.id]?.firstOrNull { msg ->
                    msg.text.contains(trimmedQuery, ignoreCase = true)
                }
                val snippet = when {
                    matchingMsg != null -> matchingMsg.text
                    chat.lastMessageText.contains(trimmedQuery, ignoreCase = true) -> chat.lastMessageText
                    else -> null
                }
                if (snippet != null) chat.id to snippet else null
            }.toMap()
        }
    }

    val filteredChats = remember(chats, contacts, searchQuery, selectedFilter, chatDisplayMap, matchedMessageMap) {
        val trimmedQuery = searchQuery.trim()
        chats.filter { chat ->
            val (dispName, _) = chatDisplayMap[chat.id] ?: Pair(chat.name, chat.groupIconUri)
            val matchesQuery = if (trimmedQuery.isBlank()) {
                true
            } else {
                val matchesContactName = dispName.contains(trimmedQuery, ignoreCase = true) ||
                    chat.name.contains(trimmedQuery, ignoreCase = true) ||
                    contacts.any { c ->
                        (c.contactDisplayName.equals(dispName, ignoreCase = true) ||
                            c.contactEmail.equals(chat.name, ignoreCase = true)) &&
                            (c.contactDisplayName.contains(trimmedQuery, ignoreCase = true) ||
                                c.contactEmail.contains(trimmedQuery, ignoreCase = true) ||
                                c.contactUsername.contains(trimmedQuery, ignoreCase = true))
                    }
                val matchesMessageContent = chat.lastMessageText.contains(trimmedQuery, ignoreCase = true) ||
                    matchedMessageMap.containsKey(chat.id)
                matchesContactName || matchesMessageContent
            }
            val matchesFilter = when (selectedFilter) {
                "Unread" -> chat.unreadCount > 0
                "Groups" -> chat.isGroup
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(topBarVisibilityState.nestedScrollConnection)
            .swipeToHideShow(topBarVisibilityState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar with Swipe to Hide / Show
            AnimatedSlideBar(
                visible = topBarVisibilityState.isVisible,
                position = SlideBarPosition.TOP,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .swipeToHideShow(topBarVisibilityState)
                ) {
                    // Header Title Bar with Linko Logo
                    Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_linko_logo),
                        contentDescription = "Linko Logo",
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Linko",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.Emerald500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Header Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by contact name or message...",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("chats_search_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("chats_search_bar")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stories Bar / Quick Contacts & Gemini AI Row
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { onOpenGeminiClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C4DFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                        }
                    }

                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { onAddContactClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddComment,
                                    contentDescription = "Add Contact",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("New Contact", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    items(contacts) { contact ->
                        val isOnline = isUserOnline(contact.contactEmail) || isUserOnline(contact.contactUserId)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    onOpenContactChat?.invoke(contact)
                                }
                        ) {
                            UserAvatar(
                                name = contact.contactDisplayName,
                                avatarUri = contact.contactAvatarUri,
                                size = 48.dp,
                                showOnlineStatus = true,
                                isOnline = isOnline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.contactDisplayName.substringBefore(" "),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row {
                    listOf("All", "Unread", "Groups").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }
    }

            // Chat List
            if (filteredChats.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Empty Chats",
                            tint = Emerald500,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        com.example.ui.components.AutoResizeText(
                            text = if (chats.isEmpty()) "No conversations yet" else "No matching conversations",
                            maxFontSize = 18.sp,
                            minFontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (chats.isEmpty()) "Scan a friend's QR code or enter their email address to start chatting!" else "Try searching for a different contact or message.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (chats.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddContactClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                com.example.ui.components.AutoResizeText(
                                    text = "Scan QR / Add Contact",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxFontSize = 14.sp,
                                    minFontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val isOnline = isChatOnline(chat)
                        val presence = getChatPresence?.invoke(chat)
                        val (dispName, dispAvatar) = chatDisplayMap[chat.id] ?: Pair(chat.name, chat.groupIconUri)
                        val matchedSnippet = matchedMessageMap[chat.id]
                        val typingText = typingStatusByChatId[chat.id]
                        ChatItemRow(
                            chat = chat,
                            displayName = dispName,
                            displayAvatarUri = dispAvatar,
                            matchedMessageSnippet = matchedSnippet,
                            typingIndicatorText = typingText,
                            isOnline = isOnline,
                            presenceInfo = presence,
                            onClick = { onSelectChat(chat.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = onCreateGroupClick,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
            }

            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = Emerald500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }
    }
}

private fun resolveChatDisplay(
    chat: ChatEntity,
    contacts: List<ContactEntity>,
    currentUser: com.example.data.local.entity.UserEntity?
): Pair<String, String?> {
    if (chat.isGroup) {
        return Pair(chat.name, chat.groupIconUri)
    }
    if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini")) {
        return Pair("Gemini AI Assistant", null)
    }
    val participantIds = try {
        if (chat.participantIdsJson.isBlank()) emptyList()
        else {
            val jsonArray = org.json.JSONArray(chat.participantIdsJson)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        }
    } catch (_: Exception) {
        emptyList()
    }
    val myId = currentUser?.id?.lowercase()?.trim()?.ifBlank { null }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.lowercase()?.trim()
        ?: ""
    val myEmail = currentUser?.email?.lowercase()?.trim()?.ifBlank { null }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.lowercase()?.trim()
        ?: ""
    val myDisplayName = currentUser?.displayName?.trim()
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName?.trim()
        ?: ""
    val myCanonical = if (myEmail.isNotBlank()) com.example.data.repository.LinkoRepository.canonicalUserIdFromEmail(myEmail, myId) else myId

    val isSelfName: (String) -> Boolean = { candidate ->
        val clean = candidate.trim().lowercase()
        clean.isNotBlank() && (
            (myDisplayName.isNotBlank() && clean == myDisplayName.lowercase()) ||
            (myEmail.isNotBlank() && clean == myEmail) ||
            (myEmail.contains("@") && clean == myEmail.substringBefore("@"))
        )
    }

    val otherTokens = participantIds
        .map { it.lowercase().trim() }
        .filter { it.isNotBlank() && it != myId && it != myEmail && it != myCanonical }

    val contact = contacts.find { c ->
        val cId = c.contactUserId.lowercase().trim()
        val cEmail = c.contactEmail.lowercase().trim()
        val cKey = com.example.data.repository.LinkoRepository.canonicalParticipantKey(cEmail.ifBlank { cId })
        otherTokens.any { token ->
            token == cId || token == cEmail || com.example.data.repository.LinkoRepository.canonicalParticipantKey(token) == cKey
        } ||
        com.example.data.repository.LinkoRepository.generateDirectChatId(myId, myEmail, cId, cEmail) == chat.id ||
        "chat_${c.contactUserId}" == chat.id ||
        (!com.example.data.repository.LinkoRepository.isGenericPlaceholderName(chat.name, myDisplayName, myEmail) &&
            !isSelfName(chat.name) &&
            (c.contactDisplayName.equals(chat.name.trim(), ignoreCase = true) ||
                c.contactEmail.equals(chat.name.trim(), ignoreCase = true)))
    } ?: run {
        val nonGemini = contacts.filter { it.contactUserId != "user_gemini" && !it.contactEmail.contains("gemini") }
        if (nonGemini.size == 1 && (com.example.data.repository.LinkoRepository.isGenericPlaceholderName(chat.name, myDisplayName, myEmail) || isSelfName(chat.name))) {
            nonGemini.first()
        } else null
    }

    val emailFallback = otherTokens.firstOrNull { it.contains("@") }
        ?.substringBefore("@")
        ?.replace(".", " ")
        ?.split(" ")
        ?.joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

    val name = contact?.contactDisplayName?.takeIf { it.isNotBlank() }
        ?: chat.name.takeIf { !com.example.data.repository.LinkoRepository.isGenericPlaceholderName(it, myDisplayName, myEmail) && !isSelfName(it) }
        ?: emailFallback
        ?: "Linko Contact"
    val avatar = contact?.contactAvatarUri ?: chat.groupIconUri
    return Pair(name, avatar)
}

@Composable
private fun ChatItemRow(
    chat: ChatEntity,
    displayName: String = chat.name,
    displayAvatarUri: String? = chat.groupIconUri,
    matchedMessageSnippet: String? = null,
    typingIndicatorText: String? = null,
    isOnline: Boolean = false,
    presenceInfo: UserPresenceInfo? = null,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = displayName,
                avatarUri = displayAvatarUri,
                isGroup = chat.isGroup,
                size = 54.dp,
                showOnlineStatus = true,
                isOnline = isOnline
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!chat.isGroup) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "End-to-End Encrypted",
                                tint = Emerald500.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTime(chat.lastMessageTimestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmailTransportBadge(
                        emailStatus = "DELIVERED_INBOX",
                        modifier = Modifier.padding(end = 5.dp)
                    )

                    val isTypingActive = !typingIndicatorText.isNullOrBlank()
                    val rawPreview = when {
                        isTypingActive -> typingIndicatorText!!
                        !matchedMessageSnippet.isNullOrBlank() -> matchedMessageSnippet
                        chat.lastMessageText.isNotBlank() -> chat.lastMessageText
                        else -> "No messages yet"
                    }
                    val previewText = if (!isTypingActive && rawPreview.contains("meet.google.com", ignoreCase = true)) {
                        if (rawPreview.contains("Audio Call", ignoreCase = true) || rawPreview.contains("Voice Call", ignoreCase = true)) {
                            "📞 Voice Call"
                        } else {
                            "📹 Video Call"
                        }
                    } else {
                        rawPreview
                    }
                    val isHighlighted = isTypingActive || matchedMessageSnippet != null
                    Text(
                        text = previewText,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        letterSpacing = 0.2.sp,
                        color = if (isHighlighted) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = Emerald500,
                            contentColor = Color.White,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text("${chat.unreadCount}")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val date = Date(timestamp)

    return when {
        isSameDay(now, timestamp) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        isYesterday(now, timestamp) -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(t1)) == fmt.format(Date(t2))
}

private fun isYesterday(now: Long, t: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = now; add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatLastSeenShort(lastSeen: Long): String {
    if (lastSeen <= 0L) return "Offline"
    val diff = System.currentTimeMillis() - lastSeen
    if (diff < 0L) return "Offline"
    val minutes = diff / 60000L
    return when {
        minutes < 1L -> "Just now"
        minutes < 60L -> "${minutes}m ago"
        minutes < 1440L -> "${minutes / 60L}h ago"
        else -> "Offline"
    }
}

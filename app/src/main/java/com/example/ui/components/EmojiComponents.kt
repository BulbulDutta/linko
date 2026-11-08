package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald500
import org.json.JSONArray
import org.json.JSONObject

/**
 * WhatsApp-style Quick Reaction Bar shown above long-pressed chat messages.
 */
@Composable
fun MessageReactionsBar(
    onSelectEmoji: (String) -> Unit,
    onMoreClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val quickEmojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥")

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("message_reactions_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickEmojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectEmoji(emoji)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                        onMoreClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "More Emojis",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Reaction badges rendered at the bottom corner of a chat bubble.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionBadges(
    reactionsJson: String,
    currentUserId: String?,
    onToggleReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactionList = remember(reactionsJson) {
        val list = mutableListOf<ReactionItem>()
        try {
            if (reactionsJson.isNotBlank() && reactionsJson != "{}") {
                val json = JSONObject(reactionsJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = json.optJSONArray(key) ?: JSONArray()
                    val userIds = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        userIds.add(arr.getString(i))
                    }
                    if (userIds.isNotEmpty()) {
                        list.add(ReactionItem(emoji = key, userIds = userIds))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    if (reactionList.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = modifier
                .padding(top = 2.dp)
                .testTag("reaction_badges_row")
        ) {
            reactionList.forEach { reaction ->
                val hasUserReacted = currentUserId != null && reaction.userIds.contains(currentUserId)

                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                    exit = scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (hasUserReacted) Emerald500.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (hasUserReacted) Emerald500 else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleReaction(reaction.emoji) }
                            .padding(horizontal = 5.dp, vertical = 1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reaction.emoji,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${reaction.userIds.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasUserReacted) Emerald500 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ReactionItem(
    val emoji: String,
    val userIds: List<String>
)

/**
 * Built-in Category-Wise Emoji Picker Panel for in-chat typing and custom reactions.
 */
@Composable
fun EmojiPickerPanel(
    onEmojiSelect: (String) -> Unit,
    onBackspace: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val categories = listOf(
        EmojiCategory("🕒", "Frequent", listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉", "✨", "🚀", "😍", "👏", "💯", "🤩", "🥳", "😎")),
        EmojiCategory("😊", "Smileys", listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓")),
        EmojiCategory("🐱", "Animals", listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🕷️", "🐢", "🐍", "🦎", "🐙", "🦐", "🦞", "🦀", "🐠", "🐟", "🐬", "🐳", "🦈", "🐊", "🐆", "🦓", "🦍", "🐘", "🦛", "🦏", "🐪", "🦒", "🦘")),
        EmojiCategory("🍕", "Food", listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥦", "🍞", "🥐", "🥖", "🥨", "🥯", "🥞", "🧇", "🧀", "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕", "🌭", "🥪", "🌮", "🌯", "🍿", "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝", "🍦", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🍫", "🍬", "🍭", "☕", "🧃", "🍷", "🍺")),
        EmojiCategory("⚽", "Objects", listOf("⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸", "🏒", "🏏", "⛳", "🏹", "🎣", "🥊", "🥋", "🛹", "🛷", "🎿", "🧘", "🏄", "🏊", "🚣", "🧗", "🚵", "🚴", "🎮", "🕹️", "🎰", "🎲", "🧩", "🧸", "🎨", "🧵", "📱", "💻", "⌨️", "🖥️", "🖨️", "📷", "📸", "📹", "🎥", "📞", "☎️", "📺", "📻", "🎙️", "⏱️", "⏰", "🔑", "🎁")),
        EmojiCategory("💡", "Symbols", listOf("🖤", "🤍", "🤎", "💜", "💙", "💚", "💛", "🧡", "❤️", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️", "✝️", "☪️", "🕉️", "☸️", "☯️", "✴️", "🉐", "㊙️", "㊗️", "❌", "⭕", "🛑", "⛔", "💯", "💢", "♨️", "❗", "❓", "⚠️", "🔰", "♻️, ✅", "❇️", "✳️", "❎", "🌐", "💤"))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag("emoji_picker_panel")
    ) {
        // Category Tabs Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 8.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = "${category.icon} ${category.name}",
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategoryIndex == index) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (onBackspace != null) {
                IconButton(onClick = onBackspace) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Grid of Emojis
        val currentCategory = categories[selectedCategoryIndex]
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(currentCategory.emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelect(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class EmojiCategory(
    val icon: String,
    val name: String,
    val emojis: List<String>
)

/**
 * Dialog for picking any custom emoji when clicking '+' in reaction bar.
 */
@Composable
fun CustomEmojiPickerDialog(
    onEmojiSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pick Emoji Reaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            EmojiPickerPanel(
                onEmojiSelect = { emoji ->
                    onEmojiSelect(emoji)
                    onDismiss()
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

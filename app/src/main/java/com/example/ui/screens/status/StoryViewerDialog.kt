package com.example.ui.screens.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import coil.compose.AsyncImage
import com.example.data.local.entity.StatusEntity
import com.example.data.model.UserStatusGroup
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500

import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoryViewerDialog(
    statusGroup: UserStatusGroup,
    onDismiss: () -> Unit,
    onMarkAsViewed: (statusId: String) -> Unit,
    onDeleteStatus: (statusId: String) -> Unit,
    onReplyToStatus: (userEmail: String, replyText: String) -> Unit
) {
    val statuses = statusGroup.statuses
    if (statuses.isEmpty()) {
        onDismiss()
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var replyText by remember { mutableStateOf("") }

    val currentStatus = statuses.getOrElse(currentIndex) { statuses.first() }

    LaunchedEffect(currentStatus.id) {
        onMarkAsViewed(currentStatus.id)
    }

    // Auto Progress Timer (5 seconds per story item)
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progress = 0f
            val totalSteps = 100
            val stepDuration = 50L // 5000ms / 100 = 50ms per tick
            for (i in 1..totalSteps) {
                if (isPaused) break
                delay(stepDuration)
                progress = i / 100f
            }
            if (!isPaused && progress >= 1f) {
                if (currentIndex < statuses.size - 1) {
                    currentIndex++
                } else {
                    onDismiss()
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = "StoryProgress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 3f) {
                                // Tap left third: go back
                                if (currentIndex > 0) {
                                    currentIndex--
                                }
                            } else {
                                // Tap right two thirds: go forward
                                if (currentIndex < statuses.size - 1) {
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Background Content View (Text vs Media)
            if (currentStatus.type == "TEXT") {
                val bg = parseHexColor(currentStatus.backgroundColorHex)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bg)
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStatus.textContent ?: "",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = getFontFamily(currentStatus.fontStyle),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                }
            } else {
                // Photo or Video Status
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentStatus.mediaUri.isNull_or_empty()) {
                        AsyncImage(
                            model = currentStatus.mediaUri,
                            contentDescription = "Status Media",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = currentStatus.textContent ?: "Photo Story",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Optional Caption Overlay
                    if (!currentStatus.textContent.isNull_or_empty() && currentStatus.type != "TEXT") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 100.dp, start = 20.dp, end = 20.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = currentStatus.textContent ?: "",
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                        }
                    }
                }
            }

            // Top Header: Progress indicators and User Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEachIndexed { idx, _ ->
                        val itemProgress = when {
                            idx < currentIndex -> 1f
                            idx == currentIndex -> animatedProgress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { itemProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                // Author Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = statusGroup.userDisplayName,
                        size = 40.dp
                    )


                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusGroup.userDisplayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatStatusTimestamp(currentStatus.createdTimestamp),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    if (statusGroup.isCurrentUser) {
                        IconButton(
                            onClick = {
                                onDeleteStatus(currentStatus.id)
                                if (statuses.size <= 1) {
                                    onDismiss()
                                } else if (currentIndex >= statuses.size - 1) {
                                    currentIndex--
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete status",
                                tint = Color.Red.copy(alpha = 0.9f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Reply Bar (Only for Friends' Statuses)
            if (!statusGroup.isCurrentUser) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Quick Emoji Reactions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("❤️", "🔥", "😂", "👏", "😮", "😍").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onReplyToStatus(statusGroup.userEmail, emoji)
                                        onDismiss()
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Text Reply Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate800, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply to ${statusGroup.userDisplayName}...", color = Color.Gray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (replyText.isNotBlank()) {
                                        onReplyToStatus(statusGroup.userEmail, replyText.trim())
                                        replyText = ""
                                        onDismiss()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReplyToStatus(statusGroup.userEmail, replyText.trim())
                                    replyText = ""
                                    onDismiss()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Reply",
                                tint = Emerald500
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

fun formatStatusTimestamp(timestamp: Long): String {
    val diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "$diffMinutes minutes ago"
        diffMinutes < 1440 -> "${diffMinutes / 60} hours ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

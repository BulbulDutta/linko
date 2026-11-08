package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Teal500

@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    showOnlineStatus: Boolean = true,
    isOnline: Boolean = false
) {
    val initial = name.firstOrNull()?.uppercase() ?: "T"
    val avatarColors = getAvatarGradients(name)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(avatarColors)),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.5f)
                    )
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        }

        if (showOnlineStatus && !isGroup && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Emerald500)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

private fun getAvatarGradients(name: String): List<Color> {
    val hash = name.hashCode()
    return when (Math.abs(hash) % 5) {
        0 -> listOf(Emerald500, Teal500)
        1 -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        2 -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
        3 -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        else -> listOf(Color(0xFFEC4899), Color(0xFFBE185D))
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AttachmentType {
    DOCUMENT,
    CAMERA,
    GALLERY,
    AUDIO,
    LOCATION,
    CONTACT,
    POLL,
    EVENT,
    AI_IMAGE,
    IN_APP_CALL
}

private data class AttachmentOptionItem(
    val type: AttachmentType,
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttachmentBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onOptionSelected: (AttachmentType) -> Unit
) {
    val options = listOf(
        AttachmentOptionItem(
            type = AttachmentType.DOCUMENT,
            title = "Document",
            icon = Icons.Default.Description,
            backgroundColor = Color(0xFF7F66FF)
        ),
        AttachmentOptionItem(
            type = AttachmentType.CAMERA,
            title = "Camera",
            icon = Icons.Default.CameraAlt,
            backgroundColor = Color(0xFFE91E63)
        ),
        AttachmentOptionItem(
            type = AttachmentType.GALLERY,
            title = "Gallery",
            icon = Icons.Default.Image,
            backgroundColor = Color(0xFFAC44CF)
        ),
        AttachmentOptionItem(
            type = AttachmentType.AUDIO,
            title = "Audio",
            icon = Icons.Default.Headphones,
            backgroundColor = Color(0xFFF57C00)
        ),
        AttachmentOptionItem(
            type = AttachmentType.LOCATION,
            title = "Google Maps",
            icon = Icons.Default.LocationOn,
            backgroundColor = Color(0xFF00C853)
        ),
        AttachmentOptionItem(
            type = AttachmentType.CONTACT,
            title = "Contact",
            icon = Icons.Default.Person,
            backgroundColor = Color(0xFF0088CC)
        ),
        AttachmentOptionItem(
            type = AttachmentType.POLL,
            title = "Poll",
            icon = Icons.Default.Poll,
            backgroundColor = Color(0xFF009688)
        ),
        AttachmentOptionItem(
            type = AttachmentType.EVENT,
            title = "Event",
            icon = Icons.Default.Event,
            backgroundColor = Color(0xFF3F51B5)
        ),
        AttachmentOptionItem(
            type = AttachmentType.AI_IMAGE,
            title = "AI Image",
            icon = Icons.Default.AutoAwesome,
            backgroundColor = Color(0xFF6200EE)
        ),
        AttachmentOptionItem(
            type = AttachmentType.IN_APP_CALL,
            title = "Google Meet",
            icon = Icons.Default.Videocam,
            backgroundColor = Color(0xFF00897B)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Share Content",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                maxItemsInEachRow = 3
            ) {
                options.forEach { option ->
                    AttachmentGridTile(
                        item = option,
                        onClick = {
                            onOptionSelected(option.type)
                            onDismissRequest()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AttachmentGridTile(
    item: AttachmentOptionItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(12.dp)
            .width(72.dp)
            .clickable { onClick() }
            .testTag("attachment_option_${item.type.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(item.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

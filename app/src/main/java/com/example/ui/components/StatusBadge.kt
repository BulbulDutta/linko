package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.MessageStatus
import com.example.ui.theme.Emerald500
import com.example.ui.theme.StatusReadBlue

private val TickGrayColor = Color(0xFF9CA3AF)

/**
 * 3-stage WhatsApp-style message receipt indicator:
 * - 'sent'      -> Single Gray Tick (Message sent from device to server)
 * - 'delivered' -> Double Gray Tick (Message delivered to recipient's device)
 * - 'read'      -> Double Blue Tick (Recipient opened the chat and message is visible on screen)
 */
@Composable
fun MessageStatusTicks(
    status: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 17.dp
) {
    when (MessageStatus.normalize(status)) {
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read (Double Blue Tick)",
                tint = StatusReadBlue,
                modifier = modifier
                    .size(iconSize)
                    .testTag("message_tick_read")
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered (Double Gray Tick)",
                tint = TickGrayColor,
                modifier = modifier
                    .size(iconSize)
                    .testTag("message_tick_delivered")
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent (Single Gray Tick)",
                tint = TickGrayColor,
                modifier = modifier
                    .size(iconSize)
                    .testTag("message_tick_sent")
            )
        }
    }
}

@Composable
fun EmailTransportBadge(
    emailStatus: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val (icon, color, desc) = when (emailStatus) {
            "DELIVERED_INBOX" -> Triple(Icons.Default.MarkEmailRead, Emerald500, "Email Delivered")
            "DISPATCHED_SMTP" -> Triple(Icons.Default.Email, Color(0xFF3B82F6), "SMTP Dispatched")
            else -> Triple(Icons.Default.Email, Color.Gray, "Email Pending")
        }

        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
    }
}

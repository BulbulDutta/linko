package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmailHeaderInfo
import com.example.ui.theme.Emerald500

@Composable
fun EmailHeaderDialog(
    info: EmailHeaderInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = "Email Transport",
                tint = Emerald500,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Email Delivery Bridge Log",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This Linko message was dispatched and mirrored over secure email infrastructure.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        HeaderRow("From", info.from)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("To", info.to)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("Subject", info.subject)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("Message ID", info.messageId, isMono = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("Transport", info.smtpRoute)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("Status", info.status, isHighlight = true)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        HeaderRow("Encryption", "AES-256-GCM (E2EE)", isHighlight = true)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Encrypted",
                        tint = Emerald500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-End Encrypted • Zero-Knowledge Server",
                        fontSize = 11.sp,
                        color = Emerald500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Text("Close Log")
            }
        }
    )
}

@Composable
private fun HeaderRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = if (isHighlight) Emerald500 else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

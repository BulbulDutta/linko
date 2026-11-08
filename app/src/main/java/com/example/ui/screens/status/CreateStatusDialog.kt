package com.example.ui.screens.status

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

val statusColorOptions = listOf(
    "#10B981" to "Emerald",
    "#8B5CF6" to "Violet",
    "#F59E0B" to "Amber",
    "#EC4899" to "Rose",
    "#3B82F6" to "Ocean",
    "#06B6D4" to "Cyan",
    "#334155" to "Slate"
)

val statusFontOptions = listOf(
    "DEFAULT" to "Classic",
    "SERIF" to "Serif",
    "MONOSPACE" to "Mono",
    "HANDWRITING" to "Cursive"
)

fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF10B981)): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16) or 0xFF000000
        Color(colorInt)
    } catch (_: Exception) {
        defaultColor
    }
}

fun getFontFamily(fontStyle: String): FontFamily {
    return when (fontStyle) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "HANDWRITING" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
}

@Composable
fun CreateTextStatusDialog(
    onDismiss: () -> Unit,
    onPostStatus: (text: String, bgHex: String, fontStyle: String) -> Unit
) {
    var textContent by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(statusColorOptions[0].first) }
    var selectedFontStyle by remember { mutableStateOf(statusFontOptions[0].first) }

    val bgColor = parseHexColor(selectedColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(20.dp)
        ) {
            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Font Toggle Chip
                    TextButton(
                        onClick = {
                            val currentIndex = statusFontOptions.indexOfFirst { it.first == selectedFontStyle }
                            val nextIndex = (currentIndex + 1) % statusFontOptions.size
                            selectedFontStyle = statusFontOptions[nextIndex].first
                        },
                        colors = ButtonDefaults.textButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            Icons.Default.FontDownload,
                            contentDescription = "Font",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusFontOptions.find { it.first == selectedFontStyle }?.second ?: "Font",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Post Button
                    Button(
                        onClick = {
                            if (textContent.isNotBlank()) {
                                onPostStatus(textContent.trim(), selectedColorHex, selectedFontStyle)
                            }
                        },
                        enabled = textContent.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = parseHexColor(selectedColorHex)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Post",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Central Large Text Input area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 80.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { if (it.length <= 280) textContent = it },
                    placeholder = {
                        Text(
                            "Type a status...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = getFontFamily(selectedFontStyle),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = getFontFamily(selectedFontStyle)
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom Palette Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "${textContent.length}/280 characters",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(statusColorOptions) { (hex, name) ->
                        val color = parseHexColor(hex)
                        val isSelected = selectedColorHex == hex

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(if (isSelected) 42.dp else 34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
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
fun CreateMediaStatusDialog(
    onDismiss: () -> Unit,
    onPostStatus: (mediaUri: String, mediaType: String, caption: String?) -> Unit
) {
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var captionText by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(false) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            isVideo = it.toString().contains("video") || it.toString().endsWith(".mp4")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900)
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Slate800, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = "New Story Update",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Button(
                    onClick = {
                        selectedMediaUri?.let { uri ->
                            val type = if (isVideo) "VIDEO" else "IMAGE"
                            onPostStatus(uri.toString(), type, captionText.ifBlank { null })
                        }
                    },
                    enabled = selectedMediaUri != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Share", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Central Media Preview Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp, bottom = 90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate800)
                    .clickable {
                        mediaPickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedMediaUri != null) {
                    AsyncImage(
                        model = selectedMediaUri,
                        contentDescription = "Selected Status Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Select Photo",
                                tint = Emerald500,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to choose photo or video",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Shares a 24-hour story with your contacts",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Bottom Caption Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Slate800, RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    placeholder = { Text("Add a caption...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { mediaPickerLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Change image",
                        tint = Emerald500
                    )
                }
            }
        }
    }
}

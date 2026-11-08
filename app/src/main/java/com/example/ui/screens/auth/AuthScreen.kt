package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AutoResizeText
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.GoogleAuthHelper
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onLoginSubmit: (email: String, displayName: String, username: String) -> Unit,
    onSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGoogleSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Slate900,
                        Color(0xFF0C1322),
                        Color(0xFF060B14)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        val isCompactHeight = maxHeight < 640.dp
        val outerBadgeSize = if (isCompactHeight) 72.dp else 86.dp
        val innerBadgeSize = if (isCompactHeight) 52.dp else 62.dp
        val shieldIconSize = if (isCompactHeight) 28.dp else 34.dp
        val cardInnerPadding = if (isCompactHeight) 16.dp else 22.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top action bar with Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isCompactHeight) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("skip_auth_button")
                ) {
                    Text(
                        text = "Skip",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Skip sign in",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // -----------------------------------------------------------------
            // App Branding & Icon (Responsive Proportions)
            // -----------------------------------------------------------------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(outerBadgeSize)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Emerald500.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald500,
                        modifier = Modifier.size(innerBadgeSize),
                        shadowElevation = 10.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Linko Security",
                                tint = Color.White,
                                modifier = Modifier.size(shieldIconSize)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

                AutoResizeText(
                    text = "LINKO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                    maxFontSize = if (isCompactHeight) 24.sp else 30.sp,
                    minFontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                AutoResizeText(
                    text = "End-to-End Encrypted Messenger",
                    color = Color(0xFF94A3B8),
                    maxFontSize = 14.sp,
                    minFontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // -----------------------------------------------------------------
            // Sign-In Card
            // -----------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.65f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardInnerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoResizeText(
                        text = "Sign In",
                        color = Color.White,
                        maxFontSize = 20.sp,
                        minFontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Sign in with your Google account to access your encrypted chats, contacts, and cloud sync.",
                        color = Color(0xFF94A3B8),
                        fontSize = if (isCompactHeight) 12.sp else 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isCompactHeight) 16.sp else 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 18.dp))

                    // Feature highlights
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                            .padding(if (isCompactHeight) 10.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 8.dp else 10.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Lock,
                            title = "Zero-Knowledge Encryption",
                            description = "Your messages and calls are encrypted on device."
                        )
                        FeatureRow(
                            icon = Icons.Default.CloudDone,
                            title = "Real-Time Cloud Sync",
                            description = "Synced seamlessly with Firebase Cloud Firestore."
                        )
                        FeatureRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Instant Verification",
                            description = "Fast, secure authentication with Google."
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 20.dp))

                    // Primary Google Sign-In Button
                    Surface(
                        onClick = {
                            if (!isGoogleSigningIn) {
                                isGoogleSigningIn = true
                                errorMessage = null
                                coroutineScope.launch {
                                    GoogleAuthHelper.signInWithGoogle(
                                        context = context,
                                        onSuccess = { _, gEmail, gName, gUsername ->
                                            isGoogleSigningIn = false
                                            onLoginSubmit(gEmail, gName, gUsername)
                                        },
                                        onError = { error ->
                                            isGoogleSigningIn = false
                                            errorMessage = error
                                        },
                                        onCancelled = {
                                            isGoogleSigningIn = false
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isGoogleSigningIn,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("auth_google_signin_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Emerald500,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                AutoResizeText(
                                    text = "Signing in...",
                                    color = Color.White,
                                    maxFontSize = 15.sp,
                                    minFontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                AutoResizeText(
                                    text = "Sign in with Google",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxFontSize = 16.sp,
                                    minFontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompactHeight) 2.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.12f)
                        )
                        Text(
                            text = "OR",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.12f)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 10.dp))

                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("auth_guest_skip_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Emerald500
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Emerald500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Skip & Continue as Guest",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Error Notification Pill
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Security Footnote
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AutoResizeText(
                    text = "Cloud Firestore Sync • Google Authentication",
                    color = Color(0xFF64748B),
                    maxFontSize = 12.sp,
                    minFontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Emerald500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            AutoResizeText(
                text = title,
                color = Color.White,
                maxFontSize = 13.sp,
                minFontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.example.ui.screens.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.ActiveCallState
import com.example.data.model.CallType
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500
import com.example.webrtc.LinkoWebRtcClient
import java.util.Locale
import kotlin.math.roundToInt
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * Full-Screen Glassmorphic WebRTC Audio & Video Call Screen for Linko.
 *
 * Features:
 * - Real-time WebRTC Remote Video (`SurfaceViewRenderer`) & Draggable Local PiP Camera View.
 * - Luxury Dark + Glassmorphic floating control dock (`WindowInsets.navigationBars` safe).
 * - Smooth spring-animated controls for:
 *   - Mute / Unmute Microphone (`call_mute_button`)
 *   - Turn On / Off Camera (`call_video_toggle_button`)
 *   - Switch Camera Front / Back (`call_switch_camera_button`)
 *   - Speaker / Earpiece Audio Routing (`call_speaker_button`)
 *   - HD 720p / Full HD 1080p Resolution Switch (`call_hd_resolution_button`)
 *   - End Call (`call_end_button`) & Incoming Call Answer/Decline (`call_answer_button`).
 * - Active indicators for 48 kHz HD Audio, Acoustic Echo Cancellation (AEC), and Noise Suppression (NS).
 */
@Composable
fun WebRtcCallScreen(
    callState: ActiveCallState,
    webRtcClient: LinkoWebRtcClient?,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggle1080p: () -> Unit,
    onAnswerIncomingCall: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onEndCall()
    }

    val context = LocalContext.current
    val isVideoCall = callState.callType == CallType.VIDEO

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = result[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(isVideoCall) {
        val needed = buildList {
            if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
            if (isVideoCall && !hasCameraPermission) add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Draggable offset for the Picture-in-Picture (PiP) Local Video Card
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .testTag("webrtc_call_screen")
    ) {
        // 1. Background Surface: Remote Video Stream (if Video Call & remote track active) or Luxury Radial Backdrop
        if (isVideoCall && callState.isRemoteVideoAvailable && webRtcClient != null) {
            WebRtcVideoRendererView(
                webRtcClient = webRtcClient,
                isLocalPreview = false,
                mirror = false,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("remote_video_renderer")
            )
        } else {
            LuxuryCallBackdrop(
                callState = callState,
                isVideoCall = isVideoCall
            )
        }

        // Subtle top & bottom vignette gradients so glassmorphic controls remain legible over bright video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 2. Top Glassmorphic Status & Security Header
        TopCallStatusHeader(
            callState = callState,
            onToggle1080p = onToggle1080p,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // 3. Draggable Local Camera Picture-in-Picture (PiP) Window for Video Calls
        AnimatedVisibility(
            visible = isVideoCall && callState.isVideoEnabled,
            enter = fadeIn(tween(220)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(180)) + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 86.dp, end = 16.dp)
                .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        pipOffsetX = (pipOffsetX + dragAmount.x).coerceIn(-650f, 40f)
                        pipOffsetY = (pipOffsetY + dragAmount.y).coerceIn(-60f, 900f)
                    }
                }
        ) {
            LocalPipPreviewCard(
                webRtcClient = webRtcClient,
                hasCameraPermission = hasCameraPermission,
                isFrontCamera = callState.isFrontCamera,
                resolutionBadge = if (callState.is1080pEnabled) "1080p" else "720p",
                onSwitchCamera = onSwitchCamera
            )
        }

        // 4. Bottom Glassmorphic Floating Call Control Dock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Active DSP / HD Audio & Noise Cancellation status pill
            HdAudioStatusPill(
                isHdAudioActive = callState.isHdAudioActive,
                isNoiseCancellationActive = callState.isNoiseCancellationActive,
                isVideoCall = isVideoCall,
                videoResolutionLabel = callState.videoResolutionLabel
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (callState.isIncoming && callState.isRinging && !callState.isConnected) {
                IncomingCallActionDock(
                    callerName = callState.contactName,
                    isVideoCall = isVideoCall,
                    onAnswer = onAnswerIncomingCall,
                    onDecline = onEndCall
                )
            } else {
                GlassmorphicControlBar(
                    isMuted = callState.isMuted,
                    isVideoCall = isVideoCall,
                    isVideoEnabled = callState.isVideoEnabled,
                    isSpeakerOn = callState.isSpeakerOn,
                    onToggleMute = onToggleMute,
                    onToggleVideo = onToggleVideo,
                    onSwitchCamera = onSwitchCamera,
                    onToggleSpeaker = onToggleSpeaker,
                    onEndCall = onEndCall
                )
            }
        }
    }
}

@Composable
private fun LuxuryCallBackdrop(
    callState: ActiveCallState,
    isVideoCall: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha_2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F2E28),
                        Color(0xFF09141E),
                        Color(0xFF05080E)
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative ambient glow orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Emerald500.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.25f),
                    radius = size.minDimension * 0.55f
                ),
                center = Offset(size.width * 0.2f, size.height * 0.25f),
                radius = size.minDimension * 0.55f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.72f),
                    radius = size.minDimension * 0.5f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.72f),
                radius = size.minDimension * 0.5f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-28).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Concentric animated audio/ringing wave rings
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(pulseScale2)
                        .border(2.dp, Emerald500.copy(alpha = pulseAlpha2), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(pulseScale1)
                        .background(Emerald500.copy(alpha = pulseAlpha1 * 0.45f), CircleShape)
                        .border(2.dp, Emerald500.copy(alpha = pulseAlpha1), CircleShape)
                )

                // Avatar with glassmorphic ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .shadow(24.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF111C28))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Emerald500,
                                    Color.White.copy(alpha = 0.6f),
                                    Emerald500.copy(alpha = 0.4f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        name = callState.contactName.ifBlank { "Linko Peer" },
                        avatarUri = callState.contactAvatarUri,
                        size = 128.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = callState.contactName.ifBlank { "Linko Contact" },
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            val subtitleText = if (callState.isConnected) {
                formatCallDuration(callState.callDurationSeconds)
            } else {
                callState.statusText
            }

            Text(
                text = subtitleText,
                color = Emerald500,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("call_timer_or_status_text")
            )

            if (callState.contactEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = callState.contactEmail,
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isVideoCall && !callState.isVideoEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Camera Paused • HD Audio Active",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCallStatusHeader(
    callState: ActiveCallState,
    onToggle1080p: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glassmorphic E2EE + Firebase WebRTC P2P Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF0E1724).copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.24f),
                            Emerald500.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "End-to-End Encrypted",
                tint = Emerald500,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (callState.isConnected) {
                    "E2EE WebRTC • ${formatCallDuration(callState.callDurationSeconds)}"
                } else {
                    "E2EE WebRTC • ${callState.statusText}"
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Adaptive HD 720p / 1080p Toggle Pill (for Video Calls)
        if (callState.callType == CallType.VIDEO) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (callState.is1080pEnabled) {
                            Emerald500.copy(alpha = 0.25f)
                        } else {
                            Color(0xFF0E1724).copy(alpha = 0.72f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (callState.is1080pEnabled) Emerald500 else Color.White.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onToggle1080p)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("call_hd_resolution_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Hd,
                    contentDescription = "Toggle HD Resolution",
                    tint = if (callState.is1080pEnabled) Emerald500 else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (callState.is1080pEnabled) "FHD 1080p" else "HD 720p",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LocalPipPreviewCard(
    webRtcClient: LinkoWebRtcClient?,
    hasCameraPermission: Boolean,
    isFrontCamera: Boolean,
    resolutionBadge: String,
    onSwitchCamera: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(118.dp)
            .height(168.dp)
            .shadow(18.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF101926))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.38f),
                        Emerald500.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("local_pip_preview_card")
    ) {
        if (hasCameraPermission && webRtcClient != null) {
            WebRtcVideoRendererView(
                webRtcClient = webRtcClient,
                isLocalPreview = true,
                mirror = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF162436), Color(0xFF0B131E))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isFrontCamera) "Front Cam" else "Back Cam",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Top-left resolution micro-badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = resolutionBadge,
                color = Emerald500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom-right quick flip camera button inside PiP
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onSwitchCamera),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Flip Camera",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HdAudioStatusPill(
    isHdAudioActive: Boolean,
    isNoiseCancellationActive: Boolean,
    isVideoCall: Boolean,
    videoResolutionLabel: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF101B29).copy(alpha = 0.78f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("hd_audio_noise_cancel_badge")
    ) {
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null,
            tint = Emerald500,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        val badgeText = buildString {
            if (isHdAudioActive) append("HD 48kHz Voice")
            if (isNoiseCancellationActive) append(" • AEC/NS Active")
            if (isVideoCall) append(" • $videoResolutionLabel")
        }
        Text(
            text = badgeText,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Emerald500,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun GlassmorphicControlBar(
    isMuted: Boolean,
    isVideoCall: Boolean,
    isVideoEnabled: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(28.dp, RoundedCornerShape(34.dp))
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF182538).copy(alpha = 0.82f),
                        Color(0xFF0C131F).copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f),
                        Emerald500.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(34.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag("call_controls_glass_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Mute / Unmute Microphone
            GlassControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = isMuted,
                activeContainerColor = Color(0xFFF59E0B),
                onClick = onToggleMute,
                testTag = "call_mute_button"
            )

            // 2. Turn On / Off Camera (shown for Video Calls, or upgrades/toggles video)
            if (isVideoCall) {
                GlassControlButton(
                    icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (isVideoEnabled) "Cam On" else "Cam Off",
                    isActive = !isVideoEnabled,
                    activeContainerColor = Color(0xFFEF4444).copy(alpha = 0.85f),
                    onClick = onToggleVideo,
                    testTag = "call_video_toggle_button"
                )

                // 3. Switch Camera (Front / Back)
                GlassControlButton(
                    icon = Icons.Default.Cameraswitch,
                    label = "Flip",
                    isActive = false,
                    onClick = onSwitchCamera,
                    testTag = "call_switch_camera_button"
                )
            }

            // 4. Speaker / Earpiece Toggle
            GlassControlButton(
                icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PhoneInTalk,
                label = if (isSpeakerOn) "Speaker" else "Earpiece",
                isActive = isSpeakerOn,
                activeContainerColor = Emerald500,
                onClick = onToggleSpeaker,
                testTag = "call_speaker_button"
            )

            // 5. End Call Button
            EndCallButton(
                onClick = onEndCall,
                modifier = Modifier.testTag("call_end_button")
            )
        }
    }
}

@Composable
private fun GlassControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeContainerColor: Color = Emerald500,
    onClick: () -> Unit,
    testTag: String
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) activeContainerColor else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(200),
        label = "btn_bg_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (isActive) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(targetState = icon, label = "control_icon") { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EndCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "End",
            color = Color(0xFFFCA5A5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IncomingCallActionDock(
    callerName: String,
    isVideoCall: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF111C2B).copy(alpha = 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(32.dp))
            .padding(horizontal = 28.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onDecline)
                    .testTag("call_decline_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Decline Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Decline", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isVideoCall) "Incoming HD Video Call" else "Incoming HD Voice Call",
                    color = Emerald500,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = callerName,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onAnswer)
                    .testTag("call_answer_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Emerald500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Answer Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Answer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Jetpack Compose wrapper around WebRTC's [SurfaceViewRenderer] with safe EGL lifecycle management.
 */
@Composable
fun WebRtcVideoRendererView(
    webRtcClient: LinkoWebRtcClient,
    isLocalPreview: Boolean,
    mirror: Boolean,
    modifier: Modifier = Modifier
) {
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(webRtcClient, isLocalPreview) {
        onDispose {
            try {
                rendererRef?.release()
            } catch (_: Exception) {}
            rendererRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                try {
                    init(webRtcClient.eglBaseContext, null)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    setMirror(mirror)
                    setZOrderMediaOverlay(isLocalPreview)
                    if (isLocalPreview) {
                        webRtcClient.bindLocalRenderer(this)
                    } else {
                        webRtcClient.bindRemoteRenderer(this)
                    }
                } catch (_: Exception) {}
                rendererRef = this
            }
        },
        update = { view ->
            try {
                view.setMirror(mirror)
                if (isLocalPreview) {
                    webRtcClient.bindLocalRenderer(view)
                } else {
                    webRtcClient.bindRemoteRenderer(view)
                }
            } catch (_: Exception) {}
        },
        modifier = modifier
    )
}

private fun formatCallDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

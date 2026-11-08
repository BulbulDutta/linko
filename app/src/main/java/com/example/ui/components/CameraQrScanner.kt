package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate900
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraQrScanner(
    onQrScanned: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isHasScanned by remember { mutableStateOf(false) }
    var isScanSuccess by remember { mutableStateOf(false) }
    var scannedPreviewText by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    val triggerPolishedScanSuccess: (String) -> Unit = { payload ->
        if (!isHasScanned) {
            isHasScanned = true
            isScanSuccess = true
            scannedPreviewText = payload
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                delay(560)
                onQrScanned(payload)
            }
        }
    }

    val barcodeScanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
        }
    }

    // Glowing Laser & Corner Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 224f,
        animationSpec = infiniteRepeatable(
            animation = tween(1650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )
    val successScale by animateFloatAsState(
        targetValue = if (isScanSuccess) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SuccessFrameScale"
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val inputImage = InputImage.fromFilePath(context, uri)
                    barcodeScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val scannedValue = barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }?.rawValue
                            if (!scannedValue.isNullOrEmpty()) {
                                triggerPolishedScanSuccess(scannedValue)
                            } else {
                                Log.d("CameraQrScanner", "No QR found in gallery image")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CameraQrScanner", "Gallery QR decode error: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.e("CameraQrScanner", "Error reading gallery file: ${e.message}")
                }
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            @OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isHasScanned) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrEmpty() && !isHasScanned) {
                                                val trimmed = rawValue.trim()
                                                val isValidLinko = trimmed.startsWith("linko://user", ignoreCase = true) ||
                                                        trimmed.contains("@")
                                                if (isValidLinko) {
                                                    triggerPolishedScanSuccess(rawValue)
                                                    break
                                                } else {
                                                    android.widget.Toast.makeText(ctx, "Invalid Linko QR Code", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraQrScanner", "Failed to bind camera: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                onRelease = {
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(it.context)
                        if (cameraProviderFuture.isDone) {
                            cameraProviderFuture.get().unbindAll()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Emerald500,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Allow camera access to scan Linko QR codes live.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Grant Permission")
                }
            }
        }

        // Ambient Vignette Scrim around Viewfinder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        // Sleek Professional Viewfinder Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = glowAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanSuccess) "Contact QR Verified!" else "Align Contact QR within Frame",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glowing Viewfinder Frame
            Box(
                modifier = Modifier
                    .size(244.dp)
                    .scale(successScale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .border(
                        width = 1.5.dp,
                        color = Emerald500.copy(alpha = 0.35f * glowAlpha),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .testTag("qr_scanner_viewfinder_box")
            ) {
                // Sleek Glowing Corner Brackets drawn on Canvas
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val strokeW = 4.5.dp.toPx()
                    val bracketLen = 34.dp.toPx()
                    val bracketColor = Emerald500.copy(alpha = glowAlpha)
                    val w = size.width
                    val h = size.height

                    // Top-Left Bracket
                    drawLine(bracketColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeW, StrokeCap.Round)

                    // Top-Right Bracket
                    drawLine(bracketColor, Offset(w - bracketLen, 0f), Offset(w, 0f), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(w, 0f), Offset(w, bracketLen), strokeW, StrokeCap.Round)

                    // Bottom-Left Bracket
                    drawLine(bracketColor, Offset(0f, h - bracketLen), Offset(0f, h), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(0f, h), Offset(bracketLen, h), strokeW, StrokeCap.Round)

                    // Bottom-Right Bracket
                    drawLine(bracketColor, Offset(w - bracketLen, h), Offset(w, h), strokeW, StrokeCap.Round)
                    drawLine(bracketColor, Offset(w, h - bracketLen), Offset(w, h), strokeW, StrokeCap.Round)
                }

                // Glowing Laser Line + Soft Trailing Gradient Aura
                if (!isScanSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = laserY.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Emerald500.copy(alpha = 0.32f * glowAlpha)
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Emerald500,
                                            Color(0xFF69F0AE),
                                            Emerald500,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Polished Success Transition Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScanSuccess,
                    enter = fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = fadeOut(tween(180)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Emerald500.copy(alpha = 0.88f))
                            .testTag("qr_scan_success_overlay"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "QR Scanned Successfully",
                                        tint = Emerald500,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Contact Added!",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Syncing profile with Linko Cloud...",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        camera?.cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) Emerald500 else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = { showManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Manual Payload",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QR Payload", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.LightGray)
            }
        }

        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = { Text("Enter / Test QR Code Payload") },
                text = {
                    Column {
                        Text(
                            "Enter a Linko QR code URI or email address:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            placeholder = { Text("linko://user?email=friend@example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                val payload = manualInput.trim()
                                showManualDialog = false
                                triggerPolishedScanSuccess(payload)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Text("Add Friend")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


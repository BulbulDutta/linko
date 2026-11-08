package com.example.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

enum class ChatWallpaper(
    val id: String,
    val displayName: String,
    val previewPrimaryColor: Color,
    val previewSecondaryColor: Color,
    val isPattern: Boolean = false,
    @DrawableRes val drawableResId: Int? = null
) {
    DEFAULT(
        id = "DEFAULT",
        displayName = "Default Clean",
        previewPrimaryColor = Color(0xFF0F172A),
        previewSecondaryColor = Color(0xFF1E293B)
    ),
    DOODLE(
        id = "DOODLE",
        displayName = "Doodle Pattern",
        previewPrimaryColor = Color(0xFF0F172A),
        previewSecondaryColor = Color(0xFF10B981),
        isPattern = true
    ),
    NEW_BG_1(
        id = "NEW_BG_1",
        displayName = "Rainbow Shards",
        previewPrimaryColor = Color(0xFF1E3A8A),
        previewSecondaryColor = Color(0xFFEC4899),
        drawableResId = R.drawable.new_bg_1
    ),
    NEW_BG_2(
        id = "NEW_BG_2",
        displayName = "Color Bokeh",
        previewPrimaryColor = Color(0xFFEF4444),
        previewSecondaryColor = Color(0xFF9333EA),
        drawableResId = R.drawable.new_bg_2
    ),
    NEW_BG_3(
        id = "NEW_BG_3",
        displayName = "Rainbow Mandala",
        previewPrimaryColor = Color(0xFF0D9488),
        previewSecondaryColor = Color(0xFFEAB308),
        drawableResId = R.drawable.new_bg_3
    ),
    NEW_BG_4(
        id = "NEW_BG_4",
        displayName = "Silk Ribbons",
        previewPrimaryColor = Color(0xFF2563EB),
        previewSecondaryColor = Color(0xFFF59E0B),
        drawableResId = R.drawable.new_bg_4
    ),
    NEW_BG_5(
        id = "NEW_BG_5",
        displayName = "Sparkle Hearts",
        previewPrimaryColor = Color(0xFFF43F5E),
        previewSecondaryColor = Color(0xFF38BDF8),
        drawableResId = R.drawable.new_bg_5
    ),
    NEW_BG_6(
        id = "NEW_BG_6",
        displayName = "Neon Warp",
        previewPrimaryColor = Color(0xFFBE185D),
        previewSecondaryColor = Color(0xFF10B981),
        drawableResId = R.drawable.new_bg_6
    ),
    NEW_BG_7(
        id = "NEW_BG_7",
        displayName = "Prism Streaks",
        previewPrimaryColor = Color(0xFF6366F1),
        previewSecondaryColor = Color(0xFFEC4899),
        drawableResId = R.drawable.new_bg_7
    ),
    NEW_BG_8(
        id = "NEW_BG_8",
        displayName = "Golden Rays",
        previewPrimaryColor = Color(0xFF0F172A),
        previewSecondaryColor = Color(0xFFD97706),
        drawableResId = R.drawable.new_bg_8
    ),
    NEW_BG_9(
        id = "NEW_BG_9",
        displayName = "Gold & Navy",
        previewPrimaryColor = Color(0xFF0F2537),
        previewSecondaryColor = Color(0xFFF59E0B),
        drawableResId = R.drawable.new_bg_9
    ),
    NEW_BG_10(
        id = "NEW_BG_10",
        displayName = "Botanical Leaf",
        previewPrimaryColor = Color(0xFF166534),
        previewSecondaryColor = Color(0xFFFDE68A),
        drawableResId = R.drawable.new_bg_10
    ),
    CHARCOAL_DARK(
        id = "CHARCOAL_DARK",
        displayName = "Charcoal Slate",
        previewPrimaryColor = Color(0xFF111827),
        previewSecondaryColor = Color(0xFF1F2937)
    ),
    EMERALD_MINT(
        id = "EMERALD_MINT",
        displayName = "Emerald Mint",
        previewPrimaryColor = Color(0xFF042F2E),
        previewSecondaryColor = Color(0xFF064E3B)
    ),
    MIDNIGHT_INDIGO(
        id = "MIDNIGHT_INDIGO",
        displayName = "Midnight Indigo",
        previewPrimaryColor = Color(0xFF0F172A),
        previewSecondaryColor = Color(0xFF312E81)
    ),
    OCEAN_AZURE(
        id = "OCEAN_AZURE",
        displayName = "Ocean Azure",
        previewPrimaryColor = Color(0xFF082F49),
        previewSecondaryColor = Color(0xFF0C4A6E)
    ),
    FOREST_PINE(
        id = "FOREST_PINE",
        displayName = "Forest Pine",
        previewPrimaryColor = Color(0xFF052E16),
        previewSecondaryColor = Color(0xFF14532D)
    ),
    SUNSET_GLOW(
        id = "SUNSET_GLOW",
        displayName = "Sunset Dusk",
        previewPrimaryColor = Color(0xFF2E1065),
        previewSecondaryColor = Color(0xFF881337)
    ),
    GEOMETRIC_DOTS(
        id = "GEOMETRIC_DOTS",
        displayName = "Dot Matrix",
        previewPrimaryColor = Color(0xFF0F172A),
        previewSecondaryColor = Color(0xFF38BDF8),
        isPattern = true
    ),
    CUSTOM(
        id = "CUSTOM",
        displayName = "Custom Gallery",
        previewPrimaryColor = Color(0xFF334155),
        previewSecondaryColor = Color(0xFF0EA5E9),
        isPattern = false
    );

    companion object {
        val selectableWallpapers: List<ChatWallpaper>
            get() = entries.filter { it != CUSTOM }
    }
}

@Composable
fun ChatWallpaperBackground(
    wallpaper: ChatWallpaper,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.animation.Crossfade(
            targetState = wallpaper,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 380),
            label = "SeamlessWallpaperCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { targetWallpaper ->
            if (targetWallpaper.drawableResId != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(targetWallpaper.drawableResId)
                            .crossfade(280)
                            .build(),
                        contentDescription = targetWallpaper.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Adaptive contrast scrim so chat bubbles always remain crisp and legible
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = if (isDarkMode) 0.28f else 0.14f),
                                        Color.Black.copy(alpha = if (isDarkMode) 0.18f else 0.08f),
                                        Color.Black.copy(alpha = if (isDarkMode) 0.32f else 0.16f)
                                    )
                                )
                            )
                    )
                }
            } else when (targetWallpaper) {
                ChatWallpaper.CUSTOM -> {
                    if (!customWallpaperUri.isNullOrBlank()) {
                        val scale = when (customWallpaperScale) {
                            "FIT" -> ContentScale.Fit
                            "FILL" -> ContentScale.FillBounds
                            else -> ContentScale.Crop
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customWallpaperUri)
                                    .crossfade(280)
                                    .build(),
                                contentDescription = "Custom Wallpaper Background",
                                contentScale = scale,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = customWallpaperDimming.coerceIn(0f, 0.85f)))
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                        )
                    }
                }
            ChatWallpaper.DEFAULT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
            ChatWallpaper.DOODLE -> {
                val baseBg = if (isDarkMode) Color(0xFF0B132B) else Color(0xFFECE5DD)
                val doodleColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1A000000)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseBg)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.dp.toPx()
                        val stepX = 90.dp.toPx()
                        val stepY = 90.dp.toPx()
                        val cols = (size.width / stepX).toInt() + 1
                        val rows = (size.height / stepY).toInt() + 1

                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val x = c * stepX + (if (r % 2 == 1) stepX / 2f else 0f)
                                val y = r * stepY
                                val symbolType = (r * 3 + c * 7) % 5

                                when (symbolType) {
                                    0 -> { // Speech bubble outline
                                        val path = Path().apply {
                                            addOval(androidx.compose.ui.geometry.Rect(x - 12f, y - 10f, x + 12f, y + 10f))
                                            moveTo(x - 6f, y + 8f)
                                            lineTo(x - 12f, y + 14f)
                                            lineTo(x, y + 10f)
                                        }
                                        drawPath(path, doodleColor, style = Stroke(strokeWidth))
                                    }
                                    1 -> { // Star icon
                                        val path = Path().apply {
                                            moveTo(x, y - 12f)
                                            lineTo(x + 3f, y - 4f)
                                            lineTo(x + 12f, y - 4f)
                                            lineTo(x + 5f, y + 2f)
                                            lineTo(x + 8f, y + 10f)
                                            lineTo(x, y + 5f)
                                            lineTo(x - 8f, y + 10f)
                                            lineTo(x - 5f, y + 2f)
                                            lineTo(x - 12f, y - 4f)
                                            lineTo(x - 3f, y - 4f)
                                            close()
                                        }
                                        drawPath(path, doodleColor, style = Stroke(strokeWidth))
                                    }
                                    2 -> { // Music note
                                        drawCircle(doodleColor, radius = 5f, center = Offset(x - 4f, y + 6f))
                                        drawLine(doodleColor, start = Offset(x + 1f, y + 6f), end = Offset(x + 1f, y - 8f), strokeWidth = strokeWidth)
                                        drawLine(doodleColor, start = Offset(x + 1f, y - 8f), end = Offset(x + 8f, y - 4f), strokeWidth = strokeWidth)
                                    }
                                    3 -> { // Heart
                                        val path = Path().apply {
                                            moveTo(x, y + 8f)
                                            cubicTo(x - 12f, y, x - 12f, y - 10f, x, y - 4f)
                                            cubicTo(x + 12f, y - 10f, x + 12f, y, x, y + 8f)
                                        }
                                        drawPath(path, doodleColor, style = Stroke(strokeWidth))
                                    }
                                    4 -> { // Paper plane
                                        val path = Path().apply {
                                            moveTo(x - 10f, y - 8f)
                                            lineTo(x + 10f, y)
                                            lineTo(x - 10f, y + 8f)
                                            lineTo(x - 4f, y)
                                            close()
                                        }
                                        drawPath(path, doodleColor, style = Stroke(strokeWidth))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ChatWallpaper.CHARCOAL_DARK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF111827), Color(0xFF1F2937))
                            )
                        )
                )
            }
            ChatWallpaper.EMERALD_MINT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF042F2E), Color(0xFF064E3B), Color(0xFF022C22))
                            )
                        )
                )
            }
            ChatWallpaper.MIDNIGHT_INDIGO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81))
                            )
                        )
                )
            }
            ChatWallpaper.OCEAN_AZURE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF082F49), Color(0xFF0C4A6E), Color(0xFF0369A1))
                            )
                        )
                )
            }
            ChatWallpaper.FOREST_PINE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF052E16), Color(0xFF14532D), Color(0xFF166534))
                            )
                        )
                )
            }
            ChatWallpaper.SUNSET_GLOW -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E1065), Color(0xFF4C0519), Color(0xFF881337))
                            )
                        )
                )
            }
            ChatWallpaper.GEOMETRIC_DOTS -> {
                val dotBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                val dotColor = if (isDarkMode) Color(0x3338BDF8) else Color(0x330284C7)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(dotBg)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = 32.dp.toPx()
                        val cols = (size.width / spacing).toInt() + 1
                        val rows = (size.height / spacing).toInt() + 1
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                drawCircle(
                                    color = dotColor,
                                    radius = 3f,
                                    center = Offset(c * spacing, r * spacing)
                                )
                            }
                        }
                    }
                }
            }
                else -> {}
            }
        }
        content()
    }
}

package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.domain.model.AppThemeMode
import com.example.utils.LocalHaptics
import com.example.utils.rememberHapticController
import kotlin.math.abs

val LocalIsDarkGlass = compositionLocalOf { false }
val LocalHasRootGlassBackground = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF173A6A),
    onPrimaryContainer = Color.White,
    secondary = LuminousCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A5F),
    onSecondaryContainer = Color.White,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B2868),
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF070D19),
    surfaceContainerLow = Color(0xFF0F1829),
    surfaceContainer = Color(0xFF152238),
    surfaceContainerHigh = Color(0xFF1C2D48),
    surfaceContainerHighest = Color(0xFF253B5E),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF475569),
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAFF),
    onPrimaryContainer = Color(0xFF002D6E),
    secondary = ElectricTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF1E1B4B),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = CrimsonError,
    onError = Color.White
)

val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DocuScanTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val haptics = rememberHapticController()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalIsDarkGlass provides darkTheme,
        LocalHaptics provides haptics,
        LocalContentColor provides colorScheme.onSurface
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes
        ) {
            CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
                content()
            }
        }
    }
}

/**
 * Authentic iPhone iOS 18 / visionOS Ambient Mesh Background with cached GPU shaders,
 * automatic root deduplication, and explicit high-contrast LocalContentColor propagation.
 */
@Composable
fun GlassScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val alreadyHasRootBg = LocalHasRootGlassBackground.current
    if (alreadyHasRootBg) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = modifier.fillMaxSize(),
                content = content
            )
        }
        return
    }

    val isDark = LocalIsDarkGlass.current

    val topBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF040814) else Color(0xFFD6E6FB),
        animationSpec = tween(260),
        label = "glassTopBg"
    )
    val midBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0A1428) else Color(0xFFEBF3FD),
        animationSpec = tween(260),
        label = "glassMidBg"
    )
    val bottomBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF060C1B) else Color(0xFFDCE8F8),
        animationSpec = tween(260),
        label = "glassBottomBg"
    )

    CompositionLocalProvider(
        LocalHasRootGlassBackground provides true,
        LocalContentColor provides contentColor
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width.coerceAtLeast(1f)
                    val h = size.height.coerceAtLeast(1f)
                    val baseBrush = Brush.verticalGradient(
                        colors = listOf(topBg, midBg, bottomBg)
                    )
                    val orb1Center = Offset(w * 0.14f, h * 0.11f)
                    val orb1Radius = w * 0.62f
                    val orb1Brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF0A84FF).copy(alpha = 0.28f) else Color(0xFF60A5FA).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = orb1Center,
                        radius = orb1Radius
                    )
                    val orb2Center = Offset(w * 0.86f, h * 0.42f)
                    val orb2Radius = w * 0.56f
                    val orb2Brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF5E5CE6).copy(alpha = 0.22f) else Color(0xFF818CF8).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = orb2Center,
                        radius = orb2Radius
                    )

                    onDrawBehind {
                        drawRect(brush = baseBrush)
                        drawCircle(brush = orb1Brush, radius = orb1Radius, center = orb1Center)
                        drawCircle(brush = orb2Brush, radius = orb2Radius, center = orb2Center)
                    }
                },
            content = content
        )
    }
}

/**
 * Smooth iPhone iOS Frosted Glass Card with cached brushes, single-pass GPU drawing,
 * and guaranteed high-contrast LocalContentColor in both Light and Dark mode.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val cardModifier = modifier
        .clip(shape)
        .drawWithCache {
            val crPx = cornerRadius.toPx()
            val corner = CornerRadius(crPx, crPx)
            val strokePx = 1.dp.toPx()
            val fillBrush = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E2D4A).copy(alpha = 0.78f),
                        Color(0xFF152238).copy(alpha = 0.82f),
                        Color(0xFF0F172A).copy(alpha = 0.88f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.82f),
                        Color.White.copy(alpha = 0.64f),
                        Color(0xFFF0F7FF).copy(alpha = 0.74f)
                    )
                )
            }
            val borderBrush = if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.16f),
                        LuminousCyan.copy(alpha = 0.28f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.88f)
                    )
                )
            }
            val topGlintColor = if (isDark) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.90f)

            onDrawWithContent {
                drawRoundRect(brush = fillBrush, cornerRadius = corner)
                drawRoundRect(
                    brush = borderBrush,
                    cornerRadius = corner,
                    style = Stroke(width = strokePx)
                )
                drawContent()
                drawLine(
                    color = topGlintColor,
                    start = Offset(crPx * 0.6f, strokePx),
                    end = Offset((size.width - crPx * 0.6f).coerceAtLeast(crPx * 0.6f), strokePx),
                    strokeWidth = strokePx
                )
            }
        }
        .then(
            if (onClick != null) {
                Modifier.clickable {
                    haptics.click()
                    onClick()
                }
            } else {
                Modifier
            }
        )

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(modifier = cardModifier) {
            content()
        }
    }
}

/**
 * Adaptive, strictly aligned iOS Frosted Glass Action Button with high-contrast text in Night & Light mode.
 */
@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    height: Dp = 44.dp
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val shape = remember { RoundedCornerShape(14.dp) }

    if (isPrimary) {
        Button(
            onClick = {
                haptics.click()
                onClick()
            },
            shape = shape,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF0A84FF) else RoyalBlue,
                contentColor = Color.White
            ),
            modifier = modifier.height(height)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        val labelColor = if (isDark) Color.White else LightOnSurface
        val iconColor = if (isDark) LuminousCyan else RoyalBlue
        Box(
            modifier = modifier
                .height(height)
                .clip(shape)
                .drawWithCache {
                    val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    val fillBrush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF243B61).copy(alpha = 0.85f),
                                Color(0xFF172742).copy(alpha = 0.90f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.90f),
                                Color(0xFFEFF6FF).copy(alpha = 0.78f)
                            )
                        )
                    }
                    val borderBrush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.48f),
                                LuminousCyan.copy(alpha = 0.32f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFF93C5FD).copy(alpha = 0.65f)
                            )
                        )
                    }
                    onDrawBehind {
                        drawRoundRect(brush = fillBrush, cornerRadius = corner)
                        drawRoundRect(
                            brush = borderBrush,
                            cornerRadius = corner,
                            style = Stroke(width = 1.1.dp.toPx())
                        )
                    }
                }
                .clickable {
                    haptics.click()
                    onClick()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3D Dynamic Glowing RGB Camera Orb:
 * Features a volumetric 3D sphere with an animated rotating RGB chromatic halo,
 * rotating beveled neon ring, inner RGB bounce light, specular 3D reflection,
 * and glowing depth shadow.
 */
@Composable
fun Glowing3dRgbCameraOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    iconSize: Dp = 25.dp,
    icon: ImageVector = Icons.Default.CameraAlt,
    contentDescription: String = "Dynamic 3D RGB Camera Scan"
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    // Dynamic rotating RGB animation
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicRgbCameraTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RgbCameraRotation"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RgbCameraPulse"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RgbCameraHaloAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.heavy()
                    onClick()
                }
            )
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h / 2f)
                val maxR = (w.coerceAtMost(h) / 2f).coerceAtLeast(1f)

                val rgbColors = listOf(
                    RgbCyan,
                    RgbBlue,
                    RgbViolet,
                    RgbMagenta,
                    RgbOrange,
                    RgbEmerald,
                    RgbCyan
                )
                val rgbSweepBrush = Brush.sweepGradient(
                    colors = rgbColors,
                    center = center
                )
                val outerHaloBrush = Brush.radialGradient(
                    colors = listOf(
                        RgbMagenta.copy(alpha = 0.65f),
                        RgbCyan.copy(alpha = 0.45f),
                        RgbViolet.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.25f
                )
                val bottomDropShadowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        RgbBlue.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(center.x, center.y + maxR * 0.18f),
                    radius = maxR * 0.98f
                )
                val sphereCoreRadius = maxR * 0.76f
                val sphereCoreBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF283566),
                        Color(0xFF111833),
                        Color(0xFF060914)
                    ),
                    center = Offset(center.x - sphereCoreRadius * 0.25f, center.y - sphereCoreRadius * 0.28f),
                    radius = sphereCoreRadius * 1.35f
                )
                val rgbInnerBounceBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        RgbCyan.copy(alpha = 0.40f),
                        RgbMagenta.copy(alpha = 0.55f)
                    ),
                    start = Offset(center.x, center.y - sphereCoreRadius * 0.2f),
                    end = Offset(center.x, center.y + sphereCoreRadius)
                )
                val specularHighlightCenter = Offset(center.x - sphereCoreRadius * 0.28f, center.y - sphereCoreRadius * 0.34f)
                val specularBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = specularHighlightCenter,
                    radius = sphereCoreRadius * 0.58f
                )
                val ringRadius = maxR * 0.84f
                val ringStrokeWidth = (maxR * 0.17f).coerceAtLeast(3.dp.toPx())
                val innerRimStrokeWidth = 1.2.dp.toPx()

                onDrawBehind {
                    // 1. 3D Drop Shadow
                    drawCircle(
                        brush = bottomDropShadowBrush,
                        radius = maxR * 0.98f,
                        center = Offset(center.x, center.y + maxR * 0.14f)
                    )

                    // 2. Dynamic Rotating Outer RGB Neon Halo Bloom
                    rotate(degrees = rotationAngle, pivot = center) {
                        drawCircle(
                            brush = outerHaloBrush,
                            radius = maxR * 1.15f * pulseGlow,
                            center = center,
                            alpha = haloAlpha
                        )
                    }

                    // 3. Dynamic Rotating 3D Outer Beveled RGB Neon Ring
                    rotate(degrees = rotationAngle, pivot = center) {
                        drawCircle(
                            brush = rgbSweepBrush,
                            radius = ringRadius,
                            center = center
                        )
                    }

                    // 4. 3D Spherical Dark Lens Core (stays steady for 3D depth)
                    drawCircle(
                        brush = sphereCoreBrush,
                        radius = sphereCoreRadius,
                        center = center
                    )

                    // 5. Dynamic Bottom RGB Ambient Bounce Light rotating smoothly
                    rotate(degrees = -rotationAngle * 0.5f, pivot = center) {
                        drawCircle(
                            brush = rgbInnerBounceBrush,
                            radius = sphereCoreRadius,
                            center = center
                        )
                    }

                    // 6. Dynamic Crisp Prismatic RGB Ring Stroke & Inner Rim Highlight
                    rotate(degrees = rotationAngle, pivot = center) {
                        drawCircle(
                            brush = rgbSweepBrush,
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = ringStrokeWidth * 0.45f)
                        )
                    }
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = sphereCoreRadius,
                        center = center,
                        style = Stroke(width = innerRimStrokeWidth)
                    )

                    // 7. Top-Left 3D Curved Specular Dome Glint
                    drawCircle(
                        brush = specularBrush,
                        radius = sphereCoreRadius * 0.56f,
                        center = specularHighlightCenter
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Dynamic glowing chromatic depth shadow under the icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RgbMagenta.copy(alpha = 0.70f),
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    translationX = 1.5f
                    translationY = 2.5f
                }
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Dynamic 3D Glowing & Blinking RGB Action Button:
 * Prominent attention-grabbing flagship button with a stationary rounded-rectangle frame,
 * continuous 360° rotating RGB SweepGradient shader, multi-layered blinking neon aura bloom,
 * pulsing RGB icon orb, and blinking live status badge.
 */
@Composable
fun DynamicRgbActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.EditNote,
    badgeText: String? = "LIVE",
    height: Dp = 44.dp,
    testTag: String = "dynamic_rgb_action_button"
) {
    val haptics = LocalHaptics.current
    val isDark = LocalIsDarkGlass.current
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicRgbButton")

    // Continuous 360° RGB color rotation around the stationary rounded rectangle perimeter
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RgbButtonSweep"
    )

    // Fast attention-seeking neon RGB blink / heartbeat strobe
    val blinkPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RgbButtonBlinkPulse"
    )

    // Subtle breathing scale so the flagship button feels alive
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.022f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RgbButtonScalePulse"
    )

    val cornerRadiusDp = if (height <= 34.dp) 10.dp else 14.dp
    val shape = RoundedCornerShape(cornerRadiusDp)

    val rgbColorInts = remember {
        intArrayOf(
            android.graphics.Color.rgb(0, 242, 254),   // Electric Cyan
            android.graphics.Color.rgb(59, 130, 246),  // Royal Blue
            android.graphics.Color.rgb(168, 85, 247),  // Neon Violet
            android.graphics.Color.rgb(255, 8, 68),    // Hot Magenta
            android.graphics.Color.rgb(255, 138, 0),   // Solar Orange
            android.graphics.Color.rgb(0, 230, 118),   // Emerald Neon
            android.graphics.Color.rgb(0, 242, 254)    // Loop back to Cyan
        )
    }

    val isCompact = height <= 36.dp

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scalePulse
                scaleY = scalePulse
            }
            .testTag(testTag)
            .drawWithContent {
                val w = size.width.coerceAtLeast(1f)
                val h = size.height.coerceAtLeast(1f)
                val cx = w / 2f
                val cy = h / 2f
                val crPx = cornerRadiusDp.toPx()

                // Stationary rounded-rectangle shader: rotate ONLY the SweepGradient matrix, NEVER the rectangle!
                val sweepShader = android.graphics.SweepGradient(cx, cy, rgbColorInts, null)
                val shaderMatrix = android.graphics.Matrix().apply {
                    setRotate(sweepAngle, cx, cy)
                }
                sweepShader.setLocalMatrix(shaderMatrix)
                val rotatingRgbBrush = ShaderBrush(sweepShader)

                // 1. Outer Unclipped Blinking RGB Neon Halo Bloom (radiates outside button edges)
                val outerPad1 = 3.5.dp.toPx()
                drawRoundRect(
                    brush = rotatingRgbBrush,
                    topLeft = Offset(-outerPad1, -outerPad1),
                    size = Size(w + outerPad1 * 2f, h + outerPad1 * 2f),
                    cornerRadius = CornerRadius(crPx + outerPad1, crPx + outerPad1),
                    style = Stroke(width = 5.5.dp.toPx()),
                    alpha = 0.22f + (0.48f * blinkPulse)
                )

                val outerPad2 = 1.2.dp.toPx()
                drawRoundRect(
                    brush = rotatingRgbBrush,
                    topLeft = Offset(-outerPad2, -outerPad2),
                    size = Size(w + outerPad2 * 2f, h + outerPad2 * 2f),
                    cornerRadius = CornerRadius(crPx + outerPad2, crPx + outerPad2),
                    style = Stroke(width = 3.2.dp.toPx()),
                    alpha = 0.50f + (0.45f * blinkPulse)
                )

                // 2. Deep High-Contrast Cyber-Glass Interior Fill so RGB Neon & White Text Pop
                val bgFillColor = if (isDark) Color(0xFF091124) else Color(0xFF0F172A)
                drawRoundRect(
                    color = bgFillColor.copy(alpha = 0.94f),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(crPx, crPx)
                )

                // 3. Inner Blinking RGB Aurora Sheen across the button face
                drawRoundRect(
                    brush = rotatingRgbBrush,
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(crPx, crPx),
                    alpha = 0.14f + (0.20f * blinkPulse)
                )

                // 4. Draw Button Content (Icon, Text, Blinking Badge)
                drawContent()

                // 5. Crisp Stationary Rounded-Rect Rotating RGB Border Outline ON TOP of Background
                val strokeInset = 1.dp.toPx()
                drawRoundRect(
                    brush = rotatingRgbBrush,
                    topLeft = Offset(strokeInset, strokeInset),
                    size = Size(w - strokeInset * 2f, h - strokeInset * 2f),
                    cornerRadius = CornerRadius((crPx - strokeInset).coerceAtLeast(2f), (crPx - strokeInset).coerceAtLeast(2f)),
                    style = Stroke(width = if (isCompact) 2.2.dp.toPx() else 2.7.dp.toPx())
                )

                // 6. Blinking White-Hot Specular Strobe Rim inside the RGB border
                val innerInset = 2.2.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f + (0.58f * blinkPulse)),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size((w - innerInset * 2f).coerceAtLeast(1f), (h - innerInset * 2f).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius((crPx - innerInset).coerceAtLeast(1f), (crPx - innerInset).coerceAtLeast(1f)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clip(shape)
            .clickable {
                haptics.heavy()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 9.dp else 12.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 5.dp else 7.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 20.dp else 24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    lerp(RgbCyan, RgbMagenta, blinkPulse),
                                    lerp(RgbMagenta, RgbEmerald, blinkPulse)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isCompact) 13.dp else 15.dp)
                    )
                }
            }
            Text(
                text = text,
                style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = lerp(RgbMagenta, RgbCyan, blinkPulse).copy(alpha = 0.28f + 0.30f * blinkPulse),
                    border = BorderStroke(
                        1.dp,
                        lerp(RgbCyan, Color.White, blinkPulse).copy(alpha = 0.65f + 0.35f * blinkPulse)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    lerp(RgbEmerald, Color.White, blinkPulse)
                                        .copy(alpha = 0.45f + 0.55f * blinkPulse)
                                )
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Dynamic Blinking & Rotating RGB Chip for toolbars, document cards & header strips.
 */
@Composable
fun DynamicRgbChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.EditNote,
    badgeText: String? = null,
    height: Dp = 34.dp,
    testTag: String = "dynamic_rgb_chip"
) {
    DynamicRgbActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        badgeText = badgeText,
        height = height,
        testTag = testTag
    )
}

/**
 * Flagship Hero Card with a stationary rounded-rectangle rotating & blinking RGB neon perimeter outline.
 */
@Composable
fun DynamicRgbGlowCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicRgbGlowCard")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RgbCardSweep"
    )
    val blinkPulse by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RgbCardBlink"
    )

    val rgbColorInts = remember {
        intArrayOf(
            android.graphics.Color.rgb(0, 242, 254),
            android.graphics.Color.rgb(59, 130, 246),
            android.graphics.Color.rgb(168, 85, 247),
            android.graphics.Color.rgb(255, 8, 68),
            android.graphics.Color.rgb(255, 138, 0),
            android.graphics.Color.rgb(0, 230, 118),
            android.graphics.Color.rgb(0, 242, 254)
        )
    }

    val shape = RoundedCornerShape(cornerRadius)
    val contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .drawWithContent {
                    val w = size.width.coerceAtLeast(1f)
                    val h = size.height.coerceAtLeast(1f)
                    val cx = w / 2f
                    val cy = h / 2f
                    val crPx = cornerRadius.toPx()

                    val sweepShader = android.graphics.SweepGradient(cx, cy, rgbColorInts, null)
                    val shaderMatrix = android.graphics.Matrix().apply {
                        setRotate(sweepAngle, cx, cy)
                    }
                    sweepShader.setLocalMatrix(shaderMatrix)
                    val rotatingRgbBrush = ShaderBrush(sweepShader)

                    // Outer pulsing RGB neon bloom around the card
                    val outerPad = 2.5.dp.toPx()
                    drawRoundRect(
                        brush = rotatingRgbBrush,
                        topLeft = Offset(-outerPad, -outerPad),
                        size = Size(w + outerPad * 2f, h + outerPad * 2f),
                        cornerRadius = CornerRadius(crPx + outerPad, crPx + outerPad),
                        style = Stroke(width = 4.5.dp.toPx()),
                        alpha = 0.24f + (0.42f * blinkPulse)
                    )

                    // Frosted glass card surface fill
                    val fillColor = if (isDark) Color(0xFF101B33).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        cornerRadius = CornerRadius(crPx, crPx)
                    )

                    // Subtle rotating RGB ambient tint inside card
                    drawRoundRect(
                        brush = rotatingRgbBrush,
                        topLeft = Offset.Zero,
                        size = Size(w, h),
                        cornerRadius = CornerRadius(crPx, crPx),
                        alpha = if (isDark) 0.10f * blinkPulse else 0.07f * blinkPulse
                    )

                    drawContent()

                    // Crisp stationary rotating RGB perimeter outline
                    val inset = 1.dp.toPx()
                    drawRoundRect(
                        brush = rotatingRgbBrush,
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2f, h - inset * 2f),
                        cornerRadius = CornerRadius((crPx - inset).coerceAtLeast(2f), (crPx - inset).coerceAtLeast(2f)),
                        style = Stroke(width = 2.2.dp.toPx()),
                        alpha = 0.75f + (0.25f * blinkPulse)
                    )
                }
                .clip(shape)
        ) {
            content()
        }
    }
}

/**
 * 1-Tap iPhone Glass Light / Dark Mode Switch Pill for headers & settings with vibration haptics.
 */
@Composable
fun GlassThemeTogglePill(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val bg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1E2D4A).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.85f),
        animationSpec = tween(220),
        label = "glassToggleBg"
    )
    val border by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.95f),
        animationSpec = tween(220),
        label = "glassToggleBorder"
    )

    Surface(
        onClick = {
            haptics.click()
            onToggle()
        },
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "Toggle Dark or Light Mode",
                tint = if (isDark) LuminousCyan else RoyalBlue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isDark) "Dark" else "Light",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else LightOnSurface
            )
        }
    }
}

data class LiquidGlassNavTab(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Ultra-smooth 120fps iPhone iOS 18 / visionOS Liquid Glass Bottom Navigation Bar
 * featuring a centerpiece 3D Glowing RGB Camera Orb in the middle of the dock.
 * - Reads continuous swipe progress inside `drawWithContent` and `graphicsLayer` (draw phase only)
 *   so swiping left/right across tabs causes ZERO recompositions!
 */
@Composable
fun LiquidGlassBottomBar(
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    selectedProgress: Float? = null,
    selectedProgressProvider: (() -> Float)? = null,
    onCenterCameraClick: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val dockShape = remember { RoundedCornerShape(32.dp) }

    val activeIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)
    val fallbackAnimatedProgress by animateFloatAsState(
        targetValue = (selectedProgress ?: activeIndex.toFloat())
            .coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat()),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquidPillFallbackProgress"
    )

    // Animated translucent glass colors so switching Light <-> Dark mode melts seamlessly
    val glassStart by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1E2D4A).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f),
        animationSpec = tween(240),
        label = "dockGlassStart"
    )
    val glassMid by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0F182A).copy(alpha = 0.88f) else Color(0xFFEFF6FF).copy(alpha = 0.78f),
        animationSpec = tween(240),
        label = "dockGlassMid"
    )
    val glassEnd by animateColorAsState(
        targetValue = if (isDark) Color(0xFF17284A).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.85f),
        animationSpec = tween(240),
        label = "dockGlassEnd"
    )

    val borderTop by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.96f),
        animationSpec = tween(240),
        label = "dockBorderTop"
    )
    val borderMid by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.28f) else Color(0xFF93C5FD).copy(alpha = 0.52f),
        animationSpec = tween(240),
        label = "dockBorderMid"
    )
    val borderBottom by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.78f),
        animationSpec = tween(240),
        label = "dockBorderBottom"
    )

    val pillFillTop by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0A84FF).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.94f),
        animationSpec = tween(220),
        label = "pillFillTop"
    )
    val pillFillBottom by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.24f) else Color(0xFFDBEAFE).copy(alpha = 0.86f),
        animationSpec = tween(220),
        label = "pillFillBottom"
    )
    val pillStrokeColor by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.70f) else RoyalBlue.copy(alpha = 0.34f),
        animationSpec = tween(220),
        label = "pillStroke"
    )

    val ambientScrimColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF050A16).copy(alpha = 0.55f) else Color(0xFFDCE8F8).copy(alpha = 0.55f),
        animationSpec = tween(240),
        label = "ambientScrim"
    )

    val topGlintColor = if (isDark) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.96f)
    val maxTabIndex = (tabs.size - 1).coerceAtLeast(0).toFloat()
    val hasCenterOrb = onCenterCameraClick != null && tabs.size == 4

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ambientScrimColor.copy(alpha = ambientScrimColor.alpha * 0.45f),
                            ambientScrimColor
                        )
                    )
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(dockShape)
                .drawWithCache {
                    val dockCorner = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                    val dockBgBrush = Brush.linearGradient(listOf(glassStart, glassMid, glassEnd))
                    val dockBorderBrush = Brush.linearGradient(listOf(borderTop, borderMid, borderBottom))
                    val borderStrokePx = 1.15.dp.toPx()
                    val tabCount = tabs.size.coerceAtLeast(1)
                    val horizontalPad = 6.dp.toPx()
                    val verticalPad = 5.dp.toPx()
                    val centerSlotW = if (hasCenterOrb) 62.dp.toPx() else 0f
                    val availableWidth = (size.width - horizontalPad * 2f - centerSlotW).coerceAtLeast(1f)
                    val slotWidth = availableWidth / tabCount
                    val pillHeight = (size.height - verticalPad * 2f).coerceAtLeast(1f)
                    val pillCorner = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    val pillFillBrush = Brush.verticalGradient(
                        colors = listOf(pillFillTop, pillFillBottom),
                        startY = verticalPad,
                        endY = verticalPad + pillHeight
                    )
                    val pillStrokePx = 1.dp.toPx()
                    val glintStartX = 28.dp.toPx()
                    val glintEndX = (size.width - 28.dp.toPx()).coerceAtLeast(glintStartX)
                    val glintY = 1.1.dp.toPx()
                    val glintStrokePx = 1.25.dp.toPx()

                    onDrawWithContent {
                        // Draw dock glass surface & border
                        drawRoundRect(brush = dockBgBrush, cornerRadius = dockCorner)
                        drawRoundRect(
                            brush = dockBorderBrush,
                            cornerRadius = dockCorner,
                            style = Stroke(width = borderStrokePx)
                        )

                        // Read continuous progress in draw phase only (zero recomposition!)
                        val currentProgress = (selectedProgressProvider?.invoke() ?: fallbackAnimatedProgress)
                            .coerceIn(0f, maxTabIndex)

                        val pillLeft = if (hasCenterOrb) {
                            when {
                                currentProgress <= 1f -> horizontalPad + currentProgress * slotWidth
                                currentProgress >= 2f -> horizontalPad + centerSlotW + currentProgress * slotWidth
                                else -> {
                                    val t = currentProgress - 1f
                                    val startX = horizontalPad + 1f * slotWidth
                                    val endX = horizontalPad + centerSlotW + 2f * slotWidth
                                    startX + (endX - startX) * t
                                }
                            }
                        } else {
                            horizontalPad + currentProgress * slotWidth
                        }

                        // Continuous sliding Liquid Glass Active Pill
                        drawRoundRect(
                            brush = pillFillBrush,
                            topLeft = Offset(pillLeft, verticalPad),
                            size = Size(slotWidth, pillHeight),
                            cornerRadius = pillCorner
                        )
                        drawRoundRect(
                            color = pillStrokeColor,
                            topLeft = Offset(pillLeft, verticalPad),
                            size = Size(slotWidth, pillHeight),
                            cornerRadius = pillCorner,
                            style = Stroke(width = pillStrokePx)
                        )

                        drawContent()

                        // Specular top-edge liquid light refraction highlight
                        drawLine(
                            color = topGlintColor,
                            start = Offset(glintStartX, glintY),
                            end = Offset(glintEndX, glintY),
                            strokeWidth = glintStrokePx
                        )
                    }
                }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                // Inject the 3D Glowing RGB Center Camera Orb right in the middle of the dock (between tab 1 and tab 2)
                if (hasCenterOrb && index == 2) {
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Glowing3dRgbCameraOrb(
                            onClick = { onCenterCameraClick?.invoke() },
                            size = 52.dp,
                            iconSize = 24.dp,
                            contentDescription = "Center 3D RGB Camera"
                        )
                    }
                }

                val activeColor = if (isDark) Color.White else RoyalBlue
                val inactiveColor = if (isDark) Color(0xFFD8E4F8) else Color(0xFF334155)
                val tintColor = if (tab.selected) activeColor else inactiveColor
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptics.click()
                                tab.onClick()
                            }
                        )
                        .padding(vertical = 7.dp, horizontal = 2.dp)
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer {
                            val currentProgress = (selectedProgressProvider?.invoke() ?: fallbackAnimatedProgress)
                                .coerceIn(0f, maxTabIndex)
                            val distance = abs(currentProgress - index.toFloat()).coerceIn(0f, 1f)
                            val selectionFraction = 1f - distance
                            val scale = 1f + (0.08f * selectionFraction)
                            scaleX = scale
                            scaleY = scale
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (tab.selected && isDark) LuminousCyan else tintColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (tab.selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = tintColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}



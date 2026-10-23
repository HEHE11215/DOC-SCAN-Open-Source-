package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
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
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 3D Glowing RGB Camera Orb:
 * Renders a volumetric 3D sphere with an outer multi-spectrum RGB neon halo, a 3D beveled RGB
 * prismatic ring, a deep spherical lens core with bottom RGB bounce light, a top-left 3D glass
 * specular reflection dome, and a crisp white camera icon.
 */
@Composable
fun Glowing3dRgbCameraOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    iconSize: Dp = 25.dp,
    icon: ImageVector = Icons.Default.CameraAlt,
    contentDescription: String = "3D RGB Camera Scan"
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

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
                        RgbMagenta.copy(alpha = 0.52f),
                        RgbCyan.copy(alpha = 0.34f),
                        RgbViolet.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * 1.18f
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
                        RgbCyan.copy(alpha = 0.35f),
                        RgbMagenta.copy(alpha = 0.45f)
                    ),
                    start = Offset(center.x, center.y - sphereCoreRadius * 0.2f),
                    end = Offset(center.x, center.y + sphereCoreRadius)
                )
                val specularHighlightCenter = Offset(center.x - sphereCoreRadius * 0.28f, center.y - sphereCoreRadius * 0.34f)
                val specularBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = specularHighlightCenter,
                    radius = sphereCoreRadius * 0.58f
                )
                val ringRadius = maxR * 0.84f
                val ringStrokeWidth = (maxR * 0.17f).coerceAtLeast(3.dp.toPx())
                val innerRimStrokeWidth = 1.2.dp.toPx()

                onDrawBehind {
                    // 1. 3D Drop Shadow + Outer RGB Neon Halo Bloom
                    drawCircle(
                        brush = bottomDropShadowBrush,
                        radius = maxR * 0.98f,
                        center = Offset(center.x, center.y + maxR * 0.14f)
                    )
                    drawCircle(
                        brush = outerHaloBrush,
                        radius = maxR * 1.12f,
                        center = center
                    )

                    // 2. 3D Outer Beveled RGB Neon Ring
                    drawCircle(
                        brush = rgbSweepBrush,
                        radius = ringRadius,
                        center = center
                    )

                    // 3. 3D Spherical Dark Lens Core
                    drawCircle(
                        brush = sphereCoreBrush,
                        radius = sphereCoreRadius,
                        center = center
                    )

                    // 4. Bottom RGB Ambient Bounce Light inside the 3D Sphere
                    drawCircle(
                        brush = rgbInnerBounceBrush,
                        radius = sphereCoreRadius,
                        center = center
                    )

                    // 5. Crisp Prismatic RGB Ring Stroke & Inner Rim Highlight
                    drawCircle(
                        brush = rgbSweepBrush,
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringStrokeWidth * 0.45f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.42f),
                        radius = sphereCoreRadius,
                        center = center,
                        style = Stroke(width = innerRimStrokeWidth)
                    )

                    // 6. Top-Left 3D Curved Specular Dome Glint
                    drawCircle(
                        brush = specularBrush,
                        radius = sphereCoreRadius * 0.56f,
                        center = specularHighlightCenter
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Subtle 3D icon depth shadow
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RgbMagenta.copy(alpha = 0.65f),
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



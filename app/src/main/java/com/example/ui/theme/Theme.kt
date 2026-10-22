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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val DarkColorScheme = darkColorScheme(
    primary = LuminousCyan,
    onPrimary = Color(0xFF041020),
    primaryContainer = Color(0xFF122B4A),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = ElectricTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF152C4A),
    onSecondaryContainer = Color(0xFFD8F0FF),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = CrimsonError
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAFF),
    onPrimaryContainer = Color(0xFF003B8E),
    secondary = ElectricTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = CrimsonError
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
        LocalHaptics provides haptics
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

/**
 * Authentic iPhone iOS 18 / visionOS Ambient Mesh Background with soft glowing radial orbs
 * that smoothly animate between Light Glass and Dark Glass modes.
 */
@Composable
fun GlassScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkGlass.current

    val topBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF040814) else Color(0xFFD6E6FB),
        animationSpec = tween(320),
        label = "glassTopBg"
    )
    val midBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0A1428) else Color(0xFFEBF3FD),
        animationSpec = tween(320),
        label = "glassMidBg"
    )
    val bottomBg by animateColorAsState(
        targetValue = if (isDark) Color(0xFF060C1B) else Color(0xFFDCE8F8),
        animationSpec = tween(320),
        label = "glassBottomBg"
    )

    val baseGradient = Brush.verticalGradient(
        colors = listOf(topBg, midBg, bottomBg)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseGradient)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0A84FF).copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.15f, h * 0.12f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.15f, h * 0.12f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5E5CE6).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.88f, h * 0.42f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.88f, h * 0.42f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5AC8FA).copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.28f, h * 0.88f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.28f, h * 0.88f)
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF60A5FA).copy(alpha = 0.34f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.12f, h * 0.10f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.12f, h * 0.10f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.90f, h * 0.38f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.90f, h * 0.38f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.28f, h * 0.88f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.28f, h * 0.88f)
                    )
                }
            },
        content = content
    )
}

/**
 * Smooth iPhone iOS Frosted Glass Card with diagonal translucent refraction sheen,
 * specular top-left highlight border, and tactile haptic feedback on tap.
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
    val shape = RoundedCornerShape(cornerRadius)

    val glassFillBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
                Color(0xFF1E293B).copy(alpha = 0.50f),
                Color(0xFF0F172A).copy(alpha = 0.62f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.76f),
                Color.White.copy(alpha = 0.52f),
                Color(0xFFF0F7FF).copy(alpha = 0.64f)
            )
        )
    }

    val specularBorderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.36f),
                Color.White.copy(alpha = 0.10f),
                Color(0xFF38BDF8).copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.85f)
            )
        )
    }

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }

    val cardModifier = modifier
        .clip(shape)
        .background(brush = glassFillBrush, shape = shape)
        .border(width = 1.1.dp, brush = specularBorderBrush, shape = shape)
        .drawWithContent {
            drawContent()
            drawLine(
                color = topGlintColor,
                start = Offset(cornerRadius.toPx() * 0.6f, 1.dp.toPx()),
                end = Offset(size.width - cornerRadius.toPx() * 0.6f, 1.dp.toPx()),
                strokeWidth = 1.1.dp.toPx()
            )
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

    Box(modifier = cardModifier) {
        content()
    }
}

/**
 * Adaptive, strictly aligned iOS Frosted Glass Action Button with built-in tactile haptics.
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
    val shape = RoundedCornerShape(14.dp)

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
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        val fillBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.58f)
                )
            )
        }
        val borderBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.55f)
                )
            )
        }

        Box(
            modifier = modifier
                .height(height)
                .clip(shape)
                .background(fillBrush, shape)
                .border(1.dp, borderBrush, shape)
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        targetValue = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.76f),
        animationSpec = tween(250),
        label = "glassToggleBg"
    )
    val border by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.95f),
        animationSpec = tween(250),
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
 * Seamless iPhone iOS 18 / visionOS Liquid Glass Bottom Navigation Bar.
 * - Blends organically into both Light Mode and Dark Mode with feather-soft bottom ambient diffusion.
 * - Supports continuous swipe-linked liquid capsule indicator (`selectedProgress`) that glides
 *   smoothly as the user swipes left/right between Library, Scan, Tools, and Settings.
 * - Triggers crisp tactile vibration haptics on every tab press.
 */
@Composable
fun LiquidGlassBottomBar(
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier,
    selectedProgress: Float? = null
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val dockShape = RoundedCornerShape(32.dp)

    val activeIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)
    val targetProgress = (selectedProgress ?: activeIndex.toFloat())
        .coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat())

    val smoothProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidPillProgress"
    )

    // Animated translucent glass colors so switching Light <-> Dark mode melts seamlessly
    val glassStart by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1E293B).copy(alpha = 0.56f) else Color.White.copy(alpha = 0.64f),
        animationSpec = tween(300),
        label = "dockGlassStart"
    )
    val glassMid by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0F172A).copy(alpha = 0.64f) else Color(0xFFEFF6FF).copy(alpha = 0.54f),
        animationSpec = tween(300),
        label = "dockGlassMid"
    )
    val glassEnd by animateColorAsState(
        targetValue = if (isDark) Color(0xFF172554).copy(alpha = 0.52f) else Color.White.copy(alpha = 0.68f),
        animationSpec = tween(300),
        label = "dockGlassEnd"
    )

    val borderTop by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.94f),
        animationSpec = tween(300),
        label = "dockBorderTop"
    )
    val borderMid by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.22f) else Color(0xFF93C5FD).copy(alpha = 0.45f),
        animationSpec = tween(300),
        label = "dockBorderMid"
    )
    val borderBottom by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.72f),
        animationSpec = tween(300),
        label = "dockBorderBottom"
    )

    val pillFillTop by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0A84FF).copy(alpha = 0.34f) else Color.White.copy(alpha = 0.88f),
        animationSpec = tween(250),
        label = "pillFillTop"
    )
    val pillFillBottom by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.18f) else Color(0xFFDBEAFE).copy(alpha = 0.78f),
        animationSpec = tween(250),
        label = "pillFillBottom"
    )
    val pillStrokeColor by animateColorAsState(
        targetValue = if (isDark) LuminousCyan.copy(alpha = 0.58f) else RoyalBlue.copy(alpha = 0.28f),
        animationSpec = tween(250),
        label = "pillStroke"
    )

    val ambientScrimColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF050A16).copy(alpha = 0.52f) else Color(0xFFDCE8F8).copy(alpha = 0.55f),
        animationSpec = tween(300),
        label = "ambientScrim"
    )

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }

    // Outer container with soft bottom ambient diffusion veil so the floating glass dock
    // melts organically into both Light and Dark mode backgrounds without any seam.
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .clip(dockShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(glassStart, glassMid, glassEnd)
                    ),
                    shape = dockShape
                )
                .border(
                    width = 1.15.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(borderTop, borderMid, borderBottom)
                    ),
                    shape = dockShape
                )
                .drawWithContent {
                    val tabCount = tabs.size.coerceAtLeast(1)
                    val horizontalPad = 6.dp.toPx()
                    val verticalPad = 5.dp.toPx()
                    val availableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                    val slotWidth = availableWidth / tabCount
                    val pillLeft = horizontalPad + smoothProgress * slotWidth
                    val pillHeight = (size.height - verticalPad * 2f).coerceAtLeast(1f)
                    val pillCorner = CornerRadius(26.dp.toPx(), 26.dp.toPx())

                    // Continuous sliding Liquid Glass Active Pill
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(pillFillTop, pillFillBottom),
                            startY = verticalPad,
                            endY = verticalPad + pillHeight
                        ),
                        topLeft = Offset(pillLeft, verticalPad),
                        size = Size(slotWidth, pillHeight),
                        cornerRadius = pillCorner
                    )
                    drawRoundRect(
                        color = pillStrokeColor,
                        topLeft = Offset(pillLeft, verticalPad),
                        size = Size(slotWidth, pillHeight),
                        cornerRadius = pillCorner,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    drawContent()

                    // Specular top-edge liquid light refraction highlight
                    drawLine(
                        color = topGlintColor,
                        start = Offset(28.dp.toPx(), 1.1.dp.toPx()),
                        end = Offset(size.width - 28.dp.toPx(), 1.1.dp.toPx()),
                        strokeWidth = 1.25.dp.toPx()
                    )
                }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val distance = abs(smoothProgress - index.toFloat()).coerceIn(0f, 1f)
                val selectionFraction = 1f - distance

                val contentTint by animateColorAsState(
                    targetValue = when {
                        selectionFraction > 0.5f && isDark -> LuminousCyan
                        selectionFraction > 0.5f && !isDark -> RoyalBlue
                        isDark -> Color(0xFF94A3B8)
                        else -> Color(0xFF475569)
                    },
                    animationSpec = tween(180),
                    label = "navContentTint_$index"
                )

                val iconScale = 1f + (0.10f * selectionFraction)
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptics.click()
                                tab.onClick()
                            }
                        )
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentTint,
                            modifier = Modifier
                                .size(21.dp)
                                .scale(iconScale)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selectionFraction > 0.5f) FontWeight.Bold else FontWeight.Medium,
                            color = contentTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

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

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkGlass provides darkTheme) {
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
 * so translucent frosted glass cards visibly refract color and depth in both Light and Dark mode.
 */
@Composable
fun GlassScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val baseGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF040814),
                Color(0xFF0A1428),
                Color(0xFF050A17)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFD5E5FA),
                Color(0xFFEAF2FC),
                Color(0xFFDCE8F8)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseGradient)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (isDark) {
                    // Glowing iOS Blue top-left orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0A84FF).copy(alpha = 0.34f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.15f, h * 0.12f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.15f, h * 0.12f)
                    )
                    // Glowing Indigo right-middle orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5E5CE6).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.88f, h * 0.42f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.88f, h * 0.42f)
                    )
                    // Glowing Cyan bottom-left orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF5AC8FA).copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.25f, h * 0.85f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.25f, h * 0.85f)
                    )
                } else {
                    // Soft Sky Blue top-left orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF60A5FA).copy(alpha = 0.36f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.12f, h * 0.10f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.12f, h * 0.10f)
                    )
                    // Soft Periwinkle Violet right orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF818CF8).copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.90f, h * 0.38f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.90f, h * 0.38f)
                    )
                    // Soft Aqua bottom orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.25f, h * 0.85f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.25f, h * 0.85f)
                    )
                }
            },
        content = content
    )
}

/**
 * Smooth iPhone iOS Frosted Glass Card with diagonal translucent refraction sheen
 * and specular top-left highlight border.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val shape = RoundedCornerShape(cornerRadius)

    val glassFillBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color(0xFF1E293B).copy(alpha = 0.55f),
                Color(0xFF0F172A).copy(alpha = 0.68f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.54f),
                Color(0xFFF0F7FF).copy(alpha = 0.64f)
            )
        )
    }

    val specularBorderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.10f),
                Color(0xFF38BDF8).copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.98f),
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.85f)
            )
        )
    }

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }

    val cardModifier = modifier
        .clip(shape)
        .background(brush = glassFillBrush, shape = shape)
        .border(width = 1.2.dp, brush = specularBorderBrush, shape = shape)
        .drawWithContent {
            drawContent()
            // Subtle iPhone top-edge specular refraction line
            drawLine(
                color = topGlintColor,
                start = Offset(cornerRadius.toPx() * 0.6f, 1.dp.toPx()),
                end = Offset(size.width - cornerRadius.toPx() * 0.6f, 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
        }
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )

    Box(modifier = cardModifier) {
        content()
    }
}

/**
 * Adaptive, strictly aligned iOS Frosted Glass Action Button.
 * Enforces uniform height, compact padding, and single-line text so buttons never wrap or misalign on any screen size.
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
    val shape = RoundedCornerShape(14.dp)

    if (isPrimary) {
        Button(
            onClick = onClick,
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
                .clickable(onClick = onClick)
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
 * 1-Tap iPhone Glass Light / Dark Mode Switch Pill for headers & settings.
 */
@Composable
fun GlassThemeTogglePill(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.78f),
        animationSpec = tween(220),
        label = "glassToggleBg"
    )
    val border by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.32f) else Color.White,
        animationSpec = tween(220),
        label = "glassToggleBorder"
    )

    Surface(
        onClick = onToggle,
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
 * Floating iPhone iOS Liquid Glass Bottom Navigation Dock.
 * Features translucent frosted acrylic gradient, specular 1.2dp refraction border,
 * top edge highlight glint, and animated glowing pill capsules for the active tab.
 */
@Composable
fun LiquidGlassBottomBar(
    tabs: List<LiquidGlassNavTab>,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkGlass.current
    val dockShape = RoundedCornerShape(28.dp)

    val dockFillBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B).copy(alpha = 0.78f),
                Color(0xFF0F172A).copy(alpha = 0.88f),
                Color(0xFF091224).copy(alpha = 0.92f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.86f),
                Color(0xFFF0F7FF).copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.88f)
            )
        )
    }

    val dockBorderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                Color(0xFF38BDF8).copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                Color(0xFFBAE6FD).copy(alpha = 0.75f),
                Color.White.copy(alpha = 0.92f)
            )
        )
    }

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = 0.30f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dockShape)
                .background(brush = dockFillBrush, shape = dockShape)
                .border(width = 1.25.dp, brush = dockBorderBrush, shape = dockShape)
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = topGlintColor,
                        start = Offset(24.dp.toPx(), 1.2.dp.toPx()),
                        end = Offset(size.width - 24.dp.toPx(), 1.2.dp.toPx()),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val pillBg by animateColorAsState(
                    targetValue = when {
                        tab.selected && isDark -> Color(0xFF0A84FF).copy(alpha = 0.28f)
                        tab.selected && !isDark -> RoyalBlue.copy(alpha = 0.14f)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(220),
                    label = "navPillBg"
                )
                val pillBorder by animateColorAsState(
                    targetValue = when {
                        tab.selected && isDark -> LuminousCyan.copy(alpha = 0.55f)
                        tab.selected && !isDark -> RoyalBlue.copy(alpha = 0.35f)
                        else -> Color.Transparent
                    },
                    animationSpec = tween(220),
                    label = "navPillBorder"
                )
                val contentTint by animateColorAsState(
                    targetValue = when {
                        tab.selected && isDark -> LuminousCyan
                        tab.selected && !isDark -> RoyalBlue
                        isDark -> Color(0xFF94A3B8)
                        else -> Color(0xFF475569)
                    },
                    animationSpec = tween(220),
                    label = "navContentTint"
                )

                val itemShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(itemShape)
                        .background(pillBg, itemShape)
                        .border(1.dp, pillBorder, itemShape)
                        .clickable(onClick = tab.onClick)
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                        .then(Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.testTag(tab.testTag),
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
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (tab.selected) FontWeight.Bold else FontWeight.Medium,
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
}


package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    primary = Zinc50,
    onPrimary = Zinc900,
    primaryContainer = Zinc800,
    onPrimaryContainer = Zinc50,
    secondary = Zinc200,
    onSecondary = Zinc900,
    secondaryContainer = Zinc900,
    onSecondaryContainer = Zinc100,
    tertiary = Zinc300,
    onTertiary = Zinc900,
    tertiaryContainer = Zinc800,
    onTertiaryContainer = Zinc100,
    background = DarkBackground,
    onBackground = Zinc50,
    surface = DarkSurface,
    onSurface = Zinc50,
    surfaceVariant = Zinc900,
    onSurfaceVariant = Zinc400,
    surfaceContainerLowest = Zinc950,
    surfaceContainerLow = Color(0xFF0C0C0F),
    surfaceContainer = Zinc900,
    surfaceContainerHigh = Color(0xFF1F1F23),
    surfaceContainerHighest = Zinc800,
    outline = Zinc700,
    outlineVariant = Zinc800,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Zinc900,
    onPrimary = Zinc50,
    primaryContainer = Zinc100,
    onPrimaryContainer = Zinc900,
    secondary = Zinc800,
    onSecondary = Zinc50,
    secondaryContainer = Zinc100,
    onSecondaryContainer = Zinc900,
    tertiary = Zinc700,
    onTertiary = Zinc50,
    tertiaryContainer = Zinc100,
    onTertiaryContainer = Zinc900,
    background = LightBackground,
    onBackground = Zinc950,
    surface = LightSurface,
    onSurface = Zinc950,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Zinc100,
    surfaceContainerHigh = Zinc200,
    surfaceContainerHighest = Zinc300,
    outline = Zinc300,
    outlineVariant = Zinc200,
    error = CrimsonError,
    onError = Color.White
)

// Crisp shadcn/ui geometric radii (8dp - 12dp)
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
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
 * Minimalist Geometric Brand Mark (shadcn/ui Scanner Reticle + Folded Document Frame).
 */
@Composable
fun ShadcnBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val isDark = LocalIsDarkGlass.current
    val containerColor = if (isDark) Zinc50 else Zinc900
    val glyphColor = if (isDark) Zinc900 else Zinc50
    val borderColor = if (isDark) Zinc700 else Zinc300

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val w = this.size.width
            val h = this.size.height
            val stroke = Stroke(
                width = (w * 0.105f).coerceAtLeast(1.6f),
                cap = StrokeCap.Square,
                join = StrokeJoin.Miter
            )
            val cornerLen = w * 0.25f

            // 4 corner scanner reticles
            // Top-Left
            drawLine(glyphColor, Offset(0f, cornerLen), Offset(0f, 0f), strokeWidth = stroke.width)
            drawLine(glyphColor, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = stroke.width)
            // Top-Right
            drawLine(glyphColor, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeWidth = stroke.width)
            drawLine(glyphColor, Offset(w, 0f), Offset(w, cornerLen), strokeWidth = stroke.width)
            // Bottom-Left
            drawLine(glyphColor, Offset(0f, h - cornerLen), Offset(0f, h), strokeWidth = stroke.width)
            drawLine(glyphColor, Offset(0f, h), Offset(cornerLen, h), strokeWidth = stroke.width)
            // Bottom-Right
            drawLine(glyphColor, Offset(w - cornerLen, h), Offset(w, h), strokeWidth = stroke.width)
            drawLine(glyphColor, Offset(w, h - cornerLen), Offset(w, h), strokeWidth = stroke.width)

            // Inner minimalist folded document silhouette
            val docLeft = w * 0.24f
            val docTop = h * 0.22f
            val docRight = w * 0.76f
            val docBottom = h * 0.78f
            val fold = w * 0.16f

            val docPath = Path().apply {
                moveTo(docLeft, docTop)
                lineTo(docRight - fold, docTop)
                lineTo(docRight, docTop + fold)
                lineTo(docRight, docBottom)
                lineTo(docLeft, docBottom)
                close()
            }
            drawPath(path = docPath, color = glyphColor, style = stroke)

            // Precision horizontal scan beam across the center
            drawLine(
                color = glyphColor,
                start = Offset(w * 0.10f, h * 0.52f),
                end = Offset(w * 0.90f, h * 0.52f),
                strokeWidth = stroke.width * 1.15f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Minimalist shadcn/ui Zinc Background Surface with subtle architectural top grid hairline.
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
    val bgColor by animateColorAsState(
        targetValue = if (isDark) Zinc950 else Color.White,
        animationSpec = tween(200),
        label = "shadcnBgColor"
    )
    val subtleTopWash = if (isDark) Zinc900.copy(alpha = 0.45f) else Zinc100.copy(alpha = 0.65f)

    CompositionLocalProvider(
        LocalHasRootGlassBackground provides true,
        LocalContentColor provides contentColor
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(subtleTopWash, Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.28f
                        )
                    )
                },
            content = content
        )
    }
}

/**
 * Crisp shadcn/ui Card with 1dp Zinc border (#E4E4E7 in Light, #27272A in Dark) and clean surface.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val contentColor = if (isDark) Zinc50 else Zinc950
    val effectiveRadius = cornerRadius.coerceAtMost(14.dp)
    val shape = remember(effectiveRadius) { RoundedCornerShape(effectiveRadius) }
    val fillColor = if (isDark) Color(0xFF0F0F12) else Color.White
    val borderColor = if (isDark) Zinc800 else Zinc200

    val cardModifier = modifier
        .clip(shape)
        .background(fillColor, shape)
        .border(BorderStroke(1.dp, borderColor), shape)
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
 * Minimalist Monochrome shadcn/ui Button (Primary solid Zinc-900 / Zinc-50 or Secondary 1dp Zinc Outline).
 */
@Composable
fun GlassActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    height: Dp = 42.dp
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val shape = remember { RoundedCornerShape(8.dp) }

    if (isPrimary) {
        val containerColor = if (isDark) Zinc50 else Zinc900
        val contentColor = if (isDark) Zinc900 else Zinc50
        Button(
            onClick = {
                haptics.click()
                onClick()
            },
            shape = shape,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            modifier = modifier.height(height)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        val bgColor = if (isDark) Zinc900 else Color.White
        val borderColor = if (isDark) Zinc800 else Zinc200
        val labelColor = if (isDark) Zinc100 else Zinc900
        val iconColor = if (isDark) Zinc300 else Zinc700

        Box(
            modifier = modifier
                .height(height)
                .clip(shape)
                .background(bgColor, shape)
                .border(BorderStroke(1.dp, borderColor), shape)
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
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
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
 * Minimalist Monochrome shadcn/ui Center Camera Button for the Navigation Dock.
 */
@Composable
fun Glowing3dRgbCameraOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    icon: ImageVector = Icons.Default.CameraAlt,
    contentDescription: String = "Camera Scan"
) {
    val isDark = LocalIsDarkGlass.current
    val haptics = LocalHaptics.current
    val containerColor = if (isDark) Zinc50 else Zinc900
    val contentColor = if (isDark) Zinc900 else Zinc50
    val ringBorderColor = if (isDark) Zinc700 else Zinc300

    Surface(
        onClick = {
            haptics.heavy()
            onClick()
        },
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, ringBorderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Minimalist Monochrome shadcn/ui Flagship Action Button (replaces RGB button with crisp high-contrast Zinc primary style).
 */
@Composable
fun DynamicRgbActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.EditNote,
    badgeText: String? = null,
    height: Dp = 42.dp,
    testTag: String = "dynamic_rgb_action_button"
) {
    val haptics = LocalHaptics.current
    val isDark = LocalIsDarkGlass.current
    val isCompact = height <= 36.dp
    val shape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)

    val containerColor = if (isDark) Zinc50 else Zinc900
    val contentColor = if (isDark) Zinc900 else Zinc50
    val borderColor = if (isDark) Zinc300 else Zinc800
    val badgeBg = if (isDark) Zinc200 else Zinc800
    val badgeFg = if (isDark) Zinc900 else Zinc100

    Surface(
        onClick = {
            haptics.click()
            onClick()
        },
        modifier = modifier
            .height(height)
            .testTag(testTag),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 10.dp else 12.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                )
                Spacer(modifier = Modifier.width(if (isCompact) 5.dp else 6.dp))
            }
            Text(
                text = text,
                style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (!badgeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact Minimalist Monochrome shadcn/ui Action Chip for toolbars & document cards.
 */
@Composable
fun DynamicRgbChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.EditNote,
    badgeText: String? = null,
    height: Dp = 32.dp,
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
 * Minimalist Monochrome shadcn/ui Feature Card with crisp 1dp Zinc border and subtle header accent.
 */
@Composable
fun DynamicRgbGlowCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    val isDark = LocalIsDarkGlass.current
    val effectiveRadius = cornerRadius.coerceAtMost(14.dp)
    val shape = RoundedCornerShape(effectiveRadius)
    val bgColor = if (isDark) Zinc900.copy(alpha = 0.75f) else Zinc50
    val borderColor = if (isDark) Zinc700 else Zinc300
    val contentColor = if (isDark) Zinc50 else Zinc950

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(bgColor, shape)
                .border(BorderStroke(1.dp, borderColor), shape)
        ) {
            content()
        }
    }
}

/**
 * Minimalist shadcn/ui 1-Tap Light / Dark Mode Switch Pill.
 */
@Composable
fun GlassThemeTogglePill(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val bg by animateColorAsState(
        targetValue = if (isDark) Zinc900 else Zinc100,
        animationSpec = tween(180),
        label = "shadcnToggleBg"
    )
    val border by animateColorAsState(
        targetValue = if (isDark) Zinc800 else Zinc200,
        animationSpec = tween(180),
        label = "shadcnToggleBorder"
    )
    val fg = if (isDark) Zinc100 else Zinc900

    Surface(
        onClick = {
            haptics.click()
            onToggle()
        },
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "Toggle Dark or Light Mode",
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (isDark) "Dark" else "Light",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = fg
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
 * Minimalist shadcn/ui Bottom Navigation Dock with 1dp Zinc border, sliding monochrome active indicator,
 * and centered minimal monochrome Camera trigger.
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
    val dockShape = remember { RoundedCornerShape(14.dp) }

    val activeIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)
    val fallbackAnimatedProgress by animateFloatAsState(
        targetValue = (selectedProgress ?: activeIndex.toFloat())
            .coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat()),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shadcnDockProgress"
    )

    val dockBgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0C0C0F) else Color.White,
        animationSpec = tween(200),
        label = "dockBg"
    )
    val dockBorderColor by animateColorAsState(
        targetValue = if (isDark) Zinc800 else Zinc200,
        animationSpec = tween(200),
        label = "dockBorder"
    )
    val pillFillColor by animateColorAsState(
        targetValue = if (isDark) Zinc800 else Zinc100,
        animationSpec = tween(200),
        label = "pillFill"
    )
    val pillStrokeColor by animateColorAsState(
        targetValue = if (isDark) Zinc700 else Zinc300,
        animationSpec = tween(200),
        label = "pillStroke"
    )

    val maxTabIndex = (tabs.size - 1).coerceAtLeast(0).toFloat()
    val hasCenterOrb = onCenterCameraClick != null && tabs.size == 4

    Box(
        modifier = modifier
            .fillMaxWidth()
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
                    val dockCorner = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    val borderStrokePx = 1.dp.toPx()
                    val tabCount = tabs.size.coerceAtLeast(1)
                    val horizontalPad = 5.dp.toPx()
                    val verticalPad = 5.dp.toPx()
                    val centerSlotW = if (hasCenterOrb) 58.dp.toPx() else 0f
                    val availableWidth = (size.width - horizontalPad * 2f - centerSlotW).coerceAtLeast(1f)
                    val slotWidth = availableWidth / tabCount
                    val pillHeight = (size.height - verticalPad * 2f).coerceAtLeast(1f)
                    val pillCorner = CornerRadius(10.dp.toPx(), 10.dp.toPx())

                    onDrawWithContent {
                        drawRoundRect(color = dockBgColor, cornerRadius = dockCorner)
                        drawRoundRect(
                            color = dockBorderColor,
                            cornerRadius = dockCorner,
                            style = Stroke(width = borderStrokePx)
                        )

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

                        drawRoundRect(
                            color = pillFillColor,
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
                    }
                }
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                if (hasCenterOrb && index == 2) {
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Glowing3dRgbCameraOrb(
                            onClick = { onCenterCameraClick?.invoke() },
                            size = 46.dp,
                            iconSize = 21.dp,
                            contentDescription = "Camera Scan"
                        )
                    }
                }

                val activeColor = if (isDark) Zinc50 else Zinc950
                val inactiveColor = if (isDark) Zinc500 else Zinc500
                val tintColor = if (tab.selected) activeColor else inactiveColor
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
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
                            val scale = 1f + (0.04f * selectionFraction)
                            scaleX = scale
                            scaleY = scale
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tintColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (tab.selected) FontWeight.SemiBold else FontWeight.Medium,
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

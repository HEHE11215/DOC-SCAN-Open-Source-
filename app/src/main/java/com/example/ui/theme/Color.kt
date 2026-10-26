package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// shadcn/ui Zinc Neutral Scale
val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

// Semantic Tokens mapped to Minimalist Monochrome Zinc shadcn/ui
val DeepNavy = Zinc950
val SlateNavy = Zinc900
val ElectricTeal = Zinc900       // Primary ink in Light / mapped via ColorScheme in Dark
val LuminousCyan = Zinc400       // Subtle monochrome highlight
val RoyalBlue = Zinc900          // Primary Zinc-900 action surface
val IndigoAccent = Zinc700       // Secondary monochrome accent
val EmeraldSuccess = Color(0xFF10B981)
val AmberWarning = Color(0xFFF59E0B)
val CrimsonError = Color(0xFFEF4444)

// Legacy accent references mapped to refined monochrome Zinc tones
val RgbCyan = Zinc300
val RgbBlue = Zinc500
val RgbViolet = Zinc400
val RgbMagenta = Zinc600
val RgbOrange = Zinc400
val RgbEmerald = Zinc300

// Light shadcn/ui Zinc Surfaces (--background: #FFFFFF, --muted: #F4F4F5, --border: #E4E4E7)
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Zinc100
val LightOnSurface = Zinc950
val LightOnSurfaceVariant = Zinc500
val LightBorder = Zinc200

// Dark shadcn/ui Zinc Surfaces (--background: #09090B, --muted: #18181B, --border: #27272A)
val DarkBackground = Zinc950
val DarkSurface = Color(0xFF0F0F12)
val DarkSurfaceVariant = Zinc900
val DarkOnSurface = Zinc50
val DarkOnSurfaceVariant = Zinc400
val DarkBorder = Zinc800

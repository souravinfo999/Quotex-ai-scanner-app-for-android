package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Professional Polish - Quotex AI Pro Cybernetic Palette
val BackgroundDark = Color(0xFF07080D)
val SurfaceDark = Color(0xFF0F111A)
val SurfaceCard = Color(0xFF161926)
val SurfaceElevated = Color(0xFF1D2234)
val SurfacePill = Color(0xFF23283E)
val SurfaceCardTranslucent = Color(0xD9161926)
val SurfaceBorder = Color(0x24FFFFFF) // crisper border
val SurfaceBorderSubtle = Color(0x12FFFFFF)
val SurfaceBorderGlow = Color(0x4000E5B9)

// Trading Brand Colors (Enhanced Neon & Dynamic Vibrancy)
val PrimaryTeal = Color(0xFF00E5B9)
val PrimaryTealVariant = Color(0xFF00BFA0)
val PrimaryTealAlpha20 = Color(0x2900E5B9)
val PrimaryTealAlpha10 = Color(0x1A00E5B9)

// Institutional Action Colors
val BullishGreen = Color(0xFF00F576)
val BullishGreenDark = Color(0xFF00BD56)
val BullishGreenGlow = Color(0x3300F576)

// Electric Green Transparent Button Palette
val ElectricGreen = Color(0xFF00F576)
val ElectricGreenTransparent = Color(0x2E00F576) // ~18% transparent electric green container
val ElectricGreenTransparentHover = Color(0x4000F576) // ~25% alpha
val ElectricGreenBorder = Color(0x8000F576) // ~50% neon border
val ElectricGreenGlow = Color(0x4D00F576)

val BearishRed = Color(0xFFFF3838)
val BearishRedDark = Color(0xFFD31818)
val BearishGradientEnd = Color(0xFFFF6961)
val BearishRedGlow = Color(0x33FF3838)

val AccentPurple = Color(0xFF865DFF)
val AccentPurpleLight = Color(0xFFA58AFF)
val AccentPurpleDark = Color(0xFF6537E8)
val AccentPurpleGlow = Color(0x33865DFF)

val UncertainYellow = Color(0xFFFFC01E)
val UncertainAmber = Color(0xFFFF9900)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9EA6BE)
val TextTertiary = Color(0xFF67708A)
val TextMuted = Color(0x80FFFFFF)

// Gradient Brushes
val CyberTealGradient = Brush.horizontalGradient(listOf(Color(0xFF00E5B9), Color(0xFF0099FF)))
val BullishCardGradient = Brush.verticalGradient(listOf(Color(0x2400F576), SurfaceCard))
val BearishCardGradient = Brush.verticalGradient(listOf(Color(0x24FF3838), SurfaceCard))
val NeutralCardGradient = Brush.verticalGradient(listOf(Color(0xFF1B2030), SurfaceCard))
val HeroCardGradient = Brush.linearGradient(listOf(Color(0xFF181D2E), Color(0xFF121522)))

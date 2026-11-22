package com.inyogeshwar.metaclean.core.ui.theme

import androidx.compose.ui.graphics.Color

// iOS Dark Cupertino Palette (Clean, lightweight, modern)
val iOSBackgroundDark = Color(0xFF000000) // Pure OLED pitch black
val iOSCardDark = Color(0xFF1C1C1E)       // iOS Secondary Grouped Background
val iOSCardElevated = Color(0xFF2C2C2E)   // iOS Tertiary Grouped Background
val iOSSeparator = Color(0xFF38383A)      // iOS Hairline Separator

// iOS Vibrant System Accents
val iOSSystemBlue = Color(0xFF0A84FF)     // Electric iOS Blue
val iOSSystemIndigo = Color(0xFF5E5CE6)   // iOS Purple/Indigo
val iOSSystemTeal = Color(0xFF64D2FF)     // iOS Cyan/Teal
val iOSSystemGreen = Color(0xFF30D158)    // iOS Vibrant Green
val iOSSystemOrange = Color(0xFFFF9F0A)   // iOS Amber/Orange
val iOSSystemRed = Color(0xFFFF453A)      // iOS Coral Red

val iOSTextPrimary = Color(0xFFFFFFFF)
val iOSTextSecondary = Color(0xFF8E8E93)  // iOS Subheadline Gray
val iOSTextTertiary = Color(0xFF636366)

// iOS Light Cupertino Palette
val iOSBackgroundLight = Color(0xFFF2F2F7) // iOS Grouped Background Light
val iOSCardLight = Color(0xFFFFFFFF)       // White card
val iOSCardElevatedLight = Color(0xFFE5E5EA)
val iOSSeparatorLight = Color(0xFFC6C6C8)

val iOSTextPrimaryLight = Color(0xFF000000)
val iOSTextSecondaryLight = Color(0xFF8E8E93)
val iOSTextTertiaryLight = Color(0xFFAEAEB2)

// Legacy aliases for compatibility
val AccentPrimary = iOSSystemBlue
val AccentSecondary = iOSSystemTeal
val AccentTertiary = iOSSystemIndigo
val SuccessGreen = iOSSystemGreen
val DangerRed = iOSSystemRed
val WarningAmber = iOSSystemOrange
val TextPrimaryDark = iOSTextPrimary
val TextSecondaryDark = iOSTextSecondary
val SurfaceDark = iOSCardDark
val SurfaceVariantDark = iOSCardDark
val SurfaceContainerDark = iOSCardElevated
val SurfaceBorderDark = iOSSeparator
val BackgroundDark = iOSBackgroundDark

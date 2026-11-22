package com.inyogeshwar.metaclean.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val iOSDarkColorScheme = darkColorScheme(
    primary = iOSSystemBlue,
    onPrimary = iOSTextPrimary,
    primaryContainer = iOSSystemIndigo,
    onPrimaryContainer = iOSTextPrimary,
    secondary = iOSSystemTeal,
    onSecondary = iOSBackgroundDark,
    background = iOSBackgroundDark,
    onBackground = iOSTextPrimary,
    surface = iOSBackgroundDark,
    onSurface = iOSTextPrimary,
    surfaceVariant = iOSCardDark,
    onSurfaceVariant = iOSTextSecondary,
    outline = iOSSeparator,
    error = iOSSystemRed,
    onError = iOSTextPrimary
)

private val iOSLightColorScheme = lightColorScheme(
    primary = iOSSystemBlue,
    onPrimary = iOSCardLight,
    primaryContainer = iOSSystemTeal,
    onPrimaryContainer = iOSTextPrimaryLight,
    secondary = iOSSystemIndigo,
    onSecondary = iOSCardLight,
    background = iOSBackgroundLight,
    onBackground = iOSTextPrimaryLight,
    surface = iOSBackgroundLight,
    onSurface = iOSTextPrimaryLight,
    surfaceVariant = iOSCardLight,
    onSurfaceVariant = iOSTextSecondaryLight,
    outline = iOSSeparatorLight,
    error = iOSSystemRed,
    onError = iOSCardLight
)

@Composable
fun MetaCleanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) iOSDarkColorScheme else iOSLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

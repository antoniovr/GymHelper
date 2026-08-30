package com.tibarra.gymhelper.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BluePastel,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    secondary = GrayText,
    error = ErrorRed,
)

private val LightColorScheme = lightColorScheme(
    primary = BlueStrong,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = DarkText,
    surface = LightSurface,
    onSurface = DarkText,
    secondary = GrayTextLight,
    error = Color.Red
)

@Composable
fun GymHelperTheme(
    themeMode: Int = 0, // 0: System, 1: Light, 2: Dark
    accentColorIndex: Int = 0, // 0: Blue, 1: Green, 2: Purple
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val primaryColor = if (darkTheme) {
        when (accentColorIndex) {
            1 -> GreenPastel
            2 -> PurplePastel
            3 -> OrangePastel
            4 -> PinkPastel
            else -> BluePastel
        }
    } else {
        when (accentColorIndex) {
            1 -> GreenStrong
            2 -> PurpleStrong
            3 -> OrangeStrong
            4 -> PinkStrong
            else -> BlueStrong
        }
    }
    
    val colorScheme = if (darkTheme) {
        val tintedSurface = Color(
            red = (DarkSurface.red * 0.95f) + (primaryColor.red * 0.05f),
            green = (DarkSurface.green * 0.95f) + (primaryColor.green * 0.05f),
            blue = (DarkSurface.blue * 0.95f) + (primaryColor.blue * 0.05f)
        )
        DarkColorScheme.copy(
            primary = primaryColor,
            secondaryContainer = primaryColor.copy(alpha = 0.3f),
            onSecondaryContainer = Color.White,
            surface = tintedSurface
        )
    } else {
        val tintedSurface = Color(
            red = (LightSurface.red * 0.97f) + (primaryColor.red * 0.03f),
            green = (LightSurface.green * 0.97f) + (primaryColor.green * 0.03f),
            blue = (LightSurface.blue * 0.97f) + (primaryColor.blue * 0.03f)
        )
        LightColorScheme.copy(
            primary = primaryColor,
            secondaryContainer = primaryColor.copy(alpha = 0.15f),
            onSecondaryContainer = primaryColor,
            surface = tintedSurface,
            background = LightBackground
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

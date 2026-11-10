package com.example.cameratest.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = CameraBlue,
    surface = CameraSurface,
    background = CameraSurface,
)

private val LightColors = lightColorScheme(
    primary = CameraBlueDark,
    surface = CameraSurfaceLight,
    background = CameraSurfaceLight,
)

/**
 * Material 3 theme for the app.
 *
 * On Android 12+ we opt into *dynamic color*: the color scheme is derived from the
 * user's wallpaper. It is a runtime API, hence the version check and the fallback
 * to our own scheme on older devices.
 */
@Composable
fun CameraTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CameraTypography,
        content = content,
    )
}

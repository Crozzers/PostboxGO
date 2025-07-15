package com.crozzers.postboxgo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // stuff for top bar
    surface = StandardPostboxCap,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,

    // stuff for bottom bar
    surfaceContainer = PostboxBase,

    // main app body
    background = StandardPostboxBody,
    onBackground = Color.White,

    // buttons and elements
    primary = PostboxBase,
    onPrimary = Color.White,
    outline = Color.White,

    // cards
    surfaceVariant = Color.Black,
    surfaceContainerHighest = PostboxInfoCard,

    // errors
    error = Color.White
)

private val DarkColorScheme = darkColorScheme(
    // stuff for top bar
    surface = BlackPostboxCap,
    onSurface = Color.White,
    onSurfaceVariant = Color.White,

    // stuff for bottom bar
    surfaceContainer = PostboxBase,

    // main app body
    background = BlackPostboxBody,
    onBackground = BlackPostboxCap,

    // buttons and elements
    primary = BlackPostboxCap,
    onPrimary = Color.White,
    outline = BlackPostboxCap,

    // cards
    surfaceVariant = Color.Black,
    surfaceContainerHighest = PostboxInfoCard,

    // errors
    error = Color.White
)

@Composable
fun PostboxGOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
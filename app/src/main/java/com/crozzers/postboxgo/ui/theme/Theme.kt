package com.crozzers.postboxgo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.crozzers.postboxgo.Setting
import com.crozzers.postboxgo.settings
import kotlinx.coroutines.flow.map

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
    surfaceContainerHigh = Color.DarkGray,

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
    surfaceContainerHigh = Color.DarkGray,

    // errors
    error = Color.White
)

enum class ColourSchemes {
    Standard, Dark, System
}

@Composable
fun PostboxGOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val settings = LocalContext.current.settings

    val colourScheme = settings.data.map { preferences ->
        val scheme = preferences[Setting.COLOUR_SCHEME]
        when (scheme) {
            ColourSchemes.System.name -> if (darkTheme) DarkColorScheme else LightColorScheme
            ColourSchemes.Dark.name -> DarkColorScheme
            else -> LightColorScheme
        }
    }.collectAsState(initial = LightColorScheme)

    MaterialTheme(
        colorScheme = colourScheme.value,
        typography = Typography,
        content = content
    )
}
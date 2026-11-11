package com.example.garmento.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = IndigoDye,
    onPrimary = Color_White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = IndigoDeep,
    secondary = Slate80,
    onSecondary = Color_White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateDeep,
    tertiary = Bronze,
    onTertiary = Color_White,
    tertiaryContainer = BronzeContainer,
    onTertiaryContainer = BronzeDeep,
    background = Color_White,
    onBackground = Charcoal,
    surface = Color_White,
    onSurface = Charcoal,
    surfaceVariant = Color_White,
    onSurfaceVariant = CharcoalMuted,
    outline = CharcoalFaint,
    outlineVariant = LinenDeep,
    error = ErrorRed,
    onError = Color_White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = NightIndigo,
    onPrimary = NightIndigoOn,
    primaryContainer = NightIndigoContainer,
    onPrimaryContainer = IndigoContainer,
    secondary = NightSlate,
    onSecondary = NightSlateOn,
    secondaryContainer = NightSlateContainer,
    onSecondaryContainer = SlateContainer,
    tertiary = NightBronze,
    onTertiary = NightBronzeOn,
    tertiaryContainer = NightBronzeContainer,
    onTertiaryContainer = BronzeContainer,
    background = Color_White,
    onBackground = Charcoal,
    surface = Color_White,
    onSurface = Charcoal,
    surfaceVariant = Color_White,
    onSurfaceVariant = InkMuted,
    outline = InkFaint,
    outlineVariant = NightDeep,
    error = NightError,
    onError = NightErrorOn,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer
)

private val GarmentoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(10.dp)
)

@Composable
fun GarmentoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    darkModeEnabled: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = darkModeEnabled ?: darkTheme

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GarmentoShapes,
        content = content
    )
}
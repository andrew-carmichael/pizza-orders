package com.andrewcarmichael.pizzaorders.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A80),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFB71C1C),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFFFFD54F),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF8C6D1F),
    onSecondaryContainer = Color.White,

    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */

    primary = Color(0xFFE53935),        // Vibrant red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2), // Light red background
    onPrimaryContainer = Color(0xFFB71C1C),

    secondary = Color(0xFFFFB300),      // Warm yellow
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE082),
    onSecondaryContainer = Color(0xFF8C6D1F),

    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
)

@Composable
fun PizzaOrdersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
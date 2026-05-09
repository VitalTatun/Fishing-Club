package com.example.fishing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Класс для ваших кастомных цветов
@Immutable
data class FishingCustomColors(
    val trophyYellow: Color = Color.Unspecified,
    val bookmarkRed: Color = Color.Unspecified,
    val textOnTrophy: Color = Color.Unspecified,
    val secondaryBackground: Color = Color.Unspecified
)

// 2. Значения для светлой темы
val LightCustomColors = FishingCustomColors(
    trophyYellow = Color(0xFFFFD71D),
    bookmarkRed = Color(0xFFFF3E00),
    textOnTrophy = Color(0xFF50250A),
    secondaryBackground = Color(0xFFF2F3F4)
)

// 3. Значения для темной темы
val DarkCustomColors = FishingCustomColors(
    trophyYellow = Color(0xFFFFE04D),
    bookmarkRed = Color(0xFFFF521A),
    textOnTrophy = Color(0xFF331706),
    secondaryBackground = Color(0xFF1E1F25) // Темный вариант для блоков
)

// 4. Local для доступа
val LocalFishingCustomColors = staticCompositionLocalOf { FishingCustomColors() }

// Объект-помощник
object FishingTheme {
    val colors: FishingCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFishingCustomColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3E5481),
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF7F9FA),
    surface = Color(0xFFF7F9FA),
    surfaceContainer = Color(0xFFF2F3F4)
)

@Composable
fun FishingTheme(
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

    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFishingCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

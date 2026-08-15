package com.rzk.kasirpro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Generously rounded — large radii are what make a dense grid of tappable tiles read as
 * soft cards rather than a spreadsheet.
 */
val KasirShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

private val LocalKasirColors = staticCompositionLocalOf { LightSemanticColors }

/** `MaterialTheme.kasirColors.cashIn` — the semantic palette Material 3 doesn't define. */
val MaterialTheme.kasirColors: KasirSemanticColors
    @Composable @ReadOnlyComposable get() = LocalKasirColors.current

@Composable
fun KasirTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Material You wallpaper colours. Off by default: a POS is a branded tool that staff
     * learn by colour, and having it change with the owner's wallpaper hurts more than it
     * delights. Flip to true for a system-integrated look.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalKasirColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KasirTypography,
            shapes = KasirShapes,
            content = content
        )
    }
}

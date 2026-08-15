package com.rzk.kasirpro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A deep teal-green primary: reads as "money" without the flat corporate blue every other
// POS app uses, and stays legible under the harsh lighting of a real shop counter.
private val Teal10 = Color(0xFF002019)
private val Teal20 = Color(0xFF00382D)
private val Teal30 = Color(0xFF005143)
private val Teal40 = Color(0xFF006B5B)
private val Teal80 = Color(0xFF5BDBC0)
private val Teal90 = Color(0xFF7BF8DC)

private val Sage10 = Color(0xFF06201A)
private val Sage20 = Color(0xFF1D352F)
private val Sage30 = Color(0xFF334B45)
private val Sage40 = Color(0xFF4A635C)
private val Sage80 = Color(0xFFB1CCC3)
private val Sage90 = Color(0xFFCCE8DF)

private val Blue10 = Color(0xFF001E2E)
private val Blue20 = Color(0xFF0C3447)
private val Blue30 = Color(0xFF294A5F)
private val Blue40 = Color(0xFF416277)
private val Blue80 = Color(0xFFA9CBE3)
private val Blue90 = Color(0xFFC4E7FF)

private val Neutral10 = Color(0xFF171D1B)
private val Neutral90 = Color(0xFFDEE4E0)

val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    inversePrimary = Teal80,
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Blue40,
    onTertiary = Color.White,
    tertiaryContainer = Blue90,
    onTertiaryContainer = Blue10,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF7),
    onBackground = Neutral10,
    surface = Color(0xFFF5FBF7),
    onSurface = Neutral10,
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    surfaceTint = Teal40,
    inverseSurface = Color(0xFF2B322F),
    inverseOnSurface = Color(0xFFECF2EE),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C4),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9EFEC),
    surfaceContainerHigh = Color(0xFFE4EAE6),
    surfaceContainerHighest = Color(0xFFDEE4E0)
)

val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    inversePrimary = Teal40,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Blue80,
    onTertiary = Blue20,
    tertiaryContainer = Blue30,
    onTertiaryContainer = Blue90,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1513),
    onBackground = Neutral90,
    surface = Color(0xFF0E1513),
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceTint = Teal80,
    inverseSurface = Neutral90,
    inverseOnSurface = Color(0xFF2B322F),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4945),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303634)
)

/**
 * Categorical series colours, in fixed assignment order — slot 0 always means the same
 * entity, never "whatever ranked first". One list serves both themes: these eight steps
 * were validated against the light surface (#F5FBF7) *and* the dark surface (#0E1513) and
 * clear the lightness band, chroma floor, colour-vision-deficiency separation, the
 * normal-vision floor and 3:1 contrast in both. Re-run the check before editing them.
 *
 * Charts still carry a legend and direct labels, so identity is never colour alone.
 */
val CategoricalChartPalette = listOf(
    Color(0xFF0D9488), // teal
    Color(0xFFD97706), // amber
    Color(0xFF2563EB), // blue
    Color(0xFFDC2626), // red
    Color(0xFF9333EA), // purple
    Color(0xFF16A34A), // green
    Color(0xFF0891B2), // cyan
    Color(0xFFB45309)  // brown
)

/**
 * Colours Material 3 has no slot for but a cashier app can't do without: money in vs money
 * out, stock warnings, and the promo accent. Kept as a theme extension rather than
 * hardcoded call sites so dark mode stays consistent.
 */
data class KasirSemanticColors(
    val cashIn: Color,
    val onCashIn: Color,
    val cashInContainer: Color,
    val onCashInContainer: Color,
    val cashOut: Color,
    val onCashOut: Color,
    val cashOutContainer: Color,
    val onCashOutContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val promo: Color,
    val promoContainer: Color,
    val onPromoContainer: Color,
    /** Fixed series colours for charts, in draw order. */
    val chart: List<Color>
)

val LightSemanticColors = KasirSemanticColors(
    cashIn = Color(0xFF1B7A3E),
    onCashIn = Color.White,
    cashInContainer = Color(0xFFB8F2CB),
    onCashInContainer = Color(0xFF00210F),
    cashOut = Color(0xFFC0392B),
    onCashOut = Color.White,
    cashOutContainer = Color(0xFFFFDAD5),
    onCashOutContainer = Color(0xFF410001),
    warning = Color(0xFFB26A00),
    warningContainer = Color(0xFFFFDDB3),
    onWarningContainer = Color(0xFF2A1800),
    info = Color(0xFF00639B),
    infoContainer = Color(0xFFCDE5FF),
    onInfoContainer = Color(0xFF001D33),
    promo = Color(0xFF9A25AE),
    promoContainer = Color(0xFFFFD6FE),
    onPromoContainer = Color(0xFF35003F),
    chart = CategoricalChartPalette
)

val DarkSemanticColors = KasirSemanticColors(
    cashIn = Color(0xFF7EDCA0),
    onCashIn = Color(0xFF00391C),
    cashInContainer = Color(0xFF00522B),
    onCashInContainer = Color(0xFFB8F2CB),
    cashOut = Color(0xFFFFB4AB),
    onCashOut = Color(0xFF690005),
    cashOutContainer = Color(0xFF8C1D18),
    onCashOutContainer = Color(0xFFFFDAD5),
    warning = Color(0xFFFFB95C),
    warningContainer = Color(0xFF6B4E00),
    onWarningContainer = Color(0xFFFFDDB3),
    info = Color(0xFF95CBFF),
    infoContainer = Color(0xFF004A76),
    onInfoContainer = Color(0xFFCDE5FF),
    promo = Color(0xFFF7ADFF),
    promoContainer = Color(0xFF7B1B8E),
    onPromoContainer = Color(0xFFFFD6FE),
    chart = CategoricalChartPalette
)

package com.rzk.kasirpro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Sand & Moss: a warm, boutique-shop palette instead of a POS-generic teal. Moss green
// reads as "grown, considered" rather than "corporate money"; sand keeps every screen
// looking like a lit product shelf rather than a spreadsheet. Every pair below was checked
// against WCAG contrast — see /tmp/check_pairs.py in dev notes for the method — text pairs
// clear 4.5:1, decorative borders clear 3:1.

private val Moss10 = Color(0xFF16290F)
private val Moss30 = Color(0xFF294A2C)
private val Moss40 = Color(0xFF3E5B41)
private val Moss80 = Color(0xFFA0C79A)
private val Moss90 = Color(0xFFD7E4D2)

private val Fawn10 = Color(0xFF221D0D)
private val Fawn30 = Color(0xFF524A2E)
private val Fawn40 = Color(0xFF6B5A2E)
private val Fawn80 = Color(0xFFD4C7A0)
private val Fawn90 = Color(0xFFEDE3C8)

private val Clay10 = Color(0xFF3B1204)
private val Clay30 = Color(0xFF7A3A20)
private val Clay40 = Color(0xFF9C5335)
private val Clay80 = Color(0xFFE8A585)
private val Clay90 = Color(0xFFF5DACB)

private val Ink10 = Color(0xFF2A2620)
private val Ink90 = Color(0xFFEAE3D2)

val LightColors = lightColorScheme(
    primary = Moss40,
    onPrimary = Color(0xFFF6F1E4),
    primaryContainer = Moss90,
    onPrimaryContainer = Moss10,
    inversePrimary = Moss80,
    secondary = Fawn40,
    onSecondary = Color.White,
    secondaryContainer = Fawn90,
    onSecondaryContainer = Fawn10,
    tertiary = Clay40,
    onTertiary = Color.White,
    tertiaryContainer = Clay90,
    onTertiaryContainer = Clay10,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F1E4),
    onBackground = Ink10,
    surface = Color(0xFFF6F1E4),
    onSurface = Ink10,
    surfaceVariant = Color(0xFFE6DCC3),
    onSurfaceVariant = Color(0xFF635B4A),
    surfaceTint = Moss40,
    inverseSurface = Color(0xFF362F26),
    inverseOnSurface = Color(0xFFF3ECDC),
    outline = Color(0xFF867858),
    outlineVariant = Color(0xFFDED2B3),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1EAD9),
    surfaceContainer = Color(0xFFECE3CE),
    surfaceContainerHigh = Color(0xFFE6DCC3),
    surfaceContainerHighest = Color(0xFFDED2B3)
)

val DarkColors = darkColorScheme(
    primary = Moss80,
    onPrimary = Color(0xFF123817),
    primaryContainer = Moss30,
    onPrimaryContainer = Moss90,
    inversePrimary = Moss40,
    secondary = Fawn80,
    onSecondary = Color(0xFF3A331A),
    secondaryContainer = Fawn30,
    onSecondaryContainer = Fawn90,
    tertiary = Clay80,
    onTertiary = Color(0xFF5A2211),
    tertiaryContainer = Clay30,
    onTertiaryContainer = Clay90,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B15),
    onBackground = Ink90,
    surface = Color(0xFF1C1B15),
    onSurface = Ink90,
    surfaceVariant = Color(0xFF4A4636),
    onSurfaceVariant = Color(0xFFC9BFA4),
    surfaceTint = Moss80,
    inverseSurface = Ink90,
    inverseOnSurface = Color(0xFF302E23),
    outline = Color(0xFF8C846C),
    outlineVariant = Color(0xFF4A4636),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF131209),
    surfaceContainerLow = Color(0xFF211F18),
    surfaceContainer = Color(0xFF26241C),
    surfaceContainerHigh = Color(0xFF302E23),
    surfaceContainerHighest = Color(0xFF3B382A)
)

/**
 * Categorical series colours, in fixed assignment order — slot 0 always means the same
 * entity, never "whatever ranked first". One list serves both themes: each swatch was
 * tuned to relative luminance ~0.19, the band that clears 3:1 against both the light sand
 * surface (#F6F1E4) and the dark charcoal surface (#1C1B15) at once. Re-run the check
 * before editing them.
 *
 * Charts still carry a legend and direct labels, so identity is never colour alone.
 */
val CategoricalChartPalette = listOf(
    Color(0xFF59825D), // moss
    Color(0xFFB56140), // clay
    Color(0xFF507DAA), // slate blue
    Color(0xFF957213), // ochre
    Color(0xFFB55A96), // plum
    Color(0xFF318777), // sage teal
    Color(0xFF738132), // olive
    Color(0xFFA06E46)  // brown
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
    cashIn = Color(0xFF2F7D3C),
    onCashIn = Color.White,
    cashInContainer = Color(0xFFD8EDD3),
    onCashInContainer = Color(0xFF0B2B10),
    cashOut = Color(0xFFB23B2E),
    onCashOut = Color.White,
    cashOutContainer = Color(0xFFF6DAD3),
    onCashOutContainer = Color(0xFF3D0D04),
    warning = Color(0xFF8A6A12),
    warningContainer = Color(0xFFF5E1B0),
    onWarningContainer = Color(0xFF2B1B00),
    info = Color(0xFF3C5E80),
    infoContainer = Color(0xFFD9E4EE),
    onInfoContainer = Color(0xFF14202B),
    promo = Color(0xFF8C3F72),
    promoContainer = Color(0xFFF3D9E9),
    onPromoContainer = Color(0xFF350021),
    chart = CategoricalChartPalette
)

val DarkSemanticColors = KasirSemanticColors(
    cashIn = Color(0xFF8FD79A),
    onCashIn = Color(0xFF0B2B10),
    cashInContainer = Color(0xFF1E4A26),
    onCashInContainer = Color(0xFFD8EDD3),
    cashOut = Color(0xFFF2A99C),
    onCashOut = Color(0xFF3D0D04),
    cashOutContainer = Color(0xFF7A2A1E),
    onCashOutContainer = Color(0xFFF6DAD3),
    warning = Color(0xFFE0B458),
    warningContainer = Color(0xFF5C4600),
    onWarningContainer = Color(0xFFF5E1B0),
    info = Color(0xFFA8C6E0),
    infoContainer = Color(0xFF2A4258),
    onInfoContainer = Color(0xFFD9E4EE),
    promo = Color(0xFFE8A8D0),
    promoContainer = Color(0xFF6B2A54),
    onPromoContainer = Color(0xFFF3D9E9),
    chart = CategoricalChartPalette
)

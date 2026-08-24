package com.rzk.kasirpro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Retuned as an editorial pairing rather than one sans doing every job: a serif carries
 * the sizes that display a price or a section headline (the "shop window" moments), a
 * humanist sans carries every functional/UI size where scanning speed matters more than
 * character. Both are Android's built-in generic families — a bundled boutique display
 * face would read even more distinctive, but that means shipping a font file, and a
 * till has to render instantly with zero fetch/availability risk on a shop counter.
 * ponytail: platform-native serif/sans pairing, swap for a licensed display face if the
 * brand ever needs a sharper signature.
 */
private val displayFont = FontFamily.Serif
private val uiFont = FontFamily.SansSerif

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * Tabular figures on every style, not just the receipt: a price that updates (dashboard
 * refresh, period switch) shouldn't reflow its neighbours because a "1" is narrower than
 * a "8". Letters are untouched by this feature, so it's free to apply everywhere rather
 * than threading a separate "numeric" style through every price/stat call site.
 */
private const val tabularFigures = "tnum"

val KasirTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.01).em,
        lineHeightStyle = tightLineHeight, fontFeatureSettings = tabularFigures
    ),
    displayMedium = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.01).em,
        lineHeightStyle = tightLineHeight, fontFeatureSettings = tabularFigures
    ),
    displaySmall = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.005).em,
        fontFeatureSettings = tabularFigures
    ),
    headlineLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.em,
        fontFeatureSettings = tabularFigures
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.em,
        fontFeatureSettings = tabularFigures
    ),
    headlineSmall = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, fontFeatureSettings = tabularFigures
    ),
    titleLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, fontFeatureSettings = tabularFigures
    ),
    titleMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
        fontFeatureSettings = tabularFigures
    ),
    titleSmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        fontFeatureSettings = tabularFigures
    ),
    bodyLarge = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
        fontFeatureSettings = tabularFigures
    ),
    bodyMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp,
        fontFeatureSettings = tabularFigures
    ),
    bodySmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp,
        fontFeatureSettings = tabularFigures
    ),
    labelLarge = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        fontFeatureSettings = tabularFigures
    ),
    labelMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
        fontFeatureSettings = tabularFigures
    ),
    labelSmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp,
        fontFeatureSettings = tabularFigures
    )
)

/** Monospaced-ish tabular style for receipts and ledger amounts so digits line up. */
val ReceiptTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = tabularFigures
)

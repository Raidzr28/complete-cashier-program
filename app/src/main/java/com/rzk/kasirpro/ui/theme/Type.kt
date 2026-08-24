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

val KasirTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.01).em,
        lineHeightStyle = tightLineHeight
    ),
    displayMedium = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.01).em,
        lineHeightStyle = tightLineHeight
    ),
    displaySmall = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.005).em
    ),
    headlineLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.em
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.em
    ),
    headlineSmall = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = displayFont, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = uiFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp
    )
)

/** Monospaced-ish tabular style for receipts and ledger amounts so digits line up. */
val ReceiptTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
)

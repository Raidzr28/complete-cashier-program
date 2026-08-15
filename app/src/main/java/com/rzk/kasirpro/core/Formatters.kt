package com.rzk.kasirpro.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Money and date formatting. Everything routes through here so a currency change in
 * Settings is one edit, not a hunt through 20 screens.
 */
object Formatters {

    private val groupingSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val grouped = DecimalFormat("#,##0", groupingSymbols)

    /** `1234567` → `"Rp 1.234.567"`. Negatives render as `-Rp 5.000`. */
    fun money(amount: Long, symbol: String = "Rp"): String {
        val body = grouped.format(abs(amount))
        val sign = if (amount < 0) "-" else ""
        return if (symbol.isBlank()) "$sign$body" else "$sign$symbol $body"
    }

    /** Signed form for ledgers: `+Rp 50.000` / `−Rp 20.000`. */
    fun signedMoney(amount: Long, symbol: String = "Rp"): String {
        val prefix = if (amount < 0) "−" else "+"
        return "$prefix${money(abs(amount), symbol)}"
    }

    /** Space-tight variant for chart axes and stat tiles: `1,2jt`, `850rb`. */
    fun compactMoney(amount: Long, symbol: String = "Rp"): String {
        val a = abs(amount)
        val sign = if (amount < 0) "-" else ""
        val body = when {
            a >= 1_000_000_000 -> trim(a / 1_000_000_000.0) + "M"
            a >= 1_000_000 -> trim(a / 1_000_000.0) + "jt"
            a >= 1_000 -> trim(a / 1_000.0) + "rb"
            else -> a.toString()
        }
        return if (symbol.isBlank()) "$sign$body" else "$sign$symbol$body"
    }

    fun number(value: Long): String = grouped.format(value)
    fun number(value: Int): String = grouped.format(value.toLong())

    fun percent(value: Double, decimals: Int = 1): String =
        String.format(Locale.US, "%.${decimals}f%%", value)

    private fun trim(v: Double): String {
        val rounded = Math.round(v * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
        else String.format(Locale.US, "%.1f", rounded).replace('.', ',')
    }

    // ------------------------------------------------------------- dates

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
    private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM yyyy • HH:mm", Locale.US)
    private val dayNameFmt = DateTimeFormatter.ofPattern("EEE", Locale.US)
    private val invoiceDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)

    fun localDateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun localDate(millis: Long): LocalDate = localDateTime(millis).toLocalDate()

    fun time(millis: Long): String = localDateTime(millis).format(timeFmt)
    fun date(millis: Long): String = localDateTime(millis).format(dateFmt)
    fun shortDate(millis: Long): String = localDateTime(millis).format(shortDateFmt)
    fun dateTime(millis: Long): String = localDateTime(millis).format(dateTimeFmt)
    fun dayName(millis: Long): String = localDateTime(millis).format(dayNameFmt)
    fun invoiceDateKey(millis: Long): String = localDateTime(millis).format(invoiceDateFmt)

    /** "Today" / "Yesterday" / "12 Aug 2026" — used as sticky headers in ledger lists. */
    fun relativeDay(millis: Long, todayLabel: String, yesterdayLabel: String): String {
        val day = localDate(millis)
        val today = LocalDate.now(zone)
        return when (day) {
            today -> todayLabel
            today.minusDays(1) -> yesterdayLabel
            else -> day.format(dateFmt)
        }
    }

    /** Turns SQLite's `yyyy-MM-dd` bucket key back into a short axis label. */
    fun dayKeyToShort(dayKey: String): String = runCatching {
        LocalDate.parse(dayKey).format(shortDateFmt)
    }.getOrDefault(dayKey)

    fun dayKeyToDayName(dayKey: String): String = runCatching {
        LocalDate.parse(dayKey).format(dayNameFmt)
    }.getOrDefault(dayKey)

    fun minuteOfDay(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    fun duration(fromMillis: Long, toMillis: Long): String {
        val totalMinutes = ((toMillis - fromMillis) / 60_000L).coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

/** Keeps only digits, so a currency field tolerates whatever the user pastes into it. */
fun String.digitsToAmount(): Long = filter { it.isDigit() }.take(15).toLongOrNull() ?: 0L

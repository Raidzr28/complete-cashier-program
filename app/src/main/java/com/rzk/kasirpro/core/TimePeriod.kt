package com.rzk.kasirpro.core

import com.rzk.kasirpro.data.model.PeriodPreset
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A closed millisecond window `[from, to]`. Every report query takes one of these, so
 * "today" means the same thing everywhere — local midnight to 23:59:59.999, not "24h ago".
 */
data class TimePeriod(
    val preset: PeriodPreset,
    val from: Long,
    val to: Long
) {
    operator fun contains(millis: Long): Boolean = millis in from..to

    val dayCount: Int
        get() = ((to - from) / 86_400_000L).toInt().coerceAtLeast(1)

    companion object {
        private val zone: ZoneId get() = ZoneId.systemDefault()

        private fun startOf(date: LocalDate): Long =
            date.atStartOfDay(zone).toInstant().toEpochMilli()

        private fun endOf(date: LocalDate): Long =
            date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

        fun of(preset: PeriodPreset, today: LocalDate = LocalDate.now(zone)): TimePeriod =
            when (preset) {
                PeriodPreset.TODAY -> TimePeriod(preset, startOf(today), endOf(today))
                PeriodPreset.YESTERDAY -> today.minusDays(1).let {
                    TimePeriod(preset, startOf(it), endOf(it))
                }
                // Rolling 7 days including today — matches what a shop owner means by
                // "this week" far better than an ISO week that resets on Monday morning.
                PeriodPreset.WEEK -> TimePeriod(preset, startOf(today.minusDays(6)), endOf(today))
                PeriodPreset.MONTH -> TimePeriod(preset, startOf(today.withDayOfMonth(1)), endOf(today))
                PeriodPreset.YEAR -> TimePeriod(preset, startOf(today.withDayOfYear(1)), endOf(today))
                PeriodPreset.ALL -> TimePeriod(preset, 0L, endOf(today.plusYears(1)))
                PeriodPreset.CUSTOM -> TimePeriod(preset, startOf(today), endOf(today))
            }

        fun custom(fromDate: LocalDate, toDate: LocalDate) =
            TimePeriod(PeriodPreset.CUSTOM, startOf(fromDate), endOf(toDate))

        fun today(): TimePeriod = of(PeriodPreset.TODAY)

        /** Same length as [period], ending right before it — the baseline for "vs previous". */
        fun previous(period: TimePeriod): TimePeriod {
            val span = period.to - period.from
            return TimePeriod(period.preset, period.from - span - 1, period.from - 1)
        }

        /** Every yyyy-MM-dd key in the window, so charts can render zero-sale days. */
        fun dayKeys(period: TimePeriod): List<String> {
            if (period.from <= 0L) return emptyList()
            val start = Formatters.localDate(period.from)
            val end = Formatters.localDate(period.to)
            val keys = mutableListOf<String>()
            var cursor = start
            var guard = 0
            while (!cursor.isAfter(end) && guard < 400) {
                keys += cursor.toString()
                cursor = cursor.plusDays(1)
                guard++
            }
            return keys
        }
    }
}

/** Percentage change from [previous] to [current]; 0 when there's no baseline. */
fun percentChange(current: Long, previous: Long): Double = when {
    previous == 0L -> if (current == 0L) 0.0 else 100.0
    else -> (current - previous) * 100.0 / previous
}

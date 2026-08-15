package com.rzk.kasirpro.domain

import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PromoScope

/** The promo that actually won for a cart line, flattened for storage on the receipt. */
data class AppliedPromo(
    val promoId: Long,
    val name: String,
    val amount: Long
)

/**
 * Decides which temporary discount applies to a line, right now.
 *
 * A promo has to clear three independent gates:
 *  1. the calendar window (`startAt`..`endAt`),
 *  2. the recurring weekday set,
 *  3. the time-of-day window — this is the happy-hour part.
 *
 * When several promos qualify, the one giving the customer the largest discount wins.
 * That is the behaviour shoppers expect and the one that avoids arguments at the counter.
 */
object PromoEngine {

    fun isLiveNow(promo: PromoEntity, now: Long): Boolean {
        if (!promo.isActive) return false
        if (now < promo.startAt || now >= promo.endAt) return false
        if (!matchesWeekday(promo, now)) return false
        return matchesTimeWindow(promo, now)
    }

    private fun matchesWeekday(promo: PromoEntity, now: Long): Boolean {
        val days = promo.daysOfWeek.trim()
        if (days.isEmpty()) return true
        val isoDay = Formatters.localDateTime(now).dayOfWeek.value  // Mon = 1 … Sun = 7
        return days.split(',').mapNotNull { it.trim().toIntOrNull() }.contains(isoDay)
    }

    private fun matchesTimeWindow(promo: PromoEntity, now: Long): Boolean {
        // Equal bounds = "all day", the default for a plain date-range promo.
        if (promo.startMinuteOfDay == promo.endMinuteOfDay) return true
        val t = Formatters.localDateTime(now)
        val minute = t.hour * 60 + t.minute
        return if (promo.startMinuteOfDay < promo.endMinuteOfDay) {
            minute >= promo.startMinuteOfDay && minute < promo.endMinuteOfDay
        } else {
            // Window wraps past midnight, e.g. 22:00 → 02:00.
            minute >= promo.startMinuteOfDay || minute < promo.endMinuteOfDay
        }
    }

    fun appliesTo(promo: PromoEntity, product: ProductEntity): Boolean = when (promo.scope) {
        PromoScope.ALL_PRODUCTS -> true
        PromoScope.CATEGORY -> promo.categoryId != null && promo.categoryId == product.categoryId
        PromoScope.PRODUCT -> promo.productId != null && promo.productId == product.id
    }

    /**
     * Discount amount for the whole line (all [qty] units), capped so it can never exceed
     * what the line is worth — a 100k "amount" promo on a 5k item still just makes it free.
     */
    fun discountForLine(promo: PromoEntity, unitPrice: Long, qty: Int): Long {
        if (qty < promo.minQty) return 0
        val lineValue = unitPrice * qty
        val raw = when (promo.discountType) {
            DiscountType.PERCENT -> lineValue * promo.value.coerceIn(0, 100) / 100
            DiscountType.AMOUNT -> promo.value * qty
        }
        val capped = if (promo.maxDiscountAmount > 0) minOf(raw, promo.maxDiscountAmount) else raw
        return capped.coerceIn(0, lineValue)
    }

    /** Best live promo for this product/qty, or null when nothing qualifies. */
    fun bestFor(
        product: ProductEntity,
        qty: Int,
        promos: List<PromoEntity>,
        now: Long = System.currentTimeMillis()
    ): AppliedPromo? = promos
        .asSequence()
        .filter { isLiveNow(it, now) && appliesTo(it, product) }
        .map { it to discountForLine(it, product.sellPrice, qty) }
        .filter { (_, amount) -> amount > 0 }
        // Most specific wins ties: a product promo beats a category promo beats store-wide.
        .maxWithOrNull(
            compareBy<Pair<PromoEntity, Long>> { it.second }
                .thenBy { it.first.scope.ordinal }
        )
        ?.let { (promo, amount) -> AppliedPromo(promo.id, promo.name, amount) }

    /** Human-readable rule summary for the promo list, e.g. "20% • Mon–Fri • 15:00–17:00". */
    fun describe(promo: PromoEntity, currencySymbol: String): String {
        val value = when (promo.discountType) {
            DiscountType.PERCENT -> "${promo.value}%"
            DiscountType.AMOUNT -> Formatters.money(promo.value, currencySymbol)
        }
        val days = promo.daysOfWeek.trim()
            .takeIf { it.isNotEmpty() }
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.sorted()
            ?.joinToString("/") { DAY_LABELS.getOrElse(it - 1) { "?" } }
        val hours = if (promo.startMinuteOfDay != promo.endMinuteOfDay) {
            "${Formatters.minuteOfDay(promo.startMinuteOfDay)}–${Formatters.minuteOfDay(promo.endMinuteOfDay)}"
        } else null
        return listOfNotNull(value, days, hours).joinToString(" • ")
    }

    private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}

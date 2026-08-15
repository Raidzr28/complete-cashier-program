package com.rzk.kasirpro.domain

import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PaymentMethod

data class CartLine(
    val product: ProductEntity,
    val qty: Int = 1,
    /** Cashier-keyed discount for this line only, in currency units. */
    val manualDiscount: Long = 0,
    val note: String = "",
    val promo: AppliedPromo? = null
) {
    val gross: Long get() = product.sellPrice * qty
    val promoDiscount: Long get() = promo?.amount ?: 0
    val totalDiscount: Long get() = (manualDiscount + promoDiscount).coerceAtMost(gross)
    val lineTotal: Long get() = (gross - totalDiscount).coerceAtLeast(0)
    val lineCost: Long get() = product.costPrice * qty
}

/** One tender in a split payment. */
data class TenderLine(
    val method: PaymentMethod,
    val amount: Long,
    val reference: String = ""
)

/**
 * The computed money for a cart. Order of operations matters and is fixed here:
 * line discounts → promo → order discount → service charge → tax → rounding.
 * Every screen reads these numbers instead of recomputing, so the receipt and the
 * payment sheet can never disagree.
 */
data class CartTotals(
    val itemCount: Int = 0,
    val gross: Long = 0,
    val promoDiscount: Long = 0,
    val lineDiscount: Long = 0,
    val subtotal: Long = 0,
    val orderDiscount: Long = 0,
    val serviceCharge: Long = 0,
    val tax: Long = 0,
    val rounding: Long = 0,
    val total: Long = 0,
    val totalCost: Long = 0
) {
    val totalDiscount: Long get() = promoDiscount + lineDiscount + orderDiscount
    val estimatedProfit: Long get() = total - tax - serviceCharge - totalCost
}

object CartCalculator {

    fun totals(
        lines: List<CartLine>,
        settings: SettingsEntity,
        orderDiscountInput: Long = 0,
        orderDiscountType: DiscountType = DiscountType.AMOUNT
    ): CartTotals {
        if (lines.isEmpty()) return CartTotals()

        val gross = lines.sumOf { it.gross }
        val promo = lines.sumOf { it.promoDiscount }
        val manual = lines.sumOf { it.manualDiscount.coerceAtMost(it.gross - it.promoDiscount) }
        val subtotal = lines.sumOf { it.lineTotal }

        val orderDiscount = when (orderDiscountType) {
            DiscountType.PERCENT -> subtotal * orderDiscountInput.coerceIn(0, 100) / 100
            DiscountType.AMOUNT -> orderDiscountInput
        }.coerceIn(0, subtotal)

        val afterDiscount = subtotal - orderDiscount

        val service = if (settings.serviceChargeEnabled && settings.serviceChargePercent > 0) {
            afterDiscount * settings.serviceChargePercent / 100
        } else 0L

        val taxable = afterDiscount + service
        val tax = when {
            !settings.taxEnabled || settings.taxPercent <= 0 -> 0L
            // Inclusive: the price already contains the tax, so back it out for reporting
            // without changing what the customer pays.
            settings.taxInclusive -> taxable - (taxable * 100 / (100 + settings.taxPercent))
            else -> taxable * settings.taxPercent / 100
        }

        val beforeRounding = if (settings.taxInclusive) taxable else taxable + tax
        val rounded = roundTo(beforeRounding, settings.roundingNearest)

        return CartTotals(
            itemCount = lines.sumOf { it.qty },
            gross = gross,
            promoDiscount = promo,
            lineDiscount = manual,
            subtotal = subtotal,
            orderDiscount = orderDiscount,
            serviceCharge = service,
            tax = tax,
            rounding = rounded - beforeRounding,
            total = rounded,
            totalCost = lines.sumOf { it.lineCost }
        )
    }

    private fun roundTo(value: Long, nearest: Int): Long {
        if (nearest <= 1) return value
        val half = nearest / 2
        return ((value + half) / nearest) * nearest
    }

    /**
     * Cash denominations a cashier is likely to be handed for [total], for the
     * one-tap buttons on the payment sheet.
     */
    fun quickCashOptions(total: Long): List<Long> {
        if (total <= 0) return emptyList()
        val denominations = listOf(1_000L, 2_000L, 5_000L, 10_000L, 20_000L, 50_000L, 100_000L)
        val suggestions = linkedSetOf<Long>()
        suggestions += total                                     // exact money
        denominations.filter { it >= total }.take(2).forEach { suggestions += it }
        listOf(1_000L, 5_000L, 10_000L, 50_000L, 100_000L).forEach { step ->
            val up = ((total + step - 1) / step) * step
            if (up > total) suggestions += up
        }
        return suggestions.sorted().take(6)
    }
}

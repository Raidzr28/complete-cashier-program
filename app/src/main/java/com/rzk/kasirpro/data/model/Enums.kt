package com.rzk.kasirpro.data.model

/**
 * All money in this app is stored as [Long] in whole currency units (rupiah).
 * IDR has no practical minor unit, and integers keep totals exact — no float drift
 * across discount + tax + split-payment arithmetic.
 */
typealias Money = Long

enum class PaymentMethod {
    CASH, QRIS, DEBIT, CREDIT, TRANSFER, EWALLET;

    /** Only cash physically moves through the drawer, so only cash affects cash-on-hand. */
    val affectsCashDrawer: Boolean get() = this == CASH
}

enum class SaleStatus {
    /** Paid and final. Counted in every report. */
    COMPLETED,

    /** Parked/held cart, not yet paid. Excluded from all reports. */
    HELD,

    /** Cancelled after payment. Stock restored, cash reversed, excluded from revenue. */
    VOID,

    /** Fully or partially returned. */
    REFUNDED
}

enum class CashFlowType { IN, OUT }

/** Where a cash movement came from, so auto-posted rows can be told apart from manual ones. */
enum class CashFlowSource {
    SALE,           // auto: a completed sale
    REFUND,         // auto: a void/refund reversal
    SHIFT_OPENING,  // auto: opening float when a shift starts
    SHIFT_CLOSING,  // auto: variance recorded when a shift closes
    PURCHASE,       // auto: stock-in that was paid from the drawer
    MANUAL          // user-entered cash in / cash out
}

enum class StockMovementType {
    PURCHASE_IN,    // goods received
    SALE_OUT,       // sold
    ADJUSTMENT,     // stock take correction (+/-)
    RETURN_IN,      // customer returned goods
    VOID_RETURN,    // sale voided, stock put back
    WASTE_OUT,      // damaged / expired / shrinkage
    INITIAL         // opening balance when the product was created
}

enum class ShiftStatus { OPEN, CLOSED }

enum class DiscountType {
    /** value is a percentage, 0..100 */
    PERCENT,

    /** value is an absolute amount in currency units */
    AMOUNT
}

/** What a temporary discount (promo) applies to. */
enum class PromoScope { ALL_PRODUCTS, CATEGORY, PRODUCT }

/** Period presets used by every reporting screen. */
enum class PeriodPreset { TODAY, YESTERDAY, WEEK, MONTH, YEAR, ALL, CUSTOM }

enum class StockStatus { OUT_OF_STOCK, LOW, HEALTHY, UNTRACKED }

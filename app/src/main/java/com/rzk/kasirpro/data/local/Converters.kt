package com.rzk.kasirpro.data.local

import androidx.room.TypeConverter
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PaymentMethod
import com.rzk.kasirpro.data.model.PromoScope
import com.rzk.kasirpro.data.model.SaleStatus
import com.rzk.kasirpro.data.model.ShiftStatus
import com.rzk.kasirpro.data.model.StockMovementType

/**
 * Enums are stored by name rather than ordinal — reordering an enum then can't silently
 * reinterpret existing rows.
 */
class Converters {
    @TypeConverter fun paymentToString(v: PaymentMethod): String = v.name
    @TypeConverter fun stringToPayment(v: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(v) }.getOrDefault(PaymentMethod.CASH)

    @TypeConverter fun saleStatusToString(v: SaleStatus): String = v.name
    @TypeConverter fun stringToSaleStatus(v: String): SaleStatus =
        runCatching { SaleStatus.valueOf(v) }.getOrDefault(SaleStatus.COMPLETED)

    @TypeConverter fun cashTypeToString(v: CashFlowType): String = v.name
    @TypeConverter fun stringToCashType(v: String): CashFlowType =
        runCatching { CashFlowType.valueOf(v) }.getOrDefault(CashFlowType.OUT)

    @TypeConverter fun cashSourceToString(v: CashFlowSource): String = v.name
    @TypeConverter fun stringToCashSource(v: String): CashFlowSource =
        runCatching { CashFlowSource.valueOf(v) }.getOrDefault(CashFlowSource.MANUAL)

    @TypeConverter fun stockTypeToString(v: StockMovementType): String = v.name
    @TypeConverter fun stringToStockType(v: String): StockMovementType =
        runCatching { StockMovementType.valueOf(v) }.getOrDefault(StockMovementType.ADJUSTMENT)

    @TypeConverter fun shiftStatusToString(v: ShiftStatus): String = v.name
    @TypeConverter fun stringToShiftStatus(v: String): ShiftStatus =
        runCatching { ShiftStatus.valueOf(v) }.getOrDefault(ShiftStatus.CLOSED)

    @TypeConverter fun discountTypeToString(v: DiscountType): String = v.name
    @TypeConverter fun stringToDiscountType(v: String): DiscountType =
        runCatching { DiscountType.valueOf(v) }.getOrDefault(DiscountType.AMOUNT)

    @TypeConverter fun promoScopeToString(v: PromoScope): String = v.name
    @TypeConverter fun stringToPromoScope(v: String): PromoScope =
        runCatching { PromoScope.valueOf(v) }.getOrDefault(PromoScope.ALL_PRODUCTS)
}

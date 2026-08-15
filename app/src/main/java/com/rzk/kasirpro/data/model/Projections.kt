package com.rzk.kasirpro.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.local.entity.SaleEntity
import com.rzk.kasirpro.data.local.entity.SaleItemEntity
import com.rzk.kasirpro.data.local.entity.SalePaymentEntity

/** Product joined with its category so the POS grid never needs a second lookup. */
data class ProductWithCategory(
    @Embedded val product: ProductEntity,
    val categoryName: String?,
    val categoryColor: Int?
) {
    val stockStatus: StockStatus
        get() = when {
            !product.trackStock -> StockStatus.UNTRACKED
            product.stock <= 0 -> StockStatus.OUT_OF_STOCK
            product.stock <= product.minStock -> StockStatus.LOW
            else -> StockStatus.HEALTHY
        }

    val margin: Long get() = product.sellPrice - product.costPrice
    val marginPercent: Double
        get() = if (product.sellPrice <= 0) 0.0 else margin * 100.0 / product.sellPrice
}

/** A full sale with every line and every tender — what the receipt and detail screen render. */
data class SaleWithDetails(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val items: List<SaleItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "saleId")
    val payments: List<SalePaymentEntity>
) {
    val itemCount: Int get() = items.sumOf { it.qty }
    val profit: Long get() = sale.total - sale.taxAmount - sale.totalCost
}

// ---------------------------------------------------------------------------
// Reporting projections
// ---------------------------------------------------------------------------

/** One row of the best-seller / worst-seller table. */
data class ProductSalesStat(
    val productId: Long?,
    val productName: String,
    val qtySold: Int,
    val revenue: Long,
    val cost: Long,
    val profit: Long,
    val orderCount: Int
) {
    val marginPercent: Double
        get() = if (revenue <= 0) 0.0 else profit * 100.0 / revenue
}

data class CategorySalesStat(
    val categoryId: Long?,
    val categoryName: String?,
    val qtySold: Int,
    val revenue: Long,
    val profit: Long
)

/** dayKey is a local "yyyy-MM-dd" produced by SQLite, ready to bucket a chart. */
data class DailyTotal(
    val dayKey: String,
    val total: Long,
    val orders: Int,
    val profit: Long
)

data class HourlyTotal(
    val hourKey: String,
    val total: Long,
    val orders: Int
)

data class PaymentBreakdown(
    val method: PaymentMethod,
    val total: Long,
    val count: Int
)

/** Aggregate sales numbers for a period. Nullable fields cover the "no rows" case. */
data class SalesSummary(
    val orders: Int = 0,
    val gross: Long = 0,
    val discount: Long = 0,
    val tax: Long = 0,
    val net: Long = 0,
    val cost: Long = 0,
    val itemsSold: Int = 0
) {
    val profit: Long get() = net - tax - cost
    val averageOrderValue: Long get() = if (orders == 0) 0 else net / orders
    val marginPercent: Double
        get() = if (net - tax <= 0) 0.0 else profit * 100.0 / (net - tax)
}

data class CashFlowSummary(
    val totalIn: Long = 0,
    val totalOut: Long = 0,
    val drawerIn: Long = 0,
    val drawerOut: Long = 0
) {
    val net: Long get() = totalIn - totalOut
    val drawerNet: Long get() = drawerIn - drawerOut
}

/** Cash-out grouped by its category, for the expense breakdown donut. */
data class CashCategoryTotal(
    val category: String,
    val total: Long,
    val entries: Int
)

data class StockValuation(
    val productCount: Int = 0,
    val totalUnits: Int = 0,
    val costValue: Long = 0,
    val retailValue: Long = 0
) {
    val potentialProfit: Long get() = retailValue - costValue
}

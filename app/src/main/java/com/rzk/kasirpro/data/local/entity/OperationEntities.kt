package com.rzk.kasirpro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.ShiftStatus
import com.rzk.kasirpro.data.model.StockMovementType

/**
 * The cash ledger. Every movement of money — automatic (a sale, a refund, a stock purchase)
 * or manual (owner took cash out for lunch) — lands here as one immutable-ish row.
 *
 * [affectsCashDrawer] is the important flag: a QRIS sale is income but never touches the
 * physical drawer, so it counts toward revenue and *not* toward cash-on-hand. Getting this
 * wrong is the usual reason a POS cash count never reconciles.
 */
@Entity(
    tableName = "cash_flows",
    indices = [
        Index("createdAt"),
        Index("type"),
        Index("source"),
        Index("shiftId"),
        Index(value = ["referenceId", "source"])
    ]
)
data class CashFlowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CashFlowType,
    val amount: Long,
    /** Free-form but preset-driven bucket: "Sales", "Stock purchase", "Salary", "Rent"… */
    val category: String = "",
    val note: String = "",
    val source: CashFlowSource = CashFlowSource.MANUAL,
    /** Id of the sale / shift / stock movement that generated this row, when automatic. */
    val referenceId: Long? = null,
    val shiftId: Long? = null,
    val affectsCashDrawer: Boolean = true,
    val attachmentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A cashier session. Opening float in, count the drawer at the end, and the difference
 * between expected and actual is the variance the owner actually cares about.
 */
@Entity(
    tableName = "shifts",
    indices = [Index("status"), Index("openedAt")]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String = "",
    val openingCash: Long = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    /** openingCash + cash in - cash out, computed at close time. */
    val expectedCash: Long = 0,
    /** What was physically counted. */
    val actualCash: Long = 0,
    /** actualCash - expectedCash. Negative = short. */
    val difference: Long = 0,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val note: String = ""
)

/**
 * Append-only stock audit trail. Product.stock is the fast cached balance; this table is
 * the explanation of how it got there.
 */
@Entity(
    tableName = "stock_movements",
    indices = [Index("productId"), Index("createdAt"), Index("type")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: StockMovementType,
    /** Signed: positive adds stock, negative removes it. */
    val qty: Int,
    val stockBefore: Int,
    val stockAfter: Int,
    /** Unit cost for inbound movements; used to recompute average cost. */
    val unitCost: Long = 0,
    val note: String = "",
    /** Sale id for SALE_OUT, etc. */
    val referenceId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Single-row table (id is pinned to 1) holding store profile + receipt configuration. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SETTINGS_ID,
    val storeName: String = "My Store",
    val storeAddress: String = "",
    val storePhone: String = "",
    val logoUri: String? = null,

    val currencySymbol: String = "Rp",
    val taxEnabled: Boolean = false,
    /** Percentage, e.g. 11 for PPN 11%. */
    val taxPercent: Int = 11,
    val taxInclusive: Boolean = false,
    val serviceChargeEnabled: Boolean = false,
    val serviceChargePercent: Int = 0,
    /** Round the grand total to the nearest N (e.g. 100 rupiah). 0 disables. */
    val roundingNearest: Int = 0,

    val receiptHeader: String = "",
    val receiptFooter: String = "Thank you for your purchase!",
    val showLogoOnReceipt: Boolean = true,
    val printBarcodeOnReceipt: Boolean = true,

    val defaultCashierName: String = "Cashier",
    val lowStockAlertEnabled: Boolean = true,
    /** Prevents selling a product whose stock is 0. */
    val blockSaleWhenOutOfStock: Boolean = true,
    /** Invoice counter state so numbers restart daily. */
    val invoicePrefix: String = "INV",
    val invoiceDateKey: String = "",
    val invoiceSequence: Int = 0
) {
    companion object { const val SETTINGS_ID = 1 }
}

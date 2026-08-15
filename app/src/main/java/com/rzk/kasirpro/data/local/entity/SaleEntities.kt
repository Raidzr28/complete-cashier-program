package com.rzk.kasirpro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PaymentMethod
import com.rzk.kasirpro.data.model.SaleStatus

/**
 * One checkout. A cart that has been parked lives here too with [status] = HELD, which is
 * why held orders survive an app kill and can be resumed on another device shift.
 */
@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["invoiceNo"], unique = true),
        Index("createdAt"),
        Index("status"),
        Index("shiftId")
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Human-facing receipt number, e.g. INV/20260810/0007. */
    val invoiceNo: String,
    val status: SaleStatus = SaleStatus.COMPLETED,
    val shiftId: Long? = null,

    /** Sum of line totals after per-line and promo discounts, before order discount and tax. */
    val subtotal: Long = 0,
    /** Total of automatic promo discounts, kept separate so promo ROI is measurable. */
    val promoDiscount: Long = 0,
    /** Manual whole-order discount the cashier keyed in. */
    val orderDiscount: Long = 0,
    val orderDiscountType: DiscountType = DiscountType.AMOUNT,
    val orderDiscountInput: Long = 0,
    val taxAmount: Long = 0,
    val serviceCharge: Long = 0,
    val roundingAdjustment: Long = 0,
    val total: Long = 0,

    /** Snapshot of cost at sale time, so historical margin doesn't move when prices change. */
    val totalCost: Long = 0,

    val paidAmount: Long = 0,
    val changeAmount: Long = 0,
    /** The dominant tender. Split payments are itemised in [SalePaymentEntity]. */
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isSplitPayment: Boolean = false,

    val customerName: String = "",
    val customerPhone: String = "",
    val note: String = "",
    val cashierName: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val voidedAt: Long? = null,
    val voidReason: String = ""
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    /** Nullable: a product can be deleted later without orphaning history. */
    val productId: Long? = null,

    // Snapshots — a receipt reprinted next year must show the price that was actually charged.
    val productName: String,
    val unitPrice: Long,
    val costPrice: Long,
    val unit: String = "pcs",

    val qty: Int = 1,
    /** Manual per-line discount. */
    val lineDiscount: Long = 0,
    /** Automatic discount from a promo that was live at checkout time. */
    val promoDiscount: Long = 0,
    val promoId: Long? = null,
    val promoName: String = "",
    /** qty * unitPrice - lineDiscount - promoDiscount */
    val lineTotal: Long = 0,
    val note: String = ""
)

/**
 * One tender line. A single-tender sale still gets one row here, so reporting by payment
 * method never has to special-case splits.
 */
@Entity(
    tableName = "sale_payments",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SalePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val method: PaymentMethod,
    val amount: Long,
    /** Card approval code, e-wallet reference, transfer note… */
    val reference: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

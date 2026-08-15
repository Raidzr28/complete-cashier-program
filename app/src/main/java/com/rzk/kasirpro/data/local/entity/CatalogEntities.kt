package com.rzk.kasirpro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PromoScope

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB int so the POS grid can colour-code chips without a lookup table. */
    val colorArgb: Int,
    val iconKey: String = "category",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["barcode"]),
        Index(value = ["name"]),
        Index(value = ["isActive"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    /** EAN/UPC/QR payload. Scanning this in the POS adds the item straight to the cart. */
    val barcode: String = "",
    val categoryId: Long? = null,
    /** What we pay for it. Drives margin and stock valuation. */
    val costPrice: Long = 0,
    /** What the customer pays before any discount. */
    val sellPrice: Long = 0,
    val stock: Int = 0,
    /** Below this the product is flagged as low stock on the dashboard. */
    val minStock: Int = 5,
    val unit: String = "pcs",
    val imageUri: String? = null,
    /** Services (haircut, delivery fee) have no stock to deduct. */
    val trackStock: Boolean = true,
    val isActive: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A temporary (time-boxed) discount. Two independent windows have to both match for it to
 * apply: a calendar window ([startAt]..[endAt]) and an optional recurring weekday +
 * time-of-day window — that second one is what makes "happy hour" work.
 */
@Entity(
    tableName = "promos",
    indices = [Index("isActive"), Index("startAt"), Index("endAt")]
)
data class PromoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val discountType: DiscountType = DiscountType.PERCENT,
    /** Percent (0..100) when [discountType] is PERCENT, otherwise an absolute amount. */
    val value: Long = 0,
    val scope: PromoScope = PromoScope.ALL_PRODUCTS,
    val categoryId: Long? = null,
    val productId: Long? = null,
    /** Calendar window, epoch millis. [endAt] is exclusive. */
    val startAt: Long,
    val endAt: Long,
    /**
     * ISO-8601 day numbers (Mon=1 … Sun=7) joined by commas, e.g. "1,2,3,4,5".
     * Blank means every day.
     */
    val daysOfWeek: String = "",
    /** Recurring daily window in minutes from midnight. 0..1439. Equal values = all day. */
    val startMinuteOfDay: Int = 0,
    val endMinuteOfDay: Int = 0,
    /** Promo only kicks in once the line reaches this quantity. */
    val minQty: Int = 1,
    /** Caps a percent promo. 0 means uncapped. */
    val maxDiscountAmount: Long = 0,
    @ColumnInfo(defaultValue = "1") val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

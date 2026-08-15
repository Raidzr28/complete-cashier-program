package com.rzk.kasirpro.data.repository

import androidx.room.withTransaction
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.StockMovementType
import kotlinx.coroutines.flow.Flow

/**
 * Every change to a stock balance goes through here, and every change writes a movement
 * row. `products.stock` is only ever a cached balance; [observeMovements] is the truth.
 */
class StockRepository(private val db: KasirDatabase) {

    private val productDao = db.productDao()
    private val stockDao = db.stockDao()
    private val cashDao = db.cashFlowDao()
    private val shiftDao = db.shiftDao()

    fun observeMovements(
        period: TimePeriod,
        productId: Long? = null,
        limit: Int = 300
    ): Flow<List<StockMovementEntity>> =
        stockDao.observeMovements(productId, period.from, period.to, limit)

    fun observeForProduct(productId: Long, limit: Int = 100): Flow<List<StockMovementEntity>> =
        stockDao.observeForProduct(productId, limit)

    /**
     * Receiving goods. Optionally updates the product's cost price to the new purchase
     * price, and optionally posts the purchase as a cash-out so buying stock shows up in
     * the cashflow instead of silently eating the drawer.
     */
    suspend fun stockIn(
        productId: Long,
        qty: Int,
        unitCost: Long,
        note: String,
        updateCostPrice: Boolean,
        payFromCash: Boolean
    ): Result<Unit> = runCatching {
        require(qty > 0) { "Quantity must be greater than zero" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: error("Product not found")
            val now = System.currentTimeMillis()

            productDao.applyStockDelta(productId, qty, now)
            if (updateCostPrice && unitCost > 0) productDao.setCostPrice(productId, unitCost, now)

            stockDao.insert(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = StockMovementType.PURCHASE_IN,
                    qty = qty,
                    stockBefore = product.stock,
                    stockAfter = product.stock + qty,
                    unitCost = unitCost,
                    note = note.trim(),
                    createdAt = now
                )
            )

            val spend = unitCost * qty
            if (payFromCash && spend > 0) {
                cashDao.insert(
                    CashFlowEntity(
                        type = CashFlowType.OUT,
                        amount = spend,
                        category = CashCategories.STOCK_PURCHASE,
                        note = "${product.name} × $qty",
                        source = CashFlowSource.PURCHASE,
                        referenceId = productId,
                        shiftId = shiftDao.getOpenShift()?.id,
                        affectsCashDrawer = true,
                        createdAt = now
                    )
                )
            }
        }
    }

    /**
     * Stock take. [countedStock] is what was physically on the shelf; the difference is
     * written as a signed adjustment so shrinkage is visible instead of hidden.
     */
    suspend fun adjustTo(
        productId: Long,
        countedStock: Int,
        reason: String
    ): Result<Int> = runCatching {
        require(countedStock >= 0) { "Counted stock cannot be negative" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: error("Product not found")
            val delta = countedStock - product.stock
            if (delta == 0) return@withTransaction 0

            val now = System.currentTimeMillis()
            productDao.setStock(productId, countedStock, now)
            stockDao.insert(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = StockMovementType.ADJUSTMENT,
                    qty = delta,
                    stockBefore = product.stock,
                    stockAfter = countedStock,
                    unitCost = product.costPrice,
                    note = reason.trim(),
                    createdAt = now
                )
            )
            delta
        }
    }

    /** Damaged, expired or lost goods. Removes stock and records why. */
    suspend fun writeOff(productId: Long, qty: Int, reason: String): Result<Unit> = runCatching {
        require(qty > 0) { "Quantity must be greater than zero" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: error("Product not found")
            require(product.stock >= qty) { "Only ${product.stock} in stock" }
            val now = System.currentTimeMillis()
            productDao.applyStockDelta(productId, -qty, now)
            stockDao.insert(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = StockMovementType.WASTE_OUT,
                    qty = -qty,
                    stockBefore = product.stock,
                    stockAfter = product.stock - qty,
                    unitCost = product.costPrice,
                    note = reason.trim(),
                    createdAt = now
                )
            )
        }
    }

    /** Customer brought goods back outside of a void — puts them on the shelf again. */
    suspend fun returnIn(productId: Long, qty: Int, reason: String): Result<Unit> = runCatching {
        require(qty > 0) { "Quantity must be greater than zero" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: error("Product not found")
            val now = System.currentTimeMillis()
            productDao.applyStockDelta(productId, qty, now)
            stockDao.insert(
                StockMovementEntity(
                    productId = productId,
                    productName = product.name,
                    type = StockMovementType.RETURN_IN,
                    qty = qty,
                    stockBefore = product.stock,
                    stockAfter = product.stock + qty,
                    unitCost = product.costPrice,
                    note = reason.trim(),
                    createdAt = now
                )
            )
        }
    }
}

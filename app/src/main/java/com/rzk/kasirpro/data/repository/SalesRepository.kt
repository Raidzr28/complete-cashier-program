package com.rzk.kasirpro.data.repository

import androidx.room.withTransaction
import com.rzk.kasirpro.core.Formatters
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.local.entity.SaleEntity
import com.rzk.kasirpro.data.local.entity.SaleItemEntity
import com.rzk.kasirpro.data.local.entity.SalePaymentEntity
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.DiscountType
import com.rzk.kasirpro.data.model.PaymentMethod
import com.rzk.kasirpro.data.model.SaleStatus
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.data.model.StockMovementType
import com.rzk.kasirpro.domain.CartLine
import com.rzk.kasirpro.domain.CartTotals
import com.rzk.kasirpro.domain.TenderLine
import kotlinx.coroutines.flow.Flow
import java.util.Locale

data class CheckoutRequest(
    val lines: List<CartLine>,
    val totals: CartTotals,
    val tenders: List<TenderLine>,
    val orderDiscountInput: Long = 0,
    val orderDiscountType: DiscountType = DiscountType.AMOUNT,
    val customerName: String = "",
    val customerPhone: String = "",
    val note: String = "",
    /** When the cashier resumed a parked cart, the held sale is consumed on checkout. */
    val resumedHeldSaleId: Long? = null
)

sealed interface CheckoutResult {
    data class Success(val sale: SaleWithDetails) : CheckoutResult
    data class InsufficientStock(val productName: String, val available: Int) : CheckoutResult
    data class Failure(val message: String) : CheckoutResult
    data object EmptyCart : CheckoutResult
    data object Underpaid : CheckoutResult
}

/**
 * Owns everything that happens at the moment of sale. [checkout] is deliberately one
 * database transaction: the sale, its lines, its tenders, the stock deductions and the
 * cash-ledger postings either all land or none do. A partial checkout is the one bug that
 * makes a POS untrustworthy.
 */
class SalesRepository(private val db: KasirDatabase) {

    private val saleDao = db.saleDao()
    private val productDao = db.productDao()
    private val stockDao = db.stockDao()
    private val cashDao = db.cashFlowDao()
    private val settingsDao = db.settingsDao()
    private val shiftDao = db.shiftDao()

    // ------------------------------------------------------------------ reads

    fun observeHistory(
        period: TimePeriod,
        query: String = "",
        limit: Int = 300
    ): Flow<List<SaleWithDetails>> =
        saleDao.observeHistory(period.from, period.to, query.trim(), limit)

    fun observeRecent(limit: Int = 8): Flow<List<SaleWithDetails>> = saleDao.observeRecent(limit)

    fun observeDetails(id: Long): Flow<SaleWithDetails?> = saleDao.observeDetails(id)

    fun observeHeld(): Flow<List<SaleWithDetails>> = saleDao.observeHeld()

    fun observeHeldCount(): Flow<Int> = saleDao.observeHeldCount()

    fun observeSummary(period: TimePeriod) = saleDao.observeSummary(period.from, period.to)

    fun observeDailyTotals(period: TimePeriod) = saleDao.observeDailyTotals(period.from, period.to)

    fun observeHourlyTotals(period: TimePeriod) = saleDao.observeHourlyTotals(period.from, period.to)

    fun observePaymentBreakdown(period: TimePeriod) =
        saleDao.observePaymentBreakdown(period.from, period.to)

    fun observeBestSellers(period: TimePeriod, limit: Int = 10) =
        saleDao.observeProductRanking(period.from, period.to, ascending = false, limit = limit)

    fun observeWorstSellers(period: TimePeriod, limit: Int = 10) =
        saleDao.observeProductRanking(period.from, period.to, ascending = true, limit = limit)

    fun observeMostProfitable(period: TimePeriod, limit: Int = 10) =
        saleDao.observeMostProfitable(period.from, period.to, limit)

    fun observeNeverSold(period: TimePeriod, limit: Int = 20) =
        saleDao.observeNeverSold(period.from, period.to, limit)

    fun observeCategoryStats(period: TimePeriod) =
        saleDao.observeCategoryStats(period.from, period.to)

    fun observeShiftSummary(shiftId: Long) = saleDao.observeShiftSummary(shiftId)

    suspend fun getDetails(id: Long): SaleWithDetails? = saleDao.getDetails(id)

    // ------------------------------------------------------------------ checkout

    suspend fun checkout(request: CheckoutRequest): CheckoutResult {
        if (request.lines.isEmpty()) return CheckoutResult.EmptyCart

        val paid = request.tenders.sumOf { it.amount }
        if (paid < request.totals.total) return CheckoutResult.Underpaid

        return try {
            db.withTransaction {
                val settings = settingsDao.get() ?: error("Settings row missing")

                // Re-read stock inside the transaction. The grid may be seconds stale, and
                // overselling the last unit is exactly the race this guards.
                if (settings.blockSaleWhenOutOfStock) {
                    request.lines.forEach { line ->
                        if (line.product.trackStock) {
                            val fresh = productDao.getById(line.product.id)
                            val available = fresh?.stock ?: 0
                            if (available < line.qty) {
                                return@withTransaction CheckoutResult.InsufficientStock(
                                    line.product.name, available
                                )
                            }
                        }
                    }
                }

                val now = System.currentTimeMillis()
                val shiftId = shiftDao.getOpenShift()?.id
                val invoiceNo = nextInvoiceNo(now, settings.invoicePrefix)
                val change = (paid - request.totals.total).coerceAtLeast(0)

                // A resumed parked cart must not linger as a second, unpaid order.
                request.resumedHeldSaleId?.let { saleDao.deleteSale(it) }

                val saleId = saleDao.insertSale(
                    SaleEntity(
                        invoiceNo = invoiceNo,
                        status = SaleStatus.COMPLETED,
                        shiftId = shiftId,
                        subtotal = request.totals.subtotal,
                        promoDiscount = request.totals.promoDiscount + request.totals.lineDiscount,
                        orderDiscount = request.totals.orderDiscount,
                        orderDiscountType = request.orderDiscountType,
                        orderDiscountInput = request.orderDiscountInput,
                        taxAmount = request.totals.tax,
                        serviceCharge = request.totals.serviceCharge,
                        roundingAdjustment = request.totals.rounding,
                        total = request.totals.total,
                        totalCost = request.totals.totalCost,
                        paidAmount = paid,
                        changeAmount = change,
                        paymentMethod = dominantMethod(request.tenders),
                        isSplitPayment = request.tenders.size > 1,
                        customerName = request.customerName.trim(),
                        customerPhone = request.customerPhone.trim(),
                        note = request.note.trim(),
                        cashierName = settings.defaultCashierName,
                        createdAt = now
                    )
                )

                saleDao.insertItems(request.lines.map { it.toItemEntity(saleId) })
                saleDao.insertPayments(
                    request.tenders.map {
                        SalePaymentEntity(
                            saleId = saleId,
                            method = it.method,
                            amount = it.amount,
                            reference = it.reference,
                            createdAt = now
                        )
                    }
                )

                applyStockForSale(request.lines, saleId, invoiceNo, now)
                postSaleToCashLedger(request.tenders, change, saleId, shiftId, invoiceNo, now)

                val saved = saleDao.getDetails(saleId)
                if (saved == null) CheckoutResult.Failure("Sale could not be read back")
                else CheckoutResult.Success(saved)
            }
        } catch (t: Throwable) {
            CheckoutResult.Failure(t.message ?: "Checkout failed")
        }
    }

    private suspend fun applyStockForSale(
        lines: List<CartLine>,
        saleId: Long,
        invoiceNo: String,
        now: Long
    ) {
        val movements = mutableListOf<StockMovementEntity>()
        lines.forEach { line ->
            if (!line.product.trackStock) return@forEach
            val before = productDao.getById(line.product.id)?.stock ?: line.product.stock
            productDao.applyStockDelta(line.product.id, -line.qty, now)
            movements += StockMovementEntity(
                productId = line.product.id,
                productName = line.product.name,
                type = StockMovementType.SALE_OUT,
                qty = -line.qty,
                stockBefore = before,
                stockAfter = before - line.qty,
                unitCost = line.product.costPrice,
                note = invoiceNo,
                referenceId = saleId,
                createdAt = now
            )
        }
        if (movements.isNotEmpty()) stockDao.insertAll(movements)
    }

    /**
     * One ledger row per tender. Change is handed back out of the drawer, so it is netted
     * off the cash row rather than posted as a separate expense — the drawer only ever
     * gained (cash tendered − change).
     */
    private suspend fun postSaleToCashLedger(
        tenders: List<TenderLine>,
        change: Long,
        saleId: Long,
        shiftId: Long?,
        invoiceNo: String,
        now: Long
    ) {
        tenders.groupBy { it.method }
            .forEach { (method, group) ->
                val gross = group.sumOf { it.amount }
                val amount = if (method == PaymentMethod.CASH) gross - change else gross
                if (amount <= 0) return@forEach
                cashDao.insert(
                    CashFlowEntity(
                        type = CashFlowType.IN,
                        amount = amount,
                        category = CashCategories.SALES,
                        note = "$invoiceNo • ${method.name}",
                        source = CashFlowSource.SALE,
                        referenceId = saleId,
                        shiftId = shiftId,
                        affectsCashDrawer = method.affectsCashDrawer,
                        createdAt = now
                    )
                )
            }
    }

    private fun dominantMethod(tenders: List<TenderLine>): PaymentMethod =
        tenders.maxByOrNull { it.amount }?.method ?: PaymentMethod.CASH

    private suspend fun nextInvoiceNo(now: Long, prefix: String): String {
        val dateKey = Formatters.invoiceDateKey(now)
        settingsDao.bumpInvoiceSequence(dateKey)
        val seq = settingsDao.currentInvoiceSequence()
        return "%s/%s/%04d".format(Locale.US, prefix.ifBlank { "INV" }, dateKey, seq)
    }

    // ------------------------------------------------------------------ hold / resume

    /** Parks the current cart so the cashier can serve the next customer. */
    suspend fun holdOrder(
        lines: List<CartLine>,
        totals: CartTotals,
        label: String
    ): Result<Long> = runCatching {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val saleId = saleDao.insertSale(
                SaleEntity(
                    invoiceNo = "HOLD/${now}",
                    status = SaleStatus.HELD,
                    subtotal = totals.subtotal,
                    promoDiscount = totals.promoDiscount + totals.lineDiscount,
                    total = totals.total,
                    totalCost = totals.totalCost,
                    customerName = label.trim(),
                    createdAt = now
                )
            )
            saleDao.insertItems(lines.map { it.toItemEntity(saleId) })
            saleId
        }
    }

    /** Removes a parked cart. Nothing else to undo — held orders never touched stock or cash. */
    suspend fun discardHeld(saleId: Long) = saleDao.deleteSale(saleId)

    // ------------------------------------------------------------------ void

    /**
     * Cancels a completed sale: stock goes back on the shelf and the cash posting is
     * reversed with an opposite entry rather than deleted, so the ledger keeps its history.
     */
    suspend fun voidSale(saleId: Long, reason: String): Result<Unit> = runCatching {
        db.withTransaction {
            val details = saleDao.getDetails(saleId) ?: error("Sale not found")
            if (details.sale.status != SaleStatus.COMPLETED) error("Only completed sales can be voided")

            val now = System.currentTimeMillis()
            saleDao.markStatus(saleId, SaleStatus.VOID.name, now, reason.trim())

            val movements = mutableListOf<StockMovementEntity>()
            details.items.forEach { item ->
                val pid = item.productId ?: return@forEach
                val product = productDao.getById(pid) ?: return@forEach
                if (!product.trackStock) return@forEach
                val before = product.stock
                productDao.applyStockDelta(pid, item.qty, now)
                movements += StockMovementEntity(
                    productId = pid,
                    productName = item.productName,
                    type = StockMovementType.VOID_RETURN,
                    qty = item.qty,
                    stockBefore = before,
                    stockAfter = before + item.qty,
                    unitCost = item.costPrice,
                    note = "Void ${details.sale.invoiceNo}",
                    referenceId = saleId,
                    createdAt = now
                )
            }
            if (movements.isNotEmpty()) stockDao.insertAll(movements)

            details.payments.groupBy { it.method }.forEach { (method, group) ->
                val gross = group.sumOf { it.amount }
                val amount =
                    if (method == PaymentMethod.CASH) gross - details.sale.changeAmount else gross
                if (amount <= 0) return@forEach
                cashDao.insert(
                    CashFlowEntity(
                        type = CashFlowType.OUT,
                        amount = amount,
                        category = CashCategories.REFUND,
                        note = "Void ${details.sale.invoiceNo} • ${method.name}",
                        source = CashFlowSource.REFUND,
                        referenceId = saleId,
                        shiftId = details.sale.shiftId,
                        affectsCashDrawer = method.affectsCashDrawer,
                        createdAt = now
                    )
                )
            }
        }
    }

    private fun CartLine.toItemEntity(saleId: Long) = SaleItemEntity(
        saleId = saleId,
        productId = product.id,
        productName = product.name,
        unitPrice = product.sellPrice,
        costPrice = product.costPrice,
        unit = product.unit,
        qty = qty,
        lineDiscount = manualDiscount,
        promoDiscount = promoDiscount,
        promoId = promo?.promoId,
        promoName = promo?.name.orEmpty(),
        lineTotal = lineTotal,
        note = note
    )
}

package com.rzk.kasirpro.data.repository

import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.model.CashCategoryTotal
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowSummary
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.DailyTotal
import kotlinx.coroutines.flow.Flow

/** Preset buckets offered in the cash in/out sheet. Free text is still allowed. */
object CashCategories {
    const val SALES = "Sales"
    const val REFUND = "Refund"
    const val OPENING_FLOAT = "Opening float"
    const val CASH_VARIANCE = "Cash variance"
    const val STOCK_PURCHASE = "Stock purchase"

    val cashIn = listOf(
        SALES,
        "Owner capital",
        "Loan received",
        "Other income",
        OPENING_FLOAT
    )

    val cashOut = listOf(
        STOCK_PURCHASE,
        "Salary",
        "Rent",
        "Utilities",
        "Transport",
        "Supplies",
        "Maintenance",
        "Tax & fees",
        "Owner draw",
        "Other expense"
    )
}

/**
 * The cash ledger. Manual entries are user-owned and editable; anything auto-posted by a
 * sale, refund or shift is read-only so the books can't be quietly rewritten.
 */
class CashFlowRepository(db: KasirDatabase) {

    private val dao = db.cashFlowDao()

    fun observeEntries(
        period: TimePeriod,
        type: CashFlowType? = null,
        manualOnly: Boolean = false
    ): Flow<List<CashFlowEntity>> =
        dao.observeEntries(period.from, period.to, type?.name ?: "ALL", manualOnly)

    fun observeRecent(limit: Int = 6): Flow<List<CashFlowEntity>> = dao.observeRecent(limit)

    fun observeSummary(period: TimePeriod): Flow<CashFlowSummary> =
        dao.observeSummary(period.from, period.to)

    /** Physical cash expected in the drawer right now, across all shifts. */
    fun observeCashOnHand(): Flow<Long> = dao.observeCashOnHand()

    fun observeExpenseByCategory(period: TimePeriod): Flow<List<CashCategoryTotal>> =
        dao.observeByCategory(CashFlowType.OUT.name, period.from, period.to)

    fun observeIncomeByCategory(period: TimePeriod): Flow<List<CashCategoryTotal>> =
        dao.observeByCategory(CashFlowType.IN.name, period.from, period.to)

    fun observeDailyInOut(period: TimePeriod): Flow<List<DailyTotal>> =
        dao.observeDailyInOut(period.from, period.to)

    fun observeUsedCategories(type: CashFlowType): Flow<List<String>> =
        dao.observeUsedCategories(type.name)

    suspend fun getById(id: Long): CashFlowEntity? = dao.getById(id)

    suspend fun addManual(
        type: CashFlowType,
        amount: Long,
        category: String,
        note: String,
        affectsCashDrawer: Boolean,
        shiftId: Long?,
        at: Long = System.currentTimeMillis()
    ): Result<Long> = runCatching {
        require(amount > 0) { "Amount must be greater than zero" }
        dao.insert(
            CashFlowEntity(
                type = type,
                amount = amount,
                category = category.trim().ifBlank { "Other" },
                note = note.trim(),
                source = CashFlowSource.MANUAL,
                shiftId = shiftId,
                affectsCashDrawer = affectsCashDrawer,
                createdAt = at
            )
        )
    }

    suspend fun update(entry: CashFlowEntity): Result<Unit> = runCatching {
        require(entry.source == CashFlowSource.MANUAL) { "Auto-generated entries are read-only" }
        require(entry.amount > 0) { "Amount must be greater than zero" }
        dao.update(entry)
    }

    suspend fun delete(entry: CashFlowEntity): Result<Unit> = runCatching {
        require(entry.source == CashFlowSource.MANUAL) { "Auto-generated entries are read-only" }
        dao.delete(entry)
    }
}

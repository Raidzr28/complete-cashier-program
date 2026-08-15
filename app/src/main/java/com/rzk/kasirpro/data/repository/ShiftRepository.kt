package com.rzk.kasirpro.data.repository

import androidx.room.withTransaction
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.local.entity.ShiftEntity
import com.rzk.kasirpro.data.model.CashFlowSource
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.ShiftStatus
import kotlinx.coroutines.flow.Flow

/**
 * Cashier sessions. Opening a shift posts the float into the drawer; closing it compares
 * the counted cash against what the ledger says should be there and records the variance.
 */
class ShiftRepository(private val db: KasirDatabase) {

    private val shiftDao = db.shiftDao()
    private val cashDao = db.cashFlowDao()

    fun observeOpenShift(): Flow<ShiftEntity?> = shiftDao.observeOpenShift()

    fun observeHistory(limit: Int = 100): Flow<List<ShiftEntity>> = shiftDao.observeHistory(limit)

    suspend fun getOpenShift(): ShiftEntity? = shiftDao.getOpenShift()

    suspend fun openShift(cashierName: String, openingCash: Long): Result<Long> = runCatching {
        require(openingCash >= 0) { "Opening cash cannot be negative" }
        db.withTransaction {
            check(shiftDao.getOpenShift() == null) { "A shift is already open" }
            val now = System.currentTimeMillis()
            val id = shiftDao.insert(
                ShiftEntity(
                    cashierName = cashierName.trim().ifBlank { "Cashier" },
                    openingCash = openingCash,
                    openedAt = now,
                    status = ShiftStatus.OPEN
                )
            )
            if (openingCash > 0) {
                cashDao.insert(
                    CashFlowEntity(
                        type = CashFlowType.IN,
                        amount = openingCash,
                        category = CashCategories.OPENING_FLOAT,
                        note = "Shift #$id opened",
                        source = CashFlowSource.SHIFT_OPENING,
                        referenceId = id,
                        shiftId = id,
                        affectsCashDrawer = true,
                        createdAt = now
                    )
                )
            }
            id
        }
    }

    /**
     * Expected cash is derived from the ledger rows tagged with this shift — not from a
     * running counter — so it stays correct even if the app was killed mid-shift.
     * A non-zero variance is posted as its own ledger row so cash-on-hand matches reality
     * going into the next shift.
     */
    suspend fun closeShift(shiftId: Long, actualCash: Long, note: String): Result<ShiftEntity> =
        runCatching {
            db.withTransaction {
                val shift = shiftDao.getById(shiftId) ?: error("Shift not found")
                check(shift.status == ShiftStatus.OPEN) { "Shift is already closed" }

                val now = System.currentTimeMillis()
                val expected = cashDao.drawerNetForShift(shiftId)
                val difference = actualCash - expected

                val closed = shift.copy(
                    closedAt = now,
                    expectedCash = expected,
                    actualCash = actualCash,
                    difference = difference,
                    status = ShiftStatus.CLOSED,
                    note = note.trim()
                )
                shiftDao.update(closed)

                if (difference != 0L) {
                    cashDao.insert(
                        CashFlowEntity(
                            type = if (difference > 0) CashFlowType.IN else CashFlowType.OUT,
                            amount = kotlin.math.abs(difference),
                            category = CashCategories.CASH_VARIANCE,
                            note = if (difference > 0) "Shift #$shiftId over" else "Shift #$shiftId short",
                            source = CashFlowSource.SHIFT_CLOSING,
                            referenceId = shiftId,
                            shiftId = shiftId,
                            affectsCashDrawer = true,
                            createdAt = now
                        )
                    )
                }
                closed
            }
        }

    /** What the drawer should hold for the currently open shift. */
    suspend fun expectedCashFor(shiftId: Long): Long = cashDao.drawerNetForShift(shiftId)
}

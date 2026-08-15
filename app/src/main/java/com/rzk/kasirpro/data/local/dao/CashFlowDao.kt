package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.model.CashCategoryTotal
import com.rzk.kasirpro.data.model.CashFlowSummary
import com.rzk.kasirpro.data.model.DailyTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface CashFlowDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CashFlowEntity): Long

    @Update
    suspend fun update(entry: CashFlowEntity)

    @Delete
    suspend fun delete(entry: CashFlowEntity)

    @Query("SELECT * FROM cash_flows WHERE id = :id")
    suspend fun getById(id: Long): CashFlowEntity?

    /** Removes the auto-posted rows for a sale when it gets voided. */
    @Query("DELETE FROM cash_flows WHERE referenceId = :saleId AND source = 'SALE'")
    suspend fun deleteForSale(saleId: Long)

    @Query(
        """
        SELECT * FROM cash_flows
        WHERE createdAt BETWEEN :from AND :to
          AND (:type = 'ALL' OR type = :type)
          AND (:manualOnly = 0 OR source = 'MANUAL')
        ORDER BY createdAt DESC
        """
    )
    fun observeEntries(from: Long, to: Long, type: String, manualOnly: Boolean): Flow<List<CashFlowEntity>>

    @Query("SELECT * FROM cash_flows ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CashFlowEntity>>

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) AS totalIn,
               IFNULL(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) AS totalOut,
               IFNULL(SUM(CASE WHEN type = 'IN' AND affectsCashDrawer = 1 THEN amount ELSE 0 END), 0) AS drawerIn,
               IFNULL(SUM(CASE WHEN type = 'OUT' AND affectsCashDrawer = 1 THEN amount ELSE 0 END), 0) AS drawerOut
        FROM cash_flows
        WHERE createdAt BETWEEN :from AND :to
        """
    )
    fun observeSummary(from: Long, to: Long): Flow<CashFlowSummary>

    /**
     * Cash physically in the drawer right now, across all time. Opening floats are posted
     * as IN rows when a shift starts, so this single expression is the whole answer.
     */
    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'IN' THEN amount ELSE -amount END), 0)
        FROM cash_flows WHERE affectsCashDrawer = 1
        """
    )
    fun observeCashOnHand(): Flow<Long>

    @Query(
        """
        SELECT IFNULL(SUM(CASE WHEN type = 'IN' THEN amount ELSE -amount END), 0)
        FROM cash_flows WHERE affectsCashDrawer = 1 AND shiftId = :shiftId
        """
    )
    suspend fun drawerNetForShift(shiftId: Long): Long

    @Query(
        """
        SELECT category AS category,
               IFNULL(SUM(amount), 0) AS total,
               COUNT(*) AS entries
        FROM cash_flows
        WHERE type = :type AND createdAt BETWEEN :from AND :to
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeByCategory(type: String, from: Long, to: Long): Flow<List<CashCategoryTotal>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') AS dayKey,
               IFNULL(SUM(CASE WHEN type = 'IN' THEN amount ELSE 0 END), 0) AS total,
               COUNT(*) AS orders,
               IFNULL(SUM(CASE WHEN type = 'OUT' THEN amount ELSE 0 END), 0) AS profit
        FROM cash_flows
        WHERE createdAt BETWEEN :from AND :to
        GROUP BY dayKey ORDER BY dayKey ASC
        """
    )
    fun observeDailyInOut(from: Long, to: Long): Flow<List<DailyTotal>>

    /** Distinct categories the user has actually used, to prefill the picker. */
    @Query("SELECT DISTINCT category FROM cash_flows WHERE category != '' AND type = :type ORDER BY category ASC")
    fun observeUsedCategories(type: String): Flow<List<String>>
}

package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rzk.kasirpro.data.local.entity.SaleEntity
import com.rzk.kasirpro.data.local.entity.SaleItemEntity
import com.rzk.kasirpro.data.local.entity.SalePaymentEntity
import com.rzk.kasirpro.data.model.CategorySalesStat
import com.rzk.kasirpro.data.model.DailyTotal
import com.rzk.kasirpro.data.model.HourlyTotal
import com.rzk.kasirpro.data.model.PaymentBreakdown
import com.rzk.kasirpro.data.model.ProductSalesStat
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.data.model.SalesSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    // ---------------------------------------------------------------- writes

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayments(payments: List<SalePaymentEntity>)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSale(saleId: Long)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsOf(saleId: Long)

    @Query("DELETE FROM sale_payments WHERE saleId = :saleId")
    suspend fun deletePaymentsOf(saleId: Long)

    @Query("UPDATE sales SET status = :status, voidedAt = :at, voidReason = :reason WHERE id = :saleId")
    suspend fun markStatus(saleId: Long, status: String, at: Long?, reason: String)

    // ---------------------------------------------------------------- reads

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getDetails(id: Long): SaleWithDetails?

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun observeDetails(id: Long): Flow<SaleWithDetails?>

    @Transaction
    @Query(
        """
        SELECT * FROM sales
        WHERE status != 'HELD'
          AND createdAt BETWEEN :from AND :to
          AND (:query = '' OR invoiceNo LIKE '%' || :query || '%'
                           OR customerName LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeHistory(from: Long, to: Long, query: String, limit: Int): Flow<List<SaleWithDetails>>

    @Transaction
    @Query("SELECT * FROM sales WHERE status = 'COMPLETED' ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SaleWithDetails>>

    /** Parked carts waiting to be resumed. */
    @Transaction
    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY createdAt DESC")
    fun observeHeld(): Flow<List<SaleWithDetails>>

    @Query("SELECT COUNT(*) FROM sales WHERE status = 'HELD'")
    fun observeHeldCount(): Flow<Int>

    @Query("SELECT invoiceNo FROM sales ORDER BY id DESC LIMIT 1")
    suspend fun lastInvoiceNo(): String?

    // ---------------------------------------------------------------- summaries

    @Query(
        """
        SELECT COUNT(*) AS orders,
               IFNULL(SUM(subtotal + promoDiscount + orderDiscount), 0) AS gross,
               IFNULL(SUM(promoDiscount + orderDiscount), 0) AS discount,
               IFNULL(SUM(taxAmount), 0) AS tax,
               IFNULL(SUM(total), 0) AS net,
               IFNULL(SUM(totalCost), 0) AS cost,
               IFNULL((SELECT SUM(si.qty) FROM sale_items si
                       INNER JOIN sales s2 ON s2.id = si.saleId
                       WHERE s2.status = 'COMPLETED' AND s2.createdAt BETWEEN :from AND :to), 0) AS itemsSold
        FROM sales
        WHERE status = 'COMPLETED' AND createdAt BETWEEN :from AND :to
        """
    )
    fun observeSummary(from: Long, to: Long): Flow<SalesSummary>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') AS dayKey,
               IFNULL(SUM(total), 0) AS total,
               COUNT(*) AS orders,
               IFNULL(SUM(total - taxAmount - totalCost), 0) AS profit
        FROM sales
        WHERE status = 'COMPLETED' AND createdAt BETWEEN :from AND :to
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """
    )
    fun observeDailyTotals(from: Long, to: Long): Flow<List<DailyTotal>>

    /** Which hours of the day actually sell — drives the "busiest hour" insight. */
    @Query(
        """
        SELECT strftime('%H', createdAt / 1000, 'unixepoch', 'localtime') AS hourKey,
               IFNULL(SUM(total), 0) AS total,
               COUNT(*) AS orders
        FROM sales
        WHERE status = 'COMPLETED' AND createdAt BETWEEN :from AND :to
        GROUP BY hourKey
        ORDER BY hourKey ASC
        """
    )
    fun observeHourlyTotals(from: Long, to: Long): Flow<List<HourlyTotal>>

    @Query(
        """
        SELECT sp.method AS method,
               IFNULL(SUM(sp.amount), 0) AS total,
               COUNT(*) AS count
        FROM sale_payments sp
        INNER JOIN sales s ON s.id = sp.saleId
        WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :from AND :to
        GROUP BY sp.method
        ORDER BY total DESC
        """
    )
    fun observePaymentBreakdown(from: Long, to: Long): Flow<List<PaymentBreakdown>>

    // ---------------------------------------------------------------- product statistics

    /**
     * Ranked product performance. [ascending] flips the same query between best-sellers and
     * worst-sellers so both lists stay consistent with each other.
     */
    @Query(
        """
        SELECT si.productId AS productId,
               si.productName AS productName,
               IFNULL(SUM(si.qty), 0) AS qtySold,
               IFNULL(SUM(si.lineTotal), 0) AS revenue,
               IFNULL(SUM(si.costPrice * si.qty), 0) AS cost,
               IFNULL(SUM(si.lineTotal) - SUM(si.costPrice * si.qty), 0) AS profit,
               COUNT(DISTINCT si.saleId) AS orderCount
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.saleId
        WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :from AND :to
        GROUP BY si.productId, si.productName
        ORDER BY
            CASE WHEN :ascending = 1 THEN SUM(si.qty) END ASC,
            CASE WHEN :ascending = 0 THEN SUM(si.qty) END DESC
        LIMIT :limit
        """
    )
    fun observeProductRanking(
        from: Long,
        to: Long,
        ascending: Boolean,
        limit: Int
    ): Flow<List<ProductSalesStat>>

    @Query(
        """
        SELECT si.productId AS productId,
               si.productName AS productName,
               IFNULL(SUM(si.qty), 0) AS qtySold,
               IFNULL(SUM(si.lineTotal), 0) AS revenue,
               IFNULL(SUM(si.costPrice * si.qty), 0) AS cost,
               IFNULL(SUM(si.lineTotal) - SUM(si.costPrice * si.qty), 0) AS profit,
               COUNT(DISTINCT si.saleId) AS orderCount
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.saleId
        WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :from AND :to
        GROUP BY si.productId, si.productName
        ORDER BY profit DESC
        LIMIT :limit
        """
    )
    fun observeMostProfitable(from: Long, to: Long, limit: Int): Flow<List<ProductSalesStat>>

    /**
     * Active products with zero sales in the window — dead stock tying up capital.
     * Reported as a stat row with zeroes so it renders in the same table as the rankings.
     */
    @Query(
        """
        SELECT p.id AS productId, p.name AS productName,
               0 AS qtySold, 0 AS revenue, 0 AS cost, 0 AS profit, 0 AS orderCount
        FROM products p
        WHERE p.isActive = 1
          AND p.id NOT IN (
              SELECT IFNULL(si.productId, -1) FROM sale_items si
              INNER JOIN sales s ON s.id = si.saleId
              WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :from AND :to
          )
        ORDER BY p.stock DESC, p.name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun observeNeverSold(from: Long, to: Long, limit: Int): Flow<List<ProductSalesStat>>

    @Query(
        """
        SELECT c.id AS categoryId,
               IFNULL(c.name, 'Uncategorised') AS categoryName,
               IFNULL(SUM(si.qty), 0) AS qtySold,
               IFNULL(SUM(si.lineTotal), 0) AS revenue,
               IFNULL(SUM(si.lineTotal) - SUM(si.costPrice * si.qty), 0) AS profit
        FROM sale_items si
        INNER JOIN sales s ON s.id = si.saleId
        LEFT JOIN products p ON p.id = si.productId
        LEFT JOIN categories c ON c.id = p.categoryId
        WHERE s.status = 'COMPLETED' AND s.createdAt BETWEEN :from AND :to
        GROUP BY c.id, c.name
        ORDER BY revenue DESC
        """
    )
    fun observeCategoryStats(from: Long, to: Long): Flow<List<CategorySalesStat>>

    // ---------------------------------------------------------------- shift scoped

    @Query(
        """
        SELECT COUNT(*) AS orders,
               IFNULL(SUM(subtotal + promoDiscount + orderDiscount), 0) AS gross,
               IFNULL(SUM(promoDiscount + orderDiscount), 0) AS discount,
               IFNULL(SUM(taxAmount), 0) AS tax,
               IFNULL(SUM(total), 0) AS net,
               IFNULL(SUM(totalCost), 0) AS cost,
               0 AS itemsSold
        FROM sales
        WHERE status = 'COMPLETED' AND shiftId = :shiftId
        """
    )
    fun observeShiftSummary(shiftId: Long): Flow<SalesSummary>
}

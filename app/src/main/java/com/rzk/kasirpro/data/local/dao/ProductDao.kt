package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rzk.kasirpro.data.local.entity.ProductEntity
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.StockValuation
import kotlinx.coroutines.flow.Flow

private const val PRODUCT_JOIN = """
    SELECT p.*, c.name AS categoryName, c.colorArgb AS categoryColor
    FROM products p LEFT JOIN categories c ON c.id = p.categoryId
"""

@Dao
interface ProductDao {

    @Query("$PRODUCT_JOIN WHERE p.isActive = 1 ORDER BY p.name COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<ProductWithCategory>>

    @Query("$PRODUCT_JOIN ORDER BY p.isActive DESC, p.name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProductWithCategory>>

    @Query(
        """
        $PRODUCT_JOIN
        WHERE p.isActive = 1
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:query = '' OR p.name LIKE '%' || :query || '%'
                           OR p.sku LIKE '%' || :query || '%'
                           OR p.barcode LIKE '%' || :query || '%')
        ORDER BY p.name COLLATE NOCASE ASC
        """
    )
    fun search(query: String, categoryId: Long?): Flow<List<ProductWithCategory>>

    @Query("$PRODUCT_JOIN WHERE p.id = :id")
    fun observeById(id: Long): Flow<ProductWithCategory?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND barcode != '' AND isActive = 1 LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ProductEntity>

    /** Products at or below their reorder point. Powers the dashboard alert. */
    @Query(
        """
        $PRODUCT_JOIN
        WHERE p.isActive = 1 AND p.trackStock = 1 AND p.stock <= p.minStock
        ORDER BY (p.stock - p.minStock) ASC, p.name COLLATE NOCASE ASC
        """
    )
    fun observeLowStock(): Flow<List<ProductWithCategory>>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND trackStock = 1 AND stock <= minStock")
    fun observeLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    fun observeProductCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) AS productCount,
               IFNULL(SUM(stock), 0) AS totalUnits,
               IFNULL(SUM(stock * costPrice), 0) AS costValue,
               IFNULL(SUM(stock * sellPrice), 0) AS retailValue
        FROM products WHERE isActive = 1 AND trackStock = 1
        """
    )
    fun observeStockValuation(): Flow<StockValuation>

    /**
     * Applies a signed delta. The `trackStock = 1` guard means service items silently
     * ignore stock movements instead of drifting negative.
     */
    @Query("UPDATE products SET stock = stock + :delta, updatedAt = :now WHERE id = :id AND trackStock = 1")
    suspend fun applyStockDelta(id: Long, delta: Int, now: Long)

    @Query("UPDATE products SET stock = :stock, updatedAt = :now WHERE id = :id")
    suspend fun setStock(id: Long, stock: Int, now: Long)

    @Query("UPDATE products SET costPrice = :cost, updatedAt = :now WHERE id = :id")
    suspend fun setCostPrice(id: Long, cost: Long, now: Long)

    @Query("UPDATE products SET isActive = :active, updatedAt = :now WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)
}

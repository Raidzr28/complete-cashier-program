package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(movements: List<StockMovementEntity>)

    @Query(
        """
        SELECT * FROM stock_movements
        WHERE (:productId IS NULL OR productId = :productId)
          AND createdAt BETWEEN :from AND :to
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeMovements(productId: Long?, from: Long, to: Long, limit: Int): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC LIMIT :limit")
    fun observeForProduct(productId: Long, limit: Int): Flow<List<StockMovementEntity>>

    @Query(
        """
        SELECT IFNULL(SUM(qty), 0) FROM stock_movements
        WHERE productId = :productId AND qty > 0 AND createdAt BETWEEN :from AND :to
        """
    )
    suspend fun totalReceived(productId: Long, from: Long, to: Long): Int

    @Query("DELETE FROM stock_movements WHERE productId = :productId")
    suspend fun deleteForProduct(productId: Long)
}

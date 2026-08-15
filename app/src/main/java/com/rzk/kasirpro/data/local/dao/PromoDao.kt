package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rzk.kasirpro.data.local.entity.PromoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(promo: PromoEntity): Long

    @Update
    suspend fun update(promo: PromoEntity)

    @Delete
    suspend fun delete(promo: PromoEntity)

    @Query("SELECT * FROM promos ORDER BY isActive DESC, endAt DESC")
    fun observeAll(): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE id = :id")
    suspend fun getById(id: Long): PromoEntity?

    /**
     * Promos whose calendar window contains [now]. The weekday / happy-hour part of the
     * rule is evaluated in Kotlin because SQLite can't express it cheaply.
     */
    @Query("SELECT * FROM promos WHERE isActive = 1 AND startAt <= :now AND endAt > :now")
    fun observeLive(now: Long): Flow<List<PromoEntity>>

    @Query("SELECT * FROM promos WHERE isActive = 1 AND startAt <= :now AND endAt > :now")
    suspend fun getLive(now: Long): List<PromoEntity>

    @Query("SELECT COUNT(*) FROM promos WHERE isActive = 1 AND startAt <= :now AND endAt > :now")
    fun observeLiveCount(now: Long): Flow<Int>

    @Query("UPDATE promos SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}

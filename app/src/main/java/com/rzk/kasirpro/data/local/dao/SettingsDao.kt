package com.rzk.kasirpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/** Settings live in a single pinned row, id = [SettingsEntity.SETTINGS_ID] (1). */
@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: SettingsEntity)

    @Update
    suspend fun update(settings: SettingsEntity)

    /**
     * Bumps the daily invoice counter. Callers run this inside the same transaction as the
     * sale insert, so two concurrent checkouts can't mint the same receipt number.
     */
    @Query(
        """
        UPDATE settings
        SET invoiceSequence = CASE WHEN invoiceDateKey = :dateKey THEN invoiceSequence + 1 ELSE 1 END,
            invoiceDateKey = :dateKey
        WHERE id = 1
        """
    )
    suspend fun bumpInvoiceSequence(dateKey: String)

    @Query("SELECT invoiceSequence FROM settings WHERE id = 1")
    suspend fun currentInvoiceSequence(): Int
}

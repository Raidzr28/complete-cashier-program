package com.rzk.kasirpro.data.repository

import com.rzk.kasirpro.data.local.DatabaseSeeder
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val db: KasirDatabase) {

    private val dao = db.settingsDao()

    /**
     * Never emits null. If the seed callback hasn't run yet (fresh install, first frame),
     * callers still get sane defaults instead of having to null-check on every screen.
     */
    val settings: Flow<SettingsEntity> = dao.observe().map { it ?: SettingsEntity() }

    suspend fun get(): SettingsEntity = dao.get() ?: SettingsEntity().also { dao.upsert(it) }

    /** Makes sure the pinned row exists before anything tries to bump the invoice counter. */
    suspend fun ensureInitialised() {
        if (dao.get() == null) dao.upsert(SettingsEntity())
    }

    suspend fun update(transform: (SettingsEntity) -> SettingsEntity): Result<Unit> = runCatching {
        val current = get()
        dao.upsert(transform(current).copy(id = SettingsEntity.SETTINGS_ID))
    }

    /** Settings → "Clear all data". Keeps the store profile, drops catalogue and history. */
    suspend fun clearBusinessData(): Result<Unit> = runCatching { DatabaseSeeder.clearAll(db) }

    /** Re-installs the demo catalogue after a wipe. */
    suspend fun restoreSampleData(): Result<Unit> = runCatching { DatabaseSeeder.seed(db) }
}

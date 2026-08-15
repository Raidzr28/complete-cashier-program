package com.rzk.kasirpro.data.repository

import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.local.entity.PromoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Temporary discounts. See [com.rzk.kasirpro.domain.PromoEngine] for how they're applied. */
class PromoRepository(db: KasirDatabase) {

    private val dao = db.promoDao()

    fun observeAll(): Flow<List<PromoEntity>> = dao.observeAll()

    /**
     * Promos inside their calendar window. The weekday / happy-hour test is left to the
     * engine so the list stays stable while the clock moves — re-querying the database
     * every minute just to hide a chip would be wasteful.
     */
    fun observeLive(now: Long = System.currentTimeMillis()): Flow<List<PromoEntity>> =
        dao.observeLive(now)

    fun observeActiveCount(now: Long = System.currentTimeMillis()): Flow<Int> =
        dao.observeLive(now).map { it.size }

    suspend fun getLive(now: Long = System.currentTimeMillis()): List<PromoEntity> = dao.getLive(now)

    suspend fun getById(id: Long): PromoEntity? = dao.getById(id)

    suspend fun save(promo: PromoEntity): Result<Long> = runCatching {
        require(promo.name.isNotBlank()) { "Promo name is required" }
        require(promo.value > 0) { "Discount value must be greater than zero" }
        require(promo.endAt > promo.startAt) { "End date must be after the start date" }
        if (promo.id == 0L) dao.insert(promo) else {
            dao.update(promo); promo.id
        }
    }

    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)

    suspend fun delete(promo: PromoEntity) = dao.delete(promo)
}

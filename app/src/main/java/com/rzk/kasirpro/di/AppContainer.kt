package com.rzk.kasirpro.di

import android.content.Context
import com.rzk.kasirpro.data.local.KasirDatabase
import com.rzk.kasirpro.data.repository.CashFlowRepository
import com.rzk.kasirpro.data.repository.CatalogRepository
import com.rzk.kasirpro.data.repository.PromoRepository
import com.rzk.kasirpro.data.repository.SalesRepository
import com.rzk.kasirpro.data.repository.SettingsRepository
import com.rzk.kasirpro.data.repository.ShiftRepository
import com.rzk.kasirpro.data.repository.StockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * Hand-rolled dependency container.
 *
 * The graph here is small and entirely singleton-shaped, so a DI framework would add a
 * build-time annotation processor and a layer of indirection for no real benefit. Everything
 * is lazy, so opening the database is deferred until the first screen actually needs it.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Outlives any single screen; used for database seeding and fire-and-forget writes. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: KasirDatabase by lazy { KasirDatabase.get(appContext, applicationScope) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(database) }
    val catalogRepository: CatalogRepository by lazy { CatalogRepository(database) }
    val stockRepository: StockRepository by lazy { StockRepository(database) }
    val promoRepository: PromoRepository by lazy { PromoRepository(database) }
    val salesRepository: SalesRepository by lazy { SalesRepository(database) }
    val cashFlowRepository: CashFlowRepository by lazy { CashFlowRepository(database) }
    val shiftRepository: ShiftRepository by lazy { ShiftRepository(database) }
}

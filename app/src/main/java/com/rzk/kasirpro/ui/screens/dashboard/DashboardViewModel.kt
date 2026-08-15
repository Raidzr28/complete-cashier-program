package com.rzk.kasirpro.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.core.percentChange
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.local.entity.ShiftEntity
import com.rzk.kasirpro.data.model.DailyTotal
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.data.model.ProductSalesStat
import com.rzk.kasirpro.data.model.ProductWithCategory
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.data.model.SalesSummary
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val today: SalesSummary = SalesSummary(),
    val yesterday: SalesSummary = SalesSummary(),
    val cashOnHand: Long = 0,
    val weekTotals: List<DailyTotal> = emptyList(),
    val recentSales: List<SaleWithDetails> = emptyList(),
    val lowStock: List<ProductWithCategory> = emptyList(),
    val openShift: ShiftEntity? = null,
    val livePromoCount: Int = 0,
    val topSellerToday: ProductSalesStat? = null
) {
    val revenueDelta: Double get() = percentChange(today.net, yesterday.net)
    val orderDelta: Double get() = percentChange(today.orders.toLong(), yesterday.orders.toLong())
    val profitDelta: Double get() = percentChange(today.profit, yesterday.profit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(container: AppContainer) : ViewModel() {

    private val sales = container.salesRepository
    private val catalog = container.catalogRepository
    private val cash = container.cashFlowRepository
    private val shifts = container.shiftRepository
    private val promos = container.promoRepository
    private val settingsRepo = container.settingsRepository

    private val today = TimePeriod.of(PeriodPreset.TODAY)
    private val yesterday = TimePeriod.of(PeriodPreset.YESTERDAY)
    private val week = TimePeriod.of(PeriodPreset.WEEK)

    // Split into two combines because combine() only overloads up to five flows.
    private val salesSignals = combine(
        sales.observeSummary(today),
        sales.observeSummary(yesterday),
        sales.observeDailyTotals(week),
        sales.observeRecent(5),
        sales.observeBestSellers(today, limit = 1)
    ) { todaySummary, yesterdaySummary, weekTotals, recent, best ->
        SalesSignals(todaySummary, yesterdaySummary, weekTotals, recent, best.firstOrNull())
    }

    private val shopSignals = combine(
        settingsRepo.settings,
        cash.observeCashOnHand(),
        catalog.observeLowStock(),
        shifts.observeOpenShift(),
        promos.observeActiveCount()
    ) { settings, cashOnHand, lowStock, shift, promoCount ->
        ShopSignals(settings, cashOnHand, lowStock, shift, promoCount)
    }

    val uiState: StateFlow<DashboardUiState> =
        combine(salesSignals, shopSignals) { s, shop ->
            DashboardUiState(
                settings = shop.settings,
                today = s.today,
                yesterday = s.yesterday,
                cashOnHand = shop.cashOnHand,
                weekTotals = s.weekTotals,
                recentSales = s.recent,
                lowStock = shop.lowStock,
                openShift = shop.shift,
                livePromoCount = shop.promoCount,
                topSellerToday = s.topSeller
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    /** Chart buckets for the last 7 days, with zero-sale days kept so the axis stays even. */
    fun weekChartKeys(): List<String> = TimePeriod.dayKeys(week)

    private data class SalesSignals(
        val today: SalesSummary,
        val yesterday: SalesSummary,
        val weekTotals: List<DailyTotal>,
        val recent: List<SaleWithDetails>,
        val topSeller: ProductSalesStat?
    )

    private data class ShopSignals(
        val settings: SettingsEntity,
        val cashOnHand: Long,
        val lowStock: List<ProductWithCategory>,
        val shift: ShiftEntity?,
        val promoCount: Int
    )
}

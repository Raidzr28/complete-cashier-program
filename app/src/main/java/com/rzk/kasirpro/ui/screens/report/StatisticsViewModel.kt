package com.rzk.kasirpro.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.CategorySalesStat
import com.rzk.kasirpro.data.model.HourlyTotal
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.data.model.ProductSalesStat
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** How the product tables are ordered. Applies to best *and* worst so they stay comparable. */
enum class RankMetric { QUANTITY, REVENUE, PROFIT }

data class StatisticsUiState(
    val period: TimePeriod = TimePeriod.of(PeriodPreset.WEEK),
    val metric: RankMetric = RankMetric.QUANTITY,
    val best: List<ProductSalesStat> = emptyList(),
    val worst: List<ProductSalesStat> = emptyList(),
    val neverSold: List<ProductSalesStat> = emptyList(),
    val categories: List<CategorySalesStat> = emptyList(),
    val hourly: List<HourlyTotal> = emptyList(),
    val settings: SettingsEntity = SettingsEntity()
) {
    val busiestHour: HourlyTotal? get() = hourly.maxByOrNull { it.total }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(container: AppContainer) : ViewModel() {

    private val sales = container.salesRepository

    private val period = MutableStateFlow(TimePeriod.of(PeriodPreset.WEEK))
    private val metric = MutableStateFlow(RankMetric.QUANTITY)

    /**
     * One query returns every product that sold in the window; best and worst are then two
     * orderings of the same list. Ranking in memory keeps the two tables guaranteed
     * consistent — with separate ASC/DESC queries a product could appear in both.
     */
    private val allStats = period.flatMapLatest { p ->
        sales.observeBestSellers(p, limit = 500)
    }

    private val extras = period.flatMapLatest { p ->
        combine(
            sales.observeNeverSold(p, limit = 25),
            sales.observeCategoryStats(p),
            sales.observeHourlyTotals(p)
        ) { neverSold, categories, hourly -> Triple(neverSold, categories, hourly) }
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        allStats,
        extras,
        container.settingsRepository.settings,
        period,
        metric
    ) { stats, extra, settings, currentPeriod, currentMetric ->
        val selector: (ProductSalesStat) -> Long = when (currentMetric) {
            RankMetric.QUANTITY -> { stat -> stat.qtySold.toLong() }
            RankMetric.REVENUE -> { stat -> stat.revenue }
            RankMetric.PROFIT -> { stat -> stat.profit }
        }
        val sorted = stats.sortedByDescending(selector)
        StatisticsUiState(
            period = currentPeriod,
            metric = currentMetric,
            best = sorted.take(10),
            // Take from the tail rather than re-sorting ascending, so a product can never
            // land in both tables when fewer than 20 products sold.
            worst = sorted.asReversed().take(10).filterNot { slow ->
                sorted.take(10).any { it.productId == slow.productId }
            },
            neverSold = extra.first,
            categories = extra.second,
            hourly = extra.third,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun setPeriod(preset: PeriodPreset) { period.value = TimePeriod.of(preset) }

    fun setMetric(value: RankMetric) { metric.value = value }
}

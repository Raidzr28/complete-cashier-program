package com.rzk.kasirpro.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.core.percentChange
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.CashCategoryTotal
import com.rzk.kasirpro.data.model.CashFlowSummary
import com.rzk.kasirpro.data.model.DailyTotal
import com.rzk.kasirpro.data.model.PaymentBreakdown
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.data.model.SalesSummary
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ReportUiState(
    val period: TimePeriod = TimePeriod.of(PeriodPreset.TODAY),
    val summary: SalesSummary = SalesSummary(),
    val previous: SalesSummary = SalesSummary(),
    val dailyTotals: List<DailyTotal> = emptyList(),
    val payments: List<PaymentBreakdown> = emptyList(),
    val cash: CashFlowSummary = CashFlowSummary(),
    val expenses: List<CashCategoryTotal> = emptyList(),
    val settings: SettingsEntity = SettingsEntity()
) {
    val revenueDelta: Double get() = percentChange(summary.net, previous.net)
    val profitDelta: Double get() = percentChange(summary.profit, previous.profit)
    val orderDelta: Double get() = percentChange(summary.orders.toLong(), previous.orders.toLong())
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(container: AppContainer) : ViewModel() {

    private val sales = container.salesRepository
    private val cash = container.cashFlowRepository

    private val period = MutableStateFlow(TimePeriod.of(PeriodPreset.TODAY))

    private val salesSide = period.flatMapLatest { p ->
        combine(
            sales.observeSummary(p),
            sales.observeSummary(TimePeriod.previous(p)),
            sales.observeDailyTotals(p),
            sales.observePaymentBreakdown(p)
        ) { summary, previous, daily, payments ->
            SalesSide(summary, previous, daily, payments)
        }
    }

    private val cashSide = period.flatMapLatest { p ->
        combine(
            cash.observeSummary(p),
            cash.observeExpenseByCategory(p)
        ) { summary, expenses -> summary to expenses }
    }

    val uiState: StateFlow<ReportUiState> = combine(
        salesSide,
        cashSide,
        container.settingsRepository.settings,
        period
    ) { s, c, settings, p ->
        ReportUiState(
            period = p,
            summary = s.summary,
            previous = s.previous,
            dailyTotals = s.daily,
            payments = s.payments,
            cash = c.first,
            expenses = c.second,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setPeriod(preset: PeriodPreset) { period.value = TimePeriod.of(preset) }

    fun dayKeys(): List<String> = TimePeriod.dayKeys(period.value)

    private data class SalesSide(
        val summary: SalesSummary,
        val previous: SalesSummary,
        val daily: List<DailyTotal>,
        val payments: List<PaymentBreakdown>
    )
}

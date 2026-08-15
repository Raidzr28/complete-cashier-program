package com.rzk.kasirpro.ui.screens.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.entity.CashFlowEntity
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.CashCategoryTotal
import com.rzk.kasirpro.data.model.CashFlowSummary
import com.rzk.kasirpro.data.model.CashFlowType
import com.rzk.kasirpro.data.model.DailyTotal
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CashFlowUiState(
    val period: TimePeriod = TimePeriod.of(PeriodPreset.TODAY),
    val typeFilter: CashFlowType? = null,
    val manualOnly: Boolean = false,
    val entries: List<CashFlowEntity> = emptyList(),
    val summary: CashFlowSummary = CashFlowSummary(),
    val cashOnHand: Long = 0,
    val expenseByCategory: List<CashCategoryTotal> = emptyList(),
    val dailyInOut: List<DailyTotal> = emptyList(),
    val settings: SettingsEntity = SettingsEntity()
)

sealed interface CashFlowEvent {
    data object Saved : CashFlowEvent
    data class Error(val message: String) : CashFlowEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class CashFlowViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.cashFlowRepository
    private val shifts = container.shiftRepository
    private val settingsRepo = container.settingsRepository

    private val period = MutableStateFlow(TimePeriod.of(PeriodPreset.TODAY))
    private val typeFilter = MutableStateFlow<CashFlowType?>(null)
    private val manualOnly = MutableStateFlow(false)

    private val events = Channel<CashFlowEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val filters = combine(period, typeFilter, manualOnly) { p, t, m -> Triple(p, t, m) }

    private val ledger = filters.flatMapLatest { (p, t, m) -> repo.observeEntries(p, t, m) }

    private val aggregates = period.flatMapLatest { p ->
        combine(
            repo.observeSummary(p),
            repo.observeExpenseByCategory(p),
            repo.observeDailyInOut(p)
        ) { summary, byCategory, daily -> Triple(summary, byCategory, daily) }
    }

    val uiState: StateFlow<CashFlowUiState> = combine(
        ledger,
        aggregates,
        repo.observeCashOnHand(),
        settingsRepo.settings,
        filters
    ) { entries, agg, onHand, settings, (p, t, m) ->
        CashFlowUiState(
            period = p,
            typeFilter = t,
            manualOnly = m,
            entries = entries,
            summary = agg.first,
            cashOnHand = onHand,
            expenseByCategory = agg.second,
            dailyInOut = agg.third,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashFlowUiState())

    fun setPeriod(preset: PeriodPreset) { period.value = TimePeriod.of(preset) }

    fun setTypeFilter(type: CashFlowType?) { typeFilter.value = type }

    fun setManualOnly(value: Boolean) { manualOnly.value = value }

    fun addEntry(
        type: CashFlowType,
        amount: Long,
        category: String,
        note: String,
        affectsCashDrawer: Boolean
    ) {
        viewModelScope.launch {
            // Tagging the entry with the open shift is what lets closing reconcile the drawer.
            val shiftId = shifts.getOpenShift()?.id
            repo.addManual(type, amount, category, note, affectsCashDrawer, shiftId)
                .onSuccess { events.send(CashFlowEvent.Saved) }
                .onFailure { events.send(CashFlowEvent.Error(it.message.orEmpty())) }
        }
    }

    fun updateEntry(entry: CashFlowEntity) {
        viewModelScope.launch {
            repo.update(entry)
                .onSuccess { events.send(CashFlowEvent.Saved) }
                .onFailure { events.send(CashFlowEvent.Error(it.message.orEmpty())) }
        }
    }

    fun deleteEntry(entry: CashFlowEntity) {
        viewModelScope.launch {
            repo.delete(entry)
                .onFailure { events.send(CashFlowEvent.Error(it.message.orEmpty())) }
        }
    }

    fun dayKeys(): List<String> = TimePeriod.dayKeys(period.value)
}

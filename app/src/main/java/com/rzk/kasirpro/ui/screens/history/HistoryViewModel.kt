package com.rzk.kasirpro.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val period: TimePeriod = TimePeriod.of(PeriodPreset.WEEK),
    val query: String = "",
    val sales: List<SaleWithDetails> = emptyList(),
    val settings: SettingsEntity = SettingsEntity()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HistoryViewModel(container: AppContainer) : ViewModel() {

    private val sales = container.salesRepository

    private val period = MutableStateFlow(TimePeriod.of(PeriodPreset.WEEK))
    private val query = MutableStateFlow("")

    // Debounced so typing an invoice number doesn't fire a query per keystroke.
    private val feed = combine(period, query.debounce(250)) { p, q -> p to q }
        .flatMapLatest { (p, q) -> sales.observeHistory(p, q) }

    val uiState: StateFlow<HistoryUiState> = combine(
        feed,
        container.settingsRepository.settings,
        period,
        query
    ) { list, settings, p, q ->
        HistoryUiState(period = p, query = q, sales = list, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setPeriod(preset: PeriodPreset) { period.value = TimePeriod.of(preset) }

    fun setQuery(value: String) { query.value = value }
}

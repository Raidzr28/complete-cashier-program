package com.rzk.kasirpro.ui.screens.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.core.TimePeriod
import com.rzk.kasirpro.data.local.entity.StockMovementEntity
import com.rzk.kasirpro.data.model.PeriodPreset
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class StockMovementUiState(
    val period: TimePeriod = TimePeriod.of(PeriodPreset.WEEK),
    val movements: List<StockMovementEntity> = emptyList(),
    val currencySymbol: String = "Rp"
)

@OptIn(ExperimentalCoroutinesApi::class)
class StockMovementViewModel(container: AppContainer) : ViewModel() {

    private val stock = container.stockRepository
    private val period = MutableStateFlow(TimePeriod.of(PeriodPreset.WEEK))

    val uiState: StateFlow<StockMovementUiState> = combine(
        period.flatMapLatest { stock.observeMovements(it) },
        container.settingsRepository.settings,
        period
    ) { movements, settings, currentPeriod ->
        StockMovementUiState(currentPeriod, movements, settings.currencySymbol)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockMovementUiState())

    fun setPeriod(preset: PeriodPreset) { period.value = TimePeriod.of(preset) }
}

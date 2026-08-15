package com.rzk.kasirpro.ui.screens.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.local.entity.ShiftEntity
import com.rzk.kasirpro.data.model.SalesSummary
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShiftUiState(
    val openShift: ShiftEntity? = null,
    val history: List<ShiftEntity> = emptyList(),
    val shiftSales: SalesSummary = SalesSummary(),
    val expectedCash: Long = 0,
    val settings: SettingsEntity = SettingsEntity()
)

sealed interface ShiftEvent {
    data object Opened : ShiftEvent
    data object Closed : ShiftEvent
    data class Error(val message: String) : ShiftEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModel(container: AppContainer) : ViewModel() {

    private val shifts = container.shiftRepository
    private val sales = container.salesRepository

    private val events = Channel<ShiftEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /** Bumped after each write so the expected-cash figure re-reads the ledger. */
    private val refresh = MutableStateFlow(0)

    private val shiftSales = shifts.observeOpenShift().flatMapLatest { shift ->
        if (shift == null) flowOf(SalesSummary()) else sales.observeShiftSummary(shift.id)
    }

    private val expected = combine(shifts.observeOpenShift(), refresh) { shift, _ -> shift }
        .flatMapLatest { shift ->
            flowOf(shift?.let { shifts.expectedCashFor(it.id) } ?: 0L)
        }

    val uiState: StateFlow<ShiftUiState> = combine(
        shifts.observeOpenShift(),
        shifts.observeHistory(),
        shiftSales,
        expected,
        container.settingsRepository.settings
    ) { open, history, summary, expectedCash, settings ->
        ShiftUiState(
            openShift = open,
            history = history.filter { it.id != open?.id },
            shiftSales = summary,
            expectedCash = expectedCash,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShiftUiState())

    fun openShift(cashierName: String, openingCash: Long) {
        viewModelScope.launch {
            shifts.openShift(cashierName, openingCash)
                .onSuccess {
                    refresh.value++
                    events.send(ShiftEvent.Opened)
                }
                .onFailure { events.send(ShiftEvent.Error(it.message.orEmpty())) }
        }
    }

    fun closeShift(actualCash: Long, note: String) {
        val shift = uiState.value.openShift ?: return
        viewModelScope.launch {
            shifts.closeShift(shift.id, actualCash, note)
                .onSuccess {
                    refresh.value++
                    events.send(ShiftEvent.Closed)
                }
                .onFailure { events.send(ShiftEvent.Error(it.message.orEmpty())) }
        }
    }
}

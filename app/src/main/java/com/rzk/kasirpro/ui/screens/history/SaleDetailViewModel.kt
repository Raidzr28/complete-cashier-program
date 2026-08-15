package com.rzk.kasirpro.ui.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.di.AppContainer
import com.rzk.kasirpro.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaleDetailUiState(
    val sale: SaleWithDetails? = null,
    val settings: SettingsEntity = SettingsEntity()
)

sealed interface SaleDetailEvent {
    data object Voided : SaleDetailEvent
    data class Error(val message: String) : SaleDetailEvent
}

class SaleDetailViewModel(
    container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sales = container.salesRepository
    private val saleId: Long = savedStateHandle.get<Long>(Routes.ARG_SALE_ID) ?: 0L

    private val events = Channel<SaleDetailEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<SaleDetailUiState> = combine(
        sales.observeDetails(saleId),
        container.settingsRepository.settings
    ) { sale, settings -> SaleDetailUiState(sale, settings) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SaleDetailUiState())

    /** Restores stock and posts the reversing cash entries — see SalesRepository.voidSale. */
    fun voidSale(reason: String) {
        viewModelScope.launch {
            sales.voidSale(saleId, reason)
                .onSuccess { events.send(SaleDetailEvent.Voided) }
                .onFailure { events.send(SaleDetailEvent.Error(it.message.orEmpty())) }
        }
    }
}

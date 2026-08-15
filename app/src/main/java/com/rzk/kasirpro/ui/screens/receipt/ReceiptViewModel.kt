package com.rzk.kasirpro.ui.screens.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.SaleWithDetails
import com.rzk.kasirpro.di.AppContainer
import com.rzk.kasirpro.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReceiptUiState(
    val sale: SaleWithDetails? = null,
    val settings: SettingsEntity = SettingsEntity()
)

class ReceiptViewModel(
    container: AppContainer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val saleId: Long = savedStateHandle.get<Long>(Routes.ARG_SALE_ID) ?: 0L

    val uiState: StateFlow<ReceiptUiState> = combine(
        container.salesRepository.observeDetails(saleId),
        container.settingsRepository.settings
    ) { sale, settings ->
        ReceiptUiState(sale, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptUiState())
}

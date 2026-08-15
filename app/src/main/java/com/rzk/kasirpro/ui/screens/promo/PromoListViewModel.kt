package com.rzk.kasirpro.ui.screens.promo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.PromoEntity
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PromoListUiState(
    val promos: List<PromoEntity> = emptyList(),
    val currencySymbol: String = "Rp"
)

class PromoListViewModel(container: AppContainer) : ViewModel() {

    private val promos = container.promoRepository

    val uiState: StateFlow<PromoListUiState> = combine(
        promos.observeAll(),
        container.settingsRepository.settings
    ) { list, settings -> PromoListUiState(list, settings.currencySymbol) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PromoListUiState())

    fun setActive(id: Long, active: Boolean) = viewModelScope.launch {
        promos.setActive(id, active)
    }

    fun delete(promo: PromoEntity) = viewModelScope.launch { promos.delete(promo) }
}

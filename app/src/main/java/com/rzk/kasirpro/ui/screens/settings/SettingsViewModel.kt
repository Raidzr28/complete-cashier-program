package com.rzk.kasirpro.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.di.AppContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object Saved : SettingsEvent
    data object DataCleared : SettingsEvent
    data class Error(val message: String) : SettingsEvent
}

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.settingsRepository

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val settings: StateFlow<SettingsEntity> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsEntity())

    /**
     * Writes straight through on every edit rather than behind a Save button: settings are
     * single-field toggles, and a half-saved store profile is worse than an eager one.
     */
    fun update(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            repo.update(transform)
                .onFailure { events.send(SettingsEvent.Error(it.message.orEmpty())) }
        }
    }

    fun clearBusinessData() {
        viewModelScope.launch {
            repo.clearBusinessData()
                .onSuccess { events.send(SettingsEvent.DataCleared) }
                .onFailure { events.send(SettingsEvent.Error(it.message.orEmpty())) }
        }
    }

    fun restoreSampleData() {
        viewModelScope.launch {
            repo.restoreSampleData()
                .onSuccess { events.send(SettingsEvent.Saved) }
                .onFailure { events.send(SettingsEvent.Error(it.message.orEmpty())) }
        }
    }
}

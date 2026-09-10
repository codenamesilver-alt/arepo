package com.example.garmento.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val gracePeriodDays: Int = 0,
    val isDarkMode: Boolean = false,
    val username: String = "",
    val isSaving: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.gracePeriodDays.collect { days ->
                _uiState.update { it.copy(gracePeriodDays = days) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { dark ->
                _uiState.update { it.copy(isDarkMode = dark) }
            }
        }
        viewModelScope.launch {
            settingsRepository.username.collect { name ->
                _uiState.update { it.copy(username = name) }
            }
        }
    }

    fun onGracePeriodChange(days: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsRepository.setGracePeriodDays(days)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onDarkModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.logout()
            onLogout()
        }
    }
}

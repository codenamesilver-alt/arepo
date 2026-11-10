package com.example.garmento.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.auth.AuthRepository
import com.example.garmento.feature.company.Company
import com.example.garmento.feature.company.CompanyRepository
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
    val isSaving: Boolean = false,
    val companies: List<Company> = emptyList(),
    val currentCompanyId: String? = UserSession.companyId,
    val isLoadingCompanies: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository?,
    private val companyRepository: CompanyRepository?,
    private val authRepository: AuthRepository?,
    private val dataStore: DataStore<Preferences>?
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(currentCompanyId = UserSession.companyId))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.gracePeriodDays.collect { days ->
                    _uiState.update { it.copy(gracePeriodDays = days) }
                }
            }
            viewModelScope.launch {
                repo.isDarkMode.collect { dark ->
                    _uiState.update { it.copy(isDarkMode = dark) }
                }
            }
            viewModelScope.launch {
                repo.username.collect { name ->
                    _uiState.update { it.copy(username = name) }
                }
            }
        }
        loadCompanies()
    }

    fun loadCompanies() {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCompanies = true) }
            try {
                val companies = companyRepository?.getMyCompanies() ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoadingCompanies = false,
                        companies = companies,
                        currentCompanyId = UserSession.companyId ?: it.currentCompanyId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCompanies = false) }
            }
        }
    }

    fun refreshCompanies() = load()

    fun switchCompany(companyId: String, onSwitched: () -> Unit = {}) {
        val company = _uiState.value.companies.firstOrNull { it.id == companyId }
        val role = if (company?.role == "owner") UserRole.ADMIN else UserRole.STAFF
        UserSession.setCompany(companyId, role)
        dataStore?.let { ds ->
            viewModelScope.launch { UserSession.save(ds) }
        }
        _uiState.update { it.copy(currentCompanyId = companyId) }
        onSwitched()
    }

    fun onGracePeriodChange(days: Int) {
        val repo = settingsRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repo.setGracePeriodDays(days)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun onDarkModeToggle(enabled: Boolean) {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.setDarkMode(enabled)
            }
        }
    }

    fun logout() {
        logout {}
    }

    fun logout(onLogout: () -> Unit) {
        viewModelScope.launch {
            try {
                authRepository?.signOut()
            } catch (_: Exception) {
            }
            if (authRepository == null) {
                try {
                    settingsRepository?.logout()
                } catch (_: Exception) {
                }
            }
            UserSession.clear()
            onLogout()
        }
    }
}
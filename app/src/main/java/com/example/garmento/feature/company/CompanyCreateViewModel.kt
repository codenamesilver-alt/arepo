package com.example.garmento.feature.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanyCreateUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class CompanyCreateViewModel @Inject constructor(
    private val repository: CompanyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyCreateUiState())
    val uiState: StateFlow<CompanyCreateUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null, success = false) }
    }

    fun create(name: String, onSuccess: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(error = "Company name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                val company = repository.createCompany(trimmed)
                val role = if (company.role == "owner") UserRole.ADMIN else UserRole.STAFF
                UserSession.setCompany(company.id, role)
                _uiState.update { it.copy(isLoading = false, success = true, name = company.name) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to create company") }
            }
        }
    }
}
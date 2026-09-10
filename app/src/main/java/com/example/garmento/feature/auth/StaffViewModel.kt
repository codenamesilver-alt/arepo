package com.example.garmento.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffUiState(
    val staffList: List<StaffMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState.asStateFlow()

    init {
        loadStaff()
    }

    fun loadStaff() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val staff = staffRepository.getStaff()
            _uiState.update { it.copy(staffList = staff, isLoading = false) }
        }
    }

    fun addStaff(username: String, password: String, role: UserRole) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Username and password required") }
            return
        }

        viewModelScope.launch {
            val success = staffRepository.addStaff(username, password, role)
            if (success) {
                _uiState.update {
                    it.copy(successMessage = "Staff member added", error = null)
                }
                loadStaff()
            } else {
                _uiState.update { it.copy(error = "Failed to add staff member") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

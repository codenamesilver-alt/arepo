package com.example.garmento.feature.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewCustomerUiState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewCustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingCustomerId: String? = savedStateHandle.get<String>("customerId")

    private val _uiState = MutableStateFlow(NewCustomerUiState())
    val uiState: StateFlow<NewCustomerUiState> = _uiState.asStateFlow()

    val isEditing: Boolean get() = editingCustomerId != null

    init {
        editingCustomerId?.let { loadCustomer(it) }
    }

    private fun loadCustomer(customerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val customer = customerRepository.getCustomerById(customerId)
            if (customer != null) {
                _uiState.update {
                    it.copy(
                        name = customer.name,
                        phone = customer.phone,
                        address = customer.address,
                        notes = customer.notes,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Customer not found") }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, error = null) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveCustomer(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(error = "Phone is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val existing = editingCustomerId?.let { customerRepository.getCustomerById(it) }
                val customer = if (existing != null) {
                    existing.copy(
                        name = state.name.trim(),
                        phone = state.phone.trim(),
                        address = state.address.trim(),
                        notes = state.notes.trim()
                    )
                } else {
                    val publicCode = customerRepository.generatePublicCode()
                    Customer(
                        name = state.name.trim(),
                        phone = state.phone.trim(),
                        address = state.address.trim(),
                        notes = state.notes.trim(),
                        publicCode = publicCode
                    )
                }
                customerRepository.saveCustomer(customer)
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
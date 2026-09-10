package com.example.garmento.feature.customers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.SubscriptionPlan
import com.example.garmento.feature.plans.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NewSubscriptionUiState(
    val customerId: UUID? = null,
    val availablePlans: List<SubscriptionPlan> = emptyList(),
    val selectedPlan: SubscriptionPlan? = null,
    val paymentMethod: String = "cash",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewSubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val planRepository: PlanRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: String? = savedStateHandle.get<String>("customerId")

    private val _uiState = MutableStateFlow(NewSubscriptionUiState())
    val uiState: StateFlow<NewSubscriptionUiState> = _uiState.asStateFlow()

    init {
        customerId?.let { _uiState.update { s -> s.copy(customerId = UUID.fromString(it)) } }
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            planRepository.syncFromRemote()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun observePlans(): StateFlow<List<SubscriptionPlan>> {
        return planRepository.getActivePlans()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.update { it.copy(selectedPlan = plan, error = null) }
    }

    fun onPaymentMethodChange(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun createSubscription(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.customerId == null || state.selectedPlan == null) {
            _uiState.update { it.copy(error = "Select a plan to continue") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                subscriptionRepository.createSubscription(
                    customerId = state.customerId,
                    plan = state.selectedPlan,
                    paymentMethod = state.paymentMethod
                )
                _uiState.update { it.copy(isSaving = false, success = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

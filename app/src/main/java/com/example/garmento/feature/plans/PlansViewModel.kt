package com.example.garmento.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.SubscriptionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PlansUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PlanEditUiState(
    val id: UUID? = null,
    val name: String = "",
    val durationDays: String = "",
    val amount: String = "",
    val totalClothes: String = "",
    val isActive: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState

    private val _editState = MutableStateFlow(PlanEditUiState())
    val editState: StateFlow<PlanEditUiState> = _editState

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                planRepository.syncFromRemote()
            } catch (_: Exception) {
                // Use cached data
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun observePlans(): StateFlow<List<SubscriptionPlan>> {
        return planRepository.getAllPlans()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun observeActivePlans(): StateFlow<List<SubscriptionPlan>> {
        return planRepository.getActivePlans()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun toggleActive(planId: UUID) {
        viewModelScope.launch {
            planRepository.toggleActive(planId.toString())
        }
    }

    fun deletePlan(planId: String) {
        viewModelScope.launch {
            try {
                planRepository.deletePlan(planId)
            } catch (_: Exception) { }
        }
    }

    fun loadPlanForEdit(planId: String) {
        viewModelScope.launch {
            val plan = planRepository.getPlanById(planId) ?: return@launch
            _editState.update {
                PlanEditUiState(
                    id = plan.id,
                    name = plan.name,
                    durationDays = plan.durationDays.toString(),
                    amount = plan.amount.toString(),
                    totalClothes = plan.totalClothes.toString(),
                    isActive = plan.isActive
                )
            }
        }
    }

    fun onEditNameChange(name: String) {
        _editState.update { it.copy(name = name, error = null) }
    }

    fun onEditDurationChange(duration: String) {
        _editState.update { it.copy(durationDays = duration, error = null) }
    }

    fun onEditAmountChange(amount: String) {
        _editState.update { it.copy(amount = amount, error = null) }
    }

    fun onEditClothesChange(clothes: String) {
        _editState.update { it.copy(totalClothes = clothes, error = null) }
    }

    fun onEditActiveChange(active: Boolean) {
        _editState.update { it.copy(isActive = active) }
    }

    fun savePlan(onSuccess: () -> Unit) {
        val state = _editState.value
        val duration = state.durationDays.toIntOrNull()
        val amount = state.amount.toDoubleOrNull()
        val clothes = state.totalClothes.toIntOrNull()

        if (state.name.isBlank()) {
            _editState.update { it.copy(error = "Name is required") }
            return
        }
        if (duration == null || duration <= 0) {
            _editState.update { it.copy(error = "Duration must be a positive number") }
            return
        }
        if (amount == null || amount <= 0) {
            _editState.update { it.copy(error = "Amount must be a positive number") }
            return
        }
        if (clothes == null || clothes <= 0) {
            _editState.update { it.copy(error = "Total clothes must be a positive number") }
            return
        }

        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true) }
            try {
                val plan = SubscriptionPlan(
                    id = state.id ?: UUID.randomUUID(),
                    name = state.name,
                    durationDays = duration,
                    amount = amount,
                    totalClothes = clothes,
                    isActive = state.isActive
                )
                planRepository.savePlan(plan)
                _editState.update { PlanEditUiState() }
                onSuccess()
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun resetEditState() {
        _editState.value = PlanEditUiState()
    }
}

package com.example.garmento.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Subscription
import com.example.garmento.core.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val activeSubscriptionCount: Int = 0,
    val expiringSoon: List<Subscription> = emptyList(),
    val revenueThisMonth: Double = 0.0,
    val ordersToday: Int = 0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                dashboardRepository.syncFromRemote()
                val activeCount = dashboardRepository.getActiveSubscriptionCount()
                val expiring = dashboardRepository.getExpiringSoon()
                val revenue = dashboardRepository.getRevenueThisMonth()
                val ordersToday = dashboardRepository.getOrdersTodayCount()

                _uiState.update {
                    it.copy(
                        activeSubscriptionCount = activeCount,
                        expiringSoon = expiring,
                        revenueThisMonth = revenue,
                        ordersToday = ordersToday,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun observeRecentTransactions(): StateFlow<List<Transaction>> {
        return dashboardRepository.getRecentTransactions(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}

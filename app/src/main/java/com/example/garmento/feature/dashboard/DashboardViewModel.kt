package com.example.garmento.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val activeSubscriptionCount: Int = 0,
    val expiringSoon: List<ExpiringSoonRow> = emptyList(),
    val revenueLast30Days: Double = 0.0,
    val revenueToday: Double = 0.0,
    val ordersToday: Int = 0,
    val recentTransactions: List<RecentTransactionRow> = emptyList(),
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
                val revenue = dashboardRepository.getRevenueLast30Days()
                val revenueToday = dashboardRepository.getRevenueToday()
                val ordersToday = dashboardRepository.getOrdersTodayCount()

                _uiState.update {
                    it.copy(
                        activeSubscriptionCount = activeCount,
                        expiringSoon = expiring,
                        revenueLast30Days = revenue,
                        revenueToday = revenueToday,
                        ordersToday = ordersToday,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun observeRecentTransactions(): StateFlow<List<RecentTransactionRow>> {
        return dashboardRepository.getRecentTransactions(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun observeUpcomingDeliveries(): StateFlow<List<UpcomingDeliveryRow>> {
        return dashboardRepository.getUpcomingDeliveries(15)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private val _selectedDeliveryDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDeliveryDate: StateFlow<String> = _selectedDeliveryDate.asStateFlow()

    fun selectDeliveryDate(date: String) {
        _selectedDeliveryDate.value = date
    }

    fun observeDeliveriesForSelectedDate(): StateFlow<List<UpcomingDeliveryRow>> {
        return _selectedDeliveryDate.flatMapLatest { d ->
            dashboardRepository.getDeliveriesForDate(d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}

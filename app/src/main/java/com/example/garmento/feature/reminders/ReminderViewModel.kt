package com.example.garmento.feature.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garmento.core.domain.model.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemindersUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val filterDays: Int = 7,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        loadReminders()
    }

    fun loadReminders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val subs = reminderRepository.getExpiringInDays(_uiState.value.filterDays)
                _uiState.update { it.copy(subscriptions = subs, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onFilterDaysChange(days: Int) {
        _uiState.update { it.copy(filterDays = days) }
        loadReminders()
    }

    fun sendReminder(context: Context, subscription: Subscription) {
        val message = buildString {
            append("Hello! Your subscription expires in ${subscription.daysRemaining} days. ")
            append("${subscription.remainingClothes} clothes remaining. ")
            append("Please renew to continue.")
        }
        val url = reminderRepository.getWhatsAppUrl("", message)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

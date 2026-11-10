package com.example.garmento.feature.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import com.example.garmento.feature.auth.AuthRepository
import com.example.garmento.feature.company.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

data class InviteAcceptUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val companyName: String? = null,
    val error: String? = null
)

@HiltViewModel
class InviteAcceptViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val companyRepository: CompanyRepository,
    private val authRepository: AuthRepository?,
    private val dataStore: DataStore<Preferences>?
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteAcceptUiState())
    val uiState: StateFlow<InviteAcceptUiState> = _uiState.asStateFlow()

    fun accept(token: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = false) }
            try {
                if (authRepository?.currentSession() == null) {
                    val signedIn = authRepository?.signInWithGoogle() ?: false
                    if (!signedIn) {
                        _uiState.update { it.copy(isLoading = false, error = "Please sign in to accept this invitation.") }
                        return@launch
                    }
                }
                val before = companyRepository.getMyCompanies().map { it.id }.toSet()
                val result = supabase.postgrest.rpc(
                    function = "accept_invitation",
                    parameters = JsonObject(mapOf("p_token" to JsonPrimitive(token)))
                )
                val returnedId = runCatching { result.decodeAs<String>() }.getOrNull()
                val after = companyRepository.getMyCompanies()
                val joined = after.firstOrNull { it.id == returnedId }
                    ?: after.firstOrNull { it.id !in before }
                    ?: after.firstOrNull { it.id == returnedId?.trim('"') }
                if (joined != null) {
                    val role = if (joined.role == "owner") UserRole.ADMIN else UserRole.STAFF
                    UserSession.setCompany(joined.id, role)
                    dataStore?.let { UserSession.save(it) }
                }
                _uiState.update {
                    it.copy(isLoading = false, success = true, companyName = joined?.name)
                }
                onSuccess()
            } catch (e: Exception) {
                val msg = mapError(e)
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    private fun mapError(e: Exception): String {
        val raw = (e.message ?: "").lowercase()
        return when {
            raw.contains("expired") -> "This invitation has expired."
            raw.contains("already accepted") -> "This invitation was already accepted."
            raw.contains("invalid") -> "This invitation link is invalid."
            raw.contains("email does not match") -> "This invitation is for a different email address."
            raw.contains("not authenticated") -> "Please sign in to accept this invitation."
            else -> e.message ?: "Failed to accept invitation"
        }
    }

    fun reset() {
        _uiState.value = InviteAcceptUiState()
    }
}
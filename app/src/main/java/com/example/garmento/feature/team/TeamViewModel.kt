package com.example.garmento.feature.team

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

data class TeamUiState(
    val members: List<TeamMember> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val inviteEmail: String = "",
    val inviteRole: UserRole = UserRole.STAFF,
    val inviteToken: String? = null,
    val isOwner: Boolean = UserSession.role == UserRole.ADMIN
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        val companyId = UserSession.companyId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val members = if (companyId != null) teamRepository.getMembers(companyId) else emptyList()
            _uiState.update { it.copy(members = members, isLoading = false) }
        }
    }

    fun onInviteEmailChange(value: String) {
        _uiState.update { it.copy(inviteEmail = value, error = null, successMessage = null) }
    }

    fun onInviteRoleChange(role: UserRole) {
        _uiState.update { it.copy(inviteRole = role) }
    }

    fun inviteStaff(email: String = _uiState.value.inviteEmail) {
        if (UserSession.role != UserRole.ADMIN) {
            _uiState.update { it.copy(error = "Only owners can invite staff") }
            return
        }
        val companyId = UserSession.companyId
        if (companyId.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No active company") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        val role = _uiState.value.inviteRole
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val token = teamRepository.inviteStaff(companyId, email, role)
            if (token != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Invitation created",
                        inviteToken = token,
                        inviteEmail = ""
                    )
                }
                loadMembers()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create invitation") }
            }
        }
    }

    fun removeMember(memberId: String) {
        if (UserSession.role != UserRole.ADMIN) {
            _uiState.update { it.copy(error = "Only owners can remove members") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val err = teamRepository.removeMember(memberId)
            if (err == null) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Member removed") }
                loadMembers()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed: $err") }
            }
        }
    }

    fun changeRole(memberId: String, role: UserRole) {
        if (UserSession.role != UserRole.ADMIN) {
            _uiState.update { it.copy(error = "Only owners can change roles") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            val err = teamRepository.changeRole(memberId, role)
            if (err == null) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Role updated") }
                loadMembers()
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed: $err") }
            }
        }
    }

    fun clearInviteToken() {
        _uiState.update { it.copy(inviteToken = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}

package com.example.garmento.feature.team

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    onNavigateBack: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isOwner = UserSession.role == UserRole.ADMIN
    var showInviteForm by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    var memberRoleExpanded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.inviteToken) {
        val token = uiState.inviteToken
        if (token != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Garmento team invitation")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "You have been invited to join a Garmento team. Use this token to accept: $token"
                )
            }
            context.startActivity(Intent.createChooser(intent, "Send invitation"))
            viewModel.clearInviteToken()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.successMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isOwner) {
                Button(
                    onClick = { showInviteForm = !showInviteForm },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showInviteForm) "Cancel" else "Invite Staff Member")
                }

                if (showInviteForm) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.inviteEmail,
                                onValueChange = viewModel::onInviteEmailChange,
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = roleExpanded,
                                onExpandedChange = { roleExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.inviteRole.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Role") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = roleExpanded,
                                    onDismissRequest = { roleExpanded = false }
                                ) {
                                    UserRole.entries.forEach { role ->
                                        DropdownMenuItem(
                                            text = { Text(role.name) },
                                            onClick = {
                                                viewModel.onInviteRoleChange(role)
                                                roleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.inviteStaff() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send Invitation")
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.members) { member ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    member.email.ifBlank { member.userId },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${member.role.name} • ${member.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (member.joinedAt.isNotBlank()) {
                                    Text(
                                        member.joinedAt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isOwner) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ExposedDropdownMenuBox(
                                        expanded = memberRoleExpanded == member.id,
                                        onExpandedChange = { expanded ->
                                            memberRoleExpanded = if (expanded) member.id else null
                                        }
                                    ) {
                                        Button(onClick = { memberRoleExpanded = member.id }) {
                                            Text("Role")
                                        }
                                        ExposedDropdownMenu(
                                            expanded = memberRoleExpanded == member.id,
                                            onDismissRequest = { memberRoleExpanded = null }
                                        ) {
                                            UserRole.entries.forEach { role ->
                                                DropdownMenuItem(
                                                    text = { Text(role.name) },
                                                    onClick = {
                                                        viewModel.changeRole(member.id, role)
                                                        memberRoleExpanded = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Button(onClick = { viewModel.removeMember(member.id) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.members.isEmpty() && !uiState.isLoading) {
                Text(
                    "No team members found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

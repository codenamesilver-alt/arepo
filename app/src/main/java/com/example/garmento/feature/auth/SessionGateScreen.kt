package com.example.garmento.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SessionGateScreen(
    onLoggedOut: () -> Unit = {},
    onNeedsCompany: () -> Unit = {},
    onPickCompany: () -> Unit = {},
    onLoggedIn: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            SessionDestination.LOGGED_OUT -> onLoggedOut()
            SessionDestination.NEEDS_COMPANY -> onNeedsCompany()
            SessionDestination.COMPANY_PICKER -> onPickCompany()
            SessionDestination.LOGGED_IN -> onLoggedIn()
            null -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
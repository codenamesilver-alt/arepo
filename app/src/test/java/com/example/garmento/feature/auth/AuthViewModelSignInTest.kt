package com.example.garmento.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelSignInTest {

    private class FakeAuthRepository(private val result: Boolean) : AuthRepository {
        override val isLoggedIn = MutableStateFlow(false)
        override val getUsername = MutableStateFlow("")
        override val storedRole = MutableStateFlow<String?>(null)
        override fun currentSession() = null
        override suspend fun signInWithGoogle(): Boolean = result
        override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
            if (result) Result.success(Unit) else Result.failure(IllegalStateException("sign-in failed"))
        override suspend fun signOut() {}
    }

    @Test
    fun signInSuccess_setsLoginSuccess() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = AuthViewModel(FakeAuthRepository(true))
        vm.signInWithGoogle()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loginSuccess)
        assertTrue(!vm.uiState.value.isLoading)
        Dispatchers.resetMain()
    }

    @Test
    fun signInFailure_setsError() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = AuthViewModel(FakeAuthRepository(false))
        vm.signInWithGoogle()
        advanceUntilIdle()
        assertTrue(!vm.uiState.value.loginSuccess)
        assertTrue(vm.uiState.value.errorMessage != null)
        Dispatchers.resetMain()
    }
}
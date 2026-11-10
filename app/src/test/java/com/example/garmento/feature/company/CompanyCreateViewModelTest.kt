package com.example.garmento.feature.company

import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanyCreateViewModelTest {

    private class FakeCompanyRepository(
        private val result: Company = Company("c1", "Acme", "owner"),
        private val throws: Boolean = false
    ) : CompanyRepository {
        var createdName: String? = null
        override suspend fun getMyCompanies(): List<Company> = emptyList()
        override suspend fun createCompany(name: String): Company {
            if (throws) throw RuntimeException("boom")
            createdName = name
            return result
        }
    }

    @Test
    fun createSuccess_setsSessionAndSuccess() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        UserSession.clear()

        val fake = FakeCompanyRepository()
        val vm = CompanyCreateViewModel(fake)
        vm.create("  Acme  ")
        advanceUntilIdle()

        assertEquals("Acme", fake.createdName)
        assertTrue(vm.uiState.value.success)
        assertEquals("Acme", vm.uiState.value.name)
        assertEquals("c1", UserSession.companyId)
        assertEquals(UserRole.ADMIN, UserSession.role)

        UserSession.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun createFailure_setsError() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        UserSession.clear()

        val fake = FakeCompanyRepository(throws = true)
        val vm = CompanyCreateViewModel(fake)
        vm.create("Acme")
        advanceUntilIdle()

        assertTrue(!vm.uiState.value.success)
        assertTrue(vm.uiState.value.error != null)
        assertEquals(null, UserSession.companyId)

        UserSession.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun blankName_setsErrorWithoutCallingRepository() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val fake = FakeCompanyRepository()
        val vm = CompanyCreateViewModel(fake)
        vm.create("   ")
        advanceUntilIdle()

        assertTrue(!vm.uiState.value.success)
        assertEquals("Company name is required", vm.uiState.value.error)
        assertEquals(null, fake.createdName)

        Dispatchers.resetMain()
    }
}
package com.example.garmento.core.util

import com.example.garmento.core.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserSessionTest {

    @Test
    fun `set and clear round trips all fields`() {
        UserSession.clear()
        UserSession.setUserId("u1")
        UserSession.setEmail("a@b.com")
        UserSession.setCompany("c1", UserRole.STAFF)

        assertEquals("u1", UserSession.userId)
        assertEquals("a@b.com", UserSession.email)
        assertEquals("c1", UserSession.companyId)
        assertEquals(UserRole.STAFF, UserSession.role)

        UserSession.clear()

        assertNull(UserSession.userId)
        assertNull(UserSession.email)
        assertNull(UserSession.companyId)
        assertNull(UserSession.role)
    }
}

package com.example.fishing.viewmodel.session

import com.example.fishing.testutil.FakeAuthRepository
import com.example.fishing.testutil.testUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class UserSessionTest {

    @Test
    fun `bind caches the user resolved from auth`() {
        val user = testUser()
        val auth = FakeAuthRepository().apply { sessionUser = user }
        val session = UserSession(auth)

        assertEquals(user.id, session.bind())
        assertEquals(user.id, session.currentUserId)
    }

    @Test
    fun `explicit user wins over the auth fallback`() {
        val authUser = testUser()
        val boundUser = UUID.randomUUID()
        val auth = FakeAuthRepository().apply { sessionUser = authUser }
        val session = UserSession(auth)

        session.set(boundUser)

        assertEquals(boundUser, session.resolve())
    }

    @Test
    fun `clear removes the bound user and allows a fresh bind`() {
        val user = testUser()
        val auth = FakeAuthRepository().apply { sessionUser = user }
        val session = UserSession(auth)

        session.bind()
        session.clear()

        assertNull(session.currentUserId)
        // The fallback is still available until auth itself is cleared.
        assertEquals(user.id, session.resolve())
    }
}

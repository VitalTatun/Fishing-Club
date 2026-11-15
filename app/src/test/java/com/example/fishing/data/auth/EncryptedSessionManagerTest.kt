package com.example.fishing.data.auth

import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(kotlin.time.ExperimentalTime::class)
class EncryptedSessionManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { encodeDefaults = true }

    private val session = UserSession(
        accessToken = "access-token-value",
        refreshToken = "refresh-token-value",
        expiresIn = 3_600,
        tokenType = "bearer"
    )

    private fun manager(
        file: File = File(tempFolder.root, "auth/session.bin"),
        cipher: SessionCipher = ReversingCipher(),
        legacy: LegacySessionStore? = null
    ) = EncryptedSessionManager(
        file = file,
        cipher = cipher,
        legacyStore = legacy,
        ioDispatcher = UnconfinedTestDispatcher()
    )

    @Test
    fun `saved session is encrypted on disk and loads back`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        val manager = manager(file = file)

        manager.saveSession(session)

        assertTrue(file.exists())
        val stored = file.readText()
        assertFalse(stored.contains(session.accessToken))
        assertFalse(stored.contains(session.refreshToken))
        assertEquals(session, manager.loadSession())
    }

    @Test
    fun `saving again replaces the previous session without leftovers`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        val manager = manager(file = file)
        manager.saveSession(session)

        val refreshed = session.copy(accessToken = "second-access")
        manager.saveSession(refreshed)

        assertEquals(refreshed, manager.loadSession())
        assertFalse(File(file.parentFile, "${file.name}.tmp").exists())
    }

    @Test
    fun `missing session loads as null`() = runTest {
        assertNull(manager().loadSessionOrNull())
    }

    @Test
    fun `corrupted file is dropped instead of failing every launch`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        file.parentFile?.mkdirs()
        file.writeText("not an encrypted payload")

        assertNull(manager(file = file).loadSessionOrNull())
        assertFalse(file.exists())
    }

    @Test
    fun `deleteSession removes the encrypted file`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        val manager = manager(file = file)
        manager.saveSession(session)

        manager.deleteSession()

        assertFalse(file.exists())
        assertNull(manager.loadSessionOrNull())
    }

    @Test
    fun `plaintext session from an older build is migrated and deleted`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        val legacy = FakeLegacyStore(json.encodeToString(UserSession.serializer(), session))

        val loaded = manager(file = file, legacy = legacy).loadSession()

        assertEquals(session, loaded)
        assertTrue(file.exists())
        assertFalse(file.readText().contains(session.accessToken))
        assertEquals(1, legacy.clearCalls)
        assertNull(legacy.raw)
    }

    @Test
    fun `failed migration keeps the plaintext session for the next launch`() = runTest {
        val file = File(tempFolder.root, "auth/session.bin")
        val legacy = FakeLegacyStore(json.encodeToString(UserSession.serializer(), session))

        val manager = manager(file = file, cipher = FailingCipher(), legacy = legacy)

        assertNull(manager.loadSessionOrNull())
        assertNotNull(legacy.raw)
        assertEquals(0, legacy.clearCalls)
        assertFalse(file.exists())
    }

    @Test
    fun `unreadable legacy session is discarded`() = runTest {
        val legacy = FakeLegacyStore("not json")

        assertNull(manager(legacy = legacy).loadSessionOrNull())
        assertNull(legacy.raw)
    }

    @Test
    fun `deleteSession clears the plaintext entry too`() = runTest {
        val legacy = FakeLegacyStore(json.encodeToString(UserSession.serializer(), session))
        val manager = manager(legacy = legacy)

        manager.deleteSession()

        assertNull(legacy.raw)
        assertEquals(1, legacy.clearCalls)
    }

    @Test
    fun `legacy key matches the one supabase-kt used`() {
        assertEquals(
            "sb-https:--newnheannhymapusagnm-supabase-co-session",
            legacyPlaintextSessionKey("https://newnheannhymapusagnm.supabase.co/")
        )
    }

    private class ReversingCipher : SessionCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray =
            byteArrayOf(MARKER) + plaintext.reversedArray()

        override fun decrypt(payload: ByteArray): ByteArray {
            require(payload.isNotEmpty() && payload[0] == MARKER) { "Bad payload" }
            return payload.copyOfRange(1, payload.size).reversedArray()
        }

        private companion object {
            const val MARKER: Byte = 7
        }
    }

    private class FailingCipher : SessionCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = error("Keystore unavailable")

        override fun decrypt(payload: ByteArray): ByteArray = error("Keystore unavailable")
    }

    private class FakeLegacyStore(var raw: String? = null) : LegacySessionStore {
        var clearCalls = 0

        override fun read(): String? = raw

        override fun clear() {
            clearCalls++
            raw = null
        }
    }
}

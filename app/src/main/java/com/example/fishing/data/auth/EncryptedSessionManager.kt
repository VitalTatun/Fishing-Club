package com.example.fishing.data.auth

import android.content.SharedPreferences
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Reads the raw session that supabase-kt used to keep in plain
 * `SharedPreferences`. Implemented separately so [EncryptedSessionManager]
 * stays testable without Android.
 */
internal interface LegacySessionStore {

    /** Raw JSON of the old plaintext session, or null when there is none. */
    fun read(): String?

    /** Removes the plaintext session from disk. */
    fun clear()
}

/** Plaintext storage left behind by supabase-kt's default session manager. */
internal class SharedPreferencesLegacySessionStore(
    private val preferences: SharedPreferences,
    private val key: String
) : LegacySessionStore {

    override fun read(): String? = runCatching { preferences.getString(key, null) }.getOrNull()

    override fun clear() {
        if (preferences.contains(key)) {
            // commit(), not apply(): the plaintext token must be gone before the
            // process can die, and this runs on the IO dispatcher.
            preferences.edit().remove(key).commit()
        }
    }
}

/**
 * Stores the auth session as an AES-GCM encrypted file instead of the plain
 * `SharedPreferences` entry used by supabase-kt by default.
 *
 * The file lives in `noBackupFilesDir`, so it is never part of Auto Backup even
 * if backup is re-enabled later; the encryption key lives in the Android
 * Keystore and cannot be extracted from the device.
 *
 * A session written by the previous plaintext manager is migrated on first
 * load and the plaintext copy is deleted, so unencrypted tokens do not survive
 * the update.
 */
internal class EncryptedSessionManager(
    private val file: File,
    private val cipher: SessionCipher,
    private val legacyStore: LegacySessionStore? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = SESSION_JSON
) : SessionManager {

    private val mutex = Mutex()

    private val tempFile: File
        get() = File(file.parentFile, "${file.name}.tmp")

    override suspend fun saveSession(session: UserSession) = withContext(ioDispatcher) {
        mutex.withLock { writeLocked(session) }
    }

    override suspend fun loadSession(): UserSession = withContext(ioDispatcher) {
        mutex.withLock {
            readLocked() ?: migrateLegacyLocked() ?: throw IllegalStateException(
                "No saved session in ${file.path}"
            )
        }
    }

    override suspend fun deleteSession(): Unit = withContext(ioDispatcher) {
        mutex.withLock {
            if (file.exists()) file.delete()
            if (tempFile.exists()) tempFile.delete()
            legacyStore?.clear()
        }
        Unit
    }

    private fun readLocked(): UserSession? {
        if (!file.exists()) return null
        return try {
            val plaintext = cipher.decrypt(file.readBytes())
            json.decodeFromString(UserSession.serializer(), plaintext.decodeToString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Corrupted payload or a Keystore key that was invalidated (for
            // example after a restore to another device). The session cannot be
            // recovered: drop the file so startup does not fail on every launch.
            file.delete()
            null
        }
    }

    private fun migrateLegacyLocked(): UserSession? {
        val store = legacyStore ?: return null
        val raw = store.read() ?: return null
        val session = try {
            json.decodeFromString(UserSession.serializer(), raw)
        } catch (e: Exception) {
            store.clear()
            return null
        }
        return try {
            writeLocked(session)
            session
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the plaintext copy: the migration can be retried on the next
            // launch instead of silently signing the user out.
            null
        }
    }

    private fun writeLocked(session: UserSession) {
        val plaintext = json.encodeToString(UserSession.serializer(), session).encodeToByteArray()
        val payload = cipher.encrypt(plaintext)
        file.parentFile?.mkdirs()
        // Write to a sibling file and rename over the target: a crash in the
        // middle of a write must never leave an undecryptable session behind.
        tempFile.writeBytes(payload)
        if (file.exists() && !file.delete()) {
            tempFile.delete()
            error("Could not replace ${file.path}")
        }
        if (!tempFile.renameTo(file)) {
            tempFile.delete()
            error("Could not persist ${file.path}")
        }
        legacyStore?.clear()
    }

    private companion object {
        val SESSION_JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

/**
 * Key used by supabase-kt's default `SettingsSessionManager`. Kept here to
 * migrate (and then delete) the plaintext session left by older builds.
 */
internal fun legacyPlaintextSessionKey(supabaseUrl: String): String =
    "sb-${supabaseUrl.removeSuffix("/").replace('/', '-').replace('.', '-')}-session"

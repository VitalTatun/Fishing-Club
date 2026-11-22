package com.example.fishing.data

import android.app.Application
import com.example.fishing.model.resolveFishSpecies
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The APK seed must mirror the approved server catalog, otherwise a fresh
 * offline install would show a different species list than an online one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class FishSpeciesSeedTest {

    @Test
    fun `bundled seed matches the approved catalog`() {
        val context = RuntimeEnvironment.getApplication()
        val raw = context.assets.open("fish_species.json").bufferedReader().use { it.readText() }
        val species = parseFishSpeciesSeed(raw)

        assertEquals(54, species.size)
        assertEquals(54, species.map { it.id }.distinct().size)
        assertEquals(10, species.count { it.isPopular })
        assertEquals(54, species.map { it.sortOrder }.distinct().size)
        assertEquals("Плотва", species.minBy { it.sortOrder }.canonicalName)
        assertEquals("abramis_brama", resolveFishSpecies("подлещик", species)?.id)
        assertEquals("esox_lucius", resolveFishSpecies("Щука", species)?.id)
    }
}

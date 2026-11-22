package com.example.fishing.ui.screens.report.create

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.toMutableStateList
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.model.FishingMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * AUD-06: `StateRestorationTester` drives these savers through the real
 * screens in the per-screen `*StateRestorationTest` files. The saver itself is
 * also covered directly so a codec regression is located without UI noise.
 */
class CreateReportFormSaversTest {

    private object AlwaysSaveable : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private fun <T> Saver<T, *>.saveForTest(value: T): Any? =
        with(AlwaysSaveable) { save(value) }

    @Test
    fun `fish list saver round trips`() {
        val fish = listOf(
            Fish(id = UUID.randomUUID(), name = "Щука", count = 3),
            Fish(id = UUID.randomUUID(), name = "Окунь", count = 1),
        )
        val state = fish.toMutableStateList()

        val saved = FishListSaver.saveForTest(state)
        val restored = FishListSaver.restore(requireNotNull(saved))

        assertEquals(fish, restored?.toList())
    }

    @Test
    fun `bait list saver round trips`() {
        val baits = listOf(Bait.WOBBLER, Bait.WORM)
        val state = mutableStateListOf<Bait>().apply { addAll(baits) }

        val saved = BaitListSaver.saveForTest(state)
        val restored = BaitListSaver.restore(requireNotNull(saved))

        assertEquals(baits, restored?.toList())
    }

    @Test
    fun `method saver round trips and ignores unknown names`() {
        val saved = FishingMethodSaver.saveForTest(FishingMethod.FEEDER)

        assertEquals("FEEDER", saved)
        assertEquals(FishingMethod.FEEDER, FishingMethodSaver.restore(saved as String))
        assertNull(FishingMethodSaver.restore("NOT_A_METHOD"))
    }

    @Test
    fun `saver restore of corrupted entries keeps only valid values`() {
        val restored = FishListSaver.restore(arrayListOf("broken", 7))
        assertTrue(restored!!.isEmpty())
    }
}

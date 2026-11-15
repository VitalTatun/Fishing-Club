package com.example.fishing.viewmodel.savedstate

import android.app.Application
import android.net.Uri
import com.example.fishing.model.Bait
import com.example.fishing.model.Fish
import com.example.fishing.viewmodel.ReportPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReportFormCodecsTest {

    @Test
    fun `photos survive a round trip`() {
        val photos = listOf(
            ReportPhoto(id = UUID.randomUUID(), uri = Uri.parse("content://media/1")),
            ReportPhoto(id = UUID.randomUUID(), uri = Uri.parse("file:///tmp/photo.jpg")),
        )

        val restored = photosFromSaved(photosToSaved(photos))

        assertEquals(photos, restored)
    }

    @Test
    fun `photo uri containing the field separator is preserved`() {
        val photo = ReportPhoto(
            id = UUID.randomUUID(),
            uri = Uri.parse("content://media/odd\u001Fname.jpg")
        )

        val restored = photosFromSaved(photosToSaved(listOf(photo)))

        assertEquals(listOf(photo), restored)
    }

    @Test
    fun `photo decoder skips malformed entries`() {
        val valid = ReportPhoto(id = UUID.randomUUID(), uri = Uri.parse("content://media/ok"))
        val saved = arrayListOf<Any?>(
            42,
            "not-a-uuid\u001Fcontent://media/nope",
            valid.id.toString(),
            "${valid.id}\u001F${valid.uri}",
        )

        val restored = photosFromSaved(saved)

        assertEquals(listOf(valid), restored)
    }

    @Test
    fun `photo decoder tolerates missing or empty input`() {
        assertTrue(photosFromSaved(null).isEmpty())
        assertTrue(photosFromSaved("garbage").isEmpty())
        assertTrue(photosFromSaved(emptyList<String>()).isEmpty())
    }

    @Test
    fun `fish survive a round trip`() {
        val fish = listOf(
            Fish(id = UUID.randomUUID(), name = "Щука", count = 3),
            Fish(id = UUID.randomUUID(), name = "Карась", count = 1),
        )

        val restored = fishFromSaved(fishToSaved(fish))

        assertEquals(fish, restored)
    }

    @Test
    fun `fish name containing the separator is preserved`() {
        val fish = Fish(id = UUID.randomUUID(), name = "Щ\u001Fука", count = 2)

        val restored = fishFromSaved(fishToSaved(listOf(fish)))

        assertEquals(listOf(fish), restored)
    }

    @Test
    fun `fish decoder skips malformed entries`() {
        val valid = Fish(id = UUID.randomUUID(), name = "Окунь", count = 2)
        val saved = arrayListOf<Any?>(
            null,
            "broken",
            "${UUID.randomUUID()}\u001Fnot-a-count\u001FРыба",
            "${UUID.randomUUID()}\u001F1",
            "${valid.id}\u001F${valid.count}\u001F${valid.name}",
        )

        assertEquals(listOf(valid), fishFromSaved(saved))
    }

    @Test
    fun `baits survive a round trip and unknown names are skipped`() {
        val baits = listOf(Bait.WOBBLER, Bait.WORM, Bait.FLY)

        val restored = baitsFromSaved(baitsToSaved(baits))

        assertEquals(baits, restored)

        val corrupted = arrayListOf<Any?>("WOBBLER", "NOT_A_BAIT", 7)
        assertEquals(listOf(Bait.WOBBLER), baitsFromSaved(corrupted))
    }

    @Test
    fun `bait decoder tolerates missing input`() {
        assertTrue(baitsFromSaved(null).isEmpty())
        assertTrue(baitsFromSaved(emptyList<String>()).isEmpty())
    }
}

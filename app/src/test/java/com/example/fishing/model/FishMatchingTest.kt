package com.example.fishing.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FishMatchingTest {

    private val species = listOf(
        FishSpecies(
            id = "esox_lucius",
            canonicalName = "Щука",
            aliases = listOf("щука обыкновенная", "щуки"),
            isPopular = true,
            sortOrder = 3
        ),
        FishSpecies(
            id = "abramis_brama",
            canonicalName = "Лещ",
            aliases = listOf("подлещик", "лещи"),
            isPopular = true,
            sortOrder = 4
        ),
        FishSpecies(
            id = "carassius_gibelio",
            canonicalName = "Карась",
            aliases = listOf("карась золотой", "караси"),
            isPopular = true,
            sortOrder = 2
        ),
        FishSpecies(
            id = "neogobius_spp",
            canonicalName = "Бычок",
            aliases = listOf("бычок-песочник"),
            sortOrder = 104
        ),
        FishSpecies(
            id = "gymnocephalus_cernua",
            canonicalName = "Ёрш",
            aliases = listOf("ерш"),
            sortOrder = 114
        )
    )

    @Test
    fun `normalization trims, lowercases and folds yo`() {
        assertEquals("плотва", normalizeFishName("  ПЛОТВА  "))
        assertEquals("ерш", normalizeFishName("Ёрш"))
        assertEquals("белый амур", normalizeFishName("белый   амур"))
    }

    @Test
    fun `resolve uses canonical names and aliases`() {
        assertEquals("esox_lucius", resolveFishSpecies("щука", species)?.id)
        assertEquals("abramis_brama", resolveFishSpecies("Подлещик", species)?.id)
        assertEquals("carassius_gibelio", resolveFishSpecies("карась золотой", species)?.id)
        assertEquals("neogobius_spp", resolveFishSpecies("бычок-песочник", species)?.id)
        assertEquals("gymnocephalus_cernua", resolveFishSpecies("ерш", species)?.id)
    }

    @Test
    fun `resolve ignores partial names and blanks`() {
        assertNull(resolveFishSpecies("щук", species))
        assertNull(resolveFishSpecies("   ", species))
    }

    @Test
    fun `resolve prefers canonical name over an alias`() {
        val catalog = species + FishSpecies(
            id = "custom",
            canonicalName = "Щукарка",
            aliases = listOf("щука"),
            sortOrder = 200
        )
        assertEquals("esox_lucius", resolveFishSpecies("щука", catalog)?.id)
    }

    @Test
    fun `search matches prefixes and ranks exact canonical first`() {
        val results = searchFishSpecies("щук", species)
        assertEquals("esox_lucius", results.first().id)

        val exact = searchFishSpecies("ерш", species)
        assertEquals("gymnocephalus_cernua", exact.first().id)
    }

    @Test
    fun `search is case and yo insensitive and returns unique species`() {
        val results = searchFishSpecies("Ерш", species)
        assertTrue(results.any { it.id == "gymnocephalus_cernua" })
        assertEquals(results.size, results.distinctBy { it.id }.size)
    }

    @Test
    fun `blank query returns nothing`() {
        assertTrue(searchFishSpecies("   ", species).isEmpty())
    }

    @Test
    fun `search finds aliases`() {
        val results = searchFishSpecies("подлещ", species)
        assertNotNull(results.firstOrNull { it.id == "abramis_brama" })
    }
}

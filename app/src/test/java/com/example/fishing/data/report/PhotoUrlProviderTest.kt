package com.example.fishing.data.report

import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoUrlProviderTest {

    private val provider = SupabasePhotoUrlProvider(mockk<SupabaseClient>(relaxed = true))

    @Test
    fun `paths inside the private bucket are storage paths`() {
        assertTrue(provider.isStoragePath("user/report/photo.jpg"))
        assertTrue(provider.isStoragePath("photo.jpg"))
    }

    @Test
    fun `absolute urls and local file paths are not storage paths`() {
        assertFalse(provider.isStoragePath("https://example.com/photo.jpg"))
        assertFalse(provider.isStoragePath("/data/user/0/cache/photo.jpg"))
    }

    @Test
    fun `already resolved urls are returned without a network call`() = runTest {
        assertEquals("https://example.com/photo.jpg", provider.getSignedUrl("https://example.com/photo.jpg"))
        assertEquals("/data/user/0/cache/photo.jpg", provider.getSignedUrl("/data/user/0/cache/photo.jpg"))
    }
}

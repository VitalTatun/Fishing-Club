package com.example.fishing.viewmodel.photos

import com.example.fishing.model.FishingPhoto
import com.example.fishing.testutil.FakeFishingRepository
import com.example.fishing.testutil.testReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PhotoUrlResolverTest {

    private fun reportWithPhotos(vararg paths: String) = testReport().copy(
        photos = paths.mapIndexed { index, path ->
            FishingPhoto(id = UUID.randomUUID(), url = path)
        }
    )

    @Test
    fun `resolves distinct storage paths in one batch`() = runTest {
        val repository = FakeFishingRepository()
        val resolver = PhotoUrlResolver(repository)
        val report = reportWithPhotos("a/1.jpg", "a/2.jpg", "a/1.jpg")

        val resolved = resolver.resolveReportsPhotoUrls(listOf(report)).single()

        assertEquals(1, repository.getPhotoSignedUrlsCallCount)
        assertEquals(listOf("a/1.jpg", "a/2.jpg"), repository.getPhotoSignedUrlsRequests.single())
        assertEquals(
            listOf("signed:a/1.jpg", "signed:a/2.jpg", "signed:a/1.jpg"),
            resolved.photos.map { it.url }
        )
    }

    @Test
    fun `cached urls are reused across calls and applied synchronously`() = runTest {
        val repository = FakeFishingRepository()
        val resolver = PhotoUrlResolver(repository)
        val report = reportWithPhotos("a/1.jpg")

        resolver.resolveReportsPhotoUrls(listOf(report))
        assertEquals(1, repository.getPhotoSignedUrlsCallCount)

        // A later Room emission with raw paths is rendered with the cached URL
        // immediately, and the async resolve does not hit the repository again.
        val cached = resolver.applyCachedPhotoUrls(listOf(report)).single()
        assertEquals("signed:a/1.jpg", cached.photos.single().url)

        resolver.resolveReportsPhotoUrls(listOf(report))
        assertEquals(1, repository.getPhotoSignedUrlsCallCount)
    }

    @Test
    fun `failed lookup is not cached and is retried`() = runTest {
        val repository = FakeFishingRepository()
        repository.getPhotoSignedUrlsException = RuntimeException("offline")
        val resolver = PhotoUrlResolver(repository)
        val report = reportWithPhotos("a/1.jpg")

        val failed = resolver.resolveReportsPhotoUrls(listOf(report)).single()
        assertEquals("a/1.jpg", failed.photos.single().url)
        assertEquals(1, repository.getPhotoSignedUrlsCallCount)

        repository.getPhotoSignedUrlsException = null
        val retried = resolver.resolveReportsPhotoUrls(listOf(report)).single()
        assertEquals("signed:a/1.jpg", retried.photos.single().url)
        assertEquals(2, repository.getPhotoSignedUrlsCallCount)
    }

    @Test
    fun `expired cache entry is refreshed`() = runTest {
        val repository = FakeFishingRepository()
        var now = 0L
        val resolver = PhotoUrlResolver(repository, now = { now })
        val report = reportWithPhotos("a/1.jpg")

        resolver.resolveReportsPhotoUrls(listOf(report))
        assertEquals(1, repository.getPhotoSignedUrlsCallCount)

        now += 24L * 60 * 60 * 1000
        resolver.resolveReportsPhotoUrls(listOf(report))
        assertEquals(2, repository.getPhotoSignedUrlsCallCount)
    }

    @Test
    fun `clear drops every cached url`() = runTest {
        val repository = FakeFishingRepository()
        val resolver = PhotoUrlResolver(repository)
        val report = reportWithPhotos("a/1.jpg")

        resolver.resolveReportsPhotoUrls(listOf(report))
        resolver.clear()

        val resolved = resolver.resolveReportsPhotoUrls(listOf(report)).single()
        assertEquals(2, repository.getPhotoSignedUrlsCallCount)
        assertTrue(resolved.photos.single().url.startsWith("signed:"))
    }
}

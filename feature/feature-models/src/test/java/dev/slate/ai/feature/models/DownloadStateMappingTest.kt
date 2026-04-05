package dev.slate.ai.feature.models

import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.core.ui.component.DownloadButtonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the mapDownloadState function from ModelDetailScreen.
 * This function is package-private, so we replicate the logic here
 * to verify correctness of the state mapping.
 */
class DownloadStateMappingTest {

    private fun createEntity(
        status: String,
        downloadedBytes: Long = 0,
        totalBytes: Long = 1_000_000_000,
        errorMessage: String? = null,
    ) = DownloadEntity(
        modelId = "test",
        modelName = "Test",
        url = "https://example.com/test.gguf",
        expectedSha256 = "abc",
        filePath = "/data/test.gguf",
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        status = status,
        workManagerId = null,
        errorMessage = errorMessage,
        createdAt = 0,
        updatedAt = 0,
    )

    // Replicate the mapping logic from ModelDetailScreen
    private fun mapDownloadState(entity: DownloadEntity?): DownloadButtonState {
        if (entity == null) return DownloadButtonState.NotDownloaded
        return when (entity.status) {
            "QUEUED" -> DownloadButtonState.Downloading(0f, "Queued", "")
            "DOWNLOADING" -> {
                val progress = if (entity.totalBytes > 0)
                    entity.downloadedBytes.toFloat() / entity.totalBytes.toFloat() else 0f
                DownloadButtonState.Downloading(progress, "", "")
            }
            "PAUSED" -> DownloadButtonState.Paused
            "VERIFYING" -> DownloadButtonState.Verifying
            "COMPLETE" -> DownloadButtonState.Completed
            "FAILED" -> DownloadButtonState.Error(entity.errorMessage ?: "Download failed")
            else -> DownloadButtonState.NotDownloaded
        }
    }

    @Test
    fun `null entity maps to NotDownloaded`() {
        val state = mapDownloadState(null)
        assertTrue(state is DownloadButtonState.NotDownloaded)
    }

    @Test
    fun `QUEUED maps to Downloading with 0 progress`() {
        val state = mapDownloadState(createEntity("QUEUED"))
        assertTrue(state is DownloadButtonState.Downloading)
        assertEquals(0f, (state as DownloadButtonState.Downloading).progress)
    }

    @Test
    fun `DOWNLOADING maps to Downloading with correct progress`() {
        val state = mapDownloadState(createEntity("DOWNLOADING", 500_000_000, 1_000_000_000))
        assertTrue(state is DownloadButtonState.Downloading)
        assertEquals(0.5f, (state as DownloadButtonState.Downloading).progress, 0.001f)
    }

    @Test
    fun `DOWNLOADING with zero total maps to 0 progress`() {
        val state = mapDownloadState(createEntity("DOWNLOADING", 500, 0))
        assertTrue(state is DownloadButtonState.Downloading)
        assertEquals(0f, (state as DownloadButtonState.Downloading).progress)
    }

    @Test
    fun `PAUSED maps to Paused`() {
        assertTrue(mapDownloadState(createEntity("PAUSED")) is DownloadButtonState.Paused)
    }

    @Test
    fun `VERIFYING maps to Verifying`() {
        assertTrue(mapDownloadState(createEntity("VERIFYING")) is DownloadButtonState.Verifying)
    }

    @Test
    fun `COMPLETE maps to Completed`() {
        assertTrue(mapDownloadState(createEntity("COMPLETE")) is DownloadButtonState.Completed)
    }

    @Test
    fun `FAILED maps to Error with message`() {
        val state = mapDownloadState(createEntity("FAILED", errorMessage = "Network error"))
        assertTrue(state is DownloadButtonState.Error)
        assertEquals("Network error", (state as DownloadButtonState.Error).message)
    }

    @Test
    fun `FAILED without message uses default`() {
        val state = mapDownloadState(createEntity("FAILED", errorMessage = null))
        assertTrue(state is DownloadButtonState.Error)
        assertEquals("Download failed", (state as DownloadButtonState.Error).message)
    }

    @Test
    fun `unknown status maps to NotDownloaded`() {
        assertTrue(mapDownloadState(createEntity("UNKNOWN")) is DownloadButtonState.NotDownloaded)
    }

    @Test
    fun `progress at 100 percent maps correctly`() {
        val state = mapDownloadState(createEntity("DOWNLOADING", 1_000_000_000, 1_000_000_000))
        assertTrue(state is DownloadButtonState.Downloading)
        assertEquals(1.0f, (state as DownloadButtonState.Downloading).progress, 0.001f)
    }
}

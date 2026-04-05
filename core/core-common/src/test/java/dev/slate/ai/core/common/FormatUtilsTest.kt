package dev.slate.ai.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatFileSize returns 0 B for zero`() {
        assertEquals("0 B", 0L.formatFileSize())
    }

    @Test
    fun `formatFileSize returns 0 B for negative`() {
        assertEquals("0 B", (-1L).formatFileSize())
    }

    @Test
    fun `formatFileSize formats bytes`() {
        assertEquals("500.0 B", 500L.formatFileSize())
    }

    @Test
    fun `formatFileSize formats kilobytes`() {
        assertEquals("1.0 KB", 1024L.formatFileSize())
    }

    @Test
    fun `formatFileSize formats megabytes`() {
        assertEquals("1.0 MB", (1024L * 1024).formatFileSize())
    }

    @Test
    fun `formatFileSize formats gigabytes`() {
        assertEquals("1.0 GB", (1024L * 1024 * 1024).formatFileSize())
    }

    @Test
    fun `formatFileSize formats 1_1 GB model size`() {
        val size = 1181116006L // SmolLM2 1.7B
        val result = size.formatFileSize()
        assert(result.contains("GB")) { "Expected GB, got $result" }
        assert(result.startsWith("1.")) { "Expected ~1.1 GB, got $result" }
    }

    @Test
    fun `formatFileSize formats 2_3 GB model size`() {
        val size = 2394472448L // Phi-3 Mini
        val result = size.formatFileSize()
        assert(result.contains("GB")) { "Expected GB, got $result" }
        assert(result.startsWith("2.")) { "Expected ~2.2 GB, got $result" }
    }

    @Test
    fun `formatTokenRate formats rate`() {
        assertEquals("5.3 tok/s", 5.3f.formatTokenRate())
    }

    @Test
    fun `formatTokenRate formats zero`() {
        assertEquals("0.0 tok/s", 0f.formatTokenRate())
    }

    @Test
    fun `formatDuration formats seconds`() {
        assertEquals("5s", 5000L.formatDuration())
    }

    @Test
    fun `formatDuration formats minutes`() {
        assertEquals("2m 30s", 150000L.formatDuration())
    }

    @Test
    fun `formatDuration formats hours`() {
        assertEquals("1h 5m", 3900000L.formatDuration())
    }
}

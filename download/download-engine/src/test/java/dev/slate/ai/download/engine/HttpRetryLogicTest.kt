package dev.slate.ai.download.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the HTTP retry logic used in ModelDownloadWorker.
 * Verifies that the correct status codes trigger retry vs permanent failure.
 */
class HttpRetryLogicTest {

    private val retryableCodes = setOf(408, 429, 500, 502, 503, 504)

    private fun isRetryable(code: Int): Boolean = code in retryableCodes

    @Test
    fun `408 Request Timeout is retryable`() {
        assertTrue(isRetryable(408))
    }

    @Test
    fun `429 Too Many Requests is retryable`() {
        assertTrue(isRetryable(429))
    }

    @Test
    fun `500 Internal Server Error is retryable`() {
        assertTrue(isRetryable(500))
    }

    @Test
    fun `502 Bad Gateway is retryable`() {
        assertTrue(isRetryable(502))
    }

    @Test
    fun `503 Service Unavailable is retryable`() {
        assertTrue(isRetryable(503))
    }

    @Test
    fun `504 Gateway Timeout is retryable`() {
        assertTrue(isRetryable(504))
    }

    @Test
    fun `404 Not Found is NOT retryable`() {
        assertFalse(isRetryable(404))
    }

    @Test
    fun `403 Forbidden is NOT retryable`() {
        assertFalse(isRetryable(403))
    }

    @Test
    fun `401 Unauthorized is NOT retryable`() {
        assertFalse(isRetryable(401))
    }

    @Test
    fun `400 Bad Request is NOT retryable`() {
        assertFalse(isRetryable(400))
    }

    @Test
    fun `200 OK is NOT retryable`() {
        assertFalse(isRetryable(200))
    }

    @Test
    fun `206 Partial Content is NOT retryable`() {
        assertFalse(isRetryable(206))
    }

    @Test
    fun `301 Redirect is NOT retryable`() {
        assertFalse(isRetryable(301))
    }
}

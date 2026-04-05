package dev.slate.ai.download.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsufficientStorageExceptionTest {

    @Test
    fun `exception contains required and available bytes`() {
        val e = InsufficientStorageException(
            required = 2_000_000_000L,
            available = 500_000_000L,
        )
        assertEquals(2_000_000_000L, e.required)
        assertEquals(500_000_000L, e.available)
    }

    @Test
    fun `exception message includes sizes`() {
        val e = InsufficientStorageException(
            required = 2_000_000_000L,
            available = 500_000_000L,
        )
        assertTrue(e.message!!.contains("2000000000"))
        assertTrue(e.message!!.contains("500000000"))
    }

    @Test
    fun `exception is a standard Exception`() {
        val e = InsufficientStorageException(100, 50)
        assertTrue(e is Exception)
    }
}

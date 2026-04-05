package dev.slate.ai.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlateResultTest {

    @Test
    fun `Success holds data`() {
        val result: SlateResult<String> = SlateResult.Success("hello")
        assertTrue(result is SlateResult.Success)
        assertEquals("hello", (result as SlateResult.Success).data)
    }

    @Test
    fun `Error holds message`() {
        val result: SlateResult<String> = SlateResult.Error("failed")
        assertTrue(result is SlateResult.Error)
        assertEquals("failed", (result as SlateResult.Error).message)
    }

    @Test
    fun `Error holds optional cause`() {
        val cause = RuntimeException("boom")
        val result: SlateResult<String> = SlateResult.Error("failed", cause)
        assertEquals(cause, (result as SlateResult.Error).cause)
    }

    @Test
    fun `Error without cause has null cause`() {
        val result = SlateResult.Error("failed")
        assertNull(result.cause)
    }

    @Test
    fun `Loading is singleton`() {
        val a: SlateResult<String> = SlateResult.Loading
        val b: SlateResult<Int> = SlateResult.Loading
        assertTrue(a === b)
    }
}

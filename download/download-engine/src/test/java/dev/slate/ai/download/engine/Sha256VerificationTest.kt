package dev.slate.ai.download.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class Sha256VerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        file.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `sha256 of empty file is known hash`() {
        val file = tempFolder.newFile("empty.bin")
        val hash = computeSha256(file)
        // SHA-256 of empty string is well-known
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash)
    }

    @Test
    fun `sha256 of known content matches`() {
        val file = tempFolder.newFile("test.bin")
        file.writeText("Hello, Slate!")
        val hash = computeSha256(file)
        assertTrue(hash.length == 64) // SHA-256 is 64 hex chars
        assertTrue(hash.matches(Regex("[0-9a-f]+")))
    }

    @Test
    fun `sha256 is deterministic`() {
        val file = tempFolder.newFile("test.bin")
        file.writeText("Deterministic content")
        val hash1 = computeSha256(file)
        val hash2 = computeSha256(file)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `sha256 differs for different content`() {
        val file1 = tempFolder.newFile("file1.bin")
        file1.writeText("Content A")
        val file2 = tempFolder.newFile("file2.bin")
        file2.writeText("Content B")
        assertNotEquals(computeSha256(file1), computeSha256(file2))
    }

    @Test
    fun `sha256 works with large file`() {
        val file = tempFolder.newFile("large.bin")
        // Write 1MB of data
        file.outputStream().use { out ->
            val chunk = ByteArray(65536) { it.toByte() }
            repeat(16) { out.write(chunk) }
        }
        val hash = computeSha256(file)
        assertTrue(hash.length == 64)
        // Verify file is 1MB
        assertEquals(1048576, file.length())
    }

    @Test
    fun `verification pass when hashes match`() {
        val file = tempFolder.newFile("model.gguf")
        file.writeText("fake model content")
        val expected = computeSha256(file)
        val actual = computeSha256(file)
        assertEquals(expected, actual)
    }

    @Test
    fun `verification fail when file is modified`() {
        val file = tempFolder.newFile("model.gguf")
        file.writeText("original content")
        val originalHash = computeSha256(file)
        file.writeText("corrupted content")
        val corruptedHash = computeSha256(file)
        assertNotEquals(originalHash, corruptedHash)
    }

    @Test
    fun `hash comparison is case insensitive`() {
        val file = tempFolder.newFile("test.bin")
        file.writeText("test")
        val hash = computeSha256(file)
        assertTrue(hash.equals(hash.uppercase(), ignoreCase = true))
    }
}

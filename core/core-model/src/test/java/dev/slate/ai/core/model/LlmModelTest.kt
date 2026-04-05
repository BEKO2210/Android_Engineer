package dev.slate.ai.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmModelTest {

    private fun createModel(
        tier: ModelTier = ModelTier.BALANCED,
        minRamMb: Int = 6144,
        sizeBytes: Long = 1_900_000_000L,
    ) = LlmModel(
        id = "test-model",
        name = "Test Model",
        description = "A test model",
        sizeBytes = sizeBytes,
        quantization = "Q4_K_M",
        parameterCount = "3B",
        tier = tier,
        license = "Apache 2.0",
        downloadUrl = "https://example.com/model.gguf",
        sha256 = "abc123",
        minRamMb = minRamMb,
        fileName = "model.gguf",
        contextLength = 2048,
        family = "TestFamily",
        capabilities = listOf("Chat", "Reasoning"),
        deviceRecommendation = "6 GB+ RAM",
    )

    @Test
    fun `model has correct defaults`() {
        val model = LlmModel(
            id = "x", name = "X", description = "d",
            sizeBytes = 100, quantization = "Q4", parameterCount = "1B",
            tier = ModelTier.TINY, license = "MIT",
            downloadUrl = "url", sha256 = "", minRamMb = 2048, fileName = "x.gguf",
        )
        assertEquals(2048, model.contextLength)
        assertEquals("", model.family)
        assertTrue(model.capabilities.isEmpty())
        assertEquals("", model.deviceRecommendation)
    }

    @Test
    fun `model tiers are ordered`() {
        assertEquals(0, ModelTier.TINY.ordinal)
        assertEquals(1, ModelTier.BALANCED.ordinal)
        assertEquals(2, ModelTier.HEAVY.ordinal)
    }

    @Test
    fun `model status values exist`() {
        val statuses = ModelStatus.entries
        assertTrue(statuses.contains(ModelStatus.NOT_DOWNLOADED))
        assertTrue(statuses.contains(ModelStatus.DOWNLOADING))
        assertTrue(statuses.contains(ModelStatus.READY))
        assertTrue(statuses.contains(ModelStatus.ERROR))
    }

    @Test
    fun `model with capabilities stores them`() {
        val model = createModel()
        assertEquals(2, model.capabilities.size)
        assertEquals("Chat", model.capabilities[0])
    }

    @Test
    fun `model size is positive`() {
        val model = createModel()
        assertTrue(model.sizeBytes > 0)
    }

    @Test
    fun `model minRamMb is reasonable`() {
        val tinyModel = createModel(tier = ModelTier.TINY, minRamMb = 3072)
        val heavyModel = createModel(tier = ModelTier.HEAVY, minRamMb = 8192)
        assertTrue(tinyModel.minRamMb < heavyModel.minRamMb)
    }
}

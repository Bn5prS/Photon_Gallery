package com.inferno.gallery.data.ai

import com.inferno.gallery.workers.ModelDownloadWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRegistryTest {
    @Test
    fun testClipViTB16Constants() {
        val expectedVisionSha = "44eece4fe5fe4e0359a88268a327adf758633a1aade3917690b952bef1501f96"
        val expectedTextSha = "9106b51e6c663a56b99182ec617c2b3d53577b037e7e24a7717eb78048a0c97a"
        val expectedTokenizerSha = "72ed5c96db5729294468543e4bc75fce14ca63f58e37300290189ba1c1e52b85"
        
        val expectedVisionSize = 87_461_602L
        val expectedTextSize = 64_504_507L
        val expectedTokenizerSize = 2_224_081L

        assertEquals(expectedVisionSha, ModelDownloadWorker.VISION_MODEL_SHA256)
        assertEquals(expectedTextSha, ModelDownloadWorker.TEXT_MODEL_SHA256)
        assertEquals(expectedTokenizerSha, ModelDownloadWorker.TOKENIZER_SHA256)

        assertEquals(expectedVisionSize, ModelDownloadWorker.VISION_MODEL_SIZE)
        assertEquals(expectedTextSize, ModelDownloadWorker.TEXT_MODEL_SIZE)
        assertEquals(expectedTokenizerSize, ModelDownloadWorker.TOKENIZER_SIZE)
        assertEquals(3, ModelDownloadWorker.SEMANTIC_MODEL_VERSION)
    }
}

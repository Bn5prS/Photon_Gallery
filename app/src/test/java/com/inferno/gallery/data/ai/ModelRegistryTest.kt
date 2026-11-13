package com.inferno.gallery.data.ai

import com.inferno.gallery.workers.ModelDownloadWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRegistryTest {
    @Test
    fun testMobileClipS2Constants() {
        val expectedVisionSha = "16e40c4f6be7111ebc8230d0ae8dce6314a1e5e3ac010584e9c256e7acb7262d"
        val expectedTextSha = "f8aaa97cf17fbadd78b0bd6d5f6a1c57f712402cacc3055ea92c1a3331357dfe"
        val expectedTokenizerSha = "72ed5c96db5729294468543e4bc75fce14ca63f58e37300290189ba1c1e52b85"
        
        val expectedVisionSize = 36_735_889L
        val expectedTextSize = 64_117_260L
        val expectedTokenizerSize = 2_224_081L

        assertEquals(expectedVisionSha, ModelDownloadWorker.VISION_MODEL_SHA256)
        assertEquals(expectedTextSha, ModelDownloadWorker.TEXT_MODEL_SHA256)
        assertEquals(expectedTokenizerSha, ModelDownloadWorker.TOKENIZER_SHA256)

        assertEquals(expectedVisionSize, ModelDownloadWorker.VISION_MODEL_SIZE)
        assertEquals(expectedTextSize, ModelDownloadWorker.TEXT_MODEL_SIZE)
        assertEquals(expectedTokenizerSize, ModelDownloadWorker.TOKENIZER_SIZE)
        assertEquals(4, ModelDownloadWorker.SEMANTIC_MODEL_VERSION)
    }
}

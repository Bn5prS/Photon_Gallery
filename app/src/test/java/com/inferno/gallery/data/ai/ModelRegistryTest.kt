package com.inferno.gallery.data.ai

import com.inferno.gallery.workers.ModelDownloadWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRegistryTest {
    @Test
    fun testMobileClip2ModelConstants() {
        val expectedVisionSha = "8bb691c4085f8767c8237e324816a430f8264b5c2d4542ec2d9f88d701faa823"
        val expectedVisionDataSha = "365f1b95476a0852aaf5b136432383ff9405eedc3deb5347c2b8398a3838f50c"
        val expectedTextSha = "c7046e2a3c4078fd0b10865f3063bfa8e89c0808de3bd2fcaf89ba0d0a2fe90f"
        val expectedTextDataSha = "b0e6225b1a9e8b15b1abedcc430ebb369c9695831c9b0922acf9e83906e03e47"
        val expectedTokenizerSha = "43d4281659ce30b6aca2c7485944b3d73925bb7f87c7234daf3b6b094a6d618c"

        assertEquals(expectedVisionSha, ModelDownloadWorker.VISION_MODEL_SHA256)
        assertEquals(expectedVisionDataSha, ModelDownloadWorker.VISION_DATA_SHA256)
        assertEquals(expectedTextSha, ModelDownloadWorker.TEXT_MODEL_SHA256)
        assertEquals(expectedTextDataSha, ModelDownloadWorker.TEXT_DATA_SHA256)
        assertEquals(expectedTokenizerSha, ModelDownloadWorker.TOKENIZER_SHA256)

        assertEquals(2_310_022L, ModelDownloadWorker.VISION_MODEL_SIZE)
        assertEquals(143_589_376L, ModelDownloadWorker.VISION_DATA_SIZE)
        assertEquals(1_517_435L, ModelDownloadWorker.TEXT_MODEL_SIZE)
        assertEquals(253_755_392L, ModelDownloadWorker.TEXT_DATA_SIZE)
        assertEquals(3_642_035L, ModelDownloadWorker.TOKENIZER_SIZE)
        assertEquals(7, ModelDownloadWorker.SEMANTIC_MODEL_VERSION)
        assertEquals(256, SmartSearchEngine.IMAGE_SIZE)
        assertEquals(512, SmartSearchEngine.EMBEDDING_DIM)
    }
}

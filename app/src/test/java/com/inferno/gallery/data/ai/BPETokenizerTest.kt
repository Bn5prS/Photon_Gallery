package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BPETokenizerTest {

    private fun getTokenizer(): BPETokenizer {
        val file = listOf(
            File("models/tokenizer.json"),
            File("../models/tokenizer.json"),
            File("app/models/tokenizer.json")
        ).firstOrNull { it.exists() } ?: throw IllegalStateException("tokenizer.json not found in search paths")
        return BPETokenizer(file.readText())
    }

    @Test
    fun testEncodeSingleWordPadsWithZero() {
        val tokenizer = getTokenizer()
        val tokens = tokenizer.encode("dog")

        assertEquals(77, tokens.size)
        assertEquals(49406L, tokens[0]) // <|startoftext|>
        assertEquals(1929L, tokens[1])  // dog</w>
        assertEquals(49407L, tokens[2]) // <|endoftext|>

        // All remaining positions must be padded with 0L for MobileCLIP bidirectional encoder
        for (i in 3 until 77) {
            assertEquals("Padding at index $i must be 0", 0L, tokens[i])
        }
    }

    @Test
    fun testEncodeEmptyString() {
        val tokenizer = getTokenizer()
        val tokens = tokenizer.encode("")

        assertEquals(77, tokens.size)
        assertEquals(49406L, tokens[0])
        assertEquals(49407L, tokens[1])
        for (i in 2 until 77) {
            assertEquals("Padding at index $i must be 0", 0L, tokens[i])
        }
    }

    @Test
    fun testEncodeNaturalPhrase() {
        val tokenizer = getTokenizer()
        val tokens = tokenizer.encode("sunset at the beach")

        assertEquals(77, tokens.size)
        assertEquals(49406L, tokens[0]) // <|startoftext|>
        assertEquals(3424L, tokens[1])  // sunset
        assertEquals(536L, tokens[2])   // at
        assertEquals(518L, tokens[3])   // the
        assertEquals(2117L, tokens[4])  // beach
        assertEquals(49407L, tokens[5]) // <|endoftext|>

        for (i in 6 until 77) {
            assertEquals("Padding at index $i must be 0", 0L, tokens[i])
        }
    }
}

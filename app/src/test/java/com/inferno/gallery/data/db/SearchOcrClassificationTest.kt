package com.inferno.gallery.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOcrClassificationTest {

    @Test
    fun testExtractOcrSnippetExactMatch() {
        val text = "Please review your Invoice #12345 total $50.00 from Mastercard yesterday"
        val snippet = DatabaseProvider.extractOcrSnippet(text, "Invoice #12345")
        assertTrue(snippet.contains("Invoice #12345"))
    }

    @Test
    fun testExtractOcrSnippetHyphenatedAndPunctuation() {
        val text = "Welcome to our cafe. The Wi-Fi network password is SecretPassword123"
        val snippet = DatabaseProvider.extractOcrSnippet(text, "Wi-Fi")
        assertTrue(snippet.contains("Wi-Fi"))
    }

    @Test
    fun testExtractOcrSnippetTokenFallback() {
        val text = "Transaction receipt from supermarket payment card"
        val snippet = DatabaseProvider.extractOcrSnippet(text, "supermarket bill")
        assertTrue(snippet.contains("supermarket"))
    }

    @Test
    fun testTokenizationSplitsPunctuation() {
        val cleanQuery = "wi-fi user@test.com $50.00"
        val terms = cleanQuery.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.isNotBlank() }
        val expected = listOf("wi", "fi", "user", "test", "com", "50", "00")
        assertEquals(expected, terms)
    }
}

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

    @Test
    fun testFtsMultiTokenQueryUsesImplicitAndSpaceSyntax() {
        val ftsKeywords = setOf("AND", "OR", "NOT", "NEAR")
        val cleanQuery = "electric bill payment"
        val terms = cleanQuery.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() && it.uppercase() !in ftsKeywords }

        val andQuery = terms.joinToString(" ") { "$it*" }
        assertEquals("electric* bill* payment*", andQuery)
        assertTrue("Query should not contain literal AND keyword", !andQuery.contains(" AND "))

        val orQuery = terms.joinToString(" OR ") { "$it*" }
        assertEquals("electric* OR bill* OR payment*", orQuery)
    }

    @Test
    fun testFtsKeywordsIgnoredInTerms() {
        val ftsKeywords = setOf("AND", "OR", "NOT", "NEAR")
        val cleanQuery = "bread and butter or jam"
        val terms = cleanQuery.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() && it.uppercase() !in ftsKeywords }

        val andQuery = terms.joinToString(" ") { "$it*" }
        assertEquals("bread* butter* jam*", andQuery)
    }
}


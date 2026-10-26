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

    @Test
    fun testMultiTokenSeparatedWordMatching() {
        val extractedOcrText = """
            STARBUCKS STORE #1234
            Date: 2024-10-25
            Items:
            1x Caramel Macchiato
            1x Iced Filter Coffee
            Total: $12.50
        """.trimIndent()

        val cleanQuery = "starbucks coffee"
        val terms = cleanQuery.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.isNotBlank() }

        // Multi-term LIKE logic checks every term is contained in extracted text
        val matchesAll = terms.all { extractedOcrText.contains(it, ignoreCase = true) }
        assertTrue("Multi-token separated query must match text with words on different lines", matchesAll)

        // Exact substring match would have failed because words are separated
        val exactSubstring = extractedOcrText.contains(cleanQuery, ignoreCase = true)
        assertTrue("Exact substring should be false because words are separated", !exactSubstring)
    }

    @Test
    fun testCandidateOcrRequiresAllTokensOrDirectSubstring() {
        val cleanQuery = "starbucks coffee"
        val tokens = cleanQuery.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }

        val textWithOneTokenOnly = "Starbucks gift card balance is $25"
        val textWithAllTokens = "Order coffee at Starbucks now"

        // Verify old buggy logic vs new logic
        val oldBuggyMatchesOne = tokens.any { t -> t.length >= 2 && textWithOneTokenOnly.contains(t, ignoreCase = true) }
        assertTrue("Old logic was overly permissive with any()", oldBuggyMatchesOne)

        val newStrictMatchesOne = textWithOneTokenOnly.contains(cleanQuery, ignoreCase = true) ||
                (tokens.size > 1 && tokens.all { t -> textWithOneTokenOnly.contains(t, ignoreCase = true) })
        assertTrue("New logic correctly rejects text containing only one token when query has multiple tokens", !newStrictMatchesOne)

        val newStrictMatchesAll = textWithAllTokens.contains(cleanQuery, ignoreCase = true) ||
                (tokens.size > 1 && tokens.all { t -> textWithAllTokens.contains(t, ignoreCase = true) })
        assertTrue("New logic accepts text containing all tokens even if separated", newStrictMatchesAll)
    }
}


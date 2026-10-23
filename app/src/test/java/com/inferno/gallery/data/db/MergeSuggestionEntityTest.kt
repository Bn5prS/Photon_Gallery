package com.inferno.gallery.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeSuggestionEntityTest {

    @Test
    fun testMergeSuggestionCreationAndOrdering() {
        val suggestion = PersonMergeSuggestionEntity(
            personIdA = 10L,
            personIdB = 25L,
            similarity = 0.72f,
            status = "PENDING"
        )

        assertEquals(10L, suggestion.personIdA)
        assertEquals(25L, suggestion.personIdB)
        assertTrue(suggestion.personIdA < suggestion.personIdB)
        assertEquals(0.72f, suggestion.similarity, 0.001f)
        assertEquals("PENDING", suggestion.status)
    }

    @Test
    fun testMergeSuggestionStatuses() {
        val validStatuses = setOf("PENDING", "DISMISSED", "ACCEPTED")
        val pending = PersonMergeSuggestionEntity(1L, 2L, 0.70f, status = "PENDING")
        val dismissed = PersonMergeSuggestionEntity(1L, 2L, 0.70f, status = "DISMISSED")
        val accepted = PersonMergeSuggestionEntity(1L, 2L, 0.70f, status = "ACCEPTED")

        assertTrue(validStatuses.contains(pending.status))
        assertTrue(validStatuses.contains(dismissed.status))
        assertTrue(validStatuses.contains(accepted.status))
    }
}

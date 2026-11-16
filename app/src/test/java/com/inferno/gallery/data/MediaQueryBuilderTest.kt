package com.inferno.gallery.data

import org.junit.Assert.assertTrue
import org.junit.Test

class MediaQueryBuilderTest {

    @Test
    fun testDocBucketConditions() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.DOC_RECEIPTS,
            filterIndex = 0
        )
        assertTrue(
            "Expected document_classifications subquery condition",
            result.conditions.any { it.contains("document_classifications WHERE category = ?") }
        )
        assertTrue(
            "Expected args to contain DOC_RECEIPTS",
            result.args.contains(BucketNames.DOC_RECEIPTS)
        )
        assertTrue(
            "Expected not hidden condition",
            result.conditions.any { it.contains("cm.bucketName != 'Trash'") }
        )
    }

    @Test
    fun testDocPaymentsConditions() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.DOC_PAYMENTS,
            filterIndex = 0
        )
        assertTrue(
            "Expected document_classifications subquery condition",
            result.conditions.any { it.contains("document_classifications WHERE category = ?") }
        )
        assertTrue(
            "Expected args to contain DOC_PAYMENTS",
            result.args.contains(BucketNames.DOC_PAYMENTS)
        )
    }

    @Test
    fun testSearchTextConditions_withIds() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.SEARCH_TEXT,
            filterIndex = 0,
            ftsIds = listOf("101", "202", "303")
        )
        assertTrue(
            "Expected IN condition for FTS ids",
            result.conditions.any { it == "cm.id IN (101,202,303)" }
        )
        assertTrue(
            "Expected not hidden condition",
            result.conditions.any { it.contains("cm.bucketName != 'Trash'") }
        )
    }

    @Test
    fun testSearchTextConditions_empty() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.SEARCH_TEXT,
            filterIndex = 0,
            ftsIds = emptyList()
        )
        assertTrue(
            "Expected 0-match condition when no FTS ids",
            result.conditions.any { it == "cm.id IN (0)" }
        )
    }

    @Test
    fun testSearchSmartConditions_withIds() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.SEARCH_SMART,
            filterIndex = 0,
            smartIds = listOf("501", "502")
        )
        assertTrue(
            "Expected IN condition for smart ids",
            result.conditions.any { it == "cm.id IN (501,502)" }
        )
        assertTrue(
            "Expected not hidden condition",
            result.conditions.any { it.contains("cm.bucketName != 'Trash'") }
        )
    }

    @Test
    fun testSearchSmartConditions_empty() {
        val result = MediaQueryBuilder.buildMediaConditions(
            bucket = BucketNames.SEARCH_SMART,
            filterIndex = 0,
            smartIds = emptyList()
        )
        assertTrue(
            "Expected 0-match condition when no smart ids",
            result.conditions.any { it == "cm.id IN (0)" }
        )
    }

    @Test
    fun testSearchSmartOrderClause() {
        val orderClause = MediaQueryBuilder.buildOrderClause(
            order = "NewToOld",
            bucket = BucketNames.SEARCH_SMART,
            smartIds = listOf("501", "502")
        )
        assertTrue(
            "Expected CASE ordering for smart search relevance",
            orderClause.contains("ORDER BY CASE cm.id WHEN 501 THEN 0 WHEN 502 THEN 1 ELSE 2 END, cm.id DESC")
        )
    }
}

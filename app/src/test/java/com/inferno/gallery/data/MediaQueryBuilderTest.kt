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
}

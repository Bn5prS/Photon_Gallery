package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentClassifierTest {

    @Test
    fun testReceiptClassification() {
        val text = "SUPERMARKET INC\nTAX INVOICE\nTOTAL: $42.50\nCASHIER: 04\nSUBTOTAL: $40.00"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/IMG_01.jpg", "Camera")
        assertTrue(tags.contains(BucketNames.DOC_RECEIPTS))
        assertFalse("Retail receipt should not be categorized as payments", tags.contains(BucketNames.DOC_PAYMENTS))
    }

    @Test
    fun testPaymentClassificationDisambiguation() {
        // Digital UPI payment screenshot with amount and total
        val text = "UPI Transaction Successful\nPaid to John Doe\nUPI Ref No: 4092819283\nTotal Amount: INR 500"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Screenshots/pay.png", "Screenshots")
        assertTrue(tags.contains(BucketNames.DOC_PAYMENTS))
        assertFalse("Digital payment confirmation should not be categorized as receipt", tags.contains(BucketNames.DOC_RECEIPTS))
    }

    @Test
    fun testIdClassification() {
        val text = "REPUBLIC OF INDIA\nDRIVING LICENCE\nDOB: 12/04/1990\nVALID THRU: 2035"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/id.jpg", "Camera")
        assertEquals(setOf(BucketNames.DOC_IDS), tags)
    }

    @Test
    fun testEventsClassification() {
        val text = "BOARDING PASS\nFLIGHT: AI 102\nGATE: 14B\nSEAT: 22A\nADMIT ONE"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Download/ticket.pdf", "Download")
        assertTrue(tags.contains(BucketNames.DOC_EVENTS))
    }

    @Test
    fun testNotesClassification() {
        val text = "Lecture Notes: Quantum Mechanics\nDefinition:\nHomework assignment due Monday\nKey formula:"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Notes/note.jpg", "Notes")
        assertTrue(tags.contains(BucketNames.DOC_NOTES))
    }

    @Test
    fun testBooksClassification() {
        val text = "Chapter 3: The Early Years\nISBN: 978-3-16-148410-0\nPublished by Oxford University Press\nTable of Contents"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Books/cover.jpg", "Books")
        assertTrue(tags.contains(BucketNames.DOC_BOOKS_MAGAZINES))
    }

    @Test
    fun testSocialClassificationByText() {
        val text = "Retweeted by @developer\n1.2k Likes  45 Retweets\nReply to @user\nView replies"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Screenshots/tweet.png", "Screenshots")
        assertTrue(tags.contains(BucketNames.DOC_SOCIAL))
    }

    @Test
    fun testWhatsAppPhotoNotClassifiedAsSocialWithoutSocialCues() {
        // Forwarded casual photo or message on WhatsApp MUST NOT be categorized as social media document
        val tags = DocumentClassifier.classify("Hey how are you?", "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/photo.jpg", "WhatsApp Images")
        assertFalse("Random photo in WhatsApp must not be classified as DOC_SOCIAL", tags.contains(BucketNames.DOC_SOCIAL))
        assertTrue(tags.isEmpty())
    }

    @Test
    fun testWordBoundaryPreventsFalsePositives() {
        // "espresso Italian press" should not match Book "press" or Payment "visa" in "advisable"
        val text = "Enjoying our new espresso machine with advisable settings."
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/coffee.jpg", "Camera")
        assertTrue(tags.isEmpty())
    }

    @Test
    fun testNonDocumentMedia() {
        val text = "Sunset at the beach"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/sunset.jpg", "Camera")
        assertTrue(tags.isEmpty())
    }
}

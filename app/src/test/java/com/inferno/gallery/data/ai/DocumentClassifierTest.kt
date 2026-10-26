package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentClassifierTest {

    @Test
    fun testReceiptClassification() {
        val text = "SUPERMARKET INC\nTAX INVOICE\nTOTAL: $42.50\nCASHIER: 04\nSUBTOTAL: $40.00"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/IMG_01.jpg", "Camera")
        assertTrue(tags.contains(BucketNames.DOC_RECEIPTS))
    }

    @Test
    fun testPaymentClassification() {
        val text = "UPI Transaction Successful\nPaid to John Doe\nUPI Ref No: 4092819283\nAmount: INR 500"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Screenshots/pay.png", "Screenshots")
        assertTrue(tags.contains(BucketNames.DOC_PAYMENTS))
    }

    @Test
    fun testIdClassification() {
        val text = "REPUBLIC OF INDIA\nDRIVING LICENCE\nDOB: 12/04/1990\nVALID THRU: 2035"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/id.jpg", "Camera")
        assertTrue(tags.contains(BucketNames.DOC_IDS))
    }

    @Test
    fun testEventsClassification() {
        val text = "BOARDING PASS\nFLIGHT: AI 102\nGATE: 14B\nSEAT: 22A\nADMIT ONE"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Download/ticket.pdf", "Download")
        assertTrue(tags.contains(BucketNames.DOC_EVENTS))
    }

    @Test
    fun testNotesClassification() {
        val text = "Lecture 4: Quantum Mechanics\nDefinition:\nHomework assignment due Monday\nKey formula:"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Notes/note.jpg", "Notes")
        assertTrue(tags.contains(BucketNames.DOC_NOTES))
    }

    @Test
    fun testBooksClassification() {
        val text = "Chapter 3: The Early Years\nISBN: 978-3-16-148410-0\nPublished by Oxford Press\nTable of Contents"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Books/cover.jpg", "Books")
        assertTrue(tags.contains(BucketNames.DOC_BOOKS_MAGAZINES))
    }

    @Test
    fun testSocialClassificationByPath() {
        val tags = DocumentClassifier.classify("Hey how are you?", "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/photo.jpg", "WhatsApp Images")
        assertTrue(tags.contains(BucketNames.DOC_SOCIAL))
    }

    @Test
    fun testSocialClassificationByText() {
        val text = "Retweeted by @developer\n1.2k Likes  45 Retweets\nReply to @user"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Screenshots/tweet.png", "Screenshots")
        assertTrue(tags.contains(BucketNames.DOC_SOCIAL))
    }

    @Test
    fun testNonDocumentMedia() {
        val text = "Sunset at the beach"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/sunset.jpg", "Camera")
        assertTrue(tags.isEmpty())
    }
}

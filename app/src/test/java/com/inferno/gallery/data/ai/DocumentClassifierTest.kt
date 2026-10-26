package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentClassifierTest {

    @Test
    fun testReceiptWithLinebreaksAndColons() {
        val text = "SUPERMARKET INC\nTAX\nINVOICE\nTOTAL: $42.50\nCASHIER: 04\nSUBTOTAL: $40.00"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/IMG_01.jpg", "Camera")
        assertTrue("Must be classified as Receipt", tags.contains(BucketNames.DOC_RECEIPTS))
        assertFalse("Must not be classified as Payment", tags.contains(BucketNames.DOC_PAYMENTS))
    }

    @Test
    fun testPaymentScreenshotWithTotal() {
        val text = "UPI Transaction Successful\nPaid to John Doe\nUPI Ref No: 4092819283\nTotal Amount: INR 500"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Screenshots/pay.png", "Screenshots")
        assertTrue("Must be classified as Payment", tags.contains(BucketNames.DOC_PAYMENTS))
        assertFalse("Payment must override and not be in Receipt", tags.contains(BucketNames.DOC_RECEIPTS))
    }

    @Test
    fun testIdCardWithNewlinesAndPunctuation() {
        val text = "GOVT. OF INDIA\nDRIVING\nLICENCE\nDOB: 12/04/1990\nDL NO: DL-0420110012345"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/DCIM/Camera/id.jpg", "Camera")
        assertEquals(setOf(BucketNames.DOC_IDS), tags)
    }

    @Test
    fun testIdCardInFacebookFolderNeverClassifiedAsSocial() {
        // If an identity card is saved in Facebook pics or WhatsApp, it MUST go to IDs and NOT Social
        val text = "INCOME TAX DEPARTMENT\nGOVT. OF INDIA\nPERMANENT ACCOUNT NUMBER CARD\nDOB: 01/01/1990"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Facebook pics/pan_download.jpg", "Facebook pics")
        assertTrue("Must be classified as ID", tags.contains(BucketNames.DOC_IDS))
        assertFalse("ID card in social folder must NEVER be classified as Social", tags.contains(BucketNames.DOC_SOCIAL))
    }

    @Test
    fun testPanCardInDownloadFolder() {
        val text = "Permanent Account Number\nABCDE1234F\nFather Name: John"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Download/Pan card/IMG_001.jpg", "Pan card")
        assertEquals(setOf(BucketNames.DOC_IDS), tags)
    }

    @Test
    fun testFacebookDownloadedPostGoesToSocial() {
        // Normal post or meme downloaded from Facebook pics
        val text = "Check out this amazing photography tip of the day!"
        val tags = DocumentClassifier.classify(text, "/storage/emulated/0/Pictures/Facebook pics/100797490_3037720689650589_n.jpg", "Facebook pics")
        assertTrue("Facebook downloaded post must be classified as Social", tags.contains(BucketNames.DOC_SOCIAL))
        assertFalse("Must not be in ID", tags.contains(BucketNames.DOC_IDS))
    }

    @Test
    fun testEventsClassificationWithNewlines() {
        val text = "BOARDING\nPASS\nFLIGHT: AI 102\nGATE: 14B\nSEAT: 22A\nADMIT ONE"
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

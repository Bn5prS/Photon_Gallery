package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames

/**
 * High-performance, offline domain classifier that categorizes media items into
 * specialized document categories based on extracted OCR text, file paths, and folder names.
 */
object DocumentClassifier {

    private val RECEIPT_KEYWORDS = listOf(
        "tax invoice", "subtotal", "sub-total", "sub total", "grand total",
        "amount due", "bill to", "bill no", "cashier", "pos terminal",
        "thank you for shopping", "itemized", "gstin", "gst no", "vat reg",
        "cgst", "sgst", "igst", "merchant copy", "customer copy", "tax slip",
        "invoice no", "total:"
    )

    private val RECEIPT_PATHS = listOf("receipt", "invoice", "bill", "receipts", "invoices", "bills")

    private val PAYMENT_KEYWORDS = listOf(
        "upi transaction", "payment successful", "transaction successful",
        "transaction id", "reference no", "utr:", "utr no", "paid to",
        "received from", "debited from", "credited to", "gpay", "google pay",
        "phonepe", "paytm", "bhim", "debit card", "credit card", "visa",
        "mastercard", "rupay", "account balance", "transfer successful",
        "payment failed", "bank transfer", "upi ref", "paid successfully"
    )

    private val PAYMENT_PATHS = listOf("gpay", "phonepe", "paytm", "payments", "bhim", "googlepay")

    private val ID_KEYWORDS = listOf(
        "driving licence", "driver license", "driver's license", "driving license",
        "passport", "republic of", "identity card", "identification card",
        "national id", "voter id", "election commission", "date of birth",
        "dob:", "valid thru", "expires on", "expiry date", "card no:",
        "aadhaar", "pan card", "permanent account number", "social security",
        "ssn", "employee id", "student id", "govt of", "government of"
    )

    private val ID_PATHS = listOf("id_cards", "identity", "passport", "passports", "license", "licence")

    private val EVENT_KEYWORDS = listOf(
        "boarding pass", "flight:", "gate:", "seat:", "terminal:",
        "departure:", "arrival:", "airline", "admit one", "e-ticket",
        "eticket", "event ticket", "ticket no", "concert", "festival",
        "conference", "venue:", "auditorium", "stadium", "invitation",
        "rsvp", "entry pass", "show time", "movie ticket", "cinema"
    )

    private val EVENT_PATHS = listOf("tickets", "boarding_pass", "passes", "events")

    private val NOTE_KEYWORDS = listOf(
        "lecture", "homework", "assignment", "study notes", "chapter notes",
        "definition:", "formula:", "notebook", "class notes", "todo list",
        "to-do", "checklist:", "summary:"
    )

    private val NOTE_PATHS = listOf("notes", "samsung notes", "keep", "camscanner", "notability", "onenote", "goodnotes")

    private val BOOK_KEYWORDS = listOf(
        "isbn:", "isbn ", "isbn-", "table of contents", "chapter 1",
        "chapter 2", "chapter 3", "chapter 4", "chapter 5", "journal of",
        "published by", "all rights reserved", "press", "bibliography",
        "edition", "publisher"
    )

    private val BOOK_PATHS = listOf("books", "magazines", "kindle", "calibre", "ebooks")

    private val SOCIAL_KEYWORDS = listOf(
        "retweet", "retweeted", "tweet", "repost", "share post",
        "view replies", "reply to @", "followers", "following",
        "direct message", "send message", "add a comment", "typing..."
    )

    private val SOCIAL_PATHS = listOf(
        "whatsapp", "instagram", "telegram", "twitter", "reddit",
        "facebook", "snapchat", "discord", "tiktok", "threads",
        "messages", "messenger"
    )

    /**
     * Classifies a photo into zero or more document categories based on its
     * OCR extracted text, file path, and parent bucket/folder name.
     */
    fun classify(
        extractedText: String?,
        filePath: String?,
        bucketName: String?
    ): Set<String> {
        val categories = mutableSetOf<String>()
        val textLower = extractedText?.lowercase() ?: ""
        val pathLower = filePath?.lowercase() ?: ""
        val bucketLower = bucketName?.lowercase() ?: ""

        // Helper to check keywords
        fun hasKeyword(keywords: List<String>): Boolean {
            if (textLower.isEmpty()) return false
            return keywords.any { textLower.contains(it) }
        }

        // Helper to check paths
        fun hasPathOrBucket(hints: List<String>): Boolean {
            return hints.any { hint ->
                bucketLower.contains(hint) ||
                pathLower.contains("/$hint/") ||
                pathLower.contains("/$hint")
            }
        }

        // 1. Receipts & Invoices
        if (hasKeyword(RECEIPT_KEYWORDS) || (hasPathOrBucket(RECEIPT_PATHS) && textLower.contains("total"))) {
            categories.add(BucketNames.DOC_RECEIPTS)
        }

        // 2. Payments / Methods
        if (hasKeyword(PAYMENT_KEYWORDS) || (hasPathOrBucket(PAYMENT_PATHS) && textLower.isNotEmpty())) {
            categories.add(BucketNames.DOC_PAYMENTS)
        }

        // 3. IDs
        if (hasKeyword(ID_KEYWORDS) || hasPathOrBucket(ID_PATHS)) {
            categories.add(BucketNames.DOC_IDS)
        }

        // 4. Events & Tickets
        if (hasKeyword(EVENT_KEYWORDS) || (hasPathOrBucket(EVENT_PATHS) && textLower.isNotEmpty())) {
            categories.add(BucketNames.DOC_EVENTS)
        }

        // 5. Notes & Whiteboards
        if (hasKeyword(NOTE_KEYWORDS) || (hasPathOrBucket(NOTE_PATHS) && textLower.isNotEmpty())) {
            categories.add(BucketNames.DOC_NOTES)
        }

        // 6. Books and Magazines
        if (hasKeyword(BOOK_KEYWORDS) || (hasPathOrBucket(BOOK_PATHS) && textLower.contains("chapter"))) {
            categories.add(BucketNames.DOC_BOOKS_MAGAZINES)
        }

        // 7. Social Media
        val isSocialText = hasKeyword(SOCIAL_KEYWORDS) || (textLower.contains("likes") && textLower.contains("retweet"))
        val isSocialPath = hasPathOrBucket(SOCIAL_PATHS)
        if (isSocialText || isSocialPath) {
            categories.add(BucketNames.DOC_SOCIAL)
        }

        return categories
    }
}

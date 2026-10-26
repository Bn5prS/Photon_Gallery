package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames
import java.util.regex.Pattern

/**
 * High-performance, precision on-device domain classifier that categorizes media items into
 * specialized document categories based on extracted OCR text, file paths, and folder names.
 *
 * Employs a weighted score accumulation system with word boundary enforcement and cross-domain
 * disambiguation to eliminate false positives.
 */
object DocumentClassifier {

    // --- 1. RECEIPT DEFINITIVE PHRASES & TOKENS ---
    private val RECEIPT_HIGH_CONFIDENCE = listOf(
        "tax invoice", "retail invoice", "cash receipt", "sales receipt", "commercial invoice",
        "subtotal", "sub-total", "grand total", "net amount", "balance due", "amount payable",
        "cashier", "pos terminal", "merchant copy", "customer copy", "thank you for shopping",
        "gstin", "vat reg", "cgst", "sgst", "igst", "hsn code", "itemized", "tax summary"
    )

    private val RECEIPT_SECONDARY_WORDS = listOf(
        "receipt", "invoice", "bill", "cashier", "qty", "subtotal", "total", "change"
    )

    private val RECEIPT_PATHS = listOf("receipt", "invoice", "bill", "receipts", "invoices", "bills")

    // --- 2. PAYMENT DEFINITIVE PHRASES & TOKENS ---
    private val PAYMENT_HIGH_CONFIDENCE = listOf(
        "payment successful", "transaction successful", "paid successfully", "transfer successful",
        "payment completed", "transaction complete", "upi transaction", "upi ref", "transaction id",
        "reference no", "utr no", "google transaction id", "paid to", "money transferred to",
        "debited from", "credited to", "account balance", "available balance", "bank transfer",
        "credit card statement", "debit card statement", "card ending in", "bhim upi", "google pay",
        "phonepe", "paytm", "gpay", "apple pay", "paypal", "venmo", "cash app"
    )

    private val PAYMENT_SECONDARY_WORDS = listOf(
        "upi", "utr", "rrn", "gpay", "phonepe", "paytm", "bhim", "neft", "imps", "rtgs", "visa",
        "mastercard", "rupay", "debit", "credit"
    )

    private val PAYMENT_PATHS = listOf("gpay", "phonepe", "paytm", "payments", "bhim", "googlepay")

    // --- 3. ID DEFINITIVE PHRASES & TOKENS ---
    private val ID_HIGH_CONFIDENCE = listOf(
        "driving licence", "driving license", "driver license", "driver's license", "motor vehicles dept",
        "passport", "republic of", "passport no", "permanent account number", "income tax department",
        "pan card", "unique identification authority", "aadhaar", "uidai", "voter id",
        "election commission", "electoral photo identity", "identity card", "identification card",
        "national id", "social security card", "social security administration", "student identity card",
        "employee id card", "staff id card", "govt of india", "government of"
    )

    private val ID_SECONDARY_WORDS = listOf(
        "passport", "licence", "license", "aadhaar", "pan", "ssn", "voter", "identity", "dob"
    )

    private val ID_PATHS = listOf("id_cards", "identity", "passport", "passports", "license", "licence", "aadhaar")

    // --- 4. EVENT / TICKET DEFINITIVE PHRASES & TOKENS ---
    private val EVENT_HIGH_CONFIDENCE = listOf(
        "boarding pass", "flight no", "flight:", "gate:", "seat:", "terminal:", "boarding time",
        "departure time", "arrival time", "admit one", "e-ticket", "eticket", "event ticket",
        "ticket no", "pnr:", "movie ticket", "cinema:", "showtime:", "box office", "concert pass",
        "festival pass", "conference pass", "stadium entry", "cordially invited", "wedding invitation",
        "save the date", "rsvp:"
    )

    private val EVENT_SECONDARY_WORDS = listOf(
        "flight", "boarding", "ticket", "tickets", "cinema", "showtime", "concert", "invitation", "rsvp"
    )

    private val EVENT_PATHS = listOf("tickets", "boarding_pass", "passes", "events")

    // --- 5. NOTE DEFINITIVE PHRASES & TOKENS ---
    private val NOTE_HIGH_CONFIDENCE = listOf(
        "study notes", "chapter notes", "class notes", "lecture notes", "homework assignment",
        "to-do list", "todo list", "action items:", "key formula:", "definition:"
    )

    private val NOTE_SECONDARY_WORDS = listOf(
        "lecture", "homework", "assignment", "whiteboard", "notebook", "checklist", "formula", "theorem"
    )

    private val NOTE_PATHS = listOf("samsung notes", "keep", "camscanner", "notability", "onenote", "goodnotes")

    // --- 6. BOOK / MAGAZINE DEFINITIVE PHRASES & TOKENS ---
    private val BOOK_HIGH_CONFIDENCE = listOf(
        "table of contents", "published by", "all rights reserved", "university press",
        "publishing group", "first edition", "second edition", "third edition", "bibliography",
        "prologue", "epilogue", "isbn-10", "isbn-13", "isbn:"
    )

    private val BOOK_SECONDARY_WORDS = listOf(
        "isbn", "prologue", "epilogue", "bibliography", "publisher"
    )

    private val BOOK_PATHS = listOf("kindle", "calibre", "ebooks")

    // --- 7. SOCIAL MEDIA DEFINITIVE PHRASES & TOKENS ---
    private val SOCIAL_HIGH_CONFIDENCE = listOf(
        "retweet", "retweeted", "quote tweet", "tweet your reply", "replying to @", "reply to @",
        "view replies", "share post", "add a comment", "view all comments", "liked by",
        "share to reddit", "upvote", "downvote", "direct message", "typing...", "voice message"
    )

    private val SOCIAL_SECONDARY_WORDS = listOf(
        "retweet", "repost", "upvote", "downvote", "replies", "comments"
    )

    // Regex cache for single word boundary matching
    private val wordPatternCache = mutableMapOf<String, Pattern>()

    private fun matchesWord(text: String, word: String): Boolean {
        val pattern = wordPatternCache.getOrPut(word) {
            Pattern.compile("\\b${Pattern.quote(word)}\\b", Pattern.CASE_INSENSITIVE)
        }
        return pattern.matcher(text).find()
    }

    private fun containsPhrase(text: String, phrase: String): Boolean {
        return text.contains(phrase, ignoreCase = true)
    }

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
        val text = extractedText?.trim() ?: ""
        if (text.length < 5) {
            // Insufficient or absent OCR text — documents require readable text
            return emptySet()
        }

        val textLower = text.lowercase()
        val pathLower = filePath?.lowercase() ?: ""
        val bucketLower = bucketName?.lowercase() ?: ""

        fun hasPathHint(hints: List<String>): Boolean {
            return hints.any { hint ->
                bucketLower.contains(hint) ||
                pathLower.contains("/$hint/") ||
                pathLower.contains("/$hint")
            }
        }

        var idScore = 0
        var paymentScore = 0
        var receiptScore = 0
        var eventScore = 0
        var noteScore = 0
        var bookScore = 0
        var socialScore = 0

        // 1. IDs evaluation
        for (phrase in ID_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) idScore += 5
        }
        for (word in ID_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) idScore += 2
        }
        if (hasPathHint(ID_PATHS)) idScore += 3

        // 2. Payments evaluation
        for (phrase in PAYMENT_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) paymentScore += 5
        }
        for (word in PAYMENT_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) paymentScore += 2
        }
        if (hasPathHint(PAYMENT_PATHS)) paymentScore += 2

        // 3. Receipts evaluation
        for (phrase in RECEIPT_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) receiptScore += 5
        }
        for (word in RECEIPT_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) receiptScore += 2
        }
        if (hasPathHint(RECEIPT_PATHS)) receiptScore += 2

        // 4. Events evaluation
        for (phrase in EVENT_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) eventScore += 5
        }
        for (word in EVENT_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) eventScore += 2
        }
        if (hasPathHint(EVENT_PATHS)) eventScore += 2

        // 5. Notes evaluation
        for (phrase in NOTE_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) noteScore += 5
        }
        for (word in NOTE_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) noteScore += 2
        }
        if (hasPathHint(NOTE_PATHS)) noteScore += 3

        // 6. Books evaluation
        for (phrase in BOOK_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) bookScore += 5
        }
        for (word in BOOK_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) bookScore += 2
        }
        if (hasPathHint(BOOK_PATHS)) bookScore += 3
        if (textLower.contains(Regex("\\bchapter\\s+\\d+\\b"))) bookScore += 4

        // 7. Social Media evaluation
        for (phrase in SOCIAL_HIGH_CONFIDENCE) {
            if (containsPhrase(textLower, phrase)) socialScore += 5
        }
        for (word in SOCIAL_SECONDARY_WORDS) {
            if (matchesWord(textLower, word)) socialScore += 2
        }
        if (textLower.contains("retweet") && textLower.contains("likes")) socialScore += 4
        if (textLower.contains("r/") || textLower.contains("u/")) socialScore += 4

        // --- DISAMBIGUATION & DECISION RULES ---
        val THRESHOLD = 4

        // A. IDs: If ID score is high, it is definitively an ID
        if (idScore >= THRESHOLD) {
            categories.add(BucketNames.DOC_IDS)
            // Suppress notes/social false positives on ID cards
            noteScore = 0
            socialScore = 0
        }

        // B. Digital Payments vs Store Receipts
        // If it's a digital payment confirmation (UPI/GPay/PhonePe/bank transfer), it belongs to DOC_PAYMENTS
        if (paymentScore >= THRESHOLD && paymentScore >= receiptScore) {
            categories.add(BucketNames.DOC_PAYMENTS)
        } else if (receiptScore >= THRESHOLD) {
            categories.add(BucketNames.DOC_RECEIPTS)
        } else if (paymentScore >= THRESHOLD) {
            categories.add(BucketNames.DOC_PAYMENTS)
        }

        // C. Events & Tickets
        if (eventScore >= THRESHOLD) {
            categories.add(BucketNames.DOC_EVENTS)
        }

        // D. Notes & Whiteboards
        if (noteScore >= THRESHOLD && !categories.contains(BucketNames.DOC_IDS)) {
            categories.add(BucketNames.DOC_NOTES)
        }

        // E. Books & Magazines
        if (bookScore >= THRESHOLD) {
            categories.add(BucketNames.DOC_BOOKS_MAGAZINES)
        }

        // F. Social Media
        // Strict: Must have high-confidence social interaction signals, NEVER just folder path
        if (socialScore >= THRESHOLD && !categories.contains(BucketNames.DOC_IDS)) {
            categories.add(BucketNames.DOC_SOCIAL)
        }

        return categories
    }
}

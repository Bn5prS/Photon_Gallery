package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames

/**
 * High-performance, high-accuracy on-device classifier that categorizes media items into
 * specialized document categories based on extracted OCR text, file paths, and folder names.
 *
 * Employs full whitespace & punctuation normalization, whole-token phrase matching,
 * weighted category scoring, and explicit cross-domain precedence to ensure both high recall
 * and zero false-positive cross-contamination (e.g. ID cards never end up in Social).
 */
object DocumentClassifier {

    // --- 1. RECEIPT KEYWORDS ---
    private val RECEIPT_HIGH_CONFIDENCE = listOf(
        "tax invoice", "retail invoice", "cash receipt", "sales receipt", "commercial invoice",
        "subtotal", "sub total", "grand total", "net amount", "balance due", "amount payable",
        "cashier", "pos terminal", "merchant copy", "customer copy", "thank you for shopping",
        "gstin", "vat reg", "cgst", "sgst", "igst", "hsn code", "tax summary", "bill to",
        "bill no", "invoice no", "total amount", "net payable"
    )

    private val RECEIPT_SECONDARY_WORDS = listOf(
        "receipt", "invoice", "cashier", "qty", "subtotal", "mrp", "discount"
    )

    private val RECEIPT_PATHS = listOf("receipt", "invoice", "bill", "receipts", "invoices", "bills")

    // --- 2. PAYMENT KEYWORDS ---
    private val PAYMENT_HIGH_CONFIDENCE = listOf(
        "payment successful", "transaction successful", "paid successfully", "transfer successful",
        "payment completed", "transaction complete", "upi transaction", "upi ref", "transaction id",
        "reference no", "utr no", "google transaction id", "paid to", "money transferred to",
        "debited from", "credited to", "account balance", "available balance", "bank transfer",
        "credit card statement", "debit card statement", "card ending in", "bhim upi", "google pay",
        "phonepe", "paytm", "gpay", "apple pay", "paypal", "venmo", "cash app", "money sent"
    )

    private val PAYMENT_SECONDARY_WORDS = listOf(
        "upi", "utr", "rrn", "gpay", "phonepe", "paytm", "bhim", "neft", "imps", "rtgs"
    )

    private val PAYMENT_PATHS = listOf("gpay", "phonepe", "paytm", "payments", "bhim", "googlepay")

    // --- 3. ID KEYWORDS ---
    private val ID_HIGH_CONFIDENCE = listOf(
        "driving licence", "driving license", "driver license", "driver license", "motor vehicles",
        "dl no", "passport", "republic of india", "republic of", "passport no", "permanent account number",
        "income tax department", "pan card", "pan no", "unique identification authority", "aadhaar",
        "aadhar", "uidai", "voter id", "election commission", "electoral photo", "epic no",
        "identity card", "identification card", "national id", "citizen id", "social security card",
        "social security administration", "student id", "student identity", "college id",
        "university id", "school id", "campus id", "employee id", "staff id", "faculty id",
        "govt of india", "government of india", "govt of", "government of", "mera aadhaar",
        "roll no", "reg no", "admission no", "enrolment no", "enrollment no"
    )

    private val ID_SECONDARY_WORDS = listOf(
        "passport", "licence", "license", "aadhaar", "aadhar", "uidai", "voter", "identity",
        "student", "employee"
    )

    private val ID_PATHS = listOf("id_cards", "id_card", "identity", "passport", "passports", "license", "licence", "aadhaar", "aadhar", "pan card", "pancard")

    // --- 4. EVENT / TICKET KEYWORDS ---
    private val EVENT_HIGH_CONFIDENCE = listOf(
        "boarding pass", "flight no", "flight", "gate no", "seat no", "terminal", "boarding time",
        "departure time", "arrival time", "admit one", "e ticket", "eticket", "event ticket",
        "ticket no", "pnr", "movie ticket", "cinema", "showtime", "box office", "bookmyshow",
        "concert pass", "festival pass", "conference pass", "stadium entry", "cordially invited",
        "wedding invitation", "save the date", "rsvp"
    )

    private val EVENT_SECONDARY_WORDS = listOf(
        "boarding", "airline", "cinema", "showtime", "concert", "invitation", "rsvp", "ticket", "tickets"
    )

    private val EVENT_PATHS = listOf("tickets", "ticket", "boarding_pass", "passes", "events")

    // --- 5. NOTE KEYWORDS ---
    private val NOTE_HIGH_CONFIDENCE = listOf(
        "study notes", "chapter notes", "class notes", "lecture notes", "homework assignment",
        "to do list", "todo list", "action items", "key formula", "definition", "lecture",
        "homework", "assignment"
    )

    private val NOTE_SECONDARY_WORDS = listOf(
        "whiteboard", "notebook", "checklist", "formula", "theorem", "derivation"
    )

    private val NOTE_PATHS = listOf("samsung notes", "keep", "camscanner", "notability", "onenote", "goodnotes", "notes")

    // --- 6. BOOK / MAGAZINE KEYWORDS ---
    private val BOOK_HIGH_CONFIDENCE = listOf(
        "table of contents", "published by", "all rights reserved", "university press",
        "publishing group", "first edition", "second edition", "third edition", "bibliography",
        "prologue", "epilogue", "isbn 10", "isbn 13", "isbn"
    )

    private val BOOK_SECONDARY_WORDS = listOf(
        "isbn", "prologue", "epilogue", "bibliography", "publisher", "magazine", "journal"
    )

    private val BOOK_PATHS = listOf("kindle", "calibre", "ebooks", "books", "magazines")

    // --- 7. SOCIAL MEDIA KEYWORDS & PATHS ---
    private val SOCIAL_HIGH_CONFIDENCE = listOf(
        "retweet", "retweeted", "quote tweet", "tweet your reply", "replying to", "reply to",
        "view replies", "share post", "add a comment", "view all comments", "liked by",
        "share to reddit", "upvote", "downvote", "direct message", "typing", "voice message",
        "instagram", "facebook", "twitter", "threads net", "tiktok"
    )

    private val SOCIAL_SECONDARY_WORDS = listOf(
        "repost", "replies", "comments", "followers", "following", "likes"
    )

    private val SOCIAL_PATHS = listOf(
        "facebook", "facebook pics", "instagram", "twitter", "reddit", "threads", "tiktok",
        "snapchat", "telegram"
    )

    /**
     * Normalizes text by converting to lowercase and collapsing all newlines, tabs,
     * and punctuation into single spaces. Returns padded text with leading and trailing spaces.
     */
    private fun normalize(raw: String): String {
        val clean = raw.lowercase()
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
        return if (clean.isEmpty()) "" else " $clean "
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
        val rawText = extractedText?.trim() ?: ""
        val paddedText = normalize(rawText)

        val pathLower = filePath?.lowercase() ?: ""
        val bucketLower = bucketName?.lowercase() ?: ""

        fun hasPathHint(hints: List<String>): Boolean {
            return hints.any { hint ->
                bucketLower.contains(hint) ||
                pathLower.contains("/$hint/") ||
                pathLower.contains("/$hint")
            }
        }

        fun containsToken(phrase: String): Boolean {
            if (paddedText.isEmpty()) return false
            val cleanPhrase = phrase.trim().replace(Regex("[^a-z0-9]+"), " ")
            return paddedText.contains(" $cleanPhrase ")
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
            if (containsToken(phrase)) idScore += 4
        }
        for (word in ID_SECONDARY_WORDS) {
            if (containsToken(word)) idScore += 2
        }
        if (hasPathHint(ID_PATHS)) idScore += 4
        if (containsToken("dob") || containsToken("date of birth")) {
            idScore += 2
        }
        if (containsToken("valid thru") || containsToken("valid till") || containsToken("valid upto")) {
            idScore += 2
        }

        // 2. Payments evaluation
        for (phrase in PAYMENT_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) paymentScore += 4
        }
        for (word in PAYMENT_SECONDARY_WORDS) {
            if (containsToken(word)) paymentScore += 2
        }
        if (hasPathHint(PAYMENT_PATHS)) paymentScore += 3

        // 3. Receipts evaluation
        for (phrase in RECEIPT_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) receiptScore += 4
        }
        for (word in RECEIPT_SECONDARY_WORDS) {
            if (containsToken(word)) receiptScore += 2
        }
        if (hasPathHint(RECEIPT_PATHS)) receiptScore += 3
        if (containsToken("total") && (containsToken("cash") || containsToken("change") || containsToken("card") || containsToken("bill"))) {
            receiptScore += 3
        }

        // 4. Events evaluation
        for (phrase in EVENT_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) eventScore += 4
        }
        for (word in EVENT_SECONDARY_WORDS) {
            if (containsToken(word)) eventScore += 2
        }
        if (hasPathHint(EVENT_PATHS)) eventScore += 3

        // 5. Notes evaluation
        for (phrase in NOTE_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) noteScore += 4
        }
        for (word in NOTE_SECONDARY_WORDS) {
            if (containsToken(word)) noteScore += 2
        }
        if (hasPathHint(NOTE_PATHS)) noteScore += 3

        // 6. Books evaluation
        for (phrase in BOOK_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) bookScore += 4
        }
        for (word in BOOK_SECONDARY_WORDS) {
            if (containsToken(word)) bookScore += 2
        }
        if (hasPathHint(BOOK_PATHS)) bookScore += 3
        if (paddedText.contains(Regex("\\bchapter\\s+\\d+\\b"))) bookScore += 4

        // 7. Social Media evaluation
        for (phrase in SOCIAL_HIGH_CONFIDENCE) {
            if (containsToken(phrase)) socialScore += 4
        }
        for (word in SOCIAL_SECONDARY_WORDS) {
            if (containsToken(word)) socialScore += 2
        }
        if (paddedText.contains(" r ") || paddedText.contains(" u ")) socialScore += 3
        if (hasPathHint(SOCIAL_PATHS)) {
            // Dedicated social folder (Facebook pics, Instagram, Reddit, Twitter, etc.)
            socialScore += 4
        }

        // --- DISAMBIGUATION & DECISION RULES ---
        val THRESHOLD = 3

        // A. IDs: Absolute highest priority. If it's an ID, it is NEVER Social, Notes, or Receipt.
        val isId = idScore >= THRESHOLD
        if (isId) {
            categories.add(BucketNames.DOC_IDS)
        }

        // B. Digital Payments vs Store Receipts
        val isPayment = paymentScore >= THRESHOLD && !isId
        val isReceipt = receiptScore >= THRESHOLD && !isId

        if (isPayment && paymentScore >= receiptScore) {
            categories.add(BucketNames.DOC_PAYMENTS)
        } else if (isReceipt) {
            categories.add(BucketNames.DOC_RECEIPTS)
        } else if (isPayment) {
            categories.add(BucketNames.DOC_PAYMENTS)
        }

        // C. Events & Tickets
        if (eventScore >= THRESHOLD && !isId && !isPayment) {
            categories.add(BucketNames.DOC_EVENTS)
        }

        // D. Notes & Whiteboards
        if (noteScore >= THRESHOLD && !isId && !isPayment && !isReceipt) {
            categories.add(BucketNames.DOC_NOTES)
        }

        // E. Books & Magazines
        if (bookScore >= THRESHOLD && !isId && !isPayment && !isReceipt) {
            categories.add(BucketNames.DOC_BOOKS_MAGAZINES)
        }

        // F. Social Media:
        // Strictly barred if image is an ID card, payment transaction, store receipt, or ticket.
        val hasSpecificDoc = isId || isPayment || isReceipt || categories.contains(BucketNames.DOC_EVENTS)
        if (socialScore >= THRESHOLD && !hasSpecificDoc) {
            categories.add(BucketNames.DOC_SOCIAL)
        }

        return categories
    }
}

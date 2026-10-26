# Documents Smart Classification Hub — Design Document

## 1. Intent & The Why
Users frequently take photos of paper documents, receipts, invoices, IDs, notes, and social media posts, but these utility captures clutter the primary camera roll and get lost among thousands of memories. 

Photon Gallery already possesses an on-device OCR engine (ML Kit) and SQLite Full-Text Search (`image_fts`). By introducing an expandable **Documents** section on the Albums screen with 7 dedicated categories, users can instantly access and organize their paperwork, payments, tickets, and reading material in a dedicated view without manual album creation.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **7 Dedicated Document Categories**:
  1. **Books and Magazines**: Book pages, covers, articles, journals, ISBNs.
  2. **Events**: Tickets, boarding passes, invitations, festival/concert flyers.
  3. **IDs**: Driver's licenses, passports, identity cards, badges, government IDs.
  4. **Notes**: Handwritten notes, whiteboards, notebook pages, study material.
  5. **Payments / Methods**: UPI transaction slips, payment proofs, QR codes, credit/debit card statements.
  6. **Receipts**: Store bills, restaurant checks, retail invoices, tax slips.
  7. **Social**: Social media screenshots, chat captures, memes, Twitter/X posts, WhatsApp forwards.
- **Dedicated Classification Storage**: A clean `document_classifications` table in SQLite (`mediaId`, `category`) with indexing and cascade cleanup triggers.
- **Instant Backfill & Rescan Support**: Fast initial backfill from existing `image_fts` text so already-indexed photos populate immediately, coupled with full rescan integration in `OcrIndexWorker` and `SettingsViewModel.rebuildOcrIndex()`.
- **Seamless Gallery Grid Navigation**: Tapping any category card routes to the standard full-screen media grid with date grouping, multiselect, and full viewer actions via `MediaQueryBuilder` (`doc:<category>`).
- **Material 3 Expressive UI**: Collapsible section header in [`AlbumsScreen.kt`](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/app/src/main/java/com/inferno/gallery/ui/AlbumsScreen.kt) with persistent expand state, responsive `LazyRow` carousel with `ShapeLargeIncreased` squircle cards, press-scale physics, and official Material Symbols Rounded icons.

### Non-Goals
- Cloud OCR or external network APIs (must remain 100% on-device and private).
- Replacing standard folder albums (Camera, Downloads, etc. remain unchanged in the Albums section).
- Heavy deep learning document boundary warping at browse-time (instant 60/120fps grid browsing).

### Anti-Goals
- Blocking the UI thread or camera roll during classification.
- Forcing users to wait hours for a complete re-scan before seeing existing photos categorized.
- Raw hex colors or non-Material Symbols icons in UI composables.

---

## 3. Approaches Considered & Tradeoffs

1. **Approach A: Pure Dynamic SQL FTS Queries**
   - *Architecture*: Executes dynamic SQLite `MATCH` / `LIKE` expressions every time the album grid loads.
   - *Tradeoffs*: Lightweight and requires no new table, but complex multi-keyword OR queries can become sluggish on large media collections (10,000+ photos).

2. **Approach B: Dedicated Classification Table & Worker Classifier (Chosen)**
   - *Architecture*: Creates a normalized `document_classifications(mediaId, category)` table. `DocumentClassifier` evaluates extracted text and folder metadata during OCR indexing. Startup backfill classifies existing `image_fts` records in milliseconds.
   - *Tradeoffs*: Requires a clean database table creation in `DatabaseProvider.kt` (using the established `onOpen` hook pattern) and worker integration. Provides indexed query performance (<2ms) and robust multi-category association.

---

## 4. Detailed Specification

### A. Database Layer (`DatabaseProvider.kt` & `GalleryDatabase.kt`)
1. **Schema**:
   ```sql
   CREATE TABLE IF NOT EXISTS document_classifications (
       mediaId INTEGER NOT NULL,
       category TEXT NOT NULL,
       PRIMARY KEY(mediaId, category)
   );
   CREATE INDEX IF NOT EXISTS index_doc_classifications_category ON document_classifications(category);
   CREATE INDEX IF NOT EXISTS index_doc_classifications_mediaId ON document_classifications(mediaId);

   CREATE TRIGGER IF NOT EXISTS core_media_doc_classifications_delete
   AFTER DELETE ON core_media
   BEGIN
       DELETE FROM document_classifications WHERE mediaId = old.id;
   END;
   ```
2. **MediaDao Queries**:
   - `observeDocumentCategoryStats(category: String): Flow<MediaAggregateStats>`
   - `observeDocumentCategoryCounts(): Flow<List<DocumentCategoryStat>>`
   - Fast backfill query: populates `document_classifications` from existing `image_fts` rows if table is empty.

### B. Classification Engine (`DocumentClassifier.kt`)
Evaluates `extractedText`, `filePath`, and `bucketName` to return `Set<String>` of matched category keys:
- `DOC_BOOKS_MAGAZINES` (`"doc:books_magazines"`): ISBN, edition, publisher, chapter, volume, author, magazine, journal, contents, page, book folders.
- `DOC_EVENTS` (`"doc:events"`): Ticket, admit one, boarding pass, gate, seat, venue, concert, conference, festival, show, invitation, pass.
- `DOC_IDS` (`"doc:ids"`): License, driving licence, passport, identity card, identification, dob, valid thru, expires, government of, national id, aadhaar, pan card, employee id.
- `DOC_NOTES` (`"doc:notes"`): Note, notes, lecture, homework, assignment, study, exam, formula, notebook, or high word count / line density with handwritten markers.
- `DOC_PAYMENTS` (`"doc:payments"`): UPI, transaction id, reference no, utr, payment successful, paid to, credited, debited, gpay, phonepe, paytm, transfer, bank, visa, mastercard.
- `DOC_RECEIPTS` (`"doc:receipts"`): Receipt, invoice, tax invoice, subtotal, grand total, gst, vat, cashier, amount due, pos, store bill.
- `DOC_SOCIAL` (`"doc:social"`): WhatsApp, Instagram, Twitter/X, Reddit, Telegram, Snapchat, Discord paths or social UI markers (retweet, tweet, replied, followers, likes, direct message, typing...).

### C. Worker Integration (`OcrIndexWorker.kt`)
- During stage 3 (batch flush), after inserting into `image_fts`, run `DocumentClassifier.classify(...)` and insert results into `document_classifications`.
- When user taps "Rebuild OCR Index" in Settings, `document_classifications` is cleared and re-populated.

### D. Navigation & Grid Routing (`MediaQueryBuilder.kt` & `MainAppLayout.kt`)
- Constants in `BucketNames.kt`:
  - `DOC_BOOKS_MAGAZINES = "doc:books_magazines"`
  - `DOC_EVENTS = "doc:events"`
  - `DOC_IDS = "doc:ids"`
  - `DOC_NOTES = "doc:notes"`
  - `DOC_PAYMENTS = "doc:payments"`
  - `DOC_RECEIPTS = "doc:receipts"`
  - `DOC_SOCIAL = "doc:social"`
- In `MediaQueryBuilder.kt`:
  ```kotlin
  bucket?.startsWith("doc:") == true -> {
      conditions.add("cm.id IN (SELECT mediaId FROM document_classifications WHERE category = ?)")
      args.add(bucket)
      conditions.add(NOT_HIDDEN)
  }
  ```
- Tapping a document card calls `onAlbumClick("doc:<category>")`, which loads into `GalleryScreen` with appropriate top bar title (e.g. "Receipts", "Notes", "IDs", etc.).

### E. UI Layer (`AlbumsScreen.kt`)
- **Header**:
  - Title: `"Documents"`
  - Total items count badge
  - Collapsible expand/collapse toggle (`viewModel.albumsExpandedDocuments`)
- **Carousel Items**:
  - Horizontal `LazyRow` with spacing `SpacingTokens.M`.
  - Cards: `140.dp` width, `96.dp` height, `ShapeLargeIncreased` (20dp corner radius), `pressScale(0.96f)`.
  - Image cover thumbnail from newest media in that category.
  - Gradient scrim + Material Symbols Rounded icon + category title + item count.
  - Icons:
    - Books and Magazines: `R.drawable.ic_ms_menu_book`
    - Events: `R.drawable.ic_ms_confirmation_number`
    - IDs: `R.drawable.ic_ms_badge`
    - Notes: `R.drawable.ic_ms_edit_note`
    - Payments / Methods: `R.drawable.ic_ms_payments`
    - Receipts: `R.drawable.ic_ms_receipt_long`
    - Social: `R.drawable.ic_ms_share`

---

## 5. What's Left to the Builder
- Ensuring drawables for any missing Material Symbols exist or are created cleanly in vector XML format.
- Adding unit tests for `DocumentClassifier` covering all 7 categories with diverse text snippets.

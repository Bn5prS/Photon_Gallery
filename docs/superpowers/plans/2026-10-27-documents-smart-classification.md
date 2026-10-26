# Documents Smart Classification Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dedicated, intelligent "Documents" expandable category to the Albums tab with 7 specialized document categories (Books and Magazines, Events, IDs, Notes, Payments / Methods, Receipts, Social), powered by on-device OCR and path metadata, navigating directly to the full-screen gallery grid.

**Architecture:** A normalized SQLite table `document_classifications(mediaId, category)` indexed for instant lookups. `DocumentClassifier` categorizes media based on OCR extracted text (`image_fts`) and folder paths during indexing, with an instant one-pass startup backfill for currently indexed items. `MediaQueryBuilder` routes `doc:*` virtual buckets to the standard `GalleryScreen` grid, and `AlbumsScreen` displays an M3 Expressive collapsible carousel with live covers, item counts, and Material Symbols Rounded icons.

**Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite), WorkManager, ML Kit OCR, Material 3 Expressive.

**Spec:** [`docs/superpowers/specs/2026-10-27-documents-smart-classification-design.md`](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-27-documents-smart-classification-design.md)

## Global Constraints
- Strictly adhere to `AGENTS.md` (Material 3 Expressive, Material Symbols Rounded icons `ic_ms_*`, Google Fonts typography, 5-tier surface elevation, corner radii strictly in official scale).
- Zero raw hex `Color(0x...)` outside `ui/theme/`.
- No non-Material-Symbols icon libraries or legacy icon packs.
- All classification happens 100% on-device and offline.
- Background classification must not block UI frame renders or cause main thread database access.

## Review Focus
1. **Multi-token and case-insensitive OCR matching**: OCR text often has uppercase/lowercase or slight whitespace variations (e.g. "TAX INVOICE", "Tax Invoice", "Sub Total", "Subtotal"); must match reliably without false negatives.
2. **Path vs OCR precedence**: A photo in the "WhatsApp" folder might also have a receipt in it; the classification system must allow multi-label tagging so a photo can legitimately appear under both Social and Receipts.
3. **Empty or deleted media cleanup**: Deleting photos from `core_media` must automatically cascade-delete rows in `document_classifications` via SQLite trigger.
4. **Startup backfill idempotency**: Startup backfill must only populate unclassified rows or run once without duplicating rows or slowing down application startup.
5. **Zero-item categories**: If a category currently has 0 photos, it should either be omitted from the carousel or gracefully display 0 items without crashing or showing broken thumbnail placeholders.

---

### Task 1: `DocumentClassifier` Pure Engine & Unit Tests

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/data/ai/DocumentClassifier.kt`
- Create: `app/src/test/java/com/inferno/gallery/data/ai/DocumentClassifierTest.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/BucketNames.kt`

**Interfaces:**
- Produces:
  - `BucketNames.DOC_BOOKS_MAGAZINES = "doc:books_magazines"`
  - `BucketNames.DOC_EVENTS = "doc:events"`
  - `BucketNames.DOC_IDS = "doc:ids"`
  - `BucketNames.DOC_NOTES = "doc:notes"`
  - `BucketNames.DOC_PAYMENTS = "doc:payments"`
  - `BucketNames.DOC_RECEIPTS = "doc:receipts"`
  - `BucketNames.DOC_SOCIAL = "doc:social"`
  - `DocumentClassifier.classify(extractedText: String?, filePath: String?, bucketName: String?): Set<String>`

- [ ] **Step 1: Write failing unit tests for all 7 document categories**
Create `app/src/test/java/com/inferno/gallery/data/ai/DocumentClassifierTest.kt`:
```kotlin
package com.inferno.gallery.data.ai

import com.inferno.gallery.data.BucketNames
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class DocumentClassifierTest {
    @Test
    fun testReceiptClassification() {
        val text = "SUPERMARKET INC\nTAX INVOICE\nTOTAL: $42.50\nCASHIER: 04"
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
        val text = "Lecture 4: Quantum Mechanics\nDefinition:\nHomework assignment due Monday"
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
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew testDebugUnitTest --tests "com.inferno.gallery.data.ai.DocumentClassifierTest"`
Expected: FAIL (Class `DocumentClassifier` not defined).

- [ ] **Step 3: Implement `BucketNames` constants & `DocumentClassifier`**
Add constants to `app/src/main/java/com/inferno/gallery/data/BucketNames.kt`:
```kotlin
const val DOC_BOOKS_MAGAZINES = "doc:books_magazines"
const val DOC_EVENTS = "doc:events"
const val DOC_IDS = "doc:ids"
const val DOC_NOTES = "doc:notes"
const val DOC_PAYMENTS = "doc:payments"
const val DOC_RECEIPTS = "doc:receipts"
const val DOC_SOCIAL = "doc:social"
```
Create `app/src/main/java/com/inferno/gallery/data/ai/DocumentClassifier.kt` with domain regex and keyword dictionaries for all 7 categories.

- [ ] **Step 4: Run test to verify it passes**
Run: `.\gradlew testDebugUnitTest --tests "com.inferno.gallery.data.ai.DocumentClassifierTest"`
Expected: PASS.

- [ ] **Step 5: Commit Task 1**
```bash
git add app/src/main/java/com/inferno/gallery/data/BucketNames.kt app/src/main/java/com/inferno/gallery/data/ai/DocumentClassifier.kt app/src/test/java/com/inferno/gallery/data/ai/DocumentClassifierTest.kt
git commit -m "feat(docs): implement DocumentClassifier and test suite for 7 categories"
```

---

### Task 2: Database Schema & MediaDao Queries

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/db/Entities.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/GalleryDatabase.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/DatabaseProvider.kt`

**Interfaces:**
- Consumes: `BucketNames.DOC_*`
- Produces:
  - Table `document_classifications` with `(mediaId, category)` primary key
  - `MediaDao.observeDocumentCategoryStats(category: String): Flow<MediaAggregateStats>`
  - `MediaDao.insertDocumentClassifications(items: List<DocumentClassificationEntity>)`
  - `MediaDao.clearDocumentClassifications()`
  - `MediaDao.deleteDocumentClassificationsForMedia(mediaId: Long)`

- [ ] **Step 1: Define `DocumentClassificationEntity` in `Entities.kt`**
```kotlin
@Entity(
    tableName = "document_classifications",
    primaryKeys = ["mediaId", "category"],
    indices = [
        Index(value = ["category"]),
        Index(value = ["mediaId"])
    ]
)
data class DocumentClassificationEntity(
    val mediaId: Long,
    val category: String
)
```

- [ ] **Step 2: Add queries in `MediaDao` in `GalleryDatabase.kt`**
Add:
```kotlin
@Query("""
    SELECT COUNT(*) as itemCount, COALESCE(SUM(cm.size), 0) as totalSizeBytes, COALESCE(MAX(cm.dateAdded), 0) as maxDate,
    (SELECT uriString FROM core_media c2 JOIN document_classifications dc2 ON c2.id = dc2.mediaId WHERE dc2.category = :category AND c2.bucketName != 'Trash' AND c2.uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY c2.dateAdded DESC LIMIT 1) as coverUriString
    FROM core_media cm
    JOIN document_classifications dc ON cm.id = dc.mediaId
    WHERE dc.category = :category AND cm.bucketName != 'Trash' AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
""")
fun observeDocumentCategoryStats(category: String): Flow<MediaAggregateStats>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertDocumentClassifications(items: List<DocumentClassificationEntity>)

@Query("DELETE FROM document_classifications WHERE mediaId = :mediaId")
suspend fun deleteDocumentClassificationsForMedia(mediaId: Long)

@Query("DELETE FROM document_classifications")
suspend fun clearDocumentClassifications()

@Query("SELECT COUNT(*) FROM document_classifications")
suspend fun getDocumentClassificationsCount(): Int
```

- [ ] **Step 3: Add `onCreate` and `onOpen` DDL in `DatabaseProvider.kt`**
In `RoomDatabase.Callback`:
Ensure table `document_classifications` and trigger `core_media_doc_classifications_delete` are created if they do not exist.

- [ ] **Step 4: Compile to verify schema and Room queries**
Run: `.\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 2**
```bash
git add app/src/main/java/com/inferno/gallery/data/db/Entities.kt app/src/main/java/com/inferno/gallery/data/db/GalleryDatabase.kt app/src/main/java/com/inferno/gallery/data/db/DatabaseProvider.kt
git commit -m "feat(docs): add document_classifications table, DAO queries and triggers"
```

---

### Task 3: Worker Integration & Startup Backfill

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/workers/OcrIndexWorker.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/DatabaseProvider.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `DocumentClassifier`, `MediaDao`
- Produces: Real-time classification on OCR extraction, startup backfill on app launch, rescan clearance on `rebuildOcrIndex()`.

- [ ] **Step 1: Integrate classification into `OcrIndexWorker.kt`**
In `OcrIndexWorker.kt` stage 3 batch insertion:
When `embedded.extractedText` is processed:
```kotlin
val categories = DocumentClassifier.classify(
    extractedText = embedded.extractedText,
    filePath = embedded.entity.filePath,
    bucketName = embedded.entity.bucketName
)
if (categories.isNotEmpty()) {
    val entities = categories.map { category ->
        DocumentClassificationEntity(mediaId = embedded.entity.id, category = category)
    }
    db.mediaDao().insertDocumentClassifications(entities)
}
```

- [ ] **Step 2: Add fast startup backfill in `DatabaseProvider.kt` or `GalleryViewModel.kt`**
If `db.mediaDao().getDocumentClassificationsCount() == 0`:
Run an asynchronous background IO pass: query existing `image_fts` entries alongside `core_media` paths, run `DocumentClassifier.classify(...)`, and insert into `document_classifications`.

- [ ] **Step 3: Update `SettingsViewModel.rebuildOcrIndex()`**
In `rebuildOcrIndex()`:
Add `db.mediaDao().clearDocumentClassifications()` so a full OCR rebuild starts from a clean slate.

- [ ] **Step 4: Verify compilation & build**
Run: `.\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 3**
```bash
git add app/src/main/java/com/inferno/gallery/workers/OcrIndexWorker.kt app/src/main/java/com/inferno/gallery/data/db/DatabaseProvider.kt app/src/main/java/com/inferno/gallery/ui/SettingsViewModel.kt
git commit -m "feat(docs): integrate DocumentClassifier into OcrIndexWorker and startup backfill"
```

---

### Task 4: Virtual Bucket Routing in `MediaQueryBuilder` & Tests

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/MediaQueryBuilder.kt`
- Modify: `app/src/test/java/com/inferno/gallery/data/MediaQueryBuilderTest.kt` (or create if needed)

**Interfaces:**
- Consumes: `BucketNames.DOC_*`
- Produces: Correct SQL where clause for `bucket?.startsWith("doc:") == true`.

- [ ] **Step 1: Write test for `MediaQueryBuilder` document routing**
In `MediaQueryBuilderTest.kt`:
```kotlin
@Test
fun testDocBucketConditions() {
    val result = MediaQueryBuilder.buildMediaConditions(
        bucket = BucketNames.DOC_RECEIPTS,
        filterIndex = 0
    )
    assertTrue(result.conditions.any { it.contains("document_classifications WHERE category = ?") })
    assertTrue(result.args.contains(BucketNames.DOC_RECEIPTS))
    assertTrue(result.conditions.any { it.contains("cm.bucketName != 'Trash'") })
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew testDebugUnitTest --tests "*MediaQueryBuilderTest*"`
Expected: FAIL.

- [ ] **Step 3: Implement `doc:` routing in `MediaQueryBuilder.kt`**
```kotlin
bucket?.startsWith("doc:") == true -> {
    conditions.add("cm.id IN (SELECT mediaId FROM document_classifications WHERE category = ?)")
    args.add(bucket)
    conditions.add(NOT_HIDDEN)
}
```

- [ ] **Step 4: Run test to verify it passes**
Run: `.\gradlew testDebugUnitTest --tests "*MediaQueryBuilderTest*"`
Expected: PASS.

- [ ] **Step 5: Commit Task 4**
```bash
git add app/src/main/java/com/inferno/gallery/data/MediaQueryBuilder.kt app/src/test/java/com/inferno/gallery/data/
git commit -m "feat(docs): add doc:* virtual bucket routing to MediaQueryBuilder"
```

---

### Task 5: Material Symbols Vector Drawables

**Files:**
- Create: `app/src/main/res/drawable/ic_ms_menu_book.xml`
- Create: `app/src/main/res/drawable/ic_ms_confirmation_number.xml`
- Create: `app/src/main/res/drawable/ic_ms_badge.xml`
- Create: `app/src/main/res/drawable/ic_ms_edit_note.xml`
- Create: `app/src/main/res/drawable/ic_ms_payments.xml`
- Create: `app/src/main/res/drawable/ic_ms_receipt_long.xml`

**Interfaces:**
- Official Material Symbols Rounded vector XMLs (24x24dp viewport, weight 400 rounded paths).

- [ ] **Step 1: Create all 6 vector drawables in `res/drawable/`**
- [ ] **Step 2: Verify all vector drawables parse cleanly**
Run: `.\gradlew mergeDebugResources`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit Task 5**
```bash
git add app/src/main/res/drawable/ic_ms_*.xml
git commit -m "feat(ui): add Material Symbols Rounded drawables for document categories"
```

---

### Task 6: `GalleryViewModel` StateFlows & Bucket Titles

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/GalleryViewModel.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt`

**Interfaces:**
- Consumes: `MediaDao.observeDocumentCategoryStats`, `SettingsRepository`
- Produces:
  - `documentBuckets: StateFlow<List<AlbumBucket>>`
  - `albumsExpandedDocuments: StateFlow<Boolean>`
  - `toggleAlbumsExpandedDocuments()`
  - Bucket title mapping in top app bar for `doc:*`

- [ ] **Step 1: Add `albumsExpandedDocumentsFlow` to `SettingsRepository.kt`**
Add persistent boolean preference `albums_expanded_documents` (default `true`).

- [ ] **Step 2: Add `documentBuckets` & toggle methods in `GalleryViewModel.kt`**
Combine flows for all 7 categories:
```kotlin
val documentBuckets: StateFlow<List<AlbumBucket>> = combine(
    listOf(
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_RECEIPTS),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_PAYMENTS),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_IDS),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_NOTES),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_BOOKS_MAGAZINES),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_EVENTS),
        database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_SOCIAL)
    )
) { statsArray -> ... }
```
In `currentBucketTitle` / display logic: map `doc:*` to user-facing titles ("Receipts", "Payments", "IDs", "Notes", "Books & Magazines", "Events", "Social").

- [ ] **Step 3: Compile to verify `GalleryViewModel`**
Run: `.\gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Task 6**
```bash
git add app/src/main/java/com/inferno/gallery/ui/GalleryViewModel.kt app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt
git commit -m "feat(docs): expose documentBuckets flow and persistence in GalleryViewModel"
```

---

### Task 7: `AlbumsScreen.kt` Collapsible Carousel UI & Integration

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/AlbumsScreen.kt`

**Interfaces:**
- Consumes: `viewModel.documentBuckets`, `viewModel.albumsExpandedDocuments`, `onAlbumClick`

- [ ] **Step 1: Add SectionHeader and LazyRow in `AlbumsScreen.kt`**
Above "Capture Modes" / `header_media_types`:
1. SectionHeader:
   ```kotlin
   SectionHeader(
       title = "Documents",
       count = if (documentBuckets.isNotEmpty()) documentBuckets.size else null,
       isExpanded = documentsExpanded,
       onToggle = { viewModel.toggleAlbumsExpandedDocuments() }
   )
   ```
2. LazyRow with animated visibility:
   Display each of the 7 document cards with `ShapeLargeIncreased`, squircle badges, `AsyncImage` cover, scrim, Material Symbol icon, title, and item count.
   Click handler: `onAlbumClick(documentBucket.bucketName)`.

- [ ] **Step 2: Compile & run unit tests**
Run: `.\gradlew testDebugUnitTest`
Expected: ALL PASS.

- [ ] **Step 3: Commit Task 7**
```bash
git add app/src/main/java/com/inferno/gallery/ui/AlbumsScreen.kt
git commit -m "feat(ui): add Documents expandable section and cards to AlbumsScreen"
```

---

### Task 8: Build Verification & Device Installation

**Files:**
- All modified and created files

- [ ] **Step 1: Assemble Release APK**
Run: `.\gradlew assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Install onto connected device**
Run: `adb install -r -d "app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk"`
Expected: Success.

- [ ] **Step 3: Launch app & verify PID**
Run: `adb shell am start -n com.inferno.gallery/.MainActivity`
Expected: Activity launched and running.

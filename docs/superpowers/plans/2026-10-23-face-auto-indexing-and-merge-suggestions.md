# Face Auto-Indexing & Proactive Merge Suggestions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable opportunistic background face indexing when new media arrives and proactively present persistent "Same Person?" merge suggestions for ambiguous face clusters in the People screen.

**Architecture:** 
1. Database layer: Room entity `PersonMergeSuggestionEntity` (Room v28, `MIGRATION_27_28`), with queries in `FaceDao` linking candidate clusters to `PersonEntity`.
2. Clustering layer: `FaceClusteringManager` calculates normalized 192D cluster centroids and detects candidate pairs in the ambiguous similarity range (`0.65f <= similarity < 0.77f`), recording them as `PENDING`.
3. Background syncing: `MediaSyncWorker` opportunistically chains `FaceIndexWorker` with `setRequiresBatteryNotLow(true)` when `faceAutoIndex` is enabled and the ONNX model is available.
4. UI layer: Material 3 Expressive `MergeSuggestionHeroCard` at top of `PeopleScreen` providing 1-tap **Merge** and **Different** (permanent dismissal) actions.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database (v28), WorkManager, ONNX Runtime MobileFaceNet, Material 3 Expressive tokens.

**Spec:** [docs/superpowers/specs/2026-10-23-face-auto-indexing-and-merge-suggestions-design.md](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-23-face-auto-indexing-and-merge-suggestions-design.md)

## Global Constraints
- Strictly M3 Expressive: Zero raw hex colors (`MaterialTheme.colorScheme.*`), official shape scale only (`ShapeLarge`, `ShapeFull`), official `ic_ms_*` icons, and Google Fonts typography.
- Strictly NO phone screencaps (`screencap`) or automated input touches on device.
- Ambiguous similarity window: `[0.65f, 0.77f]`. Below 0.65 is treated as distinct people; 0.78 and above is already handled by automatic graph clustering.
- Dismissals are permanent: Dismissed pairs must never reappear after app restart or subsequent clustering passes.

## Review Focus
1. Merging two clusters when a suggestion is accepted must cascade properly and clean up remaining suggestions referring to the merged person.
2. If one of the persons in a suggestion is deleted or hidden, the suggestion must not render or crash with null avatar/name.
3. Rapid tapping of "Merge" or "Different" must not trigger duplicate database operations or race conditions.
4. `MediaSyncWorker` must respect battery low constraints and not start `FaceIndexWorker` if battery is critical or model is not downloaded.
5. Dismissed suggestions must remain dismissed when `computeAndSaveMergeSuggestions()` runs repeatedly during future clustering passes.

---

### Task 1: Database Entity, DAO & Room Migration (v28)

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/data/db/PersonMergeSuggestionEntity.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/FaceDao.kt`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/GalleryDatabase.kt:310-330`
- Modify: `app/src/main/java/com/inferno/gallery/data/db/DatabaseProvider.kt:410-435`
- Test: `app/src/test/java/com/inferno/gallery/data/db/MergeSuggestionEntityTest.kt`

**Interfaces:**
- Produces: `PersonMergeSuggestionEntity(personIdA: Long, personIdB: Long, similarity: Float, status: String, createdAt: Long)`
- Produces: `data class MergeSuggestionWithPersons(val suggestion: PersonMergeSuggestionEntity, val personA: PersonEntity, val personB: PersonEntity)`
- Produces: `FaceDao.observePendingMergeSuggestions(): Flow<List<MergeSuggestionWithPersons>>`
- Produces: `FaceDao.insertMergeSuggestions(suggestions: List<PersonMergeSuggestionEntity>)`
- Produces: `FaceDao.setSuggestionStatus(personIdA: Long, personIdB: Long, status: String)`
- Produces: `FaceDao.getExistingSuggestionStatus(personIdA: Long, personIdB: Long): String?`

- [ ] **Step 1: Write the failing unit test**
Create `app/src/test/java/com/inferno/gallery/data/db/MergeSuggestionEntityTest.kt` verifying:
  - Canonical ordering: `personIdA < personIdB`.
  - Suggestion statuses: `"PENDING"`, `"DISMISSED"`, `"ACCEPTED"`.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.inferno.gallery.data.db.MergeSuggestionEntityTest`
Expected: FAIL (unresolved reference `PersonMergeSuggestionEntity`).

- [ ] **Step 3: Implement `PersonMergeSuggestionEntity` and DAO methods**
- Create `PersonMergeSuggestionEntity.kt` with composite primary key `(personIdA, personIdB)` and foreign keys to `PersonEntity(id)`.
- Create data class `MergeSuggestionWithPersons`.
- Add DAO methods to `FaceDao.kt`:
  - Query joining `person_merge_suggestions` with `persons` for `personIdA` and `personIdB` where `status = 'PENDING'` and `is_hidden = 0`.
  - `insertMergeSuggestions` with `OnConflictStrategy.IGNORE`.
  - `setSuggestionStatus(personIdA: Long, personIdB: Long, status: String)`.
  - `deleteSuggestionsForPerson(personId: Long)`.

- [ ] **Step 4: Add `MIGRATION_27_28` and bump version in `GalleryDatabase.kt` and `DatabaseProvider.kt`**
- In `GalleryDatabase.kt`: add `PersonMergeSuggestionEntity::class` to entities, bump `version = 28`.
- In `DatabaseProvider.kt`: add `MIGRATION_27_28` creating table `person_merge_suggestions` with indices and foreign keys, and register in `.addMigrations()`.

- [ ] **Step 5: Run unit test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.inferno.gallery.data.db.MergeSuggestionEntityTest`
Expected: PASS.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/data/db/ app/src/test/java/com/inferno/gallery/data/db/
git commit -m "feat(faces): add PersonMergeSuggestionEntity and Room migration v28"
```

---

### Task 2: Pairwise Centroid Similarity & Candidate Generation in `FaceClusteringManager`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/ai/FaceClusteringManager.kt`
- Test: `app/src/test/java/com/inferno/gallery/data/ai/FaceCentroidSimilarityTest.kt`

**Interfaces:**
- Consumes: `FaceDao.insertMergeSuggestions`, `FaceDao.getExistingSuggestionStatus`
- Produces: `FaceClusteringManager.computeAndSaveMergeSuggestions()`
- Produces: `FaceClusteringManager.calculateCosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float`

- [ ] **Step 1: Write the failing unit test**
Create `app/src/test/java/com/inferno/gallery/data/ai/FaceCentroidSimilarityTest.kt` testing:
  - Cosine similarity calculation between normalized vectors.
  - Verification that similarity in `[0.65f, 0.77f]` classifies as candidate, while `>= 0.78f` and `< 0.65f` do not.
  - Canonical pair ordering `min(idA, idB), max(idA, idB)`.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.inferno.gallery.data.ai.FaceCentroidSimilarityTest`
Expected: FAIL.

- [ ] **Step 3: Implement centroid similarity computation in `FaceClusteringManager.kt`**
- Add `computeAndSaveMergeSuggestions()`:
  - Query all visible persons and their assigned face embeddings.
  - Calculate normalized average centroid vector for each person:
    $\mathbf{c} = \frac{\sum \mathbf{e}}{\|\sum \mathbf{e}\|_2}$.
  - For each pair $(i, j)$ with $i < j$:
    - Calculate cosine similarity $S = \mathbf{c}_i \cdot \mathbf{c}_j$.
    - If $S \ge 0.65\text{f} \text{ and } S < 0.77\text{f}$:
      - Check if pair already exists in `person_merge_suggestions`.
      - If not, create `PersonMergeSuggestionEntity(personIdA = i, personIdB = j, similarity = S, status = "PENDING")`.
  - Batch insert pending suggestions via `faceDao.insertMergeSuggestions()`.
- Call `computeAndSaveMergeSuggestions()` at the end of `consolidateClusters()`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.inferno.gallery.data.ai.FaceCentroidSimilarityTest`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/data/ai/FaceClusteringManager.kt app/src/test/java/com/inferno/gallery/data/ai/FaceCentroidSimilarityTest.kt
git commit -m "feat(faces): compute centroid similarities and generate merge suggestions"
```

---

### Task 3: Background Face Index Chaining & Settings Toggle

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/inferno/gallery/workers/MediaSyncWorker.kt:100-140`
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsViewModel.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt`

**Interfaces:**
- Produces: `SettingsRepository.faceAutoIndexFlow: Flow<Boolean>`
- Produces: `SettingsRepository.setFaceAutoIndexEnabled(enabled: Boolean)`
- Consumes: `MediaSyncWorker` checks `faceAutoIndexFlow.first()`, `faceEngine.isModelDownloaded()`, and `faceDao.getUnindexedFaceMediaCount()`

- [ ] **Step 1: Add setting to `SettingsRepository.kt`**
- Add DataStore key `booleanPreferencesKey("face_auto_index_enabled")`.
- Expose `faceAutoIndexFlow: Flow<Boolean>` defaulting to `true`.
- Add `suspend fun setFaceAutoIndexEnabled(enabled: Boolean)`.

- [ ] **Step 2: Chain `FaceIndexWorker` in `MediaSyncWorker.kt`**
- After Smart Search check, read `settingsRepo.faceAutoIndexFlow.first()`.
- Check `com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(applicationContext).isModelDownloaded()`.
- Check if unindexed face count > 0 (`database.faceDao().getUnindexedMediaIds().isNotEmpty()`).
- If true, enqueue `FaceIndexWorker` with `ExistingWorkPolicy.KEEP` and constraint `setRequiresBatteryNotLow(true)`.

- [ ] **Step 3: Expose toggle in `SettingsViewModel.kt` and `SettingsScreen.kt`**
- Expose `val faceAutoIndexEnabled: StateFlow<Boolean>` in `SettingsViewModel`.
- In `SettingsScreen.kt`, add an M3 `SwitchPreference` / toggle row under Face Recognition: "Auto-index new photos".

- [ ] **Step 4: Verify compilation**
Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt app/src/main/java/com/inferno/gallery/workers/MediaSyncWorker.kt app/src/main/java/com/inferno/gallery/ui/SettingsViewModel.kt app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt
git commit -m "feat(faces): auto-enqueue FaceIndexWorker in MediaSyncWorker with settings toggle"
```

---

### Task 4: PeopleViewModel State & M3 "Same Person?" Hero Card

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/people/PeopleViewModel.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/people/PeopleScreen.kt`

**Interfaces:**
- Produces: `PeopleViewModel.pendingMergeSuggestions: StateFlow<List<MergeSuggestionWithPersons>>`
- Produces: `PeopleViewModel.acceptMergeSuggestion(suggestion: MergeSuggestionWithPersons)`
- Produces: `PeopleViewModel.dismissMergeSuggestion(suggestion: MergeSuggestionWithPersons)`

- [ ] **Step 1: Expose merge suggestion operations in `PeopleViewModel.kt`**
- Add `pendingMergeSuggestions: StateFlow<List<MergeSuggestionWithPersons>>` observing `faceDao.observePendingMergeSuggestions()`.
- Add `acceptMergeSuggestion(suggestion: MergeSuggestionWithPersons)`:
  - Calls `clusteringManager.mergePersons(sourcePersonId = suggestion.personB.id, targetPersonId = suggestion.personA.id)`.
  - Sets suggestion status to `"ACCEPTED"`.
  - Removes suggestions referencing `suggestion.personB.id`.
- Add `dismissMergeSuggestion(suggestion: MergeSuggestionWithPersons)`:
  - Sets suggestion status to `"DISMISSED"`.

- [ ] **Step 2: Build `MergeSuggestionHeroCard` composable in `PeopleScreen.kt`**
- Renders inside an `AnimatedVisibility(pendingSuggestions.isNotEmpty())` right above the photo count filter chips.
- Visual container: `Surface` or `Card` with `containerColor = MaterialTheme.colorScheme.surfaceContainerHigh`, `shape = ShapeLarge` (16dp).
- Header: Sparkle icon (`ic_ms_auto_awesome`) in `primary` tint, headline "Same person?", and counter chip e.g. "1 of 3".
- Center row:
  - Person A avatar (64dp circle) + Name / photo count.
  - Center match pill: e.g. "72% match" (`surfaceContainerHighest`, `labelSmall`).
  - Person B avatar (64dp circle) + Name / photo count.
- Action row:
  - "Yes, Merge" button (`ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)`).
  - "Not same" text/outlined button (`MaterialTheme.colorScheme.onSurfaceVariant`).
  - If multiple suggestions exist: `<` and `>` arrow buttons to navigate between suggestions.

- [ ] **Step 3: Verify compilation & lint**
Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/people/PeopleViewModel.kt app/src/main/java/com/inferno/gallery/ui/people/PeopleScreen.kt
git commit -m "feat(faces): add MergeSuggestionHeroCard and reactive suggestion handling"
```

---

### Task 5: End-to-End Build & Release Verification

**Files:**
- Entire codebase

- [ ] **Step 1: Run all unit tests**
Run: `./gradlew testDebugUnitTest`
Expected: All tests pass.

- [ ] **Step 2: Build Release APK**
Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verification & Review**
Confirm generated APK and verify zero regressions or anti-patterns.

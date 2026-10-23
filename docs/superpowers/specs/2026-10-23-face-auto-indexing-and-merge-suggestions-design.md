# Face Auto-Indexing & Proactive Merge Suggestions — Design Document

## 1. Intent & The Why
Photon Gallery has implemented on-device face recognition, clustering, people management, count filters, and search integration. However, two essential capabilities are missing to achieve feature parity with modern photo galleries (Google Photos / Apple Photos):
1. **Seamless Auto-Indexing**: Currently, face indexing only runs when manually triggered in Settings or the People tab. New photos taken or downloaded are not automatically analyzed for faces.
2. **Proactive Merge Suggestions ("Same Person?")**: Due to variations in lighting, sunglasses, facial hair, angles, or age, some faces of the same person are clustered into separate groups with high cosine similarity (e.g. 0.65 – 0.77). Instead of requiring users to manually spot and merge duplicate clusters, the app should proactively identify ambiguous cluster pairs and offer 1-tap review cards.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Automatic Background Indexing**: Automatically enqueue `FaceIndexWorker` when `MediaSyncWorker` detects new photos, governed by user preference (toggle in AI Settings) and low-battery constraints.
- **Persistent Merge Suggestions**: Detect candidate cluster pairs whose centroid cosine similarity lies in the ambiguous band (`0.65 <= similarity < 0.78`) and persist them in Room database.
- **Dismissal Permanence**: Once a user taps "No, Different" (or dismisses a pair), this rejection is recorded so that pair is never suggested again.
- **M3 Expressive Review Card**: Display a clean, non-intrusive card at the top of the People screen with side-by-side avatars, photo counts, and 1-tap "Yes, Merge" and "No, Different" actions.
- **Zero UI Stutter**: Suggestions must be precomputed and read reactively via Room Flow, never computed synchronously on the main thread during UI layout.

### Non-Goals
- Cloud-based face recognition or uploading embeddings. Everything stays strictly on-device.
- Changing the primary clustering algorithm (Chinese Whispers remains the core clustering engine).
- Forcing auto-merge on ambiguous pairs without user consent.

### Anti-Goals
- Do NOT perform pairwise cluster distance calculations on the UI thread when opening the People screen.
- Do NOT run heavy face ML indexing when device battery is critically low.
- Do NOT re-suggest pairs that the user has already rejected.

---

## 3. Constraints & Design System
- **M3 Expressive Compliance**: Strictly follow `AGENTS.md`:
  - 5-tier elevation: Card uses `surfaceContainer` or `surfaceContainerHigh`.
  - Official shape tokens: `ShapeLarge` (16dp) or `ShapeLargeIncreased` (20dp) for card, `ShapeFull` for avatar clips and action buttons.
  - Official icons: Only `ic_ms_*` drawables (e.g. `ic_ms_auto_awesome`, `ic_ms_check`, `ic_ms_close`).
  - Google Fonts typography only (Outfit for titles, Plus Jakarta Sans for body/labels).
  - No raw hex colors.
- **No Automated Device Touch / Screencap**: Do not capture phone screenshots or send automated input touches.

---

## 4. Approaches Considered & Trade-offs

### Part 1: Auto-Indexing Strategy
1. **Approach A: Opportunistic MediaSync Chaining (Selected)**  
   - Triggered whenever `MediaSyncWorker` detects new media items or sync drift.  
   - Checks if face model is downloaded, auto-index toggle is enabled, and battery is not low.  
   - *Pros*: Photos are indexed shortly after capture with zero manual intervention.  
   - *Cons*: Uses modest CPU shortly after photo capture, mitigated by WorkManager `setRequiresBatteryNotLow(true)`.
2. **Approach B: Charging-Only Overnight Worker**  
   - Only triggers when device is idle and plugged into charger.  
   - *Pros*: Minimal battery impact.  
   - *Cons*: Poor user experience; photos taken during the day remain unindexed until the following morning.

### Part 2: Merge Suggestions Storage & Computation
1. **Approach 1: Persistent Room Table + Periodic/Post-Clustering Detection (Selected)**  
   - Compute pairwise centroid cosine similarity after clustering completes in `FaceClusteringManager`.  
   - Store candidate pairs in `merge_suggestions` table with `status = PENDING / DISMISSED / ACCEPTED`.  
   - *Pros*: Instant reactive UI via Room Flow, permanent dismissal tracking, zero UI thread overhead.  
   - *Cons*: Requires a new Room migration / table.
2. **Approach 2: In-Memory Heuristic on People Screen Load**  
   - Compare all cluster centroids in memory in `PeopleViewModel`.  
   - *Pros*: No database migration.  
   - *Cons*: O(N²) matrix calculation lags UI on screen open; dismissals lost on app restart.

---

## 5. Detailed Architecture & Technical Implementation

### A. Database Layer (`FaceDao.kt` & Room Entities)
1. **New Entity**: `PersonMergeSuggestionEntity`
   ```kotlin
   @Entity(
       tableName = "person_merge_suggestions",
       primaryKeys = ["personIdA", "personIdB"],
       foreignKeys = [
           ForeignKey(
               entity = PersonEntity::class,
               parentColumns = ["id"],
               childColumns = ["personIdA"],
               onDelete = ForeignKey.CASCADE
           ),
           ForeignKey(
               entity = PersonEntity::class,
               parentColumns = ["id"],
               childColumns = ["personIdB"],
               onDelete = ForeignKey.CASCADE
           )
       ],
       indices = [Index("personIdA"), Index("personIdB")]
   )
   data class PersonMergeSuggestionEntity(
       val personIdA: Long, // ordered so personIdA < personIdB
       val personIdB: Long,
       val similarity: Float,
       val status: String = "PENDING", // PENDING, DISMISSED, ACCEPTED
       val createdAt: Long = System.currentTimeMillis()
   )
   ```
2. **DAO Methods in `FaceDao.kt`**:
   - `observePendingMergeSuggestions(): Flow<List<MergeSuggestionWithPersons>>`
   - `insertSuggestions(suggestions: List<PersonMergeSuggestionEntity>)`
   - `setSuggestionStatus(personIdA: Long, personIdB: Long, status: String)`
   - `cleanInvalidSuggestions()`

### B. Similarity Calculation in `FaceClusteringManager.kt`
- Compute normalized mean embedding vector (centroid) for each cluster:
  $$\mathbf{c}_k = \frac{\sum_{i \in C_k} \mathbf{e}_i}{\|\sum_{i \in C_k} \mathbf{e}_i\|}$$
- For all cluster pairs $(A, B)$ where neither is hidden and both have $\ge 1$ faces:
  - If cosine similarity $S(\mathbf{c}_A, \mathbf{c}_B) \in [0.65, 0.77]$:
    - Check if pair was already marked `DISMISSED` or `ACCEPTED`.
    - If not present, insert into `person_merge_suggestions` with status `PENDING`.

### C. Settings & Background Worker Trigger
1. **`SettingsRepository.kt`**:
   - Add `faceAutoIndexFlow: Flow<Boolean>` backed by DataStore key `face_auto_index_enabled` (default `true`).
   - Add `setFaceAutoIndexEnabled(enabled: Boolean)`.
2. **`MediaSyncWorker.kt`**:
   - After Room sync completes:
     ```kotlin
     val autoIndexFaces = settingsRepo.faceAutoIndexFlow.first()
     val faceEngine = FaceRecognitionEngine.getInstance(applicationContext)
     val unindexedFaceCount = database.faceDao().getUnindexedFaceMediaCount()
     if (autoIndexFaces && faceEngine.isModelDownloaded() && unindexedFaceCount > 0) {
         val constraints = Constraints.Builder()
             .setRequiresBatteryNotLow(true)
             .build()
         val faceRequest = OneTimeWorkRequestBuilder<FaceIndexWorker>()
             .setConstraints(constraints)
             .build()
         WorkManager.getInstance(applicationContext).enqueueUniqueWork(
             "FaceIndexWorker",
             ExistingWorkPolicy.KEEP,
             faceRequest
         )
     }
     ```
3. **`SettingsScreen.kt`**:
   - Add an M3 toggle in AI Settings under Face Recognition: "Auto-index new photos".

### D. UI Presentation in `PeopleScreen.kt`
- When `pendingSuggestions.isNotEmpty()`:
  - Render an animated `MergeSuggestionHeroCard` at the top of the screen (above the filter chips).
  - Features:
    - Header: Sparkle icon (`ic_ms_auto_awesome`) + "Same person?" title + stepper counter if > 1 (`1 of N`).
    - Visuals: Dual circular avatars of Person A and Person B with names / counts.
    - Match confidence badge (e.g. `"${(similarity * 100).toInt()}% match"`).
    - Actions:
      - **"Merge"** (FilledTonalButton or Button, primary colors, calls `viewModel.mergePersons(a, b)`).
      - **"Not same"** (OutlinedButton or TextButton, calls `viewModel.dismissSuggestion(a, b)`).
    - If user wants to inspect more photos before merging, tapping the card opens a side-by-side preview bottom sheet showing up to 6 sample photos of each person.

---

## 6. What's Left to the Builder
- Determine precise cosine similarity candidate thresholds (`0.65f` lower bound, `0.77f` upper bound).
- Database version bump from 27 to 28 with `MIGRATION_27_28` creating `person_merge_suggestions`.
- Handling person deletion cascade cleanly in Room.

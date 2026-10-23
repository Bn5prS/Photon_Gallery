# Face Detection & Clustering Implementation Plan

## Overview
Implement an on-device, 100% offline, battery-efficient face detection and clustering feature for Photon Gallery using MobileFaceNet INT8 (~1.2 MB), 5-point canonical affine alignment, multi-exemplar clustering with the 3-photo admission rule, and Material 3 Expressive UI.

---

## Phase 1: Core Mathematical & ML Engine

### Task 1.1: Model Asset & Model Registry
- [ ] Add `mobilefacenet_int8.onnx` (1.2 MB) to model registry / asset setup.
- [ ] Create `FaceModelRegistry.kt` with filename, URL, size, and SHA-256 checksum verification.

### Task 1.2: 5-Point Canonical Face Normalizer
- [ ] Implement `FaceNormalizer.kt`:
  - Calculate 2D similarity transform matrix mapping 5 detected landmarks (left eye, right eye, nose tip, left mouth, right mouth) to canonical ArcFace points.
  - Render transformed 112×112 RGB `Bitmap` using `android.graphics.Canvas` and `android.graphics.Matrix`.
- [ ] Unit Test `FaceNormalizerTest.kt`: Verify rotation, scaling, and translation invariance with synthetic landmark coordinates.

### Task 1.3: Face Recognition Engine (ONNX Runtime)
- [ ] Implement `FaceRecognitionEngine.kt`:
  - Initialize ONNX Runtime session with XNNPACK/NNAPI execution providers and 2 threads.
  - Input preprocessing: convert 112×112 Bitmap to NCHW `FloatBuffer` with `(pixel - 127.5f) / 128.0f`.
  - Run inference: output 512-D float array, compute L2 norm, and return normalized vector.
  - Helper functions: `cosineSimilarity(a, b)` and `cosineDistance(a, b)`.
- [ ] Unit Test `FaceRecognitionEngineTest.kt`: Verify identical embeddings give similarity = 1.0, orthogonal vectors = 0.0, and opposite vectors = -1.0.

---

## Phase 2: Database Schema & Clustering Engine

### Task 2.1: Room Database Schema & DAO (v26)
- [ ] Create `PersonEntity.kt`:
  - Columns: `id`, `name`, `coverCropPath`, `isHidden`, `isFavorite`, `isPromoted`, `faceCount`, `centroid`, `createdAt`.
- [ ] Create `FaceEntity.kt`:
  - Columns: `id`, `mediaId`, `personId`, `bboxLeft`, `bboxTop`, `bboxRight`, `bboxBottom`, `qualityScore`, `isExemplar`, `embedding`.
  - Foreign key to `PersonEntity(id)` with `SET_NULL` on delete.
- [ ] Create `FaceDao.kt`:
  - Queries for unassigned faces, faces by person, promoted persons, person centroids, and transactional merge updates.
- [ ] Update `GalleryDatabase.kt`:
  - Increment database version to `26`.
  - Add `MIGRATION_25_26` creating `persons` and `faces` tables and indices.
- [ ] Unit Test `FaceDaoTest.kt`: Verify insertions, foreign key cascades, and queries.

### Task 2.2: Chinese Whispers Graph Community Detection
- [ ] Implement `ChineseWhispers.kt`:
  - Pure Kotlin graph clustering: takes nodes and weighted edge map, runs 15 label propagation iterations.
  - Returns mapping of `FaceId -> ClusterId`.
- [ ] Unit Test `ChineseWhispersTest.kt`: Verify clustering on synthetic two-cluster and three-cluster graphs with noise edges.

### Task 2.3: Multi-Exemplar Clustering Manager
- [ ] Implement `FaceClusteringManager.kt`:
  - `assignFaceToCluster(mediaId, detectedFace)`:
    - Match against Top-3 clean exemplars of existing clusters ($d \le 0.38$).
    - If matched: assign to cluster, update face count, update running centroid.
    - If `faceCount >= 3` and `!isPromoted`: set `isPromoted = true`.
    - If unmatched: create new unpromoted candidate cluster.
  - `mergeClusters(sourcePersonId, targetPersonId)`:
    - Re-assign all faces from source to target.
    - Merge exemplars, recompute centroid, delete source person.
- [ ] Unit Test `FaceClusteringManagerTest.kt`: Test 3-photo promotion gate and multi-exemplar matching.

---

## Phase 3: Background Worker & Safety

### Task 3.1: Face Indexing Background Worker
- [ ] Implement `FaceIndexWorker.kt`:
  - Constraints: `setRequiresCharging(true)`, `setRequiresBatteryNotLow(true)`.
  - Query: strictly `WHERE isVault = 0 AND id NOT IN (SELECT mediaId FROM faces)`.
  - Image decoding: use `BitmapFactory.Options.inSampleSize` to downscale raw images to `max(w,h) = 720px`.
  - Detection: ML Kit Face Detection with `PERFORMANCE_MODE_ACCURATE`, `LANDMARK_MODE_ALL`.
  - Filtering: discard bounding boxes $< 60\times 60$, confidence $< 0.65$, or extreme pose.
  - Warp & embed: run `FaceNormalizer` and `FaceRecognitionEngine`.
  - Explicit cleanup: recycle all temporary bitmaps and close tensors.
  - Throttling: 15 images per batch with 100ms yield between batches.

### Task 3.2: Settings & Integration
- [ ] Add Face Recognition toggle and re-index controls in `SettingsScreen.kt` and `SettingsViewModel.kt`.
- [ ] Observe indexing progress via `WorkInfo`.

---

## Phase 4: Material 3 Expressive UI & User Experience

### Task 4.1: Person Dialogs
- [ ] Implement `NamePersonDialog.kt`: Modal dialog with `ShapeLarge` to set/rename a person.
- [ ] Implement `MergePersonDialog.kt`: Confirmation dialog to merge two identities.

### Task 4.2: People Carousel in AlbumsScreen
- [ ] Implement `PeopleCarousel.kt`:
  - Section header "People" with total promoted person count.
  - Horizontal list of 72dp circular avatar cards with cover crop, name, and photo count.
  - "Show all" action button opening `PeopleScreen`.
- [ ] Embed into `AlbumsScreen.kt` above Capture Modes.

### Task 4.3: PeopleScreen & PersonDetailScreen
- [ ] Implement `PeopleScreen.kt`:
  - Grid of all recognized people with name search filter.
  - Long-press selection mode for multi-select merge or hide.
- [ ] Implement `PersonDetailScreen.kt`:
  - Large hero banner with face avatar, editable name, and photo counter.
  - Staggered grid of photos containing that person with shared element transitions.
- [ ] Register routes in `NavigationGraph.kt`.

---

## Phase 5: Verification & Quality Assurance
- [ ] Run full test suite: `./gradlew testDebugUnitTest`.
- [ ] Assemble release APK: `./gradlew assembleRelease`.
- [ ] Install on connected device `10BEAK2JR50012G`.
- [ ] Verify:
  - Zero crashes during indexing.
  - No OOM memory spikes.
  - Photos with same person correctly clustered.
  - Strangers (1-2 photos) kept out of People album.
  - Private Vault photos strictly excluded.

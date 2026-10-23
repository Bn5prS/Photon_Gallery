# Face Detection & Clustering Feature Design

## 1. Intent & Context
- **Objective**: Implement a state-of-the-art, 100% offline, battery-efficient on-device face detection and clustering ("People") feature in Photon Gallery.
- **Problem Statement**: The previous prototype attempt suffered from four fatal flaws:
  1. *Model Bloat*: Attempted to download a 44 MB ResNet-50 model from raw GitHub links, leading to network timeouts and corruption.
  2. *Memory & Thermal Failures*: Full-resolution (12–108 MP) bitmaps were decoded without downsampling, triggering `OutOfMemoryError` (OOM) and thermal CPU throttling.
  3. *Centroid Drift*: Naive vector averaging caused clusters to drift and mistakenly merge distinct individuals into a single identity.
  4. *Stranger Pollution*: Every background passerby in photos was assigned a visible cluster, cluttering the UI with hundreds of single-photo faces.

---

## 2. Goals & Non-Goals

### Goals
- **Ultralight Footprint**: Utilize an INT8-quantized MobileFaceNet model (~1.2 MB) running via existing ONNX Runtime 1.22.0.
- **Mathematical 5-Point Normalization**: Affine similarity warp of detected landmarks (eyes, nose, mouth corners) into a 112×112 canonical coordinate space to guarantee high embedding fidelity.
- **Zero Stranger Pollution**: Implement the *3-Photo Admission Rule*—faces are retained in an internal candidate pool until at least 3 distinct photos contain that identity before surfacing in the UI.
- **Zero Centroid Drift**: Maintain *Top-3 Clean Frontal Exemplars* per cluster rather than unconstrained vector averaging.
- **Hybrid Clustering Strategy**:
  - *Cold-Start / Batch Scan*: Chinese Whispers graph community detection ($O(V+E)$ linear time).
  - *Incremental Updates*: Real-time Multi-Exemplar Centroid Matching ($< 10$ ms per new face).
- **Strict Vault Isolation**: Absolute barrier ensuring media marked `isVault = 1` is excluded from all indexing and clustering queries.
- **Memory & Battery Safety**: Downscale input bitmaps to max 720px before detection; enforce explicit `Bitmap.recycle()` and `OnnxTensor.close()`; constrain background indexing to charging + battery-not-low states via `WorkManager`.
- **Material 3 Expressive UI**:
  - Horizontal "People & Pets" carousel in `AlbumsScreen`.
  - Comprehensive `PeopleScreen` grid with name search.
  - `PersonDetailScreen` featuring all photos containing that identity with M3 Shared Element Transitions.
  - Dialogs for "Name Person", "Merge People", "Set as Cover", and "Hide Person".

### Non-Goals
- Cloud or server-assisted facial analysis (strictly 100% offline on-device).
- Video frame face tracking (static images only).
- Facial recognition inside biometric Private Space (Vault).

---

## 3. Technical Architecture

### A. ML Pipeline
```
[MediaStore Image]
       │
       ▼
[Thumbnail Loader: max(w,h) = 720px, inSampleSize]
       │
       ▼
[Face Detector: ML Kit Face Detection]
  ├── Quality Gate: width >= 60px, confidence >= 0.65, |yaw| <= 45°, |pitch| <= 35°
  └── Landmarks: Left Eye, Right Eye, Nose Tip, Left Mouth, Right Mouth
       │
       ▼
[Canonical 112×112 Affine Warper]
  ├── Maps 5 landmarks to canonical ArcFace points via android.graphics.Matrix
  └── Renders normalized 112×112 RGB Bitmap (<0.5 ms)
       │
       ▼
[ONNX Runtime: MobileFaceNet INT8 (1.2 MB)]
  ├── Preprocessing: (pixel - 127.5) / 128.0f
  ├── Shape: 1×3×112×112 (NCHW)
  └── Output: 512-D L2-normalized float vector
```

### B. Canonical Alignment Geometry
```kotlin
val CANONICAL_POINTS = floatArrayOf(
    38.2946f, 51.6963f,  // Left eye
    73.5318f, 51.6963f,  // Right eye
    56.0252f, 71.7366f,  // Nose tip
    41.5493f, 92.3655f,  // Left mouth corner
    70.7299f, 92.3655f   // Right mouth corner
)
```

### C. Hybrid Clustering & Multi-Exemplar Anchors
1. **Cluster Representation**:
   - `exemplars`: List of up to 3 cleanest, most frontal 512-D vectors.
   - `centroid`: Unit-normalized running average vector.
   - `faceCount`: Number of confirmed faces.
2. **Matching Criteria**:
   - Cosine distance: $d(e_1, e_2) = 1 - (e_1 \cdot e_2)$.
   - Match Threshold: $d \le 0.38$ against *any* of the person's top-3 clean exemplars.
3. **Admitting to UI**:
   - Clusters with `faceCount < 3` remain `isPromoted = false` and are hidden from user-facing screens.
   - When a 3rd distinct photo matches, the cluster is elevated (`isPromoted = true`) and appears in the People album.

### D. Database Schema (Room v26)
```kotlin
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String? = null,
    val coverCropPath: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val isPromoted: Boolean = false,       // True when faceCount >= 3
    val faceCount: Int = 0,
    val centroid: FloatArray? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "faces",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("personId"), Index("mediaId")]
)
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val personId: Long? = null,
    val bboxLeft: Float,
    val bboxTop: Float,
    val bboxRight: Float,
    val bboxBottom: Float,
    val qualityScore: Float,
    val isExemplar: Boolean = false,
    val embedding: FloatArray
)
```

---

## 4. UI & Interaction Design
1. **Albums Screen Carousel**:
   - Title: `People` with badge count of recognized people.
   - Trailing: `Show all` action button leading to `PeopleScreen`.
   - Items: Circular avatar cards (72dp) with crop preview, person name, and photo counter.
2. **People Screen**:
   - Grid layout of all recognized people cards.
   - Search bar to filter by name.
   - Multi-select mode: select 2 people -> tap "Merge" to combine duplicate identities.
3. **Person Detail Screen**:
   - Large hero header with face avatar, editable name title, and "Set as Cover" action.
   - Staggered photo grid containing all photos where that person appears.

# Motion Photos & Top Shot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement native on-device Motion Photo playback (press-and-hold to play micro-video) and an interactive Top Shot keyframe scrubber bottom sheet to extract and save alternative still frames as full-resolution JPEGs.

**Architecture:** A lightweight seekable stream parser (`MotionPhotoParser`) inspects JPEG trailers and XMP metadata for Google (`GCamera:MicroVideo`), Samsung (`MotionPhoto_Data`), and universal `ftyp` MP4 atoms. `DetailScreen` exposes a top-bar `[Motion]` pill and press-and-hold playback. Tapping the pill opens `TopShotScrubberSheet`, extracting a filmstrip via `MediaMetadataRetriever` with real-time scrubbing and MediaStore export.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 / ExoPlayer, `MediaMetadataRetriever`, Android MediaStore, Coroutines `Dispatchers.IO`, Material 3 Expressive.

**Spec:** [docs/superpowers/specs/2026-10-24-motion-photo-top-shot-design.md](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-24-motion-photo-top-shot-design.md)

## Global Constraints
- 100% On-device, zero cloud/network permissions.
- Original photo files are strictly non-destructive (never mutated).
- Strict Material 3 Expressive adherence (no raw hex, use `ic_ms_*` icons, `Shape*` tokens, `SpacingTokens`, `MotionTokens`).
- File caching inside `context.cacheDir/motion_videos/`.

## Review Focus
- High-efficiency trailer scanning: avoid reading full 50MB camera JPEGs into RAM; scan only file trailer/XMP.
- Memory safety: recycle `MediaMetadataRetriever` and frame Bitmaps properly.
- Coroutine dispatching: all I/O and video extraction on `Dispatchers.IO`.
- Seamless gesture coordination: press-and-hold playback must not interfere with pinch-to-zoom or swipe gestures in `DetailScreen`.

---

### Task 1: Motion Photo Container Parser & Extractor

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/data/motion/MotionPhotoParser.kt`
- Test: `app/src/test/java/com/inferno/gallery/data/motion/MotionPhotoParserTest.kt`

- [ ] **Step 1: Write unit tests in `MotionPhotoParserTest.kt`**
  - Test detecting Google XMP microvideo offset.
  - Test detecting Samsung `MotionPhoto_Data` marker and length.
  - Test universal `ftyp` fallback parser on appended MP4 stream.
  - Test non-motion JPEG returning `null`.
- [ ] **Step 2: Run test to confirm failure**
  - Run `./gradlew testDebugUnitTest --tests "com.inferno.gallery.data.motion.MotionPhotoParserTest"` and verify it fails due to missing class.
- [ ] **Step 3: Implement `MotionPhotoParser.kt`**
  - Define `data class MotionPhotoInfo(val isMotionPhoto: Boolean, val videoOffsetFromEnd: Long, val presentationTimestampUs: Long = 0L)`.
  - Implement `fun parseMotionPhoto(inputStream: InputStream, fileLength: Long): MotionPhotoInfo?`.
  - Implement `suspend fun getOrExtractMotionVideo(context: Context, uri: Uri, mediaId: Long): File?` which extracts the video slice to `cacheDir/motion_videos/{mediaId}.mp4`.
- [ ] **Step 4: Run test to verify it passes**
  - Run `./gradlew testDebugUnitTest --tests "com.inferno.gallery.data.motion.MotionPhotoParserTest"` and verify all tests pass.
- [ ] **Step 5: Commit changes**
  - `git commit -m "feat(motion): implement high-speed Motion Photo container parser and extractor"`

---

### Task 2: Motion Photo Viewer & Press-and-Hold Playback in `DetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

- [ ] **Step 1: Add Motion Photo state and detection in `DetailScreen.kt`**
  - In `DetailScreen`, add `var isMotionPhoto by remember(currentMedia.id) { mutableStateOf(false) }`.
  - Add `var motionVideoFile by remember(currentMedia.id) { mutableStateOf<File?>(null) }`.
  - Launch coroutine with `Dispatchers.IO` on `currentMedia.id` to detect motion photo and prepare video cache.
- [ ] **Step 2: Add `MotionPill` in `DetailTopBar`**
  - When `isMotionPhoto == true`, display `MotionPill` in the top bar using `ic_ms_motion_photos_on` and label "Motion".
  - Tapping the pill toggles `showTopShotSheet = true`.
- [ ] **Step 3: Implement Press-and-Hold playback layer**
  - When `isMotionPhoto == true` and `motionVideoFile != null`:
    - Add pointer input / long-press handler on the photo viewer.
    - While pressed, fade in an embedded looping ExoPlayer player view with haptic feedback tick (`HapticFeedbackType.LongPress`).
    - On release, cross-fade back to the high-res photo.
- [ ] **Step 4: Verify compilation**
  - Run `./gradlew compileDebugKotlin` to verify clean compilation.
- [ ] **Step 5: Commit changes**
  - `git commit -m "feat(motion): add top-bar motion indicator and press-and-hold video playback"`

---

### Task 3: Top Shot Keyframe Scrubber Bottom Sheet

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/motion/TopShotScrubberSheet.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

- [ ] **Step 1: Create `TopShotScrubberSheet.kt`**
  - Create Material 3 `ModalBottomSheet` with `ShapeEdgeTop`.
  - Header: Title "Top Shot", close button, and current timestamp.
  - Filmstrip: Use `MediaMetadataRetriever` to extract 16-20 evenly spaced thumbnails along video duration.
  - Interactive scrubber slider with live thumbnail preview.
  - Mark original still frame with a distinctive badge / dot on the filmstrip.
  - Action buttons:
    - **"Export as Photo"** (`FilledTonalButton` with `ic_ms_save`): extracts selected frame at full resolution and saves to `Pictures/Photon Gallery` via `ContentResolver`.
    - **"Save Video"** (`OutlinedButton` with `ic_ms_movie`): copies the `.mp4` into `Movies/Photon Gallery`.
- [ ] **Step 2: Connect `TopShotScrubberSheet` in `DetailScreen.kt`**
  - When `showTopShotSheet` is true, render `TopShotScrubberSheet(videoFile, onDismiss, onExportPhoto, onExportVideo)`.
- [ ] **Step 3: Verify compilation & unit tests**
  - Run `./gradlew testDebugUnitTest` to ensure all tests pass.
- [ ] **Step 4: Commit changes**
  - `git commit -m "feat(motion): implement Top Shot filmstrip keyframe scrubber and frame exporter"`

---

### Task 4: End-to-End Build & Verification

**Files:**
- Verification only

- [ ] **Step 1: Run complete test suite**
  - `./gradlew testDebugUnitTest`
- [ ] **Step 2: Build release APK**
  - `./gradlew assembleRelease`
- [ ] **Step 3: Install on device and verify hardware functionality**
  - `adb -s 10BEAK2JR50012G install -r "app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk"`
  - Verify motion photo badge, press-and-hold playback, filmstrip scrubber, and frame export.
- [ ] **Step 4: Final commit and branch finish**
  - Ensure all commits are organized and clean.

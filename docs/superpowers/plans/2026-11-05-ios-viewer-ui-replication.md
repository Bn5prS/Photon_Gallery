# iOS Gallery UI Replication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replicate the clean, floating iOS 18 Photos viewer UI in Photon Gallery's `DetailScreen.kt` and `VideoPlayerItem.kt`, featuring floating top islands, a unified video scrubber capsule, and a 3-part floating bottom dock using exclusively Material Symbols icons and without a filmstrip.

**Architecture:** Floating composable overlays on top of `HorizontalPager` with translucent dark frosted backgrounds (`Color.Black.copy(0.45f)`), zero borders/outlines, 48dp touch targets, and synchronized visibility toggles.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, AndroidX Media3 (ExoPlayer), Material Symbols Drawables.

**Spec:** [`docs/superpowers/specs/2026-11-05-ios-viewer-ui-replication-design.md`](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-11-05-ios-viewer-ui-replication-design.md)

## Global Constraints
- Strictly use Material Symbols vector drawables (`ic_ms_*`).
- Zero border strokes or outlines on any floating pills or buttons ("No Outlines Policy").
- Completely omit thumbnail filmstrip from bottom controls ("ignore film strip").
- Always target the debug build package `com.inferno.gallery.debug`.

## Review Focus
- Extremely long file names: Verify `TextOverflow.Ellipsis` and single-line clamping in the top metadata pill.
- Live / Motion photos: Ensure the "LIVE" toggle pill is neatly integrated into the top bar without overlapping the back or more buttons.
- Video playback scrubbing: Confirm `onScrubbingChanged` keeps controls visible while dragging the scrubber.
- Single-tap visibility toggle: Confirm all floating islands (top bar, video capsule, bottom dock) fade and slide in unison.
- Trash folder: Confirm bottom dock displays Restore and Permanent Delete correctly when viewing items in Trash.

---

### Task 1: Replicate Top Bar Floating Islands in `DetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

- [x] **Step 1.1**: Replace the edge-to-edge gradient top bar container with a floating row layout at `statusBarsPadding() + top = 8.dp`.
- [x] **Step 1.2**: Implement the Left Island: 40dp `CircleShape` button with `Color.Black.copy(0.45f)` and `ic_ms_arrow_back`.
- [x] **Step 1.3**: Implement the Center Island: `ShapeFull` pill with `Color.Black.copy(0.45f)` containing:
  - Top line: `currentItem.name` (13sp, SemiBold, White, single-line, `TextOverflow.Ellipsis`).
  - Bottom line: formatted Date & Time (`SimpleDateFormat("MMMM d, yyyy h:mm a")`, 11sp, White 75% alpha).
  - Integrated "LIVE" toggle badge if `motionPhotoInfo?.isMotionPhoto == true`.
- [x] **Step 1.4**: Implement the Right Island: 40dp `CircleShape` button with `Color.Black.copy(0.45f)` and `ic_ms_more_horiz`, anchoring the 3-dot dropdown menu.
- [x] **Step 1.5**: Verify code compiles cleanly with no syntax errors.

---

### Task 2: Replicate Bottom Dock & Remove Filmstrip in `DetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

- [x] **Step 2.1**: Remove `ViewerFilmstripRow` invocation and remove the filmstrip logic from `DetailScreen.kt`.
- [x] **Step 2.2**: Update `ViewerActionButtonsRow` into the iOS 18 3-part layout:
  - Left: 44dp circular `IconButton` (`CircleShape`, `Color.Black.copy(0.45f)`) with `ic_ms_share`.
  - Center Pill: `ShapeFull`, `Color.Black.copy(0.45f)`, height 48dp, containing:
    1. Favorite Heart (`ic_ms_favorite` / `ic_ms_favorite_filled` with spring bounce).
    2. Info / Details (`ic_ms_info`).
    3. Edit / Tune (`ic_ms_tune`).
  - Right: 44dp circular `IconButton` (`CircleShape`, `Color.Black.copy(0.45f)`) with `ic_ms_delete`.
- [x] **Step 2.3**: For Trash mode (`bucketName == "Trash"`), show Restore on the left and Permanent Delete on the right.
- [x] **Step 2.4**: Remove any remaining `border = BorderStroke(...)` to enforce the No Outlines Policy.
- [x] **Step 2.5**: Verify code compiles cleanly.

---

### Task 3: Replicate Floating Video Scrubber Capsule in `VideoPlayerItem.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/VideoPlayerItem.kt`

- [x] **Step 3.1**: Inspect existing dual-pill video playback controls in `VideoPlayerItem.kt`.
- [x] **Step 3.2**: Replace with a single unified floating capsule (`ShapeFull`, `Color.Black.copy(0.50f)`):
  - Left: Play/Pause icon button (`ic_ms_play_arrow` / `ic_ms_pause`, 20dp, White).
  - Center: Sleek horizontal slider with primary blue active track, dark inactive track, and compact thumb.
  - Right: Mute/Unmute audio toggle button (`ic_ms_volume_up` / `ic_ms_volume_off`, 20dp, White).
- [x] **Step 3.3**: Position the capsule neatly above the bottom dock with safe bottom spacing.
- [x] **Step 3.4**: Ensure `onScrubbingChanged` correctly fires to pause auto-hide while scrubbing.
- [x] **Step 3.5**: Verify code compiles cleanly.

---

### Task 4: Assemble, Deploy to Debug Package, and Verify on Device

**Files:**
- Output APK: `app/build/outputs/apk/debug/PhotonGallery-arm64-v8a.apk`

- [x] **Step 4.1**: Run `.\gradlew.bat assembleDebug` and confirm `BUILD SUCCESSFUL`.
- [ ] **Step 4.2**: Install debug APK to device `10BEAK2JR50012G` (`com.inferno.gallery.debug`).
- [ ] **Step 4.3**: Launch `com.inferno.gallery.debug/com.inferno.gallery.MainActivity`.
- [ ] **Step 4.4**: Open a photo and a video, take screenshots via adb to verify the iOS top islands, video scrubber capsule, and bottom dock matching the reference image.
- [ ] **Step 4.5**: Commit all changes to git.


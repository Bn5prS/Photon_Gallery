# Freeform and Aspect-Ratio-Preserving Crop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement freeform and aspect-ratio-preserving interactive cropping in the photo editor (`ImageEditorScreen.kt`), allowing users to drag corner handles, edge handles, or the inner area with fluid boundary clamping, minimum dimensions, visual feedback, and haptics.

**Architecture:** Extract crop geometry, hit-testing, and touch transformation into a pure Kotlin math/logic helper class `CropGeometryHelper` in `com.inferno.gallery.ui.editor.crop`. This separates all coordinate projection, aspect ratio preservation, boundary clamping, and hit-testing from Compose UI so that 100% of the gesture logic is covered by fast, deterministic JVM unit tests. Then integrate `CropGeometryHelper` into `CropPreview` in `ImageEditorScreen.kt`, updating canvas rendering with clear corner brackets and edge indicators, and connecting pointer drag gestures with haptic feedback.

**Tech Stack:** Kotlin, Jetpack Compose, Compose Geometry (`Rect`, `Offset`), JUnit 4.

**Spec:** Issue #24: `[Feature Request] Freeform / custom crop like standard gallery apps`. Standard Google Photos behavior:
- When "Free" is selected: full freeform dragging of any of the 4 corners and 4 edges, plus dragging inside the rectangle to pan/translate.
- When a fixed ratio is selected (1:1, 4:3, 3:4, 16:9, 9:16): corner handle dragging resizes while strictly preserving the selected aspect ratio, anchored at the opposite corner.
- Minimum crop size constraint (cannot collapse below 48dp / min size threshold).
- Hard clamping to image boundaries `(0f, 0f, 1f, 1f)` in normalized space.

## Global Constraints
- Must run completely offline with 0 cloud dependencies.
- Follow Material 3 Expressive guidelines and existing app design tokens.
- Maintain existing non-destructive editing workflow and Bitmap export pipeline in `ImageEditorScreen.kt`.
- No new third-party dependencies.

## Review Focus
- Touch boundary: Dragging outside image frame or rapid fling does not cause crop rect coordinates to exceed `[0f, 1f]` or produce negative width/height.
- Inversion prevention: Dragging top handle below bottom handle or left handle past right handle must clamp cleanly at `minSize` instead of flipping coordinates.
- Ratio precision: When resizing with a fixed aspect ratio (e.g. 1:1, 16:9), the resulting pixel aspect ratio matches the target ratio within floating point epsilon across arbitrary image orientations and dimensions.
- Effective aspect with rotation: 90/270 degree rotation swaps image dimensions, so the normalized aspect ratio calculation must respect `effectiveAspect`.
- Small image / high-density display: Hit-test touch radius adapts appropriately to screen density (dp to px) so corner handles are comfortably touchable on all screen sizes.

---

### Task 1: Create `CropGeometryHelper` and Unit Tests

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelper.kt`
- Create: `app/src/test/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelperTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CropHandle {
      NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
      LEFT, TOP, RIGHT, BOTTOM, INSIDE
  }

  object CropGeometryHelper {
      fun hitTest(
          touchX: Float,
          touchY: Float,
          cropRect: Rect,       // normalized 0..1
          imageRect: Rect,      // canvas pixels
          cornerHitRadiusPx: Float,
          edgeHitMarginPx: Float
      ): CropHandle

      fun updateCropRect(
          currentRect: Rect,     // normalized 0..1
          handle: CropHandle,
          deltaXNormalized: Float,
          deltaYNormalized: Float,
          aspectLabel: String,
          effectiveAspect: Float,
          minWidthNormalized: Float,
          minHeightNormalized: Float
      ): Rect
  }
  ```

- [ ] **Step 1: Write the failing unit tests for `CropGeometryHelper`**

Create `app/src/test/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelperTest.kt` testing:
1. `hitTest` correctly identifies `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `TOP`, `BOTTOM`, `LEFT`, `RIGHT`, `INSIDE`, and `NONE`.
2. Freeform resize for all handles (corners and edges) updates bounds accurately and clamps to `[0f, 1f]`.
3. Inversion clamp: resizing beyond the opposite handle respects `minWidthNormalized` and `minHeightNormalized`.
4. Translation (`INSIDE`) moves the rect while preserving width and height within `[0f, 1f]`.
5. Fixed aspect ratio resize (1:1, 16:9) preserves aspect ratio within epsilon ($10^{-4}$) during corner dragging.

- [ ] **Step 2: Run test to verify failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.inferno.gallery.ui.editor.crop.CropGeometryHelperTest"`
Expected: Compilation failure or FAIL (class does not exist yet).

- [ ] **Step 3: Implement `CropGeometryHelper.kt`**

Implement `CropGeometryHelper.kt` with:
- Hit testing logic with priority: corners first (Euclidean distance <= radius), then edges (perpendicular distance <= margin), then inside (point in rect).
- Freeform transformation: updates individual bounds with clamping against opposite bound minus `minSize` and image boundary `[0f, 1f]`.
- Fixed ratio transformation: calculates normalized ratio $r = R / \text{effectiveAspect}$, anchors opposite corner, projects candidate diagonal delta to enforce $w = h \times r$, and clamps to whichever boundary (`0f` or `1f`) is reached first.
- Inside translation: offsets `(left, top)` bounded by `0f` and `1f - size`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.inferno.gallery.ui.editor.crop.CropGeometryHelperTest"`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelper.kt app/src/test/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelperTest.kt
git commit -m "feat(editor): add CropGeometryHelper with freeform and aspect-ratio math and unit tests"
```

---

### Task 2: Integrate Interactive Crop into `CropPreview` in `ImageEditorScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt:1420-1550`

**Interfaces:**
- Consumes: `CropGeometryHelper`, `CropHandle` from `com.inferno.gallery.ui.editor.crop`
- Modifies: `CropPreview` composable in `ImageEditorScreen.kt`

- [ ] **Step 1: Update `CropPreview` gesture detection and state**

In `ImageEditorScreen.kt`:
1. Track active dragging handle: `var activeHandle by remember { mutableStateOf(CropHandle.NONE) }`.
2. Compute `cornerHitRadiusPx` (e.g. 36.dp to px) and `edgeHitMarginPx` (e.g. 24.dp to px).
3. Compute `minWidthNormalized` and `minHeightNormalized` (e.g. 48.dp to px converted to normalized ratio).
4. Replace existing simple `detectDragGestures` with a gesture detector that:
   - On drag start: resolves `activeHandle = CropGeometryHelper.hitTest(...)`, triggering haptic feedback if a handle is engaged.
   - On drag: calls `CropGeometryHelper.updateCropRect(...)` with normalized deltas and invokes `onCropRectChanged`.
   - On drag end / cancel: resets `activeHandle = CropHandle.NONE`.

- [ ] **Step 2: Polish visual rendering on Canvas**

1. Highlight active handle: when `activeHandle` is a corner or edge, render active handle indicator with primary/accent color or thicker stroke for immediate tactile feedback.
2. Rule-of-thirds grid: animate alpha smoothly (e.g. 0.3f when idle, 0.7f when dragging) so the grid is prominently visible while aligning the crop.
3. Draw edge center pill/notch handles in addition to the corner brackets when `"Free"` is selected, visually signaling to users that edges are also draggable.

- [ ] **Step 3: Verify compilation and tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all unit tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt
git commit -m "feat(editor): integrate interactive freeform and aspect-ratio crop gestures into ImageEditor"
```

---

### Task 3: Verification and Release Notes Update

**Files:**
- Modify: `RELEASE_NOTES.md` (or draft notes for next version)

- [ ] **Step 1: Run full unit test suite and assemble debug APK**

Run: `.\gradlew.bat testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Document improvements and close Issue #24**

Draft the comment to Issue #24 showing the implementation and mark ready to close once tested.


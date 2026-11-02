# Photon Editor Editor Replication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replicate Photon Editor' exact photo editor UI and all offline features in Photon Gallery with 60–120 FPS GPU rendering, stripping out cloud/external AI features.

**Architecture:** Modular studio architecture under `com.inferno.gallery.ui.editor`, powered by a GPU ColorMatrix/Canvas engine for instant preview and a lossless background export worker for saving.

**Tech Stack:** Jetpack Compose, Material 3 Expressive tokens, Android Graphics (Canvas, ColorMatrix, AGSL RuntimeShader on API 33+), Kotlin Coroutines.

**Spec:** `docs/superpowers/specs/2026-11-02-google-photos-editor-replication-design.md`

## Global Constraints
- `minSdk`: 31 (Android 12), `targetSdk`: 35
- Universal GPU ColorMatrix/Canvas rendering with AGSL enhancements on API 33+
- Strict offline operation: zero internet permissions or cloud AI dependencies
- Match Photon Editor Material 3 Expressive tokens (capsule sliders, rounded squircles, dynamic accent colors, smooth inertia)

## Review Focus
1. **Slider scrubbing responsiveness**: Zero bitmap allocations or frame drops during rapid touch dragging of ruler wheel dials.
2. **Lossless high-res save**: Full original resolution preserved when saving, applying the exact geometric and color transformations.
3. **Undo/Redo fidelity**: Every change across Crop, Adjust, Filter, and Markup correctly pushes to history and restores state cleanly.
4. **Hold-to-compare**: Pressing the `[|]` button or long-pressing the photo preview instantly reveals the unedited original with zero flicker.
5. **Screen rotation / configuration changes**: Editor state, active tab, and sliders remain preserved without loss of work.

---

### Task 1: Core Shell, Top Bar, Quick Action Scrim Bar, and Bottom Navigation Tabs

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/model/EditorTab.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/model/EditorHistoryState.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorTopBar.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorQuickActionBar.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorBottomTabs.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorOverflowSheet.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/EditorHistoryStateTest.kt`

**Interfaces:**
- Produces:
  - `enum class EditorTab { AUTO, CROP, ADJUST, MARKUP, FILTERS, MORE }`
  - `data class EditorHistoryState<T>(val past: List<T>, val current: T, val future: List<T>)` with `.push()`, `.undo()`, `.redo()`, `.canUndo`, `.canRedo`
  - `EditorTopBar(onCancel, onUndo, onRedo, canUndo, canRedo, hasModifications, onSaveAsCopy, onMoreOptions)`
  - `EditorQuickActionBar(onStraighten, currentAspect, onAspectClick, onCompareDown, onCompareUp, onRotate)`
  - `EditorBottomTabs(selectedTab, onSelectTab, onOpenSearch)`

- [ ] **Step 1: Write unit tests for `EditorHistoryState`**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `EditorTab.kt`, `EditorHistoryState.kt`, `EditorTopBar.kt`, `EditorQuickActionBar.kt`, `EditorBottomTabs.kt`, and `EditorOverflowSheet.kt`**
- [ ] **Step 4: Run unit tests to verify pass**
- [ ] **Step 5: Commit changes**

---

### Task 2: Photon Editor Signature Ruler Slider & Corner Bracket Crop Handles

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorRulerSlider.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/components/EditorCornerBrackets.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/EditorRulerSliderMathTest.kt`

**Interfaces:**
- Consumes: Theme tokens, Haptics
- Produces:
  - `EditorRulerSlider(value: Float, range: ClosedFloatingPointRange<Float>, step: Float = 1f, defaultValue: Float = 0f, iconRes: Int?, onValueChange: (Float) -> Unit, onReset: () -> Unit, onDone: () -> Unit)`
  - `EditorCornerBrackets(cropRect: Rect, onCropRectChange: (Rect) -> Unit, aspectRatio: Float?)`

- [ ] **Step 1: Write unit tests for ruler tick math and snap calculation**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `EditorRulerSlider.kt` with capsule container, tick marks canvas, center accent indicator line, reset button, and Done pill button**
- [ ] **Step 4: Implement `EditorCornerBrackets.kt` with 4 L-shaped corner handles, touch drag tracking, and boundary clamping**
- [ ] **Step 5: Run unit tests to verify pass**
- [ ] **Step 6: Commit changes**

---

### Task 3: Adjust Studio & Universal GPU ColorMatrix/Shader Engine

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/model/AdjustTool.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/engine/ImageProcessingEngine.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/AdjustStudio.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/ImageProcessingEngineTest.kt`

**Interfaces:**
- Produces:
  - `enum class AdjustTool` (13 controls: BRIGHTNESS, CONTRAST, TONE, WHITE_POINT, HIGHLIGHTS, SHADOWS, BLACK_POINT, SATURATION, WARMTH, TINT, SKIN_TONE, BLUE_TONE, VIGNETTE)
  - `data class TuneValues(...)`
  - `ImageProcessingEngine.buildColorMatrix(tune: TuneValues, filterMatrix: ColorMatrix?): ColorMatrix`
  - `AdjustStudio(tuneValues, onTuneValuesChange, onDone)`

- [ ] **Step 1: Write unit tests for `ImageProcessingEngine` ColorMatrix computations**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `AdjustTool.kt`, `ImageProcessingEngine.kt`, and `AdjustStudio.kt` with the 13 squircle buttons and active ruler slider**
- [ ] **Step 4: Run unit tests to verify pass**
- [ ] **Step 5: Commit changes**

---

### Task 4: Filters Studio & Auto Suggestions Studio

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/model/FilterPreset.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/FiltersStudio.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/AutoStudio.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/FilterPresetTest.kt`

**Interfaces:**
- Produces:
  - `FilterPreset` with 14 Photon Editor filters and `.interpolate(strength: Float): ColorMatrix`
  - `FiltersStudio(previewBitmap, selectedFilterIndex, filterStrength, onSelectFilter, onFilterStrengthChange, onDone)`
  - `AutoStudio(previewBitmap, onSelectSuggestion)`

- [ ] **Step 1: Write unit tests for filter matrix interpolation**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `FilterPreset.kt` (Original, Lite, Playa, Honey, Isla, Desert, Clay, Palma, Metro, Eiffel, Modena, Breeze, Vogue, Onyx)**
- [ ] **Step 4: Implement `FiltersStudio.kt` (thumbnails carousel + intensity ruler slider) and `AutoStudio.kt` (image-adaptive Enhance, Dynamic cards)**
- [ ] **Step 5: Run unit tests to verify pass**
- [ ] **Step 6: Commit changes**

---

### Task 5: Crop, Framing & Perspective Studio

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/model/AspectRatioOption.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/CropStudio.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/CropGeometryTest.kt`

**Interfaces:**
- Produces:
  - `AspectRatioOption` (Freeform, Original, Square, 5:4, 4:3, 3:2, 16:9, 4:5, 3:4, 2:3, 9:16)
  - `CropStudio(cropState, onCropStateChange, onStraightenDone)`

- [ ] **Step 1: Write unit tests for crop aspect ratio and rotation calculations**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `AspectRatioOption.kt` and `CropStudio.kt` (Crop, Straighten framing dial -45..+45, Rotate, 2-axis perspective keystone dials)**
- [ ] **Step 4: Run unit tests to verify pass**
- [ ] **Step 5: Commit changes**

---

### Task 6: Markup & Privacy Redact Studio

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/MarkupStudio.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupCanvasOverlay.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupRasterizer.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/MarkupRedactTest.kt`

**Interfaces:**
- Produces:
  - `MarkupStudio(markupState, onMarkupStateChange, onDone)`
  - Support for `MarkupTool.PEN`, `MarkupTool.HIGHLIGHTER`, `MarkupTool.REDACT` (mosaic/pixelation), `MarkupTool.TEXT`

- [ ] **Step 1: Write unit tests for Redact path geometry and rasterization**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `MarkupStudio.kt` matching Photon Editor UI with Pen/Highlighter capsule slider, color palette, Redact mosaic shader, and Text overlay**
- [ ] **Step 4: Update `MarkupCanvasOverlay.kt` and `MarkupRasterizer.kt` for mosaic redact rendering**
- [ ] **Step 5: Run unit tests to verify pass**
- [ ] **Step 6: Commit changes**

---

### Task 7: Tool Search Sheet, "More" Studio & High-Res Background Save Worker

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/ToolSearchSheet.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/studios/MoreStudio.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/engine/ImageExportWorker.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/ToolSearchTest.kt`

**Interfaces:**
- Produces:
  - `ToolSearchSheet(onToolSelected: (EditorTab, Any?) -> Unit, onDismiss)`
  - `MoreStudio(imageUri: Uri)`
  - `ImageExportWorker.exportEditedImage(context, imageUri, state): Uri`

- [ ] **Step 1: Write unit tests for ToolSearch catalog filtering**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `ToolSearchSheet.kt`, `MoreStudio.kt`, and `ImageExportWorker.kt`**
- [ ] **Step 4: Run unit tests to verify pass**
- [ ] **Step 5: Commit changes**

---

### Task 8: Main Screen Integration, Navigation Wiring & Device Verification

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/PhotonPhotosEditorScreen.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/NavigationGraph.kt`
- Test: Instrumented / device verification

**Interfaces:**
- Produces:
  - `PhotonPhotosEditorScreen(imageUri: Uri, onBack: () -> Unit, onSaved: (Uri) -> Unit)`
  - Replaces old monolithic editor route in `NavigationGraph.kt`

- [ ] **Step 1: Implement `PhotonPhotosEditorScreen.kt` uniting all studios, canvas transforms, hold-to-compare, undo/redo, and save**
- [ ] **Step 2: Wire `PhotonPhotosEditorScreen` into `NavigationGraph.kt`**
- [ ] **Step 3: Run `./gradlew assembleDebug` and verify build succeeds**
- [ ] **Step 4: Install onto connected device `10BEAK2JR50012G` via `adb install`**
- [ ] **Step 5: Launch Photon Gallery editor and capture live screenshots side-by-side with Photon Editor**
- [ ] **Step 6: Verify all offline features on device and commit**

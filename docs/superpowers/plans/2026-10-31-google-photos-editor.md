# Google Photos Suite Image Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replicate the complete client-side Google Photos Image Editor suite into Photon Gallery (`ImageEditorScreen.kt`), featuring interactive freeform/aspect-ratio crop, fine straighten angle rotation (-45° to +45°), comprehensive Adjust/Tune tools (Brightness, Contrast, Highlights, Shadows, White Point, Black Point, Saturation, Warmth, Tint, Sharpen, Vignette), one-tap Suggestions (Auto Enhance), curated Filters, and unified Material 3 Expressive bottom navigation tabs.

**Architecture:** 
1. `CropGeometryHelper` in `com.inferno.gallery.ui.editor.crop` provides pure Kotlin math for 8-point hit testing, freeform bounding, and aspect-ratio-locked corner resizing.
2. `EditorModels.kt` / `ImageEditorScreen.kt` data structures expanded for straighten angles, full tune properties, and Suggestions.
3. Enhanced ColorMatrix and Canvas rendering pipelines for live preview and high-res export (supporting straighten rotation, color matrices, tint, sharpen, and vignette).
4. Redesigned UI adhering to Material 3 Expressive standards: bottom tab bar (`Suggestions`, `Crop`, `Adjust`, `Filters`, `Markup`), horizontal parameter selector, centered zero-notch sliders with numeric badges, and interactive crop canvas.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Compose Canvas & GraphicsLayer, Android ColorMatrix & Bitmap shaders, JUnit 4.

## Global Constraints
- 100% offline with zero cloud or proprietary Google Play Services dependencies.
- Non-destructive editor state with Undo/Redo stack.
- Support both Save (new copy) and Overwrite options.
- Fast, smooth 60/120fps preview rendering without main-thread jank.

---

### Task 1: `CropGeometryHelper` and Unit Test Suite

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelper.kt`
- Create: `app/src/test/java/com/inferno/gallery/ui/editor/crop/CropGeometryHelperTest.kt`

- [ ] **Step 1: Write unit tests for crop math, hit testing, and aspect ratios**
- [ ] **Step 2: Run tests to verify failure**
- [ ] **Step 3: Implement `CropGeometryHelper` (8-point hit test, freeform resize, aspect lock, inside move)**
- [ ] **Step 4: Run tests to verify PASS**
- [ ] **Step 5: Commit**

---

### Task 2: Expand Editor Data Models & Processing Pipeline

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt`

- [ ] **Step 1: Expand `EditorTool` to `SUGGESTIONS`, `CROP`, `ADJUST`, `FILTERS`, `MARKUP`**
- [ ] **Step 2: Expand `CropState` with `straightenAngle: Float = 0f` and aspect presets ("Free", "Original", "1:1", "5:4", "4:3", "3:2", "16:9")**
- [ ] **Step 3: Expand `TuneValues` with `whitePoint`, `blackPoint`, `tint`, `sharpen`, `vignette`**
- [ ] **Step 4: Update `buildCombinedMatrix` and save pipeline to apply straighten rotation, tint, and expanded parameters**
- [ ] **Step 5: Add Auto-Enhance preset generator for `SUGGESTIONS`**
- [ ] **Step 6: Verify compilation and unit tests**
- [ ] **Step 7: Commit**

---

### Task 3: Interactive Crop & Straighten Dial UI

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt`

- [ ] **Step 1: Implement interactive gesture detection in `CropPreview` using `CropGeometryHelper`**
- [ ] **Step 2: Render 4 corner brackets, 4 edge drag pills, and dynamic rule-of-thirds grid**
- [ ] **Step 3: Build Straighten Angle dial/slider (-45°..+45°) with degree badge and zero-notch haptics**
- [ ] **Step 4: Add aspect ratio selector (Free, Original, 1:1, 5:4, 4:3, 3:2, 16:9) and 90° rotate / flip**
- [ ] **Step 5: Verify compilation and tests**
- [ ] **Step 6: Commit**

---

### Task 4: Complete Adjust Suite & Suggestions UI Polish

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt`

- [ ] **Step 1: Build horizontal Adjust parameter selector chips (Brightness, Contrast, Highlights, Shadows, White Point, Black Point, Saturation, Warmth, Tint, Vignette)**
- [ ] **Step 2: Build centered zero-notch slider with live numeric value badge and haptic feedback**
- [ ] **Step 3: Build Suggestions panel with 1-tap "Enhance", "Warm", "Cool", "Dynamic" cards**
- [ ] **Step 4: Update bottom navigation dock to Material 3 Expressive tabs**
- [ ] **Step 5: Verify full test suite and assemble APK**
- [ ] **Step 6: Commit**


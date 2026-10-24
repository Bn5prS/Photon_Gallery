# Image Editor Unified Markup & Paint Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a unified offline Markup tool tab (Pen, Highlighter, Arrow, Eraser, Draggable Text, Color Palette, and Stroke Thickness) in `ImageEditorScreen.kt` that exports at full photo resolution.

**Architecture:** Create a focused, modular `com.inferno.gallery.ui.editor.markup` package for models, touch canvas overlay, tool dock, and rasterization engine. Wire cleanly into `EditorState` for full undo/redo support and integrate into `saveImage()` in `ImageEditorScreen.kt`.

**Tech Stack:** Jetpack Compose, Compose Foundation (`Canvas`, `detectDragGestures`), Android Graphics (`android.graphics.Canvas`, `android.graphics.Paint`, `Path`), Material 3 Expressive, JUnit.

**Spec:** [docs/superpowers/specs/2026-10-24-image-editor-markup-tools-design.md](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-24-image-editor-markup-tools-design.md)

## Global Constraints
- Runs 100% offline with zero cloud or third-party SDK dependencies.
- Strictly adheres to Material 3 Expressive (`AGENTS.md`): no raw hex colors in UI, corner radii from official 10-tier scale, Google Fonts type scale, and `ic_ms_*` drawables.
- Normalized coordinate system ($0.0 .. 1.0$) for all stroke and text positions to ensure identical rendering on preview and full 4K/108MP exported images.
- Full Undo/Redo compatibility via immutable `EditorState` snapshots.

## Review Focus
1. **Touch Drag Gestures vs Zoom/Pan Conflict**: While in Markup mode, drawing gestures must smoothly capture touch without getting hijacked by container scrolling.
2. **Highlighter Alpha Bleed**: Highlighter strokes must blend evenly with `SRC_OVER` alpha (semi-transparent) without dark overlap artifacts at sharp corners.
3. **Draggable Text Boundary Clamping**: Draggable text overlays must stay clamped within image bounds $[0.0, 1.0]$ so they cannot be dragged off-screen.
4. **Eraser Accuracy**: Eraser must reliably detect stroke hit-boxes using point-to-segment distance within a comfortable touch radius.
5. **High-Res Export Scaling**: Brush thickness and text size must scale proportionally to output bitmap dimensions (e.g., $1080 \times 1920$ vs $3000 \times 4000$) so exports look identical to preview.

---

### Task 1: Markup Data Models & Unit Tests

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupModels.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/markup/MarkupModelsTest.kt`

**Interfaces:**
- Produces: `enum class MarkupTool`, `data class MarkupStroke`, `data class MarkupText`, `data class MarkupState`, `object MarkupPalette`

- [ ] **Step 1: Write the failing unit tests for MarkupModels**
```kotlin
package com.inferno.gallery.ui.editor.markup

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupModelsTest {
    @Test
    fun testStrokeHitTestDetectsPointNearSegment() {
        val stroke = MarkupStroke(
            id = 1L,
            points = listOf(Offset(0.1f, 0.1f), Offset(0.5f, 0.5f)),
            color = Color.Red,
            strokeWidth = 0.01f,
            tool = MarkupTool.PEN
        )
        // Hit near segment (0.3, 0.3)
        assertTrue(stroke.isHitBy(Offset(0.31f, 0.30f), threshold = 0.03f))
    }

    @Test
    fun testStateImmutabilityAndUndo() {
        val state1 = MarkupState()
        val stroke = MarkupStroke(1L, listOf(Offset(0.1f, 0.1f)), Color.Black, 0.01f, MarkupTool.PEN)
        val state2 = state1.withAddedStroke(stroke)
        assertEquals(0, state1.strokes.size)
        assertEquals(1, state2.strokes.size)
        val state3 = state2.withRemovedStroke(1L)
        assertEquals(0, state3.strokes.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.editor.markup.MarkupModelsTest"`
Expected: FAIL (types do not exist yet)

- [ ] **Step 3: Implement `MarkupModels.kt`**
Define `MarkupTool`, `MarkupStroke` with `isHitBy` distance formula, `MarkupText`, `MarkupState` with helper methods (`withAddedStroke`, `withRemovedStroke`, `withAddedText`, `withUpdatedText`, `withRemovedText`), and curated `MarkupPalette` (10 vibrant colors).

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.editor.markup.MarkupModelsTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupModels.kt app/src/test/java/com/inferno/gallery/ui/editor/markup/MarkupModelsTest.kt
git commit -m "feat(editor): add markup data models, hit-testing, and unit tests"
```

---

### Task 2: High-Resolution Canvas Rasterizer & Tests

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupRasterizer.kt`
- Test: `app/src/test/java/com/inferno/gallery/ui/editor/markup/MarkupRasterizerTest.kt`

**Interfaces:**
- Consumes: `MarkupState`, `MarkupStroke`, `MarkupText` from Task 1
- Produces: `fun rasterizeMarkup(canvas: android.graphics.Canvas, bitmapWidth: Int, bitmapHeight: Int, markup: MarkupState)`

- [ ] **Step 1: Write the failing unit tests for MarkupRasterizer**
```kotlin
package com.inferno.gallery.ui.editor.markup

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarkupRasterizerTest {
    @Test
    fun testRasterizerDrawsWithoutException() {
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = MarkupStroke(
            id = 1L,
            points = listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)),
            color = Color.Blue,
            strokeWidth = 0.02f,
            tool = MarkupTool.PEN
        )
        val text = MarkupText(
            id = 2L,
            text = "Test Label",
            position = Offset(0.5f, 0.5f),
            color = Color.White,
            fontSize = 24f,
            hasBackgroundBox = true
        )
        val state = MarkupState(strokes = listOf(stroke), texts = listOf(text))
        MarkupRasterizer.rasterize(canvas, 500, 500, state)
        assertNotNull(bitmap)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.editor.markup.MarkupRasterizerTest"`
Expected: FAIL (MarkupRasterizer not implemented)

- [ ] **Step 3: Implement `MarkupRasterizer.kt`**
Implement `object MarkupRasterizer`:
- Scale normalized coordinates to absolute pixels.
- Pen: Smooth round cap/join path rendering.
- Highlighter: Semi-transparent stroke with alpha `0.45f`.
- Arrow: Line with calculated equilateral arrowhead vector at terminal point.
- Text: Render background pill with rounded corners, then text with vertical/horizontal centering.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.editor.markup.MarkupRasterizerTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupRasterizer.kt app/src/test/java/com/inferno/gallery/ui/editor/markup/MarkupRasterizerTest.kt
git commit -m "feat(editor): implement high-resolution markup rasterizer for export"
```

---

### Task 3: Interactive Markup Canvas & Text Overlay Composable

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupCanvasOverlay.kt`

**Interfaces:**
- Consumes: `MarkupState`, `MarkupTool`, `MarkupStroke`, `MarkupText` from Task 1
- Produces: `@Composable fun MarkupCanvasOverlay(modifier: Modifier, markupState: MarkupState, activeTool: MarkupTool, currentColor: Color, currentStrokeWidth: Float, onStateChanged: (MarkupState) -> Unit, onEditTextRequest: (MarkupText?) -> Unit)`

- [ ] **Step 1: Implement `MarkupCanvasOverlay.kt`**
- Use Jetpack Compose `Canvas` layered over image bounds.
- Pointer input via `pointerInput`:
  - When `PEN`, `HIGHLIGHTER`, `ARROW`: record points in real-time, emit finished `MarkupStroke` on gesture release, push state.
  - When `ERASER`: perform hit-testing with `stroke.isHitBy(touchOffset)`, remove touched stroke with haptic tick.
  - When `TEXT`: tap on canvas creates a new text overlay or selects existing text; dragging moves the text with position clamped to $[0..1]$.
- Live preview of currently active stroke as it is being drawn.

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupCanvasOverlay.kt
git commit -m "feat(editor): add interactive touch markup canvas overlay"
```

---

### Task 4: Markup Bottom Dock & Text Editor Dialog

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupBottomDock.kt`
- Create: `app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupTextEditorDialog.kt`

**Interfaces:**
- Consumes: `MarkupTool`, `MarkupPalette`
- Produces: `@Composable fun MarkupBottomDock(...)`, `@Composable fun MarkupTextEditorDialog(...)`

- [ ] **Step 1: Implement `MarkupBottomDock.kt`**
- Floating pill dock above bottom bar:
  - Tool segmented buttons: Pen, Highlighter, Arrow, Eraser, Text.
  - Color picker: Row of 10 color dots with animated active ring and checkmark.
  - Brush thickness slider: Popover or expandable slider (2dp to 32dp).
  - Material 3 Expressive styling with `MotionScheme.expressive()`.

- [ ] **Step 2: Implement `MarkupTextEditorDialog.kt`**
- Modal dialog for typing or editing text annotation:
  - Multi-line `TextField` with auto-focus.
  - Toggle for background pill/box (`hasBackgroundBox`).
  - Color selector.
  - "Done" button to place or update text on canvas.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupBottomDock.kt app/src/main/java/com/inferno/gallery/ui/editor/markup/MarkupTextEditorDialog.kt
git commit -m "feat(editor): add markup bottom dock and text editor dialog"
```

---

### Task 5: Integration into `ImageEditorScreen.kt` & Full Build Verification

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt`

**Interfaces:**
- Connects `EditorTool.MARKUP`, `EditorState.markup`, `MarkupCanvasOverlay`, `MarkupBottomDock`, and `MarkupRasterizer.rasterize` inside `saveImage()`.

- [ ] **Step 1: Add `MARKUP` to `EditorTool` and `MarkupState` to `EditorState`**
```kotlin
enum class EditorTool(val label: String, @androidx.annotation.DrawableRes val iconRes: Int) {
    TUNE("Tune", R.drawable.ic_ms_tune),
    CROP("Crop", R.drawable.ic_ms_crop),
    FILTERS("Filters", R.drawable.ic_ms_palette),
    VIGNETTE("Vignette", R.drawable.ic_ms_photo_camera),
    MARKUP("Markup", R.drawable.ic_ms_edit)
}

data class EditorState(
    val tune: TuneValues = TuneValues(),
    val crop: CropState = CropState(),
    val filterIndex: Int = 0,
    val vignetteStrength: Float = 0f,
    val markup: MarkupState = MarkupState()
)
```

- [ ] **Step 2: Embed `MarkupCanvasOverlay` in image preview area**
Layer `MarkupCanvasOverlay` directly over the displayed photo, matching the exact AspectRatio/Bounds of the visible image.

- [ ] **Step 3: Connect `MarkupBottomDock` in bottom tool panel**
When `activeTool == EditorTool.MARKUP`, display the markup dock instead of tune/crop/filter panels.

- [ ] **Step 4: Connect `MarkupRasterizer` in `saveImage()`**
In `saveImage()`, immediately after applying vignette:
```kotlin
if (currentState.markup.strokes.isNotEmpty() || currentState.markup.texts.isNotEmpty()) {
    MarkupRasterizer.rasterize(canvas, resultBitmap.width, resultBitmap.height, currentState.markup)
}
```

- [ ] **Step 5: Run all unit tests**
Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all unit tests pass).

- [ ] **Step 6: Build release APK**
Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL (`PhotonGallery-arm64-v8a.apk` compiled).

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/ImageEditorScreen.kt
git commit -m "feat(editor): integrate unified markup paint and text tools into image editor"
```

# Image Editor Unified Markup & Paint Tools — Design Specification

**Issue**: [#20 Add paint tools in image redactor](https://github.com/Bn5prS/Photon_Gallery/issues/20)  
**Author**: Michalych647  
**Date**: 2026-10-24  
**Status**: DRAFT FOR APPROVAL  

---

## 1. Intent & Problem Statement

Users frequently need to annotate, draw on, redact, or add captions to photos directly within Photon Gallery (e.g., circling details, redacting sensitive text/areas, adding explanatory arrows, or typing captions). Currently, the editor only supports tone tuning, cropping, filters, and vignetting.

This feature adds a unified, full-featured **Markup** tool tab to `ImageEditorScreen.kt` that supports freehand drawing, highlighter pens, arrows, erasers, and draggable text annotations.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Unified Tool Dock**: Provide an intuitive, Apple/Google Photos-style "Markup" tab alongside Tune, Crop, Filters, and Vignette.
- **Rich Drawing Modes**:
  - **Pen**: Opaque smooth strokes for drawing, circling, or doodling.
  - **Highlighter**: Semi-transparent, wide strokes for highlighting document text or emphasizing areas.
  - **Arrow**: Freehand arrow or line with an automatic directional arrowhead.
  - **Eraser**: Tap/drag eraser to delete individual drawn strokes.
- **Adjustable Styling**:
  - **Stroke Width**: Smooth slider from fine (2dp) to thick (32dp).
  - **Color Palette**: Expressive curated palette (White, Black, Red, Yellow, Green, Cyan, Blue, Purple, Orange, Pink).
- **Text Annotations**:
  - Add customizable text overlays.
  - Movable anywhere on the image via touch drag.
  - Text color matching the palette, with optional contrast background pill/box for legibility on busy photos.
- **Non-Destructive & Full Resolution Export**:
  - Normalized coordinate system ($0.0 .. 1.0$) so drawn strokes and text scale cleanly from preview to full 4K/108MP camera resolutions without pixelation.
  - Full Undo / Redo support integrated into `EditorState`.
  - 100% offline, zero cloud dependencies.

### Non-Goals
- Complex vector Bezier curve point-by-point editing (too complex for mobile gallery markup).
- Blur / pixelate redaction filters in this initial issue (those belong in an automated privacy redaction tool).

### Anti-Goals
- Adding heavyweight third-party drawing libraries (e.g. huge canvas drawing SDKs). Everything must be implemented cleanly in native Jetpack Compose & Android graphics `Canvas`/`Paint`.
- Modifying or destroying the original photo without user consent (supports overwrite vs save copy).

---

## 3. Approaches Considered & Trade-Offs

### Approach 1: Separate "Draw" and "Text" Tabs
- *Description*: Split into two independent bottom tabs: one for freehand drawing, one for text.
- *Trade-offs*: Clutters the primary tool bar (expanding it from 4 to 6 tabs). Users switching between drawing an arrow and adding a caption have to toggle tabs back and forth.

### Approach 2: Unified "Markup" Tool (Chosen by User)
- *Description*: A single "Markup" tab (icon: `ic_ms_edit`). When opened, a secondary floating bottom dock provides quick mode toggles: **Pen**, **Highlighter**, **Arrow**, **Eraser**, **Text**, with a color bar and stroke size indicator.
- *Trade-offs*: Highest usability, aligns with Google Photos and Apple Photos markup patterns, leaves the primary editor toolbar clean and uncluttered.

---

## 4. Technical Architecture & Data Model

### Data Models
```kotlin
enum class MarkupTool {
    PEN,
    HIGHLIGHTER,
    ARROW,
    ERASER,
    TEXT
}

data class MarkupStroke(
    val id: Long = System.nanoTime(),
    val points: List<Offset>,        // Normalized 0..1 relative to image bounds
    val color: Color,
    val strokeWidth: Float,          // Normalized relative to image width
    val tool: MarkupTool             // PEN, HIGHLIGHTER, ARROW
)

data class MarkupText(
    val id: Long = System.nanoTime(),
    val text: String,
    val position: Offset,            // Normalized center 0..1
    val color: Color,
    val fontSize: Float = 22f,
    val hasBackgroundBox: Boolean = true
)

data class MarkupState(
    val strokes: List<MarkupStroke> = emptyList(),
    val texts: List<MarkupText> = emptyList()
)
```

### Integration with `EditorState`
```kotlin
data class EditorState(
    val tune: TuneValues = TuneValues(),
    val crop: CropState = CropState(),
    val filterIndex: Int = 0,
    val vignetteStrength: Float = 0f,
    val markup: MarkupState = MarkupState()
)
```
- Because `undoStack` and `redoStack` store `EditorState`, any stroke completed or text moved/edited automatically pushes onto the undo stack!

### High-Resolution Rasterization in `saveImage()`
When saving the final bitmap:
1. Crop, rotation, and color adjustments are applied.
2. A high-res `android.graphics.Canvas` draws each stroke:
   - Points are scaled from normalized $[0, 1]$ coordinates to exact pixel dimensions.
   - Pen: `Paint` with `Style.STROKE`, `Cap.ROUND`, `Join.ROUND`.
   - Highlighter: `Paint` with `alpha = 120` and `Xfermode(PorterDuff.Mode.SRC_OVER)`.
   - Arrow: Stroke line plus calculated triangular arrowhead vector.
3. Text overlays are drawn using `android.graphics.Paint` (with optional rounded background pill/box for readability).
4. Output is compressed and saved to MediaStore.

---

## 5. UI & Material 3 Expressive Specification

- **Primary Toolbar**:
  - `EditorTool.MARKUP("Markup", R.drawable.ic_ms_edit)` added to bottom navigation row.
- **Secondary Markup Dock**:
  - Horizontal pill-shaped floating bar above navigation bar:
    - Tool chips: Pen, Highlighter, Arrow, Eraser, Text.
    - Color picker row (10 vibrant circles with active checkmark).
    - Brush thickness popover or slider button.
- **Canvas Interaction**:
  - Smooth pointer input handling with haptic feedback on touch start/finish.
  - Active text editing dialog or inline expandable text editor.

---

## 6. Verification Plan

1. **Unit Tests**:
   - Serialization and normalization of stroke points and text coordinates.
   - Undo/Redo stack validation when strokes/texts are added and popped.
2. **Device / Emulator Verification**:
   - Draw smooth strokes with pen and highlighter.
   - Erase strokes using the eraser tool.
   - Add text, move it across the screen, change color.
   - Save image as copy and verify output JPEG in gallery with sharp drawing and text at 100% resolution.

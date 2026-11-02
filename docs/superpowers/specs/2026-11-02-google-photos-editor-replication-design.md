# Photon Editor Editor UI & Offline Features Replication Design

## 1. Intent & Context
Photon Gallery aims to provide a world-class, fully offline, privacy-first gallery experience on Android. Photon Editor has set the gold standard for mobile photo editing UX with its Material 3 Expressive interface: intuitive bottom tabs, live preview thumbnails, horizontal ruler wheel dials, interactive crop brackets, and granular color science.

This design specifies the complete replication of Photon Editor' photo editor UI and all offline features in Photon Gallery, stripping out any internet-dependent or cloud AI capabilities (e.g. Magic Editor, cloud Ask, cloud Unblur).

---

## 2. Goals & Non-Goals

### Goals
- **Pixel-Accurate Photon Editor Editor UI**:
  - Top floating toolbar: Cancel `X`, Undo/Redo curve buttons, "Save as copy" accent pill, and `⋮` overflow sheet.
  - Quick action toolbar above photo: Straighten shortcut, Aspect ratio dropdown (11 options), Hold-to-compare `[|]`, 90° Rotate shortcut.
  - Photo canvas with Photon Editor corner bracket crop handles (thick white L-brackets) and tap-and-hold compare.
  - Material 3 Expressive bottom tab navigation: `Auto`, `Crop`, `Adjust`, `Markup`, `Filters`, `More`, and the `🔍` tool search icon.
  - Photon Editor' signature horizontal capsule ruler/wheel dial slider component for adjustments, filter intensity, and straighten angle.
- **Complete Offline Feature Set**:
  - **Auto**: Real-time image-adaptive suggestion presets ("Enhance", "Dynamic", "Warm", "Cool") with live preview cards.
  - **Crop & Framing**: Freeform and preset aspect ratios (Freeform, Original, Square, 5:4, 4:3, 3:2, 16:9, 4:5, 3:4, 2:3, 9:16), 90° rotation, horizontal mirror, fine straighten angle (-45° to +45°), and 2-axis keystone perspective dials (Vertical & Horizontal tilt).
  - **Adjust**: 13 granular controls (Brightness, Contrast, Tone HDR, White point, Highlights, Shadows, Black point, Saturation, Warmth, Tint, Skin tone, Blue tone, Vignette).
  - **Filters**: Rich filter library ("Original", "Lite", "Playa", "Honey", "Isla", "Desert", "Clay", "Palma", "Metro", "Eiffel", "Modena", "Breeze", "Vogue", "Onyx") with interactive strength ruler (0..100).
  - **Markup**: Pen, Highlighter (with stroke slider and color palette), Redact/Mosaic privacy tool, and Text overlays.
  - **More**: External app edit intent launcher.
  - **Tool Search**: Full-screen searchable catalog of all tools with descriptions and one-tap jump.
- **Instant 60–120 FPS Real-Time GPU Engine**: Zero frame drops during slider scrubbing via Compose `graphicsLayer` ColorMatrix + Canvas Shaders, with full-resolution lossless background export on Save.

### Non-Goals
- Internet-dependent or cloud-based AI features (Photon Magic Editor generative fill, cloud Unblur, "Ask Photos", cloud portrait relighting).
- Video editing (still photo editor focus).

---

## 3. Architecture & Resolved Implementation Decisions

### 3.1 Image Processing Engine
- **Universal GPU ColorMatrix & Canvas Shaders**: Linear transformations (Brightness, Contrast, Saturation, Warmth, Tint, White Point, Black Point) use a $4 \times 5$ affine ColorMatrix computed in microsecond time.
- **Selective Tonal & Color Adjustments (Highlights, Shadows, Tone HDR, Skin Tone, Blue Tone)**: 
  - On API 33+ (Android 13+): Enhanced with AGSL `RuntimeShader` (`RenderEffect.createRuntimeShaderEffect`) for true non-linear luminance isolation and hue-gated adjustments.
  - On API 31/32 (Android 12 fallback): Mapped to tuned polynomial ColorMatrix approximations with localized contrast and saturation curves.
- **Vignette**: Hardware-accelerated `RadialGradient` overlay with blend mode `Multiply` applied directly in the rendering pipeline.
- **Save Pipeline**: Runs on a background thread (`Dispatchers.Default`), applies the exact transformation matrix and shaders to the high-resolution bitmap, and commits to MediaStore as a new copy.

### 3.2 "Revert Edits" & Save Behavior
- **Revert Edits**: Resets all sliders, crop geometry, filters, and markup back to the pristine initial state of the current editing session.
- **Save as copy**: Leaves the source file untouched and creates a new edited photo in MediaStore with `-edited` suffix, refreshing the gallery immediately.

### 3.3 Perspective Transform
- **2-Axis Keystone Transform**: Two dedicated dials on `EditorRulerSlider`: **Vertical Tilt** (-45°..+45°) and **Horizontal Tilt** (-45°..+45°), executed via 3D projective homography matrix on the Canvas and graphics layer.

### 3.4 Auto Suggestions Engine
- Fast image-adaptive histogram analysis (<10ms) on a 128x128 downscaled preview bitmap:
  - **Enhance**: Auto-exposure + white balance + contrast stretch.
  - **Dynamic**: Shadow lift + vibrance boost + local tone enhancement.
  - **Warm / Cool**: Color temperature shifts.

### 3.5 Filters Engine
- 14 Photon Editor filters implemented via calibrated ColorMatrix color grading profiles.
- Scrubbing the 0..100 strength dial computes a linear matrix interpolation (`lerp`) between the Identity Matrix (0) and the Filter Matrix (100).

### 3.6 Markup Redact Tool
- Freeform mosaic/pixelation brush painting along touch paths, dynamically downsampling and block-sampling pixels in the covered path, with an alternate solid blackout bar mode.

---

## 4. UI Component Architecture

```
com.inferno.gallery.ui.editor/
├── PhotonPhotosEditorScreen.kt       # Main container, orchestrator, back handler, save logic
├── model/
│   ├── EditorTab.kt                  # AUTO, CROP, ADJUST, MARKUP, FILTERS, MORE
│   ├── AdjustTool.kt                 # 13 adjustments, ranges, defaults, icons, descriptions
│   ├── FilterPreset.kt               # Filter presets, color matrices, thumbnail generation
│   ├── AspectRatioOption.kt          # 11 aspect ratio presets + freeform
│   └── EditorHistoryState.kt         # Undo/Redo stack for all editor operations
├── components/
│   ├── EditorTopBar.kt               # Cancel, Undo/Redo, "Save as copy", overflow ⋮
│   ├── EditorQuickActionBar.kt       # Straighten, Aspect ratio dropdown, Compare [|], Rotate
│   ├── EditorBottomTabs.kt           # Material 3 Expressive pill carousel + Search 🔍
│   ├── EditorRulerSlider.kt          # Photon Editor horizontal capsule wheel ruler slider
│   ├── EditorCornerBrackets.kt       # 4 white L-bracket handles on crop bounds
│   └── EditorOverflowSheet.kt        # Revert edits, Help & feedback
├── studios/
│   ├── AutoStudio.kt                 # Enhance & Dynamic suggestion cards carousel
│   ├── CropStudio.kt                 # Crop, Straighten (Framing), Rotate, Aspect dropdown, Perspective
│   ├── AdjustStudio.kt               # 13 squircle adjustment buttons + ruler slider view
│   ├── FiltersStudio.kt              # Filter thumbnails carousel + strength ruler slider view
│   ├── MarkupStudio.kt               # Pen, Highlighter, Redact privacy tool, Text
│   └── ToolSearchSheet.kt            # "Search editing tools" full-screen sheet + direct launch
└── engine/
    ├── ImageProcessingEngine.kt      # ColorMatrix builder, Highlights/Shadows math, Vignette, AGSL
    └── ImageExportWorker.kt          # Lossless high-res background export & MediaStore commit
```

---

## 5. Subagent Division & Execution Plan

To execute cleanly in parallel:
1. **Subagent 1 (Core Shell & Top/Bottom Nav)**:
   - `EditorTopBar.kt`, `EditorQuickActionBar.kt`, `EditorBottomTabs.kt`, `EditorOverflowSheet.kt`.
   - Photon Editor dark theme palette, typography, pill badges, and animations.
2. **Subagent 2 (Photon Editor Ruler Wheel Slider & Corner Brackets)**:
   - `EditorRulerSlider.kt` (capsule container, ruler tick marks, inertia scrolling, center indicator, tool icon, reset button, Done button).
   - `EditorCornerBrackets.kt` (interactive 4 L-bracket handles with touch scaling and boundary snapping).
3. **Subagent 3 (Adjust Studio & ColorMatrix/Shader Engine)**:
   - `AdjustTool.kt` (13 controls with official icons & descriptions), `AdjustStudio.kt`, `ImageProcessingEngine.kt` (ColorMatrix & AGSL shader generation for Brightness, Contrast, Tone, White/Black point, Highlights, Shadows, Saturation, Warmth, Tint, Skin/Blue tone, Vignette).
4. **Subagent 4 (Filters Studio & Auto Suggestions)**:
   - `FilterPreset.kt` (all 14 Photon Editor filters: Original, Lite, Playa, Honey, Isla, Desert, Clay, Palma, Metro, Eiffel, Modena, Breeze, Vogue, Onyx), `FiltersStudio.kt` (live thumbnails carousel + strength slider), `AutoStudio.kt` (image-adaptive "Enhance", "Dynamic" presets).
5. **Subagent 5 (Markup & Redact Studio)**:
   - `MarkupStudio.kt` (Pen, Highlighter with thickness slider capsule, color swatches row, Redact privacy mosaic/pixelation tool, and draggable/rotatable Text).
6. **Subagent 6 (Tool Search, Aspect Ratios & Integration)**:
   - `AspectRatioOption.kt` (11 ratios dropdown), `ToolSearchSheet.kt` (searchable list with descriptions), `PhotonPhotosEditorScreen.kt` integration, connecting to `NavigationGraph.kt`.

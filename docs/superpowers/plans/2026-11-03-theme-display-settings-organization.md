# Theme & Display Settings Organization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the "Theme & Display" section in `SettingsScreen.kt` from a giant continuously-expanded scroll into compact, categorized Material 3 cards with modal bottom sheets for advanced pickers.

**Architecture:** 
- Theme & Colors card with compact rows for Custom HSL Color and Palette Style that launch Material 3 `ModalBottomSheet`s on demand.
- Semantic splitting of loose display toggles into dedicated "Media Viewer" and "Typography & Layout" cards.
- Multi-language localized string resources for new section headers.

**Tech Stack:** Jetpack Compose, Material 3 Expressive, MaterialKolor, Kotlin 2.x, Android Gradle.

**Spec:** `docs/superpowers/specs/2026-11-03-theme-display-settings-redesign.md`

## Global Constraints
- Preserve all existing `SettingsViewModel` and `SettingsRepository` state flows, preferences keys, and functionality.
- Maintain AMOLED true black contrast rules (`isCurrentlyDark` and `useAmoledBlack`).
- No external library additions.
- Pass unit tests and compile cleanly with 0 regressions.

## Review Focus
1. Switching theme mode between System, Light, and Dark must not dismiss active modal bottom sheets unexpectedly.
2. Custom color HSL changes in the bottom sheet must dispatch reactively to `viewModel.setAppSeedColor` without lag.
3. Palette style selection in the modal bottom sheet must immediately update the active theme profile.
4. AMOLED switch must remain disabled/grayed out when in light mode.
5. All new section titles and button labels must support English, Spanish, Portuguese, French, and German.

---

### Task 1: Add Localized String Resources
**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`
- Modify: `app/src/main/res/values-pt-rBR/strings.xml`
- Modify: `app/src/main/res/values-fr/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Define string keys in `values/strings.xml`**
  - `setting_media_viewer_title`: "Media Viewer"
  - `setting_media_viewer_sub`: "Ambient lighting, blurred backdrop, minimap and brightness"
  - `setting_typography_layout_title`: "Typography & Layout"
  - `setting_typography_layout_sub`: "Fonts, scaling, timeline alignment and album badges"
  - `setting_custom_color_title`: "Custom Theme Color"
  - `setting_custom_color_sub`: "Fine-tune seed chromaticity with HSL sliders"
  - `setting_palette_style_title`: "Palette Style"
  - `setting_palette_style_sub`: "Color science profiles via MaterialKolor"
  - `action_customize`: "Customize"
  - `action_change`: "Change"
- [ ] **Step 2: Localize strings in es, pt-rBR, fr, and de resource files**
- [ ] **Step 3: Verify resources parse cleanly via Gradle**

---

### Task 2: Implement Compact Custom Color & Palette Style Modal Sheets in `SettingsScreen.kt`
**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt`

- [ ] **Step 1: Add modal sheet states in `SettingsScreen`**
  - `var showCustomColorSheet by rememberSaveable { mutableStateOf(false) }`
  - `var showPaletteStyleSheet by rememberSaveable { mutableStateOf(false) }`
- [ ] **Step 2: Refactor "Look & Feel" into 3 structured cards**:
  - **Card 1: Theme & Colors**:
    - Mode Segmented Row (System, Light, Dark)
    - AMOLED Switch
    - Dynamic Colors (Material You) Switch
    - Preset Swatches row
    - Compact Custom Color row with current color swatch, HEX text, and "Customize" button
    - Compact Palette Style row with current style badge and "Change" button
  - **Card 2: Media Viewer**:
    - Viewer Dynamic Ambient Color
    - Viewer Blur Effect
    - Zoom Minimap
    - Max Screen Brightness
  - **Card 3: Typography & Layout**:
    - Timeline Date Header Alignment (Left / Center)
    - Show Album Size
    - Use System Font
    - Font Scale Slider
- [ ] **Step 3: Add `ModalBottomSheet` containers for Custom Color and Palette Style**
  - Render `PhotonCustomColorSection` inside Custom Color bottom sheet with Drag Handle and header.
  - Render `PhotonPaletteGrid` inside Palette Style bottom sheet with Drag Handle and header.
- [ ] **Step 4: Verify build and UI layout on device**

---

### Task 3: Verification & Device Validation
**Files:**
- Test: `app/src/test/java/...`
- Device test on `10BEAK2JR50012G`

- [ ] **Step 1: Run unit tests (`.\gradlew.bat testDebugUnitTest`)**
- [ ] **Step 2: Build release APK (`.\gradlew.bat assembleRelease`)**
- [ ] **Step 3: Install onto physical device and verify navigation, bottom sheets, and toggles**


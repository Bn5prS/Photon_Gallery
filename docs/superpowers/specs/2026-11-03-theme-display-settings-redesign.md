# Theme & Display Settings Organization Design

## 1. Intent & Why
The "Theme & Display" section in `SettingsScreen.kt` currently renders 5 heavily expanded cards on a single vertical scroll. Large visual controls—such as the 3-slider HSL custom seed color picker (`PhotonCustomColorSection`) and the 9-card chromatic profile grid (`PhotonPaletteGrid`)—are permanently rendered on the main page even when dynamic system wallpaper colors or preset palettes are active. Furthermore, 7 diverse switches and sliders are lumped into a single monolithic card.

This design organizes Theme & Display into a compact, scan-friendly Material 3 interface with collapsible rows and modal bottom sheets for advanced pickers.

---

## 2. Goals & Non-Goals

### Goals
1. **Compact Theme & Color Card**:
   - Theme mode selector (System / Light / Dark) and AMOLED Black toggle.
   - Dynamic Colors (Material You) toggle and curated 4-quadrant palette swatches.
   - **Custom Theme Color**: Rendered as a compact row showing the current seed color swatch and HEX code. Tapping opens an interactive Material 3 modal bottom sheet (or smooth accordion drawer) containing the HSL sliders and color preview box.
   - **Palette Style**: Rendered as a compact clickable row showing the active style chip (e.g. "Tonal Spot"). Tapping opens a modal bottom sheet displaying the 9 MaterialKolor color science style cards.
2. **Divided & Cohesive Display Categories**:
   - **Media Viewer Card**: Groups media viewing features (Dynamic Ambient Lighting, Blurred Viewer Background, Zoom Navigator Minimap, Max Screen Brightness).
   - **Typography & Layout Card**: Groups text and interface layout features (Use System Font, Font Scale Slider, Timeline Date Header Alignment, Show Album Size).
3. **Smooth Animations**: Animated visibility and modal sheets prevent layout jarring when switching modes.

### Non-Goals
- Altering any persistent settings data keys, repositories, or view model logic.
- Removing any existing features or customization options.

---

## 3. Approaches Considered

1. **Option 1 (Chosen): In-Place Compact Rows with Modal Bottom Sheets**:
   - Keeps all settings within one predictable screen without nested navigation depth.
   - Offloads complex multi-slider and multi-grid visual pickers to modern bottom sheets.
   - Groups 7 loose switches into 2 semantically coherent cards (Media Viewer vs. Typography & Layout).
2. **Option 2: Deep Sub-Navigation Pages**:
   - Splits into "Appearance", "Media Viewer", and "Typography" child pages. Adds excessive navigation overhead and back-button fatigue for simple toggles.
3. **Option 3: Top Tab Bar**:
   - Introduces persistent top tab navigation within a sub-screen. Clutters the top app bar area and diverges from standard settings UX.

---

## 4. Detailed Component Design

### 4.1 Theme & Colors Card
- **Theme Mode**: `SingleChoiceSegmentedButtonRow` (System, Light, Dark).
- **AMOLED Black**: `SwitchSettingItem` (active only when in dark mode).
- **Dynamic Colors**: `SwitchSettingItem` (Material You).
- **Preset Palettes**: Horizontal `LazyRow` of 4-quadrant swatches.
- **Custom Color Row**:
  - Displays color dot, HEX code, and "Customize" action pill.
  - Tapping opens `ModalBottomSheet` with `PhotonCustomColorSection`.
- **Palette Style Row**:
  - Displays current `themePaletteStyle.name` formatted badge and "Change" action pill.
  - Tapping opens `ModalBottomSheet` with `PhotonPaletteGrid`.

### 4.2 Media Viewer Card
- Title: "Media Viewer" / `stringResource(R.string.setting_viewer_category)`
- Items:
  1. Dynamic Ambient Light (`viewerDynamicColor`)
  2. Blurred Background (`viewerBlurEffect`)
  3. Zoom Minimap (`viewerZoomMinimapEnabled`)
  4. Max Screen Brightness (`maxBrightnessEnabled`)

### 4.3 Typography & Layout Card
- Title: "Typography & Layout" / `stringResource(R.string.setting_typography_layout_category)`
- Items:
  1. Timeline Date Header Alignment (Left / Center)
  2. Show Album Size (`showAlbumSize`)
  3. Use System Font (`useSystemFont`)
  4. Font Scale Slider (`fontScale`)


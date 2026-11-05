# iOS Gallery UI Replication Design

## 1. Intent and Why
Replicate the clean, floating iOS Photos viewer aesthetic in Photon Gallery's full-screen viewer (`DetailScreen.kt`) and video player controls (`VideoPlayerItem.kt`). The goal is to provide a minimalist, distraction-free viewing experience where controls float elegantly as translucent islands over the media rather than being anchored in heavy full-width gradient bars.

## 2. Goals, Non-Goals, and Anti-Goals
- **Goals**:
  - Replicate the floating 3-island top bar (Back button, File metadata pill, 3-dot menu).
  - Replicate the floating 3-part bottom dock (Share on left, grouped [Favorite, Info, Edit] center pill, Trash on right).
  - Replicate the unified video scrubber capsule (Play/Pause, Scrubber slider, Mute/Unmute) floating above the bottom dock.
  - Maintain smooth show/hide behavior on single tap with auto-hide timer pausing when menus or sheets are open.
  - Keep all existing functionalities intact (Share, Favorite, Edit, Lens, Info, Trash/Delete, 3-dot options).
- **Non-Goals**:
  - Filmstrip thumbnail strip: explicitly excluded as per user instructions ("ignore film strip").
  - iOS-native iconography: only Material Symbols icons (`ic_ms_*`) will be used as per user instructions.
- **Anti-Goals**:
  - No outlines, strokes, or borders on buttons/pills (strict adherence to "No Outlines Policy").
  - No heavy full-screen or edge-to-edge dark gradients covering the photo.

## 3. Constraints
- **Iconography**: Exclusively use Material Symbols vector drawables (`ic_ms_arrow_back`, `ic_ms_more_horiz`, `ic_ms_share`, `ic_ms_favorite`, `ic_ms_info`, `ic_ms_tune`, `ic_ms_delete`, `ic_ms_play_arrow`, `ic_ms_pause`, `ic_ms_volume_up`, `ic_ms_volume_off`).
- **No Filmstrip**: Omit `ViewerFilmstripRow` from the bottom controls.
- **Styling**: Translucent dark frosted container fills (`Color.Black.copy(alpha = 0.45f)`) with zero border stroke.
- **Android Target**: minSdk 31, Jetpack Compose Material 3 Expressive.

## 4. Approaches Considered
- **Approach A: Exact iOS 18 Triple-Island Layout (Selected)**:
  - Top bar features 3 floating islands: circular Back button, center Pill with File Name + Date/Time, and circular More (`•••`) button.
  - Bottom dock features 3 floating elements: standalone circular Share button on left, center Pill holding [Favorite, Info, Edit], and standalone circular Trash button on right.
  - Video scrubber is a single floating capsule with Play/Pause, Slider, and Mute/Unmute.
  - *Why selected*: Accurately replicates the iOS reference image provided by the user, maintaining its distinct visual hierarchy and rhythm.
- **Approach B: Single Continuous Capsule Dock**:
  - Merged all 5 bottom actions into one wide continuous capsule.
  - *Why rejected*: Lacks the distinctive floating rhythm and grouping from the user's reference screenshot.

## 5. Detailed Component Specifications

### 5.1 Top Bar Floating Islands
- Positioned at `statusBarsPadding() + top = 8.dp`, floating over the photo.
- **Left Island**: 40dp circle (`CircleShape`), `Color.Black.copy(alpha = 0.45f)`, with `ic_ms_arrow_back` (22dp, White).
- **Center Island**: Pill container (`ShapeFull`), `Color.Black.copy(alpha = 0.45f)`, padding `horizontal = 16.dp, vertical = 6.dp`:
  - **Line 1 (Title)**: `currentItem.name` (13sp, SemiBold, White, single-line, `TextOverflow.Ellipsis`).
  - **Line 2 (Subtitle)**: Formatted date & time (e.g. `June 26, 2022, 7:01 PM` or `dd MMM yyyy, HH:mm`) (11sp, White 75% alpha, single-line).
  - *(If Live Photo: compact "LIVE" badge placed beside or integrated)*.
- **Right Island**: 40dp circle (`CircleShape`), `Color.Black.copy(alpha = 0.45f)`, with `ic_ms_more_horiz` (22dp, White) anchoring the 3-dot dropdown menu.

### 5.2 Bottom Dock
- Positioned at `navigationBarsPadding() + bottom = 16.dp`, horizontally centered:
- Row with `Arrangement.SpaceBetween` or centered spread spanning ~88% of screen width (max width ~360dp):
  - **Left**: 44dp circle `IconButton` (`CircleShape`, `Color.Black.copy(alpha = 0.45f)`), `ic_ms_share` (24dp, White).
  - **Center Pill**: `ShapeFull`, `Color.Black.copy(alpha = 0.45f)`, height 48dp, padding `horizontal = 8.dp`:
    - Row with `Arrangement.spacedBy(4.dp)` holding:
      1. **Favorite**: `ic_ms_favorite` / `ic_ms_favorite_filled` (24dp, animated bounce on toggle).
      2. **Info**: `ic_ms_info` (24dp, White). Opens `showInfoCard = true`.
      3. **Edit**: `ic_ms_tune` (24dp, White). Opens photo editor or video frame extractor.
  - **Right**: 44dp circle `IconButton` (`CircleShape`, `Color.Black.copy(alpha = 0.45f)`), `ic_ms_delete` (24dp, White 85% alpha).
- For items in Trash bucket:
  - Left button: Restore (`ic_ms_refresh`).
  - Right button: Permanent Delete (`ic_ms_delete`).

### 5.3 Video Controls Capsule (`VideoPlayerItem.kt`)
- Centered floating capsule positioned above the bottom dock:
  - Pill container (`ShapeFull`, `Color.Black.copy(alpha = 0.50f)`), height 44dp, width ~88% (max width 340dp).
  - Row holding:
    1. **Play/Pause**: `ic_ms_play_arrow` / `ic_ms_pause` (20dp, White).
    2. **Slider Track**: Thin, sleek blue active track (`#2979FF` or primary), dark gray inactive track, with compact thumb.
    3. **Mute/Unmute**: `ic_ms_volume_up` / `ic_ms_volume_off` (20dp, White).
- Synchronized with `onScrubbingChanged` so auto-hide pauses while user scrubs.

## 6. What's Left to the Builder
- Tune exact padding and touch targets for 48dp accessibility.
- Connect existing click handlers and state flows (`onShare`, `onFavorite`, `onEdit`, `onDelete`, `onRestore`, `onPermanentDelete`, `showInfoCard`, `showMoreMenu`).
- Ensure single-tap gestures seamlessly toggle visibility of all floating islands in unison.


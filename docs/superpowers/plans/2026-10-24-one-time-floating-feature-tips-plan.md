# One-Time Floating Feature Tips Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement non-intrusive, sleek, one-time floating feature discovery tips ("Quick Tip") across Photon Gallery to surface hidden gestures (album custom shapes & cover photo changes, people cover changes, motion photo press-and-hold, markup transformations) with spring motion and persistent DataStore dismissal.

**Architecture:** 
- `SettingsRepository` exposes reactive Flows (`isTipDismissed(tipId)`) and persistence functions (`dismissTip(tipId)`) backed by DataStore preferences.
- `FloatingTipCard` is a shared Material 3 Expressive floating banner styled in `surfaceContainerHighest` with `ShapeLargeIncreased` (20dp), attention-grabbing spring physics (`MotionScheme.fastSpatialSpec`), icon badge, "Quick Tip" title, message text, and a high-contrast "Got it" button.
- Screens integrate `FloatingTipCard` anchored above the bottom dock / toolbar, automatically fading out and storing dismissal permanently on user click or action.

**Tech Stack:** Jetpack Compose, Material 3 Expressive, Preferences DataStore, Kotlin Coroutines & Flow.

**Spec:** [docs/superpowers/specs/2026-10-24-one-time-floating-feature-tips-design.md](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-24-one-time-floating-feature-tips-design.md)

## Global Constraints
- **Strict AGENTS.md Adherence**: No raw hex/Color literals in UI composables; resolve via `MaterialTheme.colorScheme.<role>`.
- **Shapes & Radii**: Use official tokens (`ShapeLargeIncreased` 20dp, `ShapeFull`, `ShapeSmall`).
- **Icons**: Strictly Material Symbols Rounded XML drawables (`res/drawable/ic_ms_*`).
- **Typography**: Google Fonts type scale (`titleSmall` Outfit/Urbanist, `bodySmall` / `labelMedium` Plus Jakarta Sans).
- **Non-blocking / Non-modal**: The tip card must float gracefully without blocking touch events to background content unless tapped.

## Review Focus
1. Rapid dismissal: Tapping "Got it" must immediately animate out the card with zero flicker and persist to DataStore.
2. Device rotation & recomposition: Ensure the delayed entrance timer (400ms) does not restart on ordinary recompositions or configuration shifts.
3. Bottom insets & dock clearance: The floating card must calculate window navigation bars and dock padding correctly so it doesn't overlap bottom controls.
4. Concurrency: Multiple tips across different screens must use isolated keys without colliding.
5. First-run experience: Verify each tip shows exactly once and never recurs after acknowledgment.

---

### Task 1: DataStore Persistence in `SettingsRepository` & Unit Tests

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt`
- Create: `app/src/test/java/com/inferno/gallery/data/SettingsRepositoryTipTest.kt`

- [ ] **Step 1: Write unit test for tip dismissal in `SettingsRepositoryTipTest.kt`**
  - Verify `isTipDismissed` returns `false` by default for a new tip key.
  - Verify calling `dismissTip` flips the key state to `true`.
- [ ] **Step 2: Run test to confirm it fails/does not compile**
  - Command: `.\gradlew.bat testDebugUnitTest --tests "com.inferno.gallery.data.SettingsRepositoryTipTest"`
- [ ] **Step 3: Implement `isTipDismissed` and `dismissTip` in `SettingsRepository.kt`**
  - Add standard tip key constants:
    - `TIP_ALBUMS_CUSTOMIZE = "tip_albums_customize"`
    - `TIP_PEOPLE_COVER = "tip_people_cover"`
    - `TIP_MOTION_PHOTO = "tip_motion_photo"`
    - `TIP_MARKUP_GESTURES = "tip_markup_gestures"`
  - Provide `fun isTipDismissed(tipKey: String): Flow<Boolean>`
  - Provide `suspend fun dismissTip(tipKey: String)`
- [ ] **Step 4: Run unit tests and verify they pass**
  - Command: `.\gradlew.bat testDebugUnitTest --tests "com.inferno.gallery.data.SettingsRepositoryTipTest"`
- [ ] **Step 5: Commit changes**
  - `git add app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt app/src/test/java/com/inferno/gallery/data/SettingsRepositoryTipTest.kt ; git commit -m "feat(tips): add tip dismissal persistence to SettingsRepository"`

---

### Task 2: Expressive `FloatingTipCard` Component

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/components/FloatingTipCard.kt`

- [ ] **Step 1: Build `FloatingTipCard` composable**
  - Container: Elevated `Surface` or `Card` with `surfaceContainerHighest` and `ShapeLargeIncreased` (20dp).
  - Attention-grabbing entrance: `LaunchedEffect` with 400ms delay, animating `scale` and `alpha` via `MaterialTheme.motionScheme.fastSpatialSpec()`.
  - Icon: Material Symbol rounded icon in a `primaryContainer` pill container.
  - Header: Text `"Quick Tip"` in `MaterialTheme.typography.titleSmall` with bold weight.
  - Message: Informational tip text in `MaterialTheme.typography.bodySmall`.
  - Actions: High-contrast `FilledTonalButton` with `"Got it"` label + close `IconButton` (`ic_ms_close`).
  - Haptic feedback tick on press.
- [ ] **Step 2: Verify compilation with Gradle**
  - Command: `.\gradlew.bat compileDebugKotlin`
- [ ] **Step 3: Commit component**
  - `git add app/src/main/java/com/inferno/gallery/ui/components/FloatingTipCard.kt ; git commit -m "feat(ui): create expressive FloatingTipCard component"`

---

### Task 3: Integrate Floating Tips in Albums and People Screens

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/AlbumsScreen.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/people/PeopleScreen.kt`

- [ ] **Step 1: Add tip to `AlbumsScreen.kt`**
  - Collect `settingsRepo.isTipDismissed(SettingsRepository.TIP_ALBUMS_CUSTOMIZE)`.
  - Place `FloatingTipCard` inside screen `Box` aligned at `Alignment.BottomCenter` with bottom dock clearance.
  - Text: *"Press & hold any album card to customize its cover photo or choose custom shapes like Cookie, Clover & Pill."*
  - Icon: `ic_ms_auto_fix_high`.
- [ ] **Step 2: Add tip to `PeopleScreen.kt`**
  - Collect `settingsRepo.isTipDismissed(SettingsRepository.TIP_PEOPLE_COVER)`.
  - Place `FloatingTipCard` aligned at `Alignment.BottomCenter` with bottom padding.
  - Text: *"Press & hold any person to choose their best cover shot, rename them, or merge clusters."*
  - Icon: `ic_ms_person` or `ic_ms_touch_app`.
- [ ] **Step 3: Verify compilation**
  - Command: `.\gradlew.bat compileDebugKotlin`
- [ ] **Step 4: Commit changes**
  - `git add app/src/main/java/com/inferno/gallery/ui/AlbumsScreen.kt app/src/main/java/com/inferno/gallery/ui/people/PeopleScreen.kt ; git commit -m "feat(tips): integrate floating tips in Albums and People screens"`

---

### Task 4: Integrate Floating Tips in Motion Photo Viewer and Markup Editor

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/editor/PhotoEditorScreen.kt`

- [ ] **Step 1: Add tip to `DetailScreen.kt` for Motion Photos**
  - When `isMotionPhoto` is true, check `isTipDismissed(SettingsRepository.TIP_MOTION_PHOTO)`.
  - Float tip card above bottom floating toolbar.
  - Text: *"Press and hold anywhere on the photo to play live motion video, or tap the Motion pill for Top Shot frames."*
  - Icon: `ic_ms_motion_photos_on`.
- [ ] **Step 2: Add tip to `PhotoEditorScreen.kt` when Markup mode is active**
  - When editor switches to Markup mode, check `isTipDismissed(SettingsRepository.TIP_MARKUP_GESTURES)`.
  - Float tip card above bottom markup dock.
  - Text: *"Tap placed text or arrows to drag, pinch-to-scale, and rotate freely with two fingers."*
  - Icon: `ic_ms_touch_app`.
- [ ] **Step 3: Verify compilation & run tests**
  - Command: `.\gradlew.bat testDebugUnitTest`
- [ ] **Step 4: Commit changes**
  - `git add app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt app/src/main/java/com/inferno/gallery/ui/editor/PhotoEditorScreen.kt ; git commit -m "feat(tips): integrate floating tips for Motion Photos and Markup gestures"`

---

### Task 5: Build Release APK & Device Verification

- [ ] **Step 1: Build release APK**
  - Command: `.\gradlew.bat assembleRelease`
- [ ] **Step 2: Install on connected device `10BEAK2JR50012G`**
  - Command: `adb -s 10BEAK2JR50012G install -r "app\build\outputs\apk\release\PhotonGallery-arm64-v8a.apk"`
- [ ] **Step 3: Verify on device**
  - Launch Photon Gallery.
  - Navigate to Albums -> verify "Quick Tip" bounces in smoothly after 400ms.
  - Tap "Got it" -> verify smooth dismissal and that it never reappears on reload.
  - Open People tab -> verify People tip card.
  - Open Motion Photo / Markup -> verify contextual tips.

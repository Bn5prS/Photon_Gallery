# Fullscreen Viewer Distraction-Free Colors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate visual distraction and chromatic clash in the fullscreen media viewer by implementing an OLED pitch-black canvas baseline, subdued ambient blur under an 82% dark scrim, and a seamless transparent gradient bottom dock with neutral frosted-glass controls.

**Architecture:**
- Update `DetailScreen.kt` canvas background from `surfaceContainerLow` to pure `Color.Black` (`#000000`).
- Refine ambient mood extraction and blur backdrop to reduce saturation and deepen dark scrims so the photo is the sole visual hero.
- Replace the solid opaque bottom `Surface` (`surfaceContainerHighest`) with an edge-to-edge vertical gradient scrim.
- Unify bottom action buttons to frosted glass pills with clean `Color.White` icons, matching the top bar.
- Maintain Universal Immersive Dark behavior across both Light and Dark app themes during media viewing.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android WindowInsetsControllerCompat.

**Spec:** `docs/superpowers/specs/2026-11-03-viewer-distraction-free-colors-design.md`

## Global Constraints
- Target device: `10BEAK2JR50012G` (connected, arm64-v8a).
- Universal Immersive Dark: Media viewer remains pitch-black with white chrome regardless of app Light/Dark mode.
- Gestures & Motion: Preserve swipe-to-dismiss fading (`bgAlpha`), pinch-to-zoom, filmstrip synchronization, and spring tokens.
- Offline-first: No external network or analytics dependencies.

## Review Focus
- High-saturation photos in letterbox: Ambient glow must stay whisper-quiet and never bleed bright distracting hues.
- Light Mode app theme: Entering full-screen viewer must keep status/nav bars dark with white icons; exiting must smoothly restore Light Mode.
- Filmstrip scrolling: Thumbnails must render legibly over the bottom gradient scrim with a crisp white active indicator.
- Contrast & Legibility: All action buttons must remain visible and legible over both pure black and bright white photos.

---

### Task 1: Subdued Ambient Glow & OLED Black Baseline Canvas

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt:820-895, 2350-2403`

- [ ] **Step 1: Set baseline canvas color to pure pitch black**
  Update `baseCanvasColor` in `DetailScreen.kt`:
  ```kotlin
  val baseCanvasColor = Color.Black
  ```

- [ ] **Step 2: Subdue ambient mood extraction saturation**
  In `extractAmbientMoodColor` in `DetailScreen.kt`, adjust saturation and value clamping:
  ```kotlin
  hsv[1] = (hsv[1] * 0.40f).coerceIn(0.10f, 0.30f)
  hsv[2] = 0.08f
  ```

- [ ] **Step 3: Tune ambient radial gradient and blur overlay scrim**
  In `DetailScreen.kt`:
  - When `viewerDynamicColor` is active, adjust radial gradient:
    ```kotlin
    colors = listOf(
        animatedBgColor.copy(alpha = 0.35f * bgAlpha),
        animatedBgColor.copy(alpha = 0.12f * bgAlpha),
        Color.Black.copy(alpha = 0.98f * bgAlpha)
    )
    ```
  - When `viewerBlurEffect` is active, adjust blur radius and dark scrim:
    ```kotlin
    .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    ...
    Color.Black.copy(alpha = 0.82f * bgAlpha)
    ```

- [ ] **Step 4: Verify Kotlin compilation**
  Run `.\gradlew.bat compileDebugKotlin` to ensure all changes compile cleanly.

- [ ] **Step 5: Run unit tests**
  Run `.\gradlew.bat testDebugUnitTest` to ensure all existing tests pass.

- [ ] **Step 6: Commit Task 1**
  Commit with message: `feat(viewer): implement OLED black baseline canvas and subdued ambient glow`

---

### Task 2: Seamless Bottom Gradient Scrim & Frosted Glass Action Dock

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt:2015-2340`

- [ ] **Step 1: Convert docked bottom bar container from solid Surface to transparent Box with gradient scrim**
  Replace `Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, ...)` with:
  ```kotlin
  Box(
      modifier = Modifier
          .fillMaxWidth()
          .background(
              Brush.verticalGradient(
                  colors = listOf(
                      Color.Transparent,
                      Color.Black.copy(alpha = 0.40f),
                      Color.Black.copy(alpha = 0.80f)
                  )
              )
          )
  )
  ```

- [ ] **Step 2: Style floating dock (when filmstrip disabled) with frosted dark glass**
  Replace `color = MaterialTheme.colorScheme.surfaceContainerHighest` with `Color.Black.copy(alpha = 0.45f)` and add a subtle border `BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))`.

- [ ] **Step 3: Style active filmstrip thumbnail border**
  In `ViewerFilmstripRow`, update the selected thumbnail border from `MaterialTheme.colorScheme.primary` to `BorderStroke(2.dp, Color.White)`.

- [ ] **Step 4: Unify action buttons palette to neutral white**
  In `ViewerActionButtonsRow`:
  - Set all button icon tints to `Color.White` (Share, Edit, Google Lens, Restore).
  - Set Delete tint to `Color.White.copy(alpha = 0.85f)` to avoid aggressive red flash against dark photos.
  - Set inactive Favorite tint to `Color.White`; keep active Favorite with `favoriteHarmonized`.

- [ ] **Step 5: Verify Kotlin compilation and unit tests**
  Run `.\gradlew.bat compileDebugKotlin` and `.\gradlew.bat testDebugUnitTest`.

- [ ] **Step 6: Commit Task 2**
  Commit with message: `feat(viewer): add transparent bottom gradient scrim and frosted neutral action dock`

---

### Task 3: Build, Deploy & Verify on Physical Device

**Files:**
- Build: `app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk`

- [ ] **Step 1: Assemble Release APK**
  Run `.\gradlew.bat assembleRelease`.

- [ ] **Step 2: Install to connected device**
  Run `adb -s 10BEAK2JR50012G install -r app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk`.

- [ ] **Step 3: Launch app and verify fullscreen viewer**
  Run `adb -s 10BEAK2JR50012G shell am start -n com.inferno.gallery/.MainActivity`.
  Inspect on device:
  - Open photo: Verify true OLED black canvas behind photo.
  - Verify bottom dock has transparent gradient scrim with no solid opaque card.
  - Verify action buttons and filmstrip active border are crisp white.
  - Verify exit back to timeline restores system theme without flicker.


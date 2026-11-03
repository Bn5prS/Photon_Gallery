# Predictive Back Gesture & Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement fluid, real-time Predictive Back gesture support across the fullscreen media viewer and all navigation destinations in Photon Gallery.

**Architecture:**
- Integrate `androidx.activity.compose.PredictiveBackHandler` in `DetailScreen.kt` to bind edge-swipe progress directly to card scaling (`1.0f -> 0.88f`), corner rounding (`0.dp -> 28.dp`), edge-aware drift, and scrim fading.
- Handle filmstrip overview collapse interactively via `PredictiveBackHandler`.
- Configure `NavigationGraph.kt` `NavHost` and composable destination transitions to support system predictive back curves (scale + slide) across Settings, Albums, Trash, Folders, and Editor.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive motion tokens, `androidx.activity.compose.PredictiveBackHandler`, `androidx.navigation.compose`.

**Spec:** `docs/superpowers/specs/2026-11-03-predictive-back-gesture-design.md`

## Global Constraints
- Target device: `10BEAK2JR50012G` (connected, arm64-v8a).
- Must preserve existing vertical swipe-to-dismiss gesture (`dismissOffsetY`) and horizontal pager swipe gestures.
- Cancellation safety: Canceling an edge swipe mid-gesture must spring back smoothly with `MotionTokens.snappySpring()` without triggering accidental dismissal.
- Android 14/15 native gesture compatibility (`BackEventCompat`).

## Review Focus
- Edge conflicts: Swiping near the screen edge to navigate back must not conflict with photo zoom panning or pager paging.
- Scrim transparency: Peeking back to the gallery grid must fade the black scrim without exposing white canvas artifacts.
- Filmstrip overview mode: Predictive back must collapse the filmstrip overview before popping the detail viewer.
- Navigation Compose: Popping from sub-screens (e.g. Settings or Albums) must smoothly scale and slide without jarring snap cuts.

---

### Task 1: Interactive Predictive Back in Fullscreen Viewer

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt:500-535, 820-915`

- [ ] **Step 1: Import PredictiveBackHandler and BackEventCompat**
  Add imports in `DetailScreen.kt`:
  ```kotlin
  import androidx.activity.BackEventCompat
  import androidx.activity.compose.PredictiveBackHandler
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.ui.draw.clip
  import kotlinx.coroutines.CancellationException
  ```

- [ ] **Step 2: Add predictive back gesture state and handler**
  In `DetailScreen.kt`, add:
  ```kotlin
  var predictiveProgress by remember { mutableFloatStateOf(0f) }
  var predictiveEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
  var isPredictiveBackActive by remember { mutableStateOf(false) }

  val animatedPredictiveProgress by animateFloatAsState(
      targetValue = predictiveProgress,
      animationSpec = MotionTokens.snappySpring(),
      label = "predictiveProgress"
  )

  PredictiveBackHandler(enabled = !isFilmstripOverview) { progressFlow ->
      try {
          isPredictiveBackActive = true
          progressFlow.collect { backEvent ->
              predictiveProgress = backEvent.progress
              predictiveEdge = backEvent.swipeEdge
          }
          safeOnBack()
      } catch (e: CancellationException) {
          isPredictiveBackActive = false
          predictiveProgress = 0f
      } finally {
          isPredictiveBackActive = false
          predictiveProgress = 0f
      }
  }

  PredictiveBackHandler(enabled = isFilmstripOverview) { progressFlow ->
      try {
          progressFlow.collect { }
          isFilmstripOverview = false
      } catch (e: CancellationException) { }
  }
  ```

- [ ] **Step 3: Integrate predictive progress into root viewer transform & clipping**
  Update root container graphicsLayer in `DetailScreen.kt`:
  - Calculate `combinedScale = dismissScale * (1f - animatedPredictiveProgress * 0.12f)`.
  - Calculate `predictiveTranslationX = if (predictiveEdge == BackEventCompat.EDGE_LEFT) animatedPredictiveProgress * with(LocalDensity.current) { 36.dp.toPx() } else -animatedPredictiveProgress * with(LocalDensity.current) { 36.dp.toPx() }`.
  - Calculate `bgAlpha = (1f - dismissProgress) * (1f - animatedPredictiveProgress * 0.45f)`.
  - Apply `clip(RoundedCornerShape((animatedPredictiveProgress * 28).dp))` on the content layer.

- [ ] **Step 4: Verify Kotlin compilation**
  Run `.\gradlew.bat compileDebugKotlin`.

- [ ] **Step 5: Run unit tests**
  Run `.\gradlew.bat testDebugUnitTest`.

- [ ] **Step 6: Commit Task 1**
  Commit with message: `feat(viewer): implement interactive predictive back gesture in detail viewer`

---

### Task 2: Predictive Back Curves Across Navigation Graph

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/NavigationGraph.kt:80-170, 215-230`

- [ ] **Step 1: Refine NavHost pop transitions for predictive back**
  In `NavigationGraph.kt`, update `popExitTransition` and `popEnterTransition` to ensure smooth scaling and sliding compatible with Compose 2.9.8 predictive back transitions:
  - `popExitTransition`: `slideOutHorizontally(targetOffsetX = { (it * 0.30f).toInt() }) + scaleOut(targetScale = 0.90f) + fadeOut()`
  - `popEnterTransition`: `slideInHorizontally(initialOffsetX = { -(it * 0.15f).toInt() }) + scaleIn(initialScale = 0.94f) + fadeIn()`

- [ ] **Step 2: Update detail composable pop exit transition**
  Ensure the `detail` composable entry in `NavigationGraph.kt` supports fluid scaling during pop:
  - `popExitTransition = { scaleOut(targetScale = 0.88f) + fadeOut() }`

- [ ] **Step 3: Verify Kotlin compilation and unit tests**
  Run `.\gradlew.bat compileDebugKotlin` and `.\gradlew.bat testDebugUnitTest`.

- [ ] **Step 4: Commit Task 2**
  Commit with message: `feat(navigation): configure predictive back motion transitions across navigation destinations`

---

### Task 3: Build, Deploy & Verify on Connected Device

**Files:**
- Build: `app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk`

- [ ] **Step 1: Assemble Release APK**
  Run `.\gradlew.bat assembleRelease`.

- [ ] **Step 2: Install and launch on device**
  Run `adb -s 10BEAK2JR50012G install -r app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk` and launch `MainActivity`.

- [ ] **Step 3: Verify predictive back gesture on device**
  - In photo viewer: Swipe from left edge and hold -> verify photo scales down, corners round, and grid peeks through.
  - Release mid-swipe -> verify it smoothly pops back.
  - Push back to edge and release -> verify it springs back into full screen cleanly.
  - Test back gesture in Settings and Albums -> verify smooth interactive cross-screen preview.

# Predictive Back Gesture & Animations Design

## 1. Intent & Why
In Android 14 and 15, the Predictive Back gesture is an essential modern navigation paradigm. Rather than a blind, abrupt screen pop, swiping from either display edge provides a real-time, interactive preview of the destination underneath. If the user decides not to navigate away mid-swipe, they can push the gesture back to the edge to seamlessly cancel without losing their place.

This design implements rich, physical Predictive Back across Photon Gallery:
1. **Fullscreen Photo Viewer (`DetailScreen.kt`)**: Swiping from either edge interactively scales down the photo, morphs rounded corners, and fades the black canvas scrim to preview the gallery grid underneath.
2. **Navigation Compose Destinations (`NavigationGraph.kt`)**: Standard sub-screens (Settings, Albums, Trash, Folders, Editor) use coordinated Material 3 Expressive pop slide, scale, and fade curves tied to the system back gesture.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
1. **Interactive Viewer Dismiss**:
   - Edge back gesture provides a fluid, real-time preview of the underlying gallery grid.
   - Smoothly interpolates card scale (`1.0f -> 0.88f`), corner radius (`0.dp -> 28.dp`), and horizontal drift tracking the active edge.
   - Canceling the swipe smoothly springs the viewer back to fullscreen with zero visual stutter.
2. **Coordinated Navigation Transitions**:
   - `NavHost` destination pop transitions configured with Material 3 Expressive motion tokens (`scaleOut = 0.92f`, `slideOutHorizontally`, and `fadeIn`/`fadeOut`).
3. **Cancellation Resilience**:
   - If user cancels mid-gesture (`CancellationException`), all animated values spring back smoothly without triggering premature navigation.

### Non-Goals
- Altering existing vertical swipe-to-dismiss gesture (`dismissOffsetY`) or filmstrip scrolling.
- Changing system back behavior when viewing dialogs or bottom sheets.

### Anti-Goals
- Abrupt pops or white flashes during edge swipe previews.
- Jitter or conflict between edge swipe and horizontal pager photo swipe.

---

## 3. Approaches Considered

1. **Option 1 (Chosen): Dedicated `PredictiveBackHandler` in `DetailScreen` + M3 NavHost Curves**:
   - Uses `androidx.activity.compose.PredictiveBackHandler` in `DetailScreen.kt` to bind the interactive gesture progress directly to the viewer's `graphicsLayer` (scale, translation, clip, alpha).
   - Uses Navigation Compose 2.9.8 built-in predictive back coordination for child screens in `NavigationGraph.kt`.
   - *Why chosen*: Delivers an ultra-responsive, native feel identical to Google Photos and modern Pixel UI.

2. **Option 2: Relying Solely on Navigation Compose Default Transitions**:
   - Only changes `popExitTransition` in `NavHost`.
   - *Trade-off*: Does not give the photo viewer custom corner-rounding, edge-aware drift, or scrim fade matching the in-screen state.

---

## 4. Detailed Component Design

### 4.1 Fullscreen Photo Viewer (`DetailScreen.kt`)
- Use `androidx.activity.compose.PredictiveBackHandler(enabled = !isFilmstripOverview)`.
- State variables:
  - `var predictiveProgress by remember { mutableFloatStateOf(0f) }`
  - `var predictiveEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }`
  - `var isPredictiveBackActive by remember { mutableStateOf(false) }`
- Gesture Progress Collection:
  ```kotlin
  PredictiveBackHandler(enabled = !isFilmstripOverview) { progressFlow ->
      try {
          isPredictiveBackActive = true
          progressFlow.collect { event ->
              predictiveProgress = event.progress
              predictiveEdge = event.swipeEdge
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
  ```
- Visual Layer Integration:
  - Spring-animated progress `animatedPredictiveProgress by animateFloatAsState(targetValue = predictiveProgress, animationSpec = MotionTokens.snappySpring())`.
  - Scrim fading: `bgAlpha = (1f - dismissProgress) * (1f - animatedPredictiveProgress * 0.40f)`.
  - Scale: `scale = (1f - dismissProgress * 0.15f) * (1f - animatedPredictiveProgress * 0.12f)`.
  - Corner radius: `clip(RoundedCornerShape((animatedPredictiveProgress * 28).dp))`.
  - Horizontal drift: `translationX = if (predictiveEdge == BackEventCompat.EDGE_LEFT) animatedPredictiveProgress * 32.dp.toPx() else -animatedPredictiveProgress * 32.dp.toPx()`.

### 4.2 Filmstrip Overview Interception
- When `isFilmstripOverview` is true:
  ```kotlin
  PredictiveBackHandler(enabled = isFilmstripOverview) { progressFlow ->
      try {
          progressFlow.collect { /* tracks collapse progress */ }
          isFilmstripOverview = false
      } catch (e: CancellationException) {
          // keep expanded
      }
  }
  ```

### 4.3 Navigation Graph Pop Curves (`NavigationGraph.kt`)
- Update `popExitTransition` and `popEnterTransition` on all destinations to ensure full cross-page predictive back compatibility with scale and slide.

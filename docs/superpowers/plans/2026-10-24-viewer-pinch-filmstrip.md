# iQOO-Style Pinch-to-Filmstrip Gesture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the iQOO/OriginOS-style pinch-to-filmstrip overview gesture in `DetailScreen`, allowing users to pinch in on any full-screen photo/video to shrink it to ~55% locked in the center, hide all chrome, and browse horizontally across peeked neighboring photos like a continuous filmstrip, tapping any image to restore full screen.

**Architecture:** 
- In `DetailScreen.kt`, introduce `isFilmstripOverview` boolean state and `overviewProgress` (0f..1f float animated via `MotionTokens.gentleSpring()`).
- Enhance the image pointer input gesture detection: when `scale <= 1.05f` and a two-finger pinch-in is detected (`accumulatedZoom < 0.85f` or sub-1.0 pinch), smoothly activate `isFilmstripOverview = true` and hide UI chrome (`showUi = false`). In overview mode, pinching out (`accumulatedZoom > 1.15f`) restores full screen.
- Configure `HorizontalPager` with dynamic `contentPadding` (`PaddingValues(horizontal = (screenWidth * 0.20f) * overviewProgress)`) and `pageSpacing` (`16.dp * overviewProgress`).
- In overview mode, apply rounded card corners (`20.dp`), subtle depth shadow, and neighbor scale/alpha interpolation.
- Wire tap detection in overview mode: tapping the centered image animates back to 1.0x full screen; tapping a neighbor image animates the pager to that item and expands to full screen with chrome restored.
- Add `BackHandler(enabled = isFilmstripOverview)` so system back exits overview mode back to full screen.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, Compose Foundation Pager, Coil 3.

**Spec:** Direct user requirement matching OriginOS / iQOO Albums pinch-to-filmstrip interaction.

## Global Constraints
- **NO COMMITS WITHOUT PERMISSION**: Never execute `git commit` without explicit user permission.
- **Branch**: All changes stay strictly on `dev`.
- **Material 3 Expressive Compliance**: Strictly follow `AGENTS.md` (no raw hex, use `MotionTokens.gentleSpring()`, `ShapeLargeIncreased` / 20.dp, standard surface tiers).

## Review Focus
1. Normal zoom behavior: zooming in (> 1.0f) and panning must remain 100% unaffected. Pinching out to zoom in must never trigger overview mode.
2. Pinch-in threshold: must feel natural and not trigger accidentally on casual swipes or slight finger adjustments.
3. Tap-to-restore: tapping any photo in filmstrip mode must reliably restore full screen and UI controls.
4. Edge cases: single-photo albums must gracefully scale the single photo in the center without crashing or layout issues.
5. Back navigation: hardware/gesture back while in overview mode must exit back to full screen instead of exiting the entire viewer.

---

### Task 1: Overview State, BackHandler & Gesture Detection

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

**Interfaces:**
- State:
  - `var isFilmstripOverview by remember { mutableStateOf(false) }`
  - `val overviewProgress by animateFloatAsState(targetValue = if (isFilmstripOverview) 1f else 0f, animationSpec = MotionTokens.gentleSpring())`
- Gestures:
  - Pinch-in detection at scale 1.0f triggers `isFilmstripOverview = true`, `showUi = false`.
  - Pinch-out in overview mode triggers `isFilmstripOverview = false`, `showUi = true`.
- Navigation:
  - `BackHandler(enabled = isFilmstripOverview) { isFilmstripOverview = false; showUi = true }`

- [ ] **Step 1: Add overview state variables and BackHandler**
- [ ] **Step 2: Update pointer input gesture detection to handle sub-1.0 pinch-in and pinch-out**
- [ ] **Step 3: Verify compilation with `./gradlew.bat compileDebugKotlin`**

---

### Task 2: Pager Dynamic Padding, Page Transformations & Tap-to-Restore

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

**Interfaces:**
- Layout:
  - `HorizontalPager` horizontal `contentPadding` driven by `overviewProgress` (peeking adjacent cards into viewport).
  - `HorizontalPager` `pageSpacing` driven by `overviewProgress`.
  - `HorizontalPager` `userScrollEnabled` enabled when `isFilmstripOverview` is active.
- Visuals:
  - Card shape clip (`(20.dp * overviewProgress)`) and shadow elevation in overview mode.
  - Neighbor card scaling (`lerp(0.85f, 1.0f, ...)`) and alpha attenuation (`lerp(0.70f, 1.0f, ...)`).
- Interactivity:
  - In overview mode, tap on centered item restores full screen (`isFilmstripOverview = false`, `showUi = true`).
  - In overview mode, tap on neighbor item scrolls to that page and restores full screen.
  - Video items render static preview card in overview mode without player controls.

- [ ] **Step 1: Configure `HorizontalPager` contentPadding and pageSpacing**
- [ ] **Step 2: Implement page card transform (shape, scale, alpha, elevation)**
- [ ] **Step 3: Implement tap gestures for centering & expanding cards**
- [ ] **Step 4: Verify compilation with `./gradlew.bat compileDebugKotlin`**

---

### Task 3: Unit Testing, APK Build & Device Verification

**Files:**
- Create: `app/src/test/java/com/inferno/gallery/ui/FilmstripGestureMathTest.kt`

- [ ] **Step 1: Write unit test validating layout calculations (padding, scale interpolation, offset clamping)**
- [ ] **Step 2: Run unit tests with `./gradlew.bat testDebugUnitTest`**
- [ ] **Step 3: Build release APK with `./gradlew.bat assembleRelease`**
- [ ] **Step 4: Install on connected device `10BEAK2JR50012G` via `adb install -r`**
- [ ] **Step 5: Verify gesture behavior on device**

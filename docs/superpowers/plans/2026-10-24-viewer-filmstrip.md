# Viewer Optional Filmstrip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an optional edge-to-edge thumbnail filmstrip for the image viewer (`DetailScreen`) with a toggle switch in Settings (`SettingsScreen`), preserving the classic floating pill dock when disabled.

**Architecture:** 
- Add `viewer_filmstrip_enabled` preference to `SettingsRepository` and `SettingsViewModel` (default `false`).
- Expose an M3 Expressive toggle switch in `SettingsScreen` under "Display Preferences" with descriptive copy that notes the bottom pill design changes to a docked bar when active.
- In `DetailScreen`, conditionally render either the preserved floating pill dock (when disabled) or a unified docked full-width bottom bar (when enabled) featuring an upper edge-to-edge `LazyRow` filmstrip with bidirectional sync to `pagerState` and a lower viewer action tier.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 Expressive, DataStore Preferences, Coil 3.

**Spec:** `docs/superpowers/specs/2026-10-24-viewer-filmstrip-design.md`

## Global Constraints
- **NO GIT COMMITS**: Do not execute `git commit` at any step per explicit user rule.
- All code resides strictly on the `dev` branch.
- Material 3 Expressive tokens only: no raw `Color(0x...)` or `#hex` in UI composables; use `MaterialTheme.colorScheme.*`, `ShapeSmall` (8dp), `ShapeFull`, and `MotionTokens.gentleSpring()`.

## Review Focus
1. Rapid swiping across hundreds of photos: filmstrip `LazyRow` must scroll smoothly without lag or memory leaks using thumbnail pre-caching.
2. Single-image albums or single-photo views: filmstrip must gracefully handle `galleryItems.size <= 1` without crashing.
3. Trash folder viewer: action buttons in the lower tier of the docked bar must show "Restore" and "Permanently Delete" instead of standard gallery actions.
4. UI visibility toggle: tapping the photo toggles `showUi` and cleanly slides/fades the docked bottom bar in/out without clipping.
5. Backward compatibility: toggling filmstrip OFF immediately restores the floating pill dock with 0 regressions.

---

### Task 1: DataStore & SettingsRepository Integration

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsViewModel.kt`
- Test: `app/src/test/java/com/inferno/gallery/data/SettingsRepositoryFilmstripTest.kt`

**Interfaces:**
- Produces:
  - `SettingsRepository.VIEWER_FILMSTRIP_ENABLED: Preferences.Key<Boolean>`
  - `SettingsRepository.viewerFilmstripEnabledFlow: Flow<Boolean>`
  - `suspend fun SettingsRepository.updateViewerFilmstripEnabled(enabled: Boolean)`
  - `SettingsViewModel.viewerFilmstripEnabled: StateFlow<Boolean>`
  - `fun SettingsViewModel.setViewerFilmstripEnabled(enabled: Boolean)`

- [ ] **Step 1: Write failing unit test for filmstrip preference**

Create `app/src/test/java/com/inferno/gallery/data/SettingsRepositoryFilmstripTest.kt`:
```kotlin
package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsRepositoryFilmstripTest {
    @Test
    fun testViewerFilmstripKeyExists() {
        assertNotNull(SettingsRepository.VIEWER_FILMSTRIP_ENABLED)
        assertEquals("viewer_filmstrip_enabled", SettingsRepository.VIEWER_FILMSTRIP_ENABLED.name)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.inferno.gallery.data.SettingsRepositoryFilmstripTest`
Expected: FAIL (unresolved reference `VIEWER_FILMSTRIP_ENABLED`)

- [ ] **Step 3: Implement preference in `SettingsRepository.kt` and `SettingsViewModel.kt`**

In `SettingsRepository.kt`:
- Add `val VIEWER_FILMSTRIP_ENABLED = booleanPreferencesKey("viewer_filmstrip_enabled")`
- Add:
  ```kotlin
  val viewerFilmstripEnabledFlow: Flow<Boolean> = context.dataStore.data
      .map { preferences ->
          preferences[VIEWER_FILMSTRIP_ENABLED] ?: false
      }

  suspend fun updateViewerFilmstripEnabled(enabled: Boolean) {
      context.dataStore.edit { preferences ->
          preferences[VIEWER_FILMSTRIP_ENABLED] = enabled
      }
  }
  ```

In `SettingsViewModel.kt`:
- Add:
  ```kotlin
  val viewerFilmstripEnabled: StateFlow<Boolean> = repository.viewerFilmstripEnabledFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
  )

  fun setViewerFilmstripEnabled(enabled: Boolean) {
      viewModelScope.launch {
          repository.updateViewerFilmstripEnabled(enabled)
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests com.inferno.gallery.data.SettingsRepositoryFilmstripTest`
Expected: PASS

---

### Task 2: Settings UI Switch

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt:170-175, 545-560`

**Interfaces:**
- Consumes: `SettingsViewModel.viewerFilmstripEnabled`, `SettingsViewModel.setViewerFilmstripEnabled`

- [ ] **Step 1: Collect `viewerFilmstripEnabled` in `SettingsScreen.kt`**

In `SettingsScreen.kt`:
```kotlin
val viewerFilmstripEnabled by viewModel.viewerFilmstripEnabled.collectAsState()
```

- [ ] **Step 2: Add `SwitchSettingItem` under "Display Preferences"**

Add item in `SettingsGroupCard(title = "Display Preferences", ...)`:
```kotlin
SwitchSettingItem(
    title = "Thumbnail Filmstrip",
    subtitle = "Show an edge-to-edge thumbnail strip (replaces floating pill with docked bottom bar)",
    icon = ImageVector.vectorResource(R.drawable.ic_ms_view_column),
    checked = viewerFilmstripEnabled,
    onCheckedChange = { viewModel.setViewerFilmstripEnabled(it) }
)
```

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: DetailScreen Filmstrip & Docked Bottom Bar

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

**Interfaces:**
- Consumes: `settingsRepo.viewerFilmstripEnabledFlow`, `pagerState`, `galleryItems`

- [ ] **Step 1: Observe `viewerFilmstripEnabled` in `DetailScreen.kt`**

```kotlin
val viewerFilmstripEnabled by settingsRepo.viewerFilmstripEnabledFlow.collectAsState(initial = false)
```

- [ ] **Step 2: Create `ViewerFilmstripRow` composable**

Build `ViewerFilmstripRow` with:
- `LazyRow` using `rememberLazyListState()`.
- `LaunchedEffect(pagerState.currentPage)` to auto-scroll with `animateScrollToItem(pagerState.currentPage)`.
- Thumbnails: 38dp width, 50dp height, `ShapeSmall` (8dp) rounded corners, Coil `AsyncImage`.
- Active thumbnail styling: 2dp `MaterialTheme.colorScheme.primary` border, alpha 1.0f.
- Inactive thumbnails: alpha 0.65f.
- Click action: `coroutineScope.launch { pagerState.animateScrollToPage(index) }`.

- [ ] **Step 3: Integrate unified docked bottom bar when `viewerFilmstripEnabled == true`**

- If `!viewerFilmstripEnabled`: render the existing floating action pill dock (100% preserved).
- If `viewerFilmstripEnabled`: render full-width docked container:
  ```kotlin
  Surface(
      shape = ShapeNone,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      tonalElevation = 3.dp,
      shadowElevation = 8.dp,
      modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
  ) {
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(top = 8.dp, bottom = 6.dp)
      ) {
          if (galleryItems.size > 1) {
              ViewerFilmstripRow(
                  galleryItems = galleryItems,
                  currentPage = pagerState.currentPage,
                  onThumbnailClick = { index ->
                      coroutineScope.launch { pagerState.animateScrollToPage(index) }
                  }
              )
              Spacer(modifier = Modifier.height(6.dp))
          }
          // Actions row (Share, Edit, Lens, Favorite, Delete or Trash)
          ViewerActionsRow(...)
      }
  }
  ```
- Adjust `FloatingTipCard` bottom padding: `if (viewerFilmstripEnabled) 146.dp else 84.dp` to prevent overlap.

- [ ] **Step 4: Verify compilation and tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: ALL PASS

---

### Task 4: Release Build & Device Verification

**Files:**
- N/A (Build and install)

- [ ] **Step 1: Build release APK**

Run: `.\gradlew.bat assembleRelease`
Expected: `app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk` created.

- [ ] **Step 2: Install APK on device**

Run: `adb -s 10BEAK2JR50012G install -r app/build/outputs/apk/release/PhotonGallery-arm64-v8a.apk`
Expected: Success

- [ ] **Step 3: Manual functional verification**

Verify:
1. Open Settings -> Display Preferences: see "Thumbnail Filmstrip" toggle with descriptive copy.
2. Toggle ON: open photo viewer -> full-width filmstrip appears above action buttons.
3. Swipe photos: filmstrip follows and centers current photo thumbnail with primary border.
4. Tap thumbnails: viewer immediately navigates to selected photo.
5. Toggle OFF: verify floating pill dock is cleanly restored.

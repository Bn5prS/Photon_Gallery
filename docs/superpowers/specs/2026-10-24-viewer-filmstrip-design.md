# Viewer Optional Filmstrip Design Document

**Date**: 2026-10-24  
**Author**: Deepak & Antigravity  
**Status**: Pending Approval  

---

## 1. Intent & The Why
In the full-screen photo viewer (`DetailScreen`), users often want to scrub rapidly through nearby photos or see surrounding context without having to swipe one page at a time. An optional edge-to-edge thumbnail filmstrip allows effortless photo scrubbing while preserving the minimal floating pill dock for users who prefer a cleaner canvas.

---

## 2. Goals & Non-Goals
### Goals
- **Configurable Toggle**: Add `viewer_filmstrip_enabled` preference in `SettingsRepository` & `SettingsViewModel`, toggled in `SettingsScreen` under "Display Preferences" (default `false`).
- **Preserved Floating Pill**: When `viewer_filmstrip_enabled == false`, the existing floating pill action dock is 100% preserved with zero regressions.
- **Unified Full-Width Bottom Bar**: When `viewer_filmstrip_enabled == true`, render an edge-to-edge docked bottom bar containing:
  - Upper tier: Full-width horizontal thumbnail filmstrip (`LazyRow`) with smooth bidirectional synchronization (swiping main pager auto-scrolls filmstrip to center active thumbnail; tapping thumbnail navigates to that photo).
  - Lower tier: Viewer actions row (Share, Edit, Favorite, Delete, Lens, or Trash Restore/Permanent Delete).
- **Expressive Motion & Theming**: Integrated into `showUi` visibility toggle with `MotionTokens.gentleSpring()`, M3 `surfaceContainerHighest` tonal surface, `ShapeSmall` (8dp) thumbnail corners, and `primary` 2dp active thumbnail border.

### Non-Goals / Anti-Goals
- No permanent removal of the floating pill dock.
- No raw color literals or non-standard corner radii.
- No heavy blocking thumbnail generation; reuse existing Coil `AsyncImage` caching pipeline.

---

## 3. Constraints
- Strictly follow user directive: **DO NOT COMMIT** to git without explicit user permission.
- Work strictly on branch `dev`.
- Material 3 Expressive guidelines from `AGENTS.md`.

---

## 4. Approaches Considered
1. **Option 1: Full-Width Strip Floating Above Pill**:
   - Floating pill remains at the bottom; a full-width strip hovers 78dp above it.
   - Rejected by user in favor of Option 2.
2. **Option 2: Unified Docked Bottom Bar (Chosen)**:
   - When filmstrip is enabled, bottom chrome transitions into a unified docked edge-to-edge surface containing the filmstrip on top and action buttons below.
   - When filmstrip is disabled, classic floating pill dock is retained.

---

## 5. Implementation Details

### A. Settings Layer
- `SettingsRepository.kt`:
  - `val VIEWER_FILMSTRIP_ENABLED = booleanPreferencesKey("viewer_filmstrip_enabled")`
  - `val viewerFilmstripEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[VIEWER_FILMSTRIP_ENABLED] ?: false }`
  - `suspend fun updateViewerFilmstripEnabled(enabled: Boolean)`
- `SettingsViewModel.kt`:
  - `val viewerFilmstripEnabled: StateFlow<Boolean>`
  - `fun setViewerFilmstripEnabled(enabled: Boolean)`
- `SettingsScreen.kt`:
  - Under "Display Preferences", add `SwitchSettingItem` with icon `R.drawable.ic_ms_view_column`, title "Thumbnail Filmstrip", subtitle "Show an edge-to-edge thumbnail strip (replaces floating pill with docked bottom bar)".

### B. DetailScreen.kt
- Observe `val viewerFilmstripEnabled by settingsRepo.viewerFilmstripEnabledFlow.collectAsState(initial = false)`.
- When `viewerFilmstripEnabled == false`:
  - Render existing `AnimatedVisibility` block for floating pill action dock.
- When `viewerFilmstripEnabled == true`:
  - Render unified full-width bottom bar at `Alignment.BottomCenter` with `.fillMaxWidth()`, `.background(MaterialTheme.colorScheme.surfaceContainerHighest)`, `.navigationBarsPadding()`.
  - Top section: `FilmstripRow(galleryItems, pagerState, coroutineScope)`:
    - `LazyRow` with `rememberLazyListState()`.
    - Centering effect: `LaunchedEffect(pagerState.currentPage)` calls `lazyListState.animateScrollToItem(pagerState.currentPage)`.
    - Item: 38dp width × 50dp height, `ShapeSmall` (8dp) clip, Coil `AsyncImage`. Active item highlighted with `border(2.dp, MaterialTheme.colorScheme.primary, ShapeSmall)`. Non-active items with `alpha = 0.7f`.
    - Click on item: `coroutineScope.launch { pagerState.animateScrollToPage(index) }`.
  - Bottom section: Action icons row (Share, Edit, Favorite, Delete, Google Lens, or Trash actions).
- Adjust `FloatingTipCard` bottom padding if filmstrip is enabled to avoid collision.

---

## 6. What's Left to the Builder
- Precise thumbnail dimensions (38dp x 50dp with 4dp item spacing and 12dp horizontal content padding) to ensure smooth scrubbing and responsive touch targets.
- Haptic feedback tick on thumbnail selection.

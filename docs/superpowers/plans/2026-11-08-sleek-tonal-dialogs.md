# Sleek Minimal Tonal Dialogs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all stock, unstyled `AlertDialog` components with sleek, minimal tonal dialogs conforming to Style 2 (Asymmetric Clean Studio) and powered purely by the user's wallpaper dynamic accent color palette.

**Architecture:** Create a unified, reusable `PhotonDialog` component (with a specialized `PhotonInputDialog` variant) in `com.inferno.gallery.ui.components` that leverages `BasicAlertDialog`, `surfaceContainer`, 24.dp squircle radius, and an asymmetric 44.dp icon header. Migrate all confirmation, deletion, restoration, album creation, folder creation, and renaming dialogs across `DetailScreen.kt`, `MainAppLayout.kt`, and `AlbumPickerSheet.kt`.

**Tech Stack:** Jetpack Compose, Material 3 Expressive, Compose Foundation, AndroidX Activity / MediaStore Scoped Storage.

**Spec:** `docs/superpowers/specs/2026-11-08-sleek-tonal-dialogs-design.md`

## Global Constraints
- Pure wallpaper dynamic accent palette: Use `MaterialTheme.colorScheme.primary`, `primaryContainer`, `onPrimaryContainer`, and `surfaceContainer` tokens. Absolutely no hardcoded red, green, or blue colors.
- Zero glows, pure solid tonal layering with 1.dp `outlineVariant.copy(alpha = 0.15f)` border.
- Layout: Asymmetric Clean Studio (left-aligned 44.dp squircle icon badge, right-aligned tonal Cancel + filled primary action buttons).
- Preserve existing Scoped Storage permission intents (`MediaStore.createTrashRequest`, `MediaStore.createDeleteRequest`, `renameMedia`).

## Review Focus
1. **Empty / Blank Input Submissions**: When creating an album, folder, or renaming a file, blank or whitespace-only inputs must disable the confirm button or prevent action.
2. **File Extension Corruption**: In the rename dialog, changing the base name must not accidentally strip, duplicate, or alter the file extension (e.g. `.jpg`, `.mp4`).
3. **Soft Keyboard Autofocus**: Text fields in `PhotonInputDialog` must request focus smoothly on display so the soft keyboard appears without an extra tap.
4. **Dialog Dismissal & Cancellation**: Pressing outside the dialog or tapping Cancel must safely reset internal text states and clear any pending action references without launching requests.
5. **Dynamic Theme Contrast**: High legibility in both light and dark wallpaper dynamic color schemes (`onPrimary` against `primary`, `onSurface` against `surfaceContainer`).

---

### Task 1: Create Reusable `PhotonDialog` & `PhotonInputDialog`

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable
  fun PhotonDialog(
      onDismissRequest: () -> Unit,
      title: String,
      subtitle: String? = null,
      icon: ImageVector,
      confirmText: String,
      onConfirm: () -> Unit,
      dismissText: String = stringResource(R.string.action_cancel),
      onDismiss: () -> Unit = onDismissRequest,
      isConfirmEnabled: Boolean = true,
      content: (@Composable ColumnScope.() -> Unit)? = null
  )

  @Composable
  fun PhotonInputDialog(
      onDismissRequest: () -> Unit,
      title: String,
      subtitle: String? = null,
      icon: ImageVector,
      value: String,
      onValueChange: (String) -> Unit,
      placeholder: String = "",
      extensionSuffix: String? = null,
      confirmText: String,
      onConfirm: () -> Unit,
      dismissText: String = stringResource(R.string.action_cancel),
      onDismiss: () -> Unit = onDismissRequest,
      isConfirmEnabled: Boolean = value.isNotBlank()
  )
  ```

- [ ] **Step 1: Implement `PhotonDialog` and `PhotonInputDialog` in `PhotonDialog.kt`**
  - Implement `BasicAlertDialog` with `DialogProperties(usePlatformDefaultWidth = false)`
  - Surface with `color = MaterialTheme.colorScheme.surfaceContainer`, `shape = ShapeExtraLarge` (24.dp), `border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))`
  - Asymmetric header row with 44.dp squircle container (`shape = RoundedCornerShape(14.dp)`, `color = MaterialTheme.colorScheme.primaryContainer`) housing `Icon` (`tint = MaterialTheme.colorScheme.onPrimaryContainer`)
  - Title with `style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface`
  - Subtitle with `style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant`
  - For `PhotonInputDialog`: styled `BasicTextField` / `OutlinedTextField` with subtle tonal background (`surfaceContainerHighest`), focus border (`primary`), clear button if non-empty, and optional extension pill badge (e.g. `.jpg`)
  - Action button row: right-aligned with tonal `Cancel` (`surfaceContainerHighest`) and filled `Confirm` pill (`MaterialTheme.colorScheme.primary`, text `onPrimary`)
- [ ] **Step 2: Verify compilation**
  Run: `.\gradlew compileDebugKotlin`
  Expected: SUCCESS
- [ ] **Step 3: Commit**
  Run: `git add app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt ; git commit -m "feat(ui): add reusable PhotonDialog and PhotonInputDialog components"`

---

### Task 2: Migrate Dialogs in `DetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt` (lines 725-825)

**Interfaces:**
- Consumes: `PhotonDialog`, `PhotonInputDialog` from Task 1

- [ ] **Step 1: Replace single-item Move to Trash dialog**
  - In `DetailScreen.kt`, replace `androidx.compose.material3.AlertDialog` (lines 728-771) with `PhotonDialog`:
    - `title = stringResource(R.string.dialog_move_to_trash_title)`
    - `subtitle = "Item stays in Recycle Bin for 30 days"`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_delete)`
    - `confirmText = stringResource(R.string.dialog_move_to_trash_confirm)`
- [ ] **Step 2: Replace Rename File dialog**
  - In `DetailScreen.kt`, replace `androidx.compose.material3.AlertDialog` (lines 775-820) with `PhotonInputDialog`:
    - `title = stringResource(R.string.dialog_rename_title)`
    - `subtitle = "Preserves original file extension"`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_edit)`
    - Split base name and extension so only base name is editable in text input while extension suffix is shown in fixed pill badge
    - `confirmText = stringResource(R.string.action_save)`
- [ ] **Step 3: Verify compilation**
  Run: `.\gradlew compileDebugKotlin`
  Expected: SUCCESS
- [ ] **Step 4: Commit**
  Run: `git add app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt ; git commit -m "refactor(detail): migrate trash and rename dialogs to PhotonDialog"`

---

### Task 3: Migrate Dialogs in `MainAppLayout.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt` (lines 1780-1810, 2077-2107, 2248-2380, 2573-2640)

**Interfaces:**
- Consumes: `PhotonDialog`, `PhotonInputDialog` from Task 1

- [ ] **Step 1: Replace Multi-item Trash dialog**
  - Replace `showMultiDeleteConfirmDialog` (lines 2248-2292) with `PhotonDialog`:
    - `title = stringResource(R.string.dialog_move_to_trash_title)`
    - `subtitle = stringResource(R.string.dialog_multi_trash_msg, selectedUris.size)`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_delete)`
    - `confirmText = stringResource(R.string.dialog_move_to_trash_confirm)`
- [ ] **Step 2: Replace Trash Restore & Permanent Delete dialogs**
  - Replace `showTrashRestoreConfirmDialog` (lines 2296-2340) with `PhotonDialog`:
    - `title = stringResource(R.string.dialog_restore_items_title)`
    - `subtitle = stringResource(R.string.dialog_restore_items_msg, selectedUris.size)`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_restore_from_trash)`
    - `confirmText = stringResource(R.string.action_restore)`
  - Replace `showTrashDeleteConfirmDialog` (lines 2342-2380) with `PhotonDialog`:
    - `title = stringResource(R.string.dialog_delete_perm_title)`
    - `subtitle = stringResource(R.string.dialog_delete_perm_msg, selectedUris.size)`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_delete_forever)`
    - `confirmText = stringResource(R.string.action_delete)`
- [ ] **Step 3: Replace Create Album & Create Folder dialogs**
  - Replace `showCreateAlbumDialog` custom Dialog (lines 2573-2640) with `PhotonInputDialog`:
    - `title = stringResource(R.string.new_album)`
    - `subtitle = "Creates a folder in storage to organize media"`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder)`
    - `placeholder = stringResource(R.string.album_name_hint)`
    - `confirmText = stringResource(R.string.action_create)`
  - Replace `showCreateFolderDialog` (lines 1780-1810 & 2077-2107) with `PhotonInputDialog`:
    - `title = stringResource(R.string.dialog_create_folder_title)`
    - `subtitle = "Creates a new subfolder in current directory"`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_folder)`
    - `confirmText = stringResource(R.string.action_create)`
- [ ] **Step 4: Verify compilation**
  Run: `.\gradlew compileDebugKotlin`
  Expected: SUCCESS
- [ ] **Step 5: Commit**
  Run: `git add app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt ; git commit -m "refactor(main): migrate trash, restore, delete, album and folder dialogs to PhotonDialog"`

---

### Task 4: Migrate Folder Creation in `AlbumPickerSheet.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/components/AlbumPickerSheet.kt` (lines 238-282)

**Interfaces:**
- Consumes: `PhotonInputDialog` from Task 1

- [ ] **Step 1: Replace `showCreateFolderDialog` in `AlbumPickerSheet.kt`**
  - Replace stock `AlertDialog` with `PhotonInputDialog`:
    - `title = stringResource(R.string.dialog_create_folder_title)`
    - `subtitle = "Creates a folder in current directory"`
    - `icon = ImageVector.vectorResource(R.drawable.ic_ms_folder)`
    - `confirmText = stringResource(R.string.action_create)`
- [ ] **Step 2: Verify compilation**
  Run: `.\gradlew compileDebugKotlin`
  Expected: SUCCESS
- [ ] **Step 3: Commit**
  Run: `git add app/src/main/java/com/inferno/gallery/ui/components/AlbumPickerSheet.kt ; git commit -m "refactor(albumpicker): migrate create folder dialog to PhotonInputDialog"`

---

### Task 5: End-to-End Build & Device Verification

- [ ] **Step 1: Build full debug APK**
  Run: `.\gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL
- [ ] **Step 2: Install on target device `10BEAK2JR50012G`**
  Run: `adb -s 10BEAK2JR50012G install -r -g "app\build\outputs\apk\debug\PhotonGallery-arm64-v8a.apk"`
  Expected: Success
- [ ] **Step 3: Verify dialog display and theming**
  - Verify Move to Trash in photo view.
  - Verify Rename dialog with extension badge.
  - Verify Create Album dialog.
  - Verify multi-item trash dialog.
  - Confirm colors match system wallpaper accent without red/green/blue.

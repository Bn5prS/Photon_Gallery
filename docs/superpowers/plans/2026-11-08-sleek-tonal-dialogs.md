# Sleek Minimal Tonal Dialogs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace stock Material 3 `AlertDialog`s across the app with a unified, sleek, minimal tonal `PhotonDialog` component that follows Style 2 (Asymmetric Clean Studio) with pure wallpaper dynamic accent colors and clustered action buttons.

**Architecture:** Create a reusable `PhotonDialog` composable in `com.inferno.gallery.ui.components` utilizing `BasicAlertDialog`, an elevated `surfaceContainer` (24dp rounded corners), an asymmetric 44dp squircle icon header, and tightly clustered action buttons aligned to the right. Then migrate existing dialogs in `DetailScreen.kt`, `MainAppLayout.kt`, and `AlbumPickerSheet.kt`.

**Tech Stack:** Jetpack Compose, Material 3, Material You Dynamic Color Scheme, Kotlin Coroutines, MediaStore Scoped Storage.

**Spec:** [`docs/superpowers/specs/2026-11-08-sleek-tonal-dialogs-design.md`](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-11-08-sleek-tonal-dialogs-design.md)

## Global Constraints
- Pure wallpaper dynamic accent color palette (`MaterialTheme.colorScheme.primary`, `primaryContainer`, `onPrimaryContainer`, `surfaceContainer`, `surfaceContainerHighest`).
- Absolutely NO hardcoded red, green, or blue colors.
- Button layout constraint: Cancel and Confirm (Delete/Create/Save) buttons must be clustered closely together (e.g. `Arrangement.spacedBy(8.dp, Alignment.End)`), never spread far-left and far-right.
- Preserve all existing MediaStore `IntentSenderRequest` permissions and coroutine flows.
- Target device for testing: `10BEAK2JR50012G`. Do not auto-launch app. Do not take screenshots without explicit user instruction.

## Review Focus
1. Empty or whitespace-only inputs in New Album / Rename / New Folder must keep Confirm button disabled or trimmed safely.
2. File extension handling in Rename File must prevent accidental loss of extension (e.g. `.jpg`, `.mp4`).
3. Dialog dismissing via scrim tap or back button must properly reset temporary input state without persisting partial changes.
4. Keyboard IME action (Done/Enter) on text inputs should trigger confirm if input is valid.
5. High-contrast legibility across both Light and Dark dynamic wallpaper themes.

---

### Task 1: Create Reusable `PhotonDialog` Component

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt`

**Interfaces:**
- Produces:
  ```kotlin
  package com.inferno.gallery.ui.components

  @Composable
  fun PhotonDialog(
      onDismissRequest: () -> Unit,
      title: String,
      subtitle: String? = null,
      icon: androidx.compose.ui.graphics.vector.ImageVector,
      confirmText: String,
      onConfirm: () -> Unit,
      dismissText: String = stringResource(R.string.action_cancel),
      onDismiss: () -> Unit = onDismissRequest,
      isConfirmEnabled: Boolean = true,
      content: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null
  )
  ```

- [ ] **Step 1: Write `PhotonDialog.kt`**
  - Implement `BasicAlertDialog` with `DialogProperties(usePlatformDefaultWidth = false)`.
  - Set container width `fillMaxWidth(0.92f).widthIn(max = 420.dp)`.
  - Surface with `shape = RoundedCornerShape(24.dp)` (`ShapeExtraLarge`), color `surfaceContainer`, border `1.dp` with `outlineVariant.copy(alpha = 0.15f)`.
  - Asymmetric header: 44dp squircle icon badge in `primaryContainer`, `primary` icon tint, beside `title` (bold titleMedium) and `subtitle` (bodySmall onSurfaceVariant).
  - Optional body `content` slot with vertical spacing.
  - Clustered button row: `horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)`, top padding 16dp.
  - Cancel button: tonal pill (`surfaceContainerHighest`, `onSurface`).
  - Confirm button: wallpaper accent filled pill (`primary`, `onPrimary`, disabled alpha 0.4f).

- [ ] **Step 2: Verify `PhotonDialog.kt` compiles**
  - Run: `./gradlew compileDebugKotlin`
  - Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
  - Run: `git add app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt ; git commit -m "feat(ui): add reusable sleek tonal PhotonDialog component"`

---

### Task 2: Migrate `DetailScreen.kt` Dialogs (Move to Trash & Rename File)

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt:728-820`

**Interfaces:**
- Consumes: `com.inferno.gallery.ui.components.PhotonDialog`

- [ ] **Step 1: Replace Move to Trash dialog in `DetailScreen.kt`**
  - Replace lines 728-771 with `PhotonDialog`.
  - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_delete)`.
  - Title: `stringResource(R.string.dialog_move_to_trash_title)`.
  - Subtitle: item name or "1 item selected".
  - Body: explanatory text `stringResource(R.string.dialog_move_to_trash_msg)`.
  - Confirm text: `stringResource(R.string.dialog_move_to_trash_confirm)`.
  - Ensure buttons are clustered at the bottom right.

- [ ] **Step 2: Replace Rename File dialog in `DetailScreen.kt`**
  - Replace lines 775-820 with `PhotonDialog`.
  - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_edit)`.
  - Title: `stringResource(R.string.dialog_rename_title)`.
  - Content: modern input field preserving file extension automatically.
  - Confirm text: `stringResource(R.string.action_save)`.
  - Confirm enabled only when trimmed name is non-empty and changed.

- [ ] **Step 3: Verify compilation**
  - Run: `./gradlew compileDebugKotlin`
  - Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
  - Run: `git add app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt ; git commit -m "refactor(detail): migrate trash and rename dialogs to PhotonDialog"`

---

### Task 3: Migrate `MainAppLayout.kt` Dialogs

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt`

**Interfaces:**
- Consumes: `com.inferno.gallery.ui.components.PhotonDialog`

- [ ] **Step 1: Replace Multi-item Move to Trash dialog**
  - Replace `showMultiDeleteConfirmDialog` (lines 2248-2292) with `PhotonDialog`.
  - Title: `stringResource(R.string.dialog_move_to_trash_title)`.
  - Subtitle: `stringResource(R.string.dialog_multi_trash_msg, selectedUris.size)`.
  - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_delete)`.
  - Confirm: `stringResource(R.string.dialog_move_to_trash_confirm)`.

- [ ] **Step 2: Replace Trash Restore & Permanent Delete dialogs**
  - Replace `showTrashRestoreConfirmDialog` (lines 2296-2340) with `PhotonDialog`.
    - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_restore_from_trash)`.
    - Confirm: `stringResource(R.string.action_restore)`.
  - Replace `showTrashDeleteConfirmDialog` (lines 2342-2380) with `PhotonDialog`.
    - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_delete)`.
    - Confirm: `stringResource(R.string.action_delete)`.

- [ ] **Step 3: Replace Create New Album and Create Folder dialogs**
  - Replace `showCreateAlbumDialog` (lines 2573-2640) with `PhotonDialog`.
    - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder)`.
    - Input: styled text field with album name hint.
    - Confirm: `stringResource(R.string.action_create)`.
  - Replace `showCreateFolderDialog` (lines 1780-1810 & 2077-2107) with `PhotonDialog`.

- [ ] **Step 4: Verify compilation**
  - Run: `./gradlew compileDebugKotlin`
  - Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**
  - Run: `git add app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt ; git commit -m "refactor(main): migrate trash, restore, delete, album and folder dialogs to PhotonDialog"`

---

### Task 4: Migrate `AlbumPickerSheet.kt` Create Folder Dialog

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/components/AlbumPickerSheet.kt:240-279`

**Interfaces:**
- Consumes: `com.inferno.gallery.ui.components.PhotonDialog`

- [ ] **Step 1: Replace Create Folder dialog in `AlbumPickerSheet.kt`**
  - Replace stock `AlertDialog` with `PhotonDialog`.
  - Icon: `ImageVector.vectorResource(R.drawable.ic_ms_create_new_folder)`.
  - Title: `stringResource(R.string.dialog_create_folder_title)`.
  - Input: text field with `dialog_folder_name_label`.
  - Confirm: `stringResource(R.string.action_create)`.

- [ ] **Step 2: Verify compilation**
  - Run: `./gradlew compileDebugKotlin`
  - Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
  - Run: `git add app/src/main/java/com/inferno/gallery/ui/components/AlbumPickerSheet.kt ; git commit -m "refactor(sheet): migrate album picker folder creation dialog to PhotonDialog"`

---

### Task 5: Build, Installation & Verification

**Files:**
- None (Verification task)

- [ ] **Step 1: Assemble debug APK**
  - Run: `./gradlew assembleDebug`
  - Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Install on target device**
  - Run: `adb -s 10BEAK2JR50012G install -r -g "app\build\outputs\apk\debug\PhotonGallery-arm64-v8a.apk"`
  - Expected: Success

- [ ] **Step 3: Verification check**
  - Confirm buttons are clustered side-by-side on the right.
  - Confirm colors use dynamic wallpaper accent (no red/green/blue).
  - Inform user device is ready for testing.

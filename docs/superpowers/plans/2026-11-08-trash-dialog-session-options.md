# Trash Dialog Session Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add session-scoped "Don't ask confirmation in this session" and "Skip Recycle Bin (delete permanently)" options to the Trash card dialog, enabling instant single-tap deletion and direct permanent deletion across the app.

**Architecture:** Session-level in-memory state in `GalleryViewModel` drives whether the confirmation dialog is skipped and whether deletion targets the MediaStore Trash or executes permanent deletion directly. A dedicated composable `TrashSessionOptions` provides sleek, tonal checkbox rows with dynamic wallpaper accent styling and adaptive button labels.

**Tech Stack:** Jetpack Compose, Material 3, Android MediaStore API (`createTrashRequest`, `createDeleteRequest`), Kotlin State.

**Spec:** `docs/superpowers/specs/2026-11-08-trash-dialog-session-options-design.md`

## Global Constraints
- Pure wallpaper dynamic accent color palette (`MaterialTheme.colorScheme.primary`, `surfaceContainerHighest`, `onSurface`, `onSurfaceVariant`); no hardcoded red, green, or blue.
- Action buttons in dialogs must remain clustered together on the right (`Arrangement.spacedBy(8.dp, Alignment.End)`).
- Session state is in-memory only and resets when the app is restarted.
- Do not bypass Android OS-level Scoped Storage system prompts on API 30+.
- Never auto-launch the app via ADB; never capture screenshots without explicit user instruction.

## Review Focus
- Checkbox toggle state isolation: toggling "Skip Recycle Bin" must immediately update the confirm button text to "Delete Permanently" and set `isDanger = true`.
- Both options working together: when both are checked, subsequent deletions must directly trigger permanent delete without showing the dialog.
- Multi-selection vs single-photo consistency: both `DetailScreen` and `MainAppLayout` must share the same session flags and behavior.
- Dismissing / canceling the dialog: canceling the dialog must not commit the checkbox changes to the active session.

---

### Task 1: Add Session State to `GalleryViewModel.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/GalleryViewModel.kt`

- [ ] **Step 1: Declare session state properties and helper methods in `GalleryViewModel`**
  Add in-memory properties:
  ```kotlin
  var sessionSkipDeleteConfirmation by mutableStateOf(false)
      private set

  var sessionSkipRecycleBin by mutableStateOf(false)
      private set

  fun setTrashSessionOptions(skipConfirm: Boolean, skipRecycleBin: Boolean) {
      sessionSkipDeleteConfirmation = skipConfirm
      sessionSkipRecycleBin = skipRecycleBin
  }

  fun resetTrashSessionOptions() {
      sessionSkipDeleteConfirmation = false
      sessionSkipRecycleBin = false
  }
  ```
- [ ] **Step 2: Verify `GalleryViewModel.kt` compiles cleanly**
- [ ] **Step 3: Commit Task 1**
  ```bash
  git add app/src/main/java/com/inferno/gallery/ui/GalleryViewModel.kt
  git commit -m "feat(trash): add in-memory trash session state to GalleryViewModel"
  ```

---

### Task 2: Create `TrashSessionOptions.kt` Reusable Composable

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/components/TrashSessionOptions.kt`

- [ ] **Step 1: Implement `TrashSessionOptions` Composable**
  Create the component with two rows:
  - Row 1: "Don't ask confirmation in this session" with subtext "Deletes immediately on tapping delete".
  - Row 2: "Skip Recycle Bin (permanently delete)" with subtext "Files cannot be restored from Trash".
  - Use `Surface` or `Row` with `Modifier.toggleable()` or `clickable()`, `RoundedCornerShape(12.dp)`, `Checkbox` with `CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)`.
- [ ] **Step 2: Add preview / compile check**
- [ ] **Step 3: Commit Task 2**
  ```bash
  git add app/src/main/java/com/inferno/gallery/ui/components/TrashSessionOptions.kt
  git commit -m "feat(ui): add TrashSessionOptions tonal toggle rows component"
  ```

---

### Task 3: Integrate Session Options into `DetailScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

- [ ] **Step 1: Update dock `onDeleteAction` to respect `sessionSkipDeleteConfirmation`**
  When `viewModel.sessionSkipDeleteConfirmation` is `true`:
  - If `viewModel.sessionSkipRecycleBin == true`: call `onPermanentDeleteAction()`.
  - Else: trigger direct `createTrashRequest`.
  When `false`: show `showDeleteConfirmDialog = true`.
- [ ] **Step 2: Update the `PhotonDialog` in `DetailScreen.kt`**
  - Track local dialog state `var dialogSkipConfirm by remember { mutableStateOf(viewModel.sessionSkipDeleteConfirmation) }` and `var dialogSkipRecycleBin by remember { mutableStateOf(viewModel.sessionSkipRecycleBin) }`.
  - Pass `TrashSessionOptions` into `content`.
  - Dynamically set `confirmText = if (dialogSkipRecycleBin) stringResource(R.string.action_delete_permanently) else stringResource(R.string.dialog_move_to_trash_confirm)`.
  - Dynamically set `isDanger = dialogSkipRecycleBin`.
  - On confirm: call `viewModel.setTrashSessionOptions(dialogSkipConfirm, dialogSkipRecycleBin)`, then execute permanent delete or move to trash accordingly.
- [ ] **Step 3: Commit Task 3**
  ```bash
  git add app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt
  git commit -m "feat(viewer): integrate trash session options in DetailScreen"
  ```

---

### Task 4: Integrate Session Options into `MainAppLayout.kt` (Multi-Selection)

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt`

- [ ] **Step 1: Update selection bar multi-delete action to respect `sessionSkipDeleteConfirmation`**
  When `viewModel.sessionSkipDeleteConfirmation` is `true`:
  - If `viewModel.sessionSkipRecycleBin == true`: trigger `createDeleteRequest` for selected URIs.
  - Else: trigger `createTrashRequest` for selected URIs.
  When `false`: show `showMultiDeleteConfirmDialog = true`.
- [ ] **Step 2: Update multi-delete `PhotonDialog` in `MainAppLayout.kt`**
  - Embed `TrashSessionOptions`.
  - Dynamically update `confirmText` and `isDanger`.
  - On confirm: update `viewModel.setTrashSessionOptions(...)` and execute.
- [ ] **Step 3: Commit Task 4**
  ```bash
  git add app/src/main/java/com/inferno/gallery/ui/MainAppLayout.kt
  git commit -m "feat(grid): integrate trash session options in multi-selection dialog"
  ```

---

### Task 5: Compilation, Deployment & Verification

- [ ] **Step 1: Compile debug APK with `./gradlew assembleDebug`**
- [ ] **Step 2: Install APK on device if connected**
- [ ] **Step 3: Verify single photo deletion in `DetailScreen`**
  - Verify "Don't ask confirmation" skips dialog on next delete.
  - Verify "Skip Recycle Bin" calls permanent delete.
  - Verify both options enabled together.
- [ ] **Step 4: Verify multi-selection deletion in `MainAppLayout`**

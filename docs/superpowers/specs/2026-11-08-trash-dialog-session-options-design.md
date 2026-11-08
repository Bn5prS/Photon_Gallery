# Trash Dialog Session Options Design Spec

**Date**: 2026-11-08  
**Author**: Antigravity Pair Programmer & Deepak  
**Status**: In Review  
**Topic**: In-session skip confirmation & direct permanent delete options for Trash Dialog  

---

## 1. Intent & The Why

When triaging media in Photon Gallery (such as reviewing a burst of photos full-screen in `DetailScreen` or selecting multiple unwanted photos in the grid), repeatedly tapping through confirmation dialogs causes significant friction. Additionally, users often need to completely purge unwanted files immediately without filling the Recycle Bin / Trash storage.

This feature adds two session-level options to the Trash confirmation dialog:
1. **"Don't ask confirmation in this session"**: Suppresses subsequent confirmation dialogs, allowing one-tap direct deletion while reviewing photos.
2. **"Skip Recycle Bin (delete permanently) in this session"**: Bypasses the Recycle Bin, issuing direct permanent deletion requests (`MediaStore.createDeleteRequest`).
3. **Combined**: Subsequent deletions are triggered permanently with a single tap.

**Scope**: Active across the entire app process lifetime until the app is closed.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- Provide two clean, interactive option rows inside the `PhotonDialog` trash card.
- Allow toggling each option independently or together.
- In-memory session state maintained across the entire app (`GalleryViewModel`), persisting until the app process terminates.
- Dynamic visual feedback:
  - If "Skip Recycle Bin" is checked, the confirmation button dynamically updates to **"Delete Permanently"** with prominent destructive accent styling.
  - Explanatory subtitles clearly convey the behavior of each option.
- When "Don't ask confirmation" is active, clicking Delete in `DetailScreen` or the selection bar immediately executes the corresponding deletion (Trash or Permanent) without opening `PhotonDialog`.
- Provide a clear, non-intrusive feedback toast when session shortcuts are activated.

### Non-Goals
- Bypassing OS-level Android Scoped Storage prompts on API 30+ (Android requires an explicit `IntentSender` approval dialog for modifying/deleting media not owned by the app; this cannot and should not be bypassed).
- Persisting these flags across app cold restarts (by design, they are session-only to prevent unexpected accidental deletions later).

### Anti-Goals
- Disrupting other dialogs that use `PhotonDialog` (such as Create Album, Rename File, etc.).
- Hardcoding bright red or unnatural accent colors—the UI must strictly honor the app's dynamic wallpaper accent tokens.

---

## 3. Approaches Considered

1. **Approach 1: Sleek Tonal Option Rows inside Trash Card (SELECTED)**
   - Two vertically stacked rows below the file name/subtitle.
   - Each row has a rounded container, a dynamic-accent `Checkbox`, and title + subtext.
   - Confirm button dynamically adapts its label and destructive weight.
   - *Pros*: Legible, natural fit into our minimal tonal card architecture, zero ambiguity for dangerous permanent deletion.
2. **Approach 2: Compact Expressive Toggle Chips**
   - Two selectable chips (`[ ⚡ No confirm ]`, `[ ⚠️ Direct Permanent ]`).
   - *Cons*: Insufficient room for explanatory warning copy for permanent deletion.
3. **Approach 3: Long-press or Dock Mode Switch**
   - *Cons*: Hidden affordances, difficult for users to discover.

---

## 4. Technical Architecture & Implementation

### 4.1 State Management (`GalleryViewModel.kt`)
In-memory session state variables:
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

### 4.2 Reusable Component (`TrashSessionOptions.kt`)
Created in `app/src/main/java/com/inferno/gallery/ui/components/TrashSessionOptions.kt`:
- **Row 1**: Checkbox for "Don't ask confirmation in this session" (Subtext: "Deletes immediately on tapping delete").
- **Row 2**: Checkbox for "Skip Recycle Bin (permanently delete)" (Subtext: "Files cannot be restored from Trash").
- Styled with `MaterialTheme.colorScheme.surfaceContainerHighest`, `RoundedCornerShape(12.dp)`, `CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)`.

### 4.3 `DetailScreen.kt` Integration
- When tapping the dock Delete icon:
  - Check `viewModel.sessionSkipDeleteConfirmation`.
  - If `true`:
    - If `viewModel.sessionSkipRecycleBin == true`: call `onPermanentDeleteAction()`.
    - Else: call `onTrashActionDirect()`.
  - If `false`:
    - Show `PhotonDialog` with `TrashSessionOptions`.
    - When confirmed:
      - Call `viewModel.setTrashSessionOptions(skipConfirm, skipRecycleBin)`.
      - Execute the selected delete action.

### 4.4 `MainAppLayout.kt` (Multi-Selection) Integration
- When tapping Delete in selection mode:
  - Check `viewModel.sessionSkipDeleteConfirmation`.
  - If `true`:
    - If `viewModel.sessionSkipRecycleBin == true`: execute `createDeleteRequest` for selected URIs.
    - Else: execute `createTrashRequest` for selected URIs.
  - If `false`:
    - Show `PhotonDialog` with `TrashSessionOptions`.
    - On confirmation, record session options and execute.

---

## 5. Verification & Testing Plan
1. Open a photo in `DetailScreen` and tap Delete.
2. Confirm the new options are displayed cleanly with dynamic wallpaper accent styling.
3. Test Case 1: Select "Don't ask confirmation in this session" only. Confirm.
   - Verify photo is moved to Trash.
   - Tap Delete on the next photo: Verify it immediately triggers trash without showing `PhotonDialog`.
4. Test Case 2: Select "Skip Recycle Bin (permanently delete)" only.
   - Verify confirm button says "Delete Permanently".
   - Verify photo is permanently deleted via `createDeleteRequest`.
5. Test Case 3: Select both options.
   - Verify subsequent deletes immediately trigger permanent deletion without dialog interruption.
6. Verify app restarts reset both flags to safe defaults.


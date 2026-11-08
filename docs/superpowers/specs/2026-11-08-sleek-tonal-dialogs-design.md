# Design Spec: Sleek Minimal Tonal Dialogs (Asymmetric Clean Studio)

**Date**: 2026-11-08  
**Status**: Awaiting User Approval  
**Scope**: Modal Dialogs across `DetailScreen.kt`, `MainAppLayout.kt`, and `AlbumPickerSheet.kt`  
**Prototype & Visual Spec**: `dialog_design_concepts.html` (Style 2: Asymmetric Clean Studio)

---

## 1. Intent & Context
Currently, dialogs for **Move to Trash**, **Restore Items**, **Permanent Delete**, **Create Album**, **Create Folder**, and **Rename File** rely on stock Material 3 `AlertDialog` components. These default dialogs use flat text titles, unstyled text content, and plain text buttons floating in the bottom-right corner, appearing disconnected from Photon Gallery's refined Material 3 Expressive interface (squircles, tonal containers, springy interactions).

This design upgrades all confirmation and input dialogs to **Style 2: Asymmetric Clean Studio**, a sleek, minimal tonal card architecture chosen by the user.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Unified Dialog System**: Replace fragmented, stock `AlertDialog`s with a single, highly reusable `PhotonDialog` component.
- **Pure Wallpaper Accent Palette (No Red, Green, or Blue)**:
  - Strictly adhere to the device's dynamic Material You wallpaper accent color scheme (`MaterialTheme.colorScheme.primary`, `primaryContainer`, `onPrimaryContainer`, `surfaceContainer`, and neutral surface tones).
  - Absolutely NO hardcoded red, green, blue, or rainbow colors. All dialog types (Trash, Restore, Delete, Create Album, Rename) harmoniously draw from the user's active wallpaper theme tokens.
  - Icon squircle badges use `MaterialTheme.colorScheme.primaryContainer` (or `surfaceContainerHighest` with subtle accent border) and `primary` icon tint.
  - Action pills use dynamic wallpaper accent `primary` (or tonal `primaryContainer`) with `onPrimary` text.
- **Compact & Sleek Ergonomics**: Left-aligned 44dp squircle icon beside headline and subtitle, keeping vertical dialog height low while maintaining high touch affordance.
- **Preserve Safety & Permissions**: Seamlessly integrate with Android MediaStore's `IntentSenderRequest` (scoped storage permission dialogs for trash/delete/rename) without altering underlying coroutine logic.

### Non-Goals
- Adding complex multi-step wizards or unnecessary prompt steps.
- Hardcoded multi-color accents (red, green, blue) or gradient glow bloat (pure solid tonal layering conforming to Photon guidelines).

### Anti-Goals
- Cluttering the dialog with redundant UI elements that slow down frequent actions like trashing or creating folders.

---

## 3. Explored Approaches & Decision

During visual brainstorming, three tonal design directions were evaluated in `dialog_design_concepts.html`:

| Approach | Header / Icon | Layout Style | Button Arrangement | Tradeoffs |
|---|---|---|---|---|
| **Approach 1: Centered Expressive Squircle** | 56dp centered squircle icon badge | Centered headline & text | 50/50 dual horizontal pill buttons | Very bold, but requires more vertical screen height. |
| **Approach 2: Asymmetric Clean Studio** *(Selected)* | 44dp squircle icon aligned to the left of Title & Subtitle | Left-aligned, compact, studio feel | Right-aligned compact action pills | **Most compact, utilitarian, sleek, and fastest for rapid workflows.** |
| **Approach 3: Stacked Touch Pill** | 48dp centered icon | Centered header | Full-width vertical stacked buttons | Thumb-friendly, but tall cards push dialogs higher on screen. |

**Decision**: The user selected **Style 2: Asymmetric Clean Studio**.

---

## 4. Technical Architecture: `PhotonDialog.kt`

A new reusable composable file will be created at:  
[`app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt`](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/app/src/main/java/com/inferno/gallery/ui/components/PhotonDialog.kt)

### Composable Signature
```kotlin
@Composable
fun PhotonDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    confirmText: String,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary,
    confirmTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    onConfirm: () -> Unit,
    dismissText: String = stringResource(R.string.action_cancel),
    onDismiss: () -> Unit = onDismissRequest,
    isConfirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
)
```

### Card Specifications
1. **Container**:
   - Component: `androidx.compose.material3.BasicAlertDialog` with `DialogProperties(usePlatformDefaultWidth = false)`
   - Window Constraint: `Modifier.fillMaxWidth(0.92f).widthIn(max = 420.dp)`
   - Surface: `MaterialTheme.colorScheme.surfaceContainer`
   - Shape: `ShapeExtraLarge` (24.dp corner radius)
   - Border: `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))`
   - Tonal Elevation: `6.dp`
2. **Header Row (Asymmetric)**:
   - Row: `Alignment.CenterVertically`, spacing `14.dp`
   - Icon Badge: `Surface(shape = RoundedCornerShape(14.dp), color = iconContainerColor, modifier = Modifier.size(44.dp))`
   - Icon: `Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))`
   - Title Column:
     - `Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)`
     - `subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }`
3. **Body Area (Optional)**:
   - For confirm dialogs: Descriptive retention text (e.g., "Items stay in the Recycle Bin for 30 days before being permanently deleted").
   - For input dialogs: Modern styled `OutlinedTextField` or `BasicTextField` inside a tonal pill container with character limits, clear button, and extension badge for filenames.
4. **Action Buttons (Clustered Together)**:
   - Row: `horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)`, top padding `16.dp`.
   - **Crucial Layout Rule**: Cancel and Confirm (Delete/Create/Save) buttons are placed immediately next to each other as a tight pair (8dp gap), aligned to the end. They are NEVER separated with space-between across opposite far-left and far-right edges.
   - Dismiss Button: Compact tonal pill button (`MaterialTheme.colorScheme.surfaceContainerHighest`, text `onSurface`).
   - Confirm Button: Compact filled pill button in wallpaper accent (`confirmButtonColor`, text `confirmTextColor`, spring-pressed scale haptics).

---

## 5. Screen Migration Targets

### Target 1: `DetailScreen.kt`
- **Move to Trash Dialog**:
  - Replace stock `AlertDialog` (lines 728-771) with `PhotonDialog`.
  - Icon: `ic_ms_delete` in wallpaper accent `primaryContainer`, confirm button in wallpaper accent `primary`.
- **Rename File Dialog**:
  - Replace stock `AlertDialog` (lines 775-820) with `PhotonDialog`.
  - Icon: `ic_ms_edit` in wallpaper accent `primaryContainer`.
  - Input field with automatic extension isolation (`.jpg`, `.mp4`, `.png` badge).

### Target 2: `MainAppLayout.kt`
- **Multi-item Move to Trash**:
  - Replace `showMultiDeleteConfirmDialog` (lines 2248-2292) with `PhotonDialog`.
  - Title: "Move to Recycle Bin", Subtitle: "$count items selected".
  - Icon: `ic_ms_delete` in wallpaper accent `primaryContainer`.
- **Restore Items from Trash**:
  - Replace `showTrashRestoreConfirmDialog` (lines 2296-2340) with `PhotonDialog`.
  - Icon: `ic_ms_restore_from_trash` in wallpaper accent `primaryContainer`.
- **Permanent Delete Confirmation**:
  - Replace `showTrashDeleteConfirmDialog` (lines 2342-2380) with `PhotonDialog`.
  - Wallpaper accent tonal styling with permanent deletion note.
- **Create New Album**:
  - Replace custom `Dialog` (lines 2573-2640) with `PhotonDialog`.
  - Icon: `ic_ms_create_new_folder` / `folder` in wallpaper accent `primaryContainer`.
- **Create Folder (File Browser)**:
  - Replace `showCreateFolderDialog` (lines 1780-1810 & 2077-2107) with `PhotonDialog`.

### Target 3: `AlbumPickerSheet.kt`
- **Create Folder**:
  - Replace `showCreateFolderDialog` (lines 240-279) with `PhotonDialog`.

---

## 6. What's Left to the Builder
- Implementing the reusable `PhotonDialog` composable in `PhotonDialog.kt`.
- Wiring up the migration across `DetailScreen.kt`, `MainAppLayout.kt`, and `AlbumPickerSheet.kt`.
- Ensuring smooth keyboard autofocus on text fields when the rename and album creation dialogs open.
- Verifying compilation via `./gradlew assembleDebug` and testing on device `10BEAK2JR50012G`.


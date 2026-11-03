# Fullscreen Viewer Distraction-Free Colors & Backdrop Design

## 1. Intent & Why
In the fullscreen media viewer (`DetailScreen.kt`), user focus should remain entirely on the photograph. Currently:
- The base canvas defaults to `MaterialTheme.colorScheme.surfaceContainerLow`, which on many themes renders as grey or tinted, creating harsh boundaries against letterboxed photos.
- Dynamic ambient lighting and blur background options can produce overly saturated or bright backdrops that compete chromatically with the image.
- While the top bar has been updated to a transparent gradient scrim, the bottom controls and filmstrip dock still render inside a solid opaque M3 `surfaceContainerHighest` card with drop shadows.
- Action icons use theme-dependent `onSurface` and high-contrast error red tints.

This design reimagines the full-screen viewer color palette with a **cinematic subdued ambient glow** and **seamless frosted-glass chrome**, ensuring zero distraction from the photograph.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
1. **OLED Pure Black Canvas Baseline**:
   - Ensure the baseline canvas is true pitch black (`#000000` / `Color.Black`), turning off OLED pixels and eliminating tinted letterbox boundaries.
2. **Subdued Ambient Color Blend**:
   - When dynamic ambient glow / blur is active, heavily mute and desaturate the color blend by layering under an 80–85% pitch-black scrim (`Color.Black.copy(alpha = 0.82f)`).
   - Produces a soft, atmospheric diffusion from the image edges into pure black without chromatic competition.
3. **Seamless Bottom Gradient Scrim & Frosted Controls**:
   - Replace the solid opaque bottom `Surface` (`surfaceContainerHighest`) with a transparent container with vertical gradient scrim (`Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.75f)))`).
   - When filmstrip is enabled, thumbnails sit cleanly on the gradient with a crisp active thumbnail border.
   - Action buttons adopt frosted glass circular pills (`Color.Black.copy(alpha = 0.35f)`) with clean `Color.White` icons, matching the top bar aesthetic.
4. **Preserved Gestures & Motion**:
   - Swipe-to-dismiss fading (`bgAlpha`), pinch-to-zoom, filmstrip thumbnail synchronization, and smooth spring animations remain intact.

### Non-Goals
- Changing viewer logic, EXIF inspector sheet, rename dialogs, or editing workflows.
- Altering user preference keys or repository data contracts.

### Anti-Goals
- Opaque solid color blocks or harsh cards across the screen.
- Over-saturated ambient glows that distract the user's eye from photo details.

---

## 3. Approaches Considered

1. **Approach 1 (Chosen): Deep Cinematic Scrim & Ambient Glow**:
   - True OLED black base canvas.
   - Subdued ambient blur under an 82% dark scrim.
   - Bottom bar and filmstrip rendered over an edge-to-edge transparent gradient scrim, with frosted glass pill action buttons (`Color.Black.copy(alpha = 0.35f)` + `Color.White`).
   - *Why chosen*: Unifies top and bottom viewer chrome, completely eliminates opaque blocks, and lets photos bleed edge-to-edge with maximum immersive focus.

2. **Approach 2: Frosted Dark Acrylic Capsule**:
   - Monochromatic grey-toned ambient blur.
   - Bottom dock retained as a floating pill capsule with frosted acrylic glass and hairline border.
   - *Trade-off*: An enclosed capsule covers a horizontal slice of the photo rather than letting the photo bleed edge-to-edge seamlessly through a natural gradient scrim.

---

## 4. Detailed Component Design

### 4.1 Canvas Backdrop (`DetailScreen.kt`)
- Baseline `baseCanvasColor = Color.Black`.
- Radial ambient gradient (when `viewerDynamicColor` is true) tuned to:
  - Center: `animatedBgColor.copy(alpha = 0.40f * bgAlpha)` (reduced from 0.85f).
  - Mid: `animatedBgColor.copy(alpha = 0.15f * bgAlpha)` (reduced from 0.40f).
  - Outer: `Color.Black.copy(alpha = 0.98f * bgAlpha)`.
- Blur effect (when `viewerBlurEffect` is true):
  - AsyncImage blur increased to `36.dp`.
  - Dark overlay scrim set to `Color.Black.copy(alpha = 0.82f * bgAlpha)` (deepened from 0.45f).

### 4.2 Bottom Bar & Filmstrip Dock
- Container replaced from solid `Surface(color = surfaceContainerHighest, elevation = ...)` to `Box`:
  ```kotlin
  Box(
      modifier = Modifier
          .fillMaxWidth()
          .background(
              Brush.verticalGradient(
                  colors = listOf(
                      Color.Transparent,
                      Color.Black.copy(alpha = 0.45f),
                      Color.Black.copy(alpha = 0.80f)
                  )
              )
          )
  )
  ```
- **Filmstrip Row**: Sits directly on this gradient with `navigationBarsPadding()` respected. Active thumbnail receives a crisp `2.dp` border in `Color.White` with rounded corners.
- **Floating Dock (when filmstrip disabled)**: Replaced with a sleek frosted glass pill:
  - Background: `Color.Black.copy(alpha = 0.45f)`.
  - Border: `1.dp` in `Color.White.copy(alpha = 0.12f)`.
  - Shape: `ShapeFull`.

### 4.3 Action Buttons (`ViewerActionButtonsRow`)
- Each action button (Share, Edit, Favorite, Delete, Google Lens, Restore) styled with:
  - Container: Circular frosted pill or clean transparent tap target.
  - Icon Tint: `Color.White` (or `Color.White.copy(alpha = 0.90f)`).
  - Delete Tint: Softened white or subtle pastel error tint that does not flash harsh red against dark photos.
  - Favorite Tint: Active state uses `favoriteHarmonized` with spring bounce; inactive state uses `Color.White`.

### 4.4 Theme Behavior (Universal Immersive Dark Cinema)
- In accordance with photography industry standards (Google Photos, Apple Photos), when viewing media full screen in `DetailScreen.kt`, the viewer maintains a **Universal Immersive Dark** palette regardless of whether the app or system is in Light Mode or Dark Mode.
- This ensures:
  1. System status bar icons remain light/white (`isAppearanceLightStatusBars = false`).
  2. Navigation bar icons remain light/white (`isAppearanceLightNavigationBars = false`).
  3. Zero glare or contrast loss from bright white borders when viewing media in Light Mode.
  4. Returning to the main timeline or albums restores the user's chosen Light Mode theme seamlessly.



# One-Time Floating Feature Tips Design Specification

## Overview
Photon Gallery includes several advanced, fluid gesture capabilities (such as long-press album card customization for shape morphing and cover selection, face cover selection and cluster merging, motion photo press-and-hold video playback, and markup two-finger resizing/rotation). Because these interactions have minimal explicit UI buttons, average users may never discover them.

This specification introduces **One-Time Floating Feature Discovery Tips** with "Quick Tip" heading, playful spring entrance physics, high-contrast "Got it" confirmation, and persistent DataStore dismissal.

---

## Visual & Motion Design (Material 3 Expressive)
1. **Container & Surfaces**:
   - `surfaceContainerHighest` elevated floating container with 20dp corners (`ShapeLargeIncreased`).
   - 0.5dp border with `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)`.
   - Ambient drop shadow for distinct separation from background photo grids.
2. **Typography**:
   - Title: `"Quick Tip"` in `MaterialTheme.typography.titleSmall` (Outfit/Urbanist semi-bold).
   - Body: 1–2 sentence clear explanation in `MaterialTheme.typography.bodySmall` (Plus Jakarta Sans).
3. **Icons**:
   - Material Symbols Rounded (`ic_ms_auto_fix_high`, `ic_ms_touch_app`, `ic_ms_motion_photos_on`, `ic_ms_person`).
   - Tinted icon inside a `primaryContainer` pill shape.
4. **Motion Scheme**:
   - Entrance: 400ms delay after screen composition, followed by spring-in with overshoot using `MaterialTheme.motionScheme.fastSpatialSpec()`.
   - Exit: Spring shrink + fade out on "Got it" click.
5. **Persistence**:
   - Backed by Preferences DataStore in `SettingsRepository`.
   - Once dismissed, the preference flag is permanently set to `true`, preventing future appearances.

---

## Targeted Key Interactions
1. **Albums Screen**:
   - Key: `tip_albums_customize`
   - Icon: `ic_ms_auto_fix_high`
   - Title: `Quick Tip`
   - Message: *"Press & hold any album card to customize its cover photo or choose custom shapes like Cookie, Clover & Pill."*
2. **People & Faces Screen**:
   - Key: `tip_people_cover`
   - Icon: `ic_ms_person`
   - Title: `Quick Tip`
   - Message: *"Press & hold any person to choose their best cover shot, rename them, or merge clusters."*
3. **Motion Photo Viewer**:
   - Key: `tip_motion_photo`
   - Icon: `ic_ms_motion_photos_on`
   - Title: `Quick Tip`
   - Message: *"Press and hold anywhere on the photo to play live motion video, or tap the Motion pill for Top Shot frames."*
4. **Editor Markup Canvas**:
   - Key: `tip_markup_gestures`
   - Icon: `ic_ms_touch_app`
   - Title: `Quick Tip`
   - Message: *"Tap placed text or arrows to drag, pinch-to-scale, and rotate freely with two fingers."*

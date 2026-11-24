# Release Notes — v4.3.1

### Improvements & Localization
* **Full-Screen Photo Stack UI**: Simplified the viewer stack bar to a mini-thumbnail strip with a menu button opening **Keep as best shot**, **Remove from stack**, **Unstack all**, and **Keep this, delete rest**.
* **Italian Translations for v4.3.0 (#62)**: Added Italian translations for People matches, Photo Stacks, and Video Trimmer (thanks to @Hanamichi27).

---

### Bug Fixes
* **KDE Connect & System Share Compatibility (#61)**: Fixed share intent `ClipData` MIME metadata, normalized MediaStore URIs, manifest `<queries>` entries, and chooser intent flags so sharing photos and videos works reliably with KDE Connect and other share targets.
* **Photo Stack Multi-Photo Deletion & Warning**: Fixed deleting or trashing a selected Photo Stack from the timeline so all photos in the stack are removed together in one step instead of one shot at a time, with a clear stack warning inside the confirmation dialog.
* **RAW / DNG Image Orientation**: Fixed portrait camera RAW photos (`.dng`, `.cr2`, `.cr3`, `.nef`, `.arw`, `.orf`, `.raf`, `.rw2`) appearing rotated sideways in both grid cells and the full-screen viewer by applying EXIF and MediaStore orientation consistently without double-rotating.
* **Video Trimmer Compact Row Layout**: Added flex/ellipsis protection to the Video Trimmer action chips so longer translated labels never push the mute button off-screen on narrow displays.
* **Android Lint & CI Stability**: Fixed pre-existing Lint errors (`RuntimeShader` API 33 guard, Compose `LocalResources`/`LocalLocale` usage, and Media3 `@UnstableApi` opt-in) and configured incomplete community translations as Lint warnings.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures
* **PhotonGallery-armeabi-v7a.apk** — 32-bit ARM devices
* **PhotonGallery-x86_64.apk** — 64-bit x86 emulators and devices

# Release Notes — v4.0.0

### New Features
* **Customizable Toolbars**: Reorder and choose actions for the photo viewer bar and the multi-select bar in Settings.
* **Collections Tab**: Moved Collections to its own tab with pinned albums at the top, a 2–4 column pinch gesture on Albums, and collapsible Utilities & Tools.
* **Ultra HDR & Motion Photos**: Added Ultra HDR display support in the viewer (with a Settings toggle) and Motion Photo playback/extraction.
* **Viewer Actions**: Added quick rotate with EXIF saving, copy image to clipboard, print, and an option to strip EXIF on copy or overwrite.
* **Memories Views**: Added Stories and Spotlight views for Memories.
* **Trash Options**: Added session toggles in the delete dialog to skip confirmation or delete permanently without moving to Trash.

---

### Improvements
* **Photo Editor**: Expanded with markup drawing, text tools, blur redaction, mirror, overwrite-save, and hold-to-compare.
* **Theme Picker**: Replaced the horizontal palette scroll row with a 4×4 color picker card and narrowed palette styles to 4 options.
* **Timeline Grid & Scrolling**: Reworked the main photo grid for smoother scrolling, drag-to-select, better fast-scroller behavior, and preserved scroll position when returning from a photo.
* **Transitions & Navigation**: Updated the bottom navigation bar with a separate Search button, moved sort/view options into a compact bottom sheet, and added screen/photo open transitions.
* **Dialogs & Settings Screens**: Cleaned up the delete dialog, About screen, Update dialog, EXIF details sheet, onboarding background, and Smart Search model cards (with Face Recognition moved to second place).
* **Smart Search & Document Sorting**: Switched Smart Search to MobileCLIP2-S2 FP32, improved document category accuracy (receipts, IDs, notes, etc.), and added a 50-image cooldown during background indexing so the phone stays cool.
* **Fresh-Install Defaults**: Center-aligned date headers, Videos tab off by default, and 4-column grid (existing saved settings, pinned albums, and face names stay intact).

---

### Bug Fixes
* **Trash Bin**: Excluded non-media trashed files (like PDFs or APKs), fixed trashed videos being treated as images, and prevented trashed items from disappearing on tap or sync.
* **Viewer & Editor**: Show capture date instead of file modified date, keep controls visible while menus are open, and fix crop gestures, bitmap decoding, and HSL sliders in the editor.
* **Sync & Stability**: Added pull-to-refresh, removed blank thumbnails during sync, fixed background face-scan crashes, and silenced noisy background indexing notifications.

---

### Translations
* Added **Italian** and **Spanish (es-ES)** translations, plus updates and fixes for **French**, **German**, and **Portuguese (BR)**.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures

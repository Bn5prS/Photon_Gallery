# Release Notes — v4.0.0

### New Features
* **Customizable Toolbars**: Reorder and choose actions for the photo viewer bar (including Copy to Clipboard, Rename, and Print) and the multi-select bar (including Collage and Stitch) in Settings.
* **Collections Tab**: Moved Collections to its own tab with pinned albums at the top, a 2–4 column pinch gesture on Albums, long-press **Delete Album** with permanent confirmation, and collapsible Utilities & Tools.
* **Timeline Grouping (Day / Month / Year)**: Group photos by **Day**, **Month**, or **Year** when using Grouped view in the Sort & View sheet.
* **Collage**: Redesigned with new layouts, undo, shuffle, photo swapping, add/remove photos, fit/fill controls, separate gap, margin, and corner sliders, gradient backgrounds, and `4:5`, `3:2`, and `2:3` aspect ratios.
* **Secret Vault**: Added an in-vault photo and video viewer, PIN fallback when hiding media, current-PIN check before changing your PIN, encrypted storage, and temporary lockout after repeated wrong attempts.
* **Ultra HDR & Motion Photos**: Added Ultra HDR display support in the viewer (with a Settings toggle) and Motion Photo playback/extraction.
* **Viewer Actions**: Added quick rotate with EXIF saving, copy image to clipboard, print, and an option to strip EXIF on copy or overwrite.
* **Memories Views**: Added Stories and Spotlight views for Memories.
* **Trash Options**: Added session toggles in the delete dialog to skip confirmation or delete permanently without moving to Trash.

---

### Improvements
* **Photo Editor**: Redesigned the editor layout with cleaner icons, aligned hold-to-compare button, markup drawing, text tools, blur redaction, mirror, and overwrite-save.
* **Theme Picker**: Replaced the horizontal palette scroll row with a 4-column color palette grid and custom color picker.
* **Timeline Grid & Scrolling**: Reworked the main photo grid for smoother scrolling, animated pinch-to-zoom column changes, drag-to-select, better fast-scroller behavior, and preserved scroll position when returning from a photo.
* **Transitions & Navigation**: Updated the bottom navigation bar with a separate Search button, fixed the `"Collections"` tab label clipping in the pill dock, moved sort/view options into a compact bottom sheet (including Pinned Albums), and added screen/photo open transitions.
* **Dialogs & Settings Screens**: Cleaned up the delete dialog, About screen, Update dialog, EXIF details sheet, onboarding background, enlarged the inner app logo symbols in Onboarding and About, and updated Smart Search model cards (with Face Recognition moved to second place).
* **Smart Search & Document Sorting**: Switched Smart Search to MobileCLIP2-S2 FP32, improved document category accuracy (receipts, IDs, notes, etc.), and added a 50-image cooldown during background indexing so the phone stays cool.

---

### Bug Fixes
* **Trash Bin**: Excluded non-media trashed files (like PDFs or APKs), fixed trashed videos being treated as images, and prevented trashed items from disappearing on tap or sync.
* **Viewer & Editor**: Show capture date instead of file modified date, keep controls visible while menus are open, and fix crop gestures and bitmap decoding in the editor.
* **Sync & Stability**: Added pull-to-refresh, removed blank thumbnails during sync, fixed background face-scan crashes, fixed crashes when opening Settings or dragging collage sliders to zero, fixed metadata stripping status reporting, and silenced noisy background indexing notifications.

---

### Translations
* Added **Italian** and **Spanish (es-ES)** translations, plus updates and fixes for **French**, **German**, and **Portuguese (BR)**.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures

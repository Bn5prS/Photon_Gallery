# Release Notes — v4.0.0

### New Features
* Added customizable viewer and multi-select toolbars in Settings (#32).
* Added a dedicated Collections tab with collapsible People, Places, Documents, and Utilities & Tools sections.
* Added Pinned Albums at the top of the Albums screen with an All Pinned Albums view.
* Added long-press Delete Album with permanent deletion confirmation.
* Added Day, Month, and Year timeline grouping in the Sort & View sheet.
* Added Collage Studio presets (Bento, Polaroid, 35mm Film, Scrapbook, Arch, Capsule, Geometric), frame sliders, backdrops, and 4:5, 3:2, and 2:3 ratios.
* Added Stitch Studio presets, per-clip rotate/flip/screenshot-bar trim, frame sliders, and backdrops.
* Added a pitch-black (`#000000`) studio theme across Photo Editor, Collage, and Stitch.
* Added markup drawing, text tools, blur redaction, freeform crop, mirror, and overwrite-save in the Photo Editor (#20, #24).
* Added Ultra HDR display support in the photo viewer (#37).
* Added Motion Photo playback and frame extraction in the viewer.
* Added EXIF metadata stripping (copy or overwrite), quick rotate, rename, copy to clipboard, and print actions in the viewer (#23, #36).
* Added an in-vault photo and video viewer in Secret Vault.
* Added pull-to-refresh on the main gallery timeline (#27).
* Added Stories and Spotlight views for Memories.
* Added session toggles in the delete dialog to skip confirmation or delete permanently.

---

### Improvements
* Improved the theme picker with a 4-column color palette grid and custom color picker.
* Improved the bottom navigation dock with a separate Search button and auto-fitted tab labels.
* Improved timeline pinch-to-zoom with smooth animated column transitions and a draggable fast-scroll pill (#41).
* Improved Sort & View controls with a unified bottom sheet across Photos, Albums, and Pinned Albums.
* Improved Settings organization across Look & Feel, Layout & Navigation, General, and Privacy & Security.
* Improved the About screen, Update dialog, EXIF details sheet, and Onboarding screen.
* Improved Smart Search with MobileCLIP2-S2 FP32, better document sorting, and a 50-image indexing cooldown.
* Improved default gallery app and supported link handling in system settings (#26).

---

### Bug Fixes
* Fixed Secret Vault accepting the device lockscreen PIN instead of the custom Vault PIN and added failed-attempt lockout (#30).
* Fixed the photo viewer showing file modification date instead of the original EXIF capture date (#39, #40).
* Fixed crop handle dragging, pan direction, and aspect-ratio bounds in the Photo Editor (#29).
* Fixed Recycle Bin deletion, trashed video handling, and items disappearing on tap or sync (#25, #34).
* Fixed blank thumbnails after modifying or syncing media (#28).
* Fixed video player pinch-to-zoom resetting back to normal scale when lifting fingers.
* Fixed crashes when opening Settings, scanning faces in the background, or dragging studio frame sliders to zero.

---

### Translations
* Added Italian (#33) and Spanish (es-ES) (#38) translations, and improved French, German, and Portuguese (BR) strings.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures

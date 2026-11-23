# Release Notes — v4.3.0

### New Features & Improvements
* **Photo Stacks**: Automatically groups burst shots and quick retakes under a single Best Shot thumbnail on the timeline, with Best Shot switching, Keep Best Only confirmation, and manual stack / unstack controls.
* **Video Trimmer (#52)**: Trim videos on-device with a 4-tab studio dock for precise in/out trimming, playback speed (0.5×–2×), rotate & flip transforms, mute toggle, and quality export (Original, 1080p, 720p, 480p).
* **Combined Search & Metadata Filters (#53)**: Cleaner Search layout with an integrated Filters button and unified Filters sheet to combine Date Range (presets & custom picker), Media Type, Document Type, People, Albums, and Location filters on their own or alongside search queries.
* **Duplicate & Similar Photo Cleaner (#51)**: Redesigned Clean Up screen with exact duplicate and similar-shot detection, sharpness & resolution readouts below unobstructed previews, pinch-to-zoom inspection, side-by-side comparison, and Trash confirmation.
* **Smarter People Merging & Multi-Select Cleanup (#57, #60)**:
  * Added similarity-ranked **Closest matches** in the Merge picker and one-tap **Find more matches** discovery.
  * Added a **Select** option in People alongside long-press, with a clear bottom bar for **Merge**, **Favorite**, **Hide**, and **Not a face** (also available inside Person Detail). Marking detections as **Not a face** removes them without touching your photos and automatically ignores similar false detections in future scans.
* **Unified Timeline Badges & Selection Polish**: Clean, consistent corner indicators across Standard and Mosaic grids (Favorites bottom-left; Video duration, Motion Photo, RAW, GIF, and Panorama bottom-right; Photo Stacks top-right) plus clearer tonal **Select All / Deselect All** pill buttons.
* **Italian Translation Improvements (#50)**: Improved Italian localization consistency across the app.

---

### Bug Fixes
* **Faster New-Photo Detection (#56)**: Added incremental delta sync so newly taken photos and screenshots show up in the timeline almost immediately.
* **Video Playback Crash (#58)**: Fixed an ONNX Runtime session concurrency bug causing crashes during video playback.
* **Face Recognition & Clustering Accuracy (#60)**: Fixed a left/right landmark swap that was shrinking straight-on face crops during recognition, tightened filtering for random objects and extreme dark/bright lighting patches, and prevented hidden people from merging into visible ones. Existing libraries automatically refresh face-containing photos in the background while keeping all names, favorites, and hidden people intact.
* **Theme-Aware "Hide Content in Recents" Overlay (#59)**: Matched the recents privacy overlay and window background to your current light/dark surface theme instead of showing a bright white screen in dark mode.
* **Person Detail Transitions**: Fixed open/close container-transform animations in Person Detail so returning never breaks main timeline transitions.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures
* **PhotonGallery-armeabi-v7a.apk** — 32-bit ARM devices
* **PhotonGallery-x86_64.apk** — 64-bit x86 emulators and devices

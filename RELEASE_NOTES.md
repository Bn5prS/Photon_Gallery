# Release Notes — v3.0.0

### ⚠️ Important Notice for v2.x Users
* **In-app update not supported from v2.x**: Due to migrating to a permanent official release signing key, updating directly from within the v2.x app will fail with a signature mismatch error.
* **How to upgrade**: Please uninstall your existing v2.x app from your device, then download and install the v3.0.0 package directly from GitHub Releases.
* **Future updates**: All future releases from v3.0.0 onward use this permanent key and will update seamlessly inside the app with 1 tap.

---

### Features
* **People & Face Grouping**: On-device facial recognition and clustering running 100% locally. Automatically organizes recurring faces into individual identities with support for custom names, favorites, hidden profiles, and smart merge suggestions.
* **Document Smart Classification**: Automated on-device OCR categorization that sorts photos into 7 document types (Receipts, Invoices, IDs & Cards, Notes, Forms, Whiteboards, and Documents) via a collapsible carousel in the Albums tab.

---

### Improvements
* **Adaptive App Icon**: Redesigned borderless launcher icon with dynamic Material You themed icon support on Android 13+.
* **Edge-to-Edge Photo Grid**: Reduced thumbnail spacing to 1dp for a cleaner, high-density browsing layout.
* **Album Sorting**: Added "Date Added" sorting alongside Date Taken, with new album overflow management controls.
* **Onboarding Setup**: Integrated the offline face model download into the initial setup wizard with real-time transfer speed and byte progress.
* **Search & OCR Precision**: Upgraded OCR decode resolution to 2560px for improved text detection, paired with refined CLIP semantic search matching thresholds.
* **Viewer Controls**: Added a Settings toggle to show or hide the floating zoom minimap tracker in the full-screen photo viewer.

---

### Bug Fixes
* **OCR Search Query Syntax**: Resolved query parsing errors to ensure multi-word text searches return accurate matches.
* **Social Media Routing**: Fixed a category precedence issue where Facebook downloads failed to map into the Social album.
* **Viewer Face Highlights**: Removed automatic face bounding box overlays from displaying unexpectedly upon opening photos.
* **Large Group Merges**: Resolved a SQLite `CursorWindow` memory allocation failure when merging large person clusters.
* **Background Indexing Stability**: Fixed coroutine lifecycle management during background face indexing to prevent interrupted scans and ensure accurate photo reassignments.
* **Timeline Header Spacing**: Corrected vertical padding and alignment around date headers in the primary timeline grid.

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures

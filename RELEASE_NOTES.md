# Release Notes — v4.1.0

### New Features
* Added **App Lock** in Settings → Privacy & Security to protect Photon Gallery and the media picker with Fingerprint / Biometrics or a 4-digit PIN, with configurable auto-lock timeouts (#42).
* Added **persistent Private Space storage (`.PhotonPrivateSpace`)** with a PIN-wrapped encryption key and encrypted manifest so hidden photos, videos, and your PIN survive app data clears and reinstalls.
* Upgraded the **Private Space grid** with two-finger pinch-to-zoom column sizing (`1–8`), Day / Month / Year date section headers, long-press drag-to-select with edge auto-scroll, Select All / Deselect All, and custom thumbnail corner radius.
* Upgraded the **Private Space viewer** with horizontal swipe paging, pinch & double-tap zoom, vertical swipe-to-dismiss, ambient blur background, and `FLAG_SECURE` window protection.

---

### Improvements
* Redesigned **Blur Viewer Background** into a smooth ambient color glow (`96×96` downsample + `72.dp` rectangle blur + vertical vignette scrim) so it elevates the photo without ghosting or mirroring edges (#45).
* Added smooth hero container-transform open/close animations to the **Editorial Mosaic** timeline grid.
* Updated the **Private Space** and **Person Detail** multi-selection bars to use segmented **Layered Tool Pills**.
* Added a Settings shortcut icon to the Search tab top bar.
* Added `v4.0.0` and `v3.0.0` release history to the in-app Update Log screen.

---

### Bug Fixes
* Fixed horizontal swiping not working in the image viewer when opening the first photo in Photos or Albums (#44).
* Fixed **Blur Viewer Background** appearing almost pure black in the full-screen viewer (#45).
* Fixed the Onboarding **Grant** button skipping the All Files Access permission prompt after granting media access.
* Fixed hiding media into Private Space under Scoped Storage when All Files Access is not granted by staging items for MediaStore delete confirmation.
* Fixed Settings category pages (including the About card) opening with a split/delayed transition and fixed predictive back navigation across Settings and Update Log.
* Fixed TopBar background glow and title flicker when opening and closing albums.
* Fixed video playback continuing in the background when the app is stopped and clamped double-tap zoom-out bounce.

---

### Translations
* Updated Italian translations (#43).

---

### Downloads & Assets
* **PhotonGallery-arm64-v8a.apk** — Recommended for 64-bit ARM devices (Android 12+)
* **PhotonGallery-universal.apk** — Compatible across all supported architectures

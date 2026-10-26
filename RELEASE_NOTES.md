# Release Notes — v3.0.0

## What's New

### People & Face Grouping (New Feature!)
Photon Gallery can now automatically recognize and group faces across your photos — running **100% locally on your device with complete privacy (no cloud uploads)**:
* Dedicated People Section: Browse identified people cards with customizable zoom (2 to 5 columns).
* Automatic Grouping: Discovers and groups recurring faces into individual identities offline.
* Smart Merge Suggestions: Recommends merging matching identities with side-by-side comparisons and quick confirmation.
* Names & Favorites: Assign names, mark favorites, or hide specific people from your library.
* Photo Count Filter: Filter out casual background faces (3+, 5+, 10+ photos) so only meaningful people appear.
* Face Tags in Photo Info: Tap any recognized person chip in the details sheet to view all photos of that person.

---

### Markup & Paint Tools in Editor (#20)
* Complete Drawing Suite: Annotate photos inside the built-in editor using pen, highlighter, neon brush, blur redaction, and geometric shapes (arrows, rectangles, circles).
* Interactive Text Annotations: Add movable, scalable, and rotatable text labels with custom styling and background fills.
* High-Resolution Export: Annotations and sketches render at full original image resolution upon export.

---

### Full-Screen Viewer & Timeline Navigation
* Pinch-to-Filmstrip Overview: Pinch in on any full-screen photo to view an interactive horizontal filmstrip of adjacent photos, and tap any photo to return to full screen.
* Equidistant Date Headers: Re-aligned date headers in the timeline grid with balanced spacing above and below so headers sit centered between date groups.
* Immersive Single-Tap View: Single-tap in the photo viewer now toggles both the top and bottom controls and the system status bar.

---

### Text & Document Search (OCR)
* Multi-Word Text Search: Completely restored full-text search across photos containing documents, bills, signs, and receipts, ensuring multi-word queries (e.g., "electric bill", "wifi password") return instant matches.
* Automatic Text Indexing: Newly taken or imported photos are automatically indexed for text search in the background.
* Accurate Relevance Ranking: Photos with matching recognized text are prioritized alongside exact filename matches.

---

### Privacy & Metadata Sanitization
* Byte-Level Sanitization: The "Strip metadata when sharing" option strips 100% of metadata bytes across all supported media formats, including JPEG, PNG, WebP, animated GIFs, and MP4/MOV videos.
* Location & Hardware Scrubbing: Completely eliminates all EXIF, XMP, IPTC records, MakerNotes, camera serial numbers, owner tags, and GPS coordinates.
* Video Privacy Protection: ISOBMFF user data boxes and QuickTime metadata are scrubbed, and recording/modification timestamps are cleared to prevent leaking coordinates or capture times.
* Orientation Baking: Images captured at tilted angles are automatically normalized into physical pixels before stripping so shared photos always display upright.

---

### Instant Update Notifications
* Launch Check: Promptly checks for GitHub releases when opening the app so you are notified as soon as a new build is available.
* Notification Alert: Displays an expansive in-app changelog dialog and posts a status bar notification to update at your convenience.

---

### Bug Fixes & Stability
* Direct / Quick Share Attachments (#17): Resolved an issue where photos shared via Direct Share / Quick Share to messaging apps (such as Google Messages) would not attach properly.
* `.nomedia` & Vault Media Refresh (#18): Gallery synchronization now immediately hides encrypted files and newly created `.nomedia` folders without requiring an app reinstall.
* Deletion Transition Crash (#19): Resolved a crash (`IllegalStateException: Cannot transition entry that is not in the back stack`) when deleting photos from within the viewer.
* Accurate Photo Counts: Corrected face count calculations under person chips and merge suggestion cards to strictly reflect unique photos.
* Destructive Action Prompts: Added safety confirmation dialogs before rescanning faces, deleting offline AI models, or rebuilding search indexes.

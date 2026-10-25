# Release Notes

## What's New

### People & Face Grouping (New Feature!)
Photon Gallery can now automatically recognize and group faces across your photos — running **100% locally on your device with complete privacy (no cloud uploads)**:
* **People Section**: Dedicated tab with clean portrait cards and customizable grid zoom (2 to 5 columns).
* **Automatic Clustering**: Discovers and clusters faces into individual identities.
* **Smart Merge Suggestions**: Recommends merging matching identities with side-by-side comparisons, match percentages, and quick dismiss/re-launch controls.
* **Names & Favorites**: Assign names, mark favorites, or hide specific faces from your library.
* **Photo Count Filter**: Filter out casual background faces (3+, 5+, 10+ photos) so only meaningful people appear.
* **Face Tags in Photo Info**: Tap any recognized person chip in the details sheet to see all photos of that person.

---

### Markup & Paint Tools in Editor (#20)
* **Drawing & Annotation**: Complete markup toolset inside the photo editor featuring pen, highlighter, neon brush, blur redaction, and geometric shapes (arrows, rectangles, circles).
* **Interactive Text**: Add movable, scalable, and rotatable text annotations with custom styling.
* **High-Res Export**: Annotations and drawings render at original image resolution upon export.

---

### Instant Update Notifications
* **Prompt on Launch**: Checks for GitHub releases immediately when opening the app (with a 5-minute cooldown) so you're notified as soon as a new build is out.
* **Notification Drawer Alert**: Displays an in-app dialog and posts a status bar notification so you can update at your convenience.

---

### Bug Fixes & Stability
* **Direct / Quick Share Attachments (#17)**: Fixed an issue where photos shared via Direct Share / Quick Share to messaging apps (e.g., Google Messages) would not attach properly.
* **`.nomedia` & Vault Media Refresh (#18)**: Fixed gallery synchronization so encrypted files and newly added `.nomedia` folders are immediately hidden from the gallery without requiring an app reinstall.
* **Deletion Transition Crash (#19)**: Resolved a `java.lang.IllegalStateException: Cannot transition entry that is not in the back stack` crash when deleting photos in the viewer.
* **Accurate Photo Counts**: Corrected face count calculations under person chips and merge suggestion cards to strictly reflect unique photos.
* **Safety Confirmation Prompts**: Added confirmation dialogs before executing critical actions (rescan/clear faces, delete AI models, or rebuild search indexes).
* **Immersive Full-Screen**: Single-tap in the photo viewer now cleanly toggles both the UI controls and the system status bar.

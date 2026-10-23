# On-Device AI Privacy Card & Model Source Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a non-technical privacy explainer card at the top of the "Smart Search & OCR" settings screen, and add direct open-source model repository links to the Semantic Search (MobileCLIP) and Face Recognition (MobileFaceNet) cards.

**Architecture:** Render a dedicated Material 3 Expressive `Surface` banner at the top of the `"Smart Search & OCR"` section in `SettingsScreen.kt`. In both model cards, render a clickable source attribution row using `LocalUriHandler` and `ic_ms_open_in_new` pointing to their respective open-source repositories.

**Tech Stack:** Jetpack Compose, Material 3 Expressive (`MaterialExpressiveTheme`, `ShapeLarge`, `IconSizeTokens`), Android URI handler.

**Spec:** User request and approved design proposal in chat.

## Global Constraints

- Material 3 Expressive compliance: No raw hex/Color literals (use `MaterialTheme.colorScheme.*`), 10-tier shape scale only (`ShapeLarge`), Material Symbols Rounded icons only (`ic_ms_*`), Google Fonts typography only.
- Strict local branch: Do NOT push to remote (`leave the face branch local only`).
- No release creation: Do NOT create any release or tag.

## Review Focus

- Non-technical clarity: Ensure wording avoids jargon, confirms 100% on-device processing, single-time download, and zero cloud uploads.
- Valid URLs: Verify that GitHub and Hugging Face URLs are well-formed and open properly with `LocalUriHandler`.
- Layout responsiveness: Ensure the banner and source link rows fit nicely on 393dp target width without text truncation or overflow.

---

### Task 1: Add Top Privacy Explainer Card in Smart Search & OCR

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt:905-925`

**Interfaces:**
- Consumes: `MaterialTheme.colorScheme`, `ShapeLarge`, `ShapeFull`, `IconSizeTokens.M`, `R.drawable.ic_ms_shield_lock`
- Produces: Visual privacy explainer card rendered at top of `"Smart Search & OCR"` section

- [ ] **Step 1: Add top privacy banner card at the start of `"Smart Search & OCR"` section**
Add the `Surface` banner before `SettingsGroupCard(title = "Local Text Search (OCR)", ...)`:
- Title: "100% On-Device & Open Source"
- Body: "These AI models are open source and completely optional. You only need to download them once. All photo scanning, text recognition, and face clustering run entirely on your device—no images or personal data will ever be sent to any cloud or external server."

- [ ] **Step 2: Verify Kotlin compilation**
Run: `.\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt
git commit -m "feat(settings): add on-device AI privacy banner card in Smart Search & OCR"
```

---

### Task 2: Add Model Source Repository Links to Model Cards

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt:1115-1175` (MobileCLIP semantic search card)
- Modify: `app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt:1380-1430` (MobileFaceNet face recognition card)

**Interfaces:**
- Consumes: `LocalUriHandler.current`, `LocalHapticFeedback.current`, `R.drawable.ic_ms_open_in_new`, `IconSizeTokens.S`, `MaterialTheme.colorScheme.primary`
- Produces: Clickable source link rows opening GitHub and Hugging Face model pages

- [ ] **Step 1: Add clickable GitHub source link to Smart Search (MobileCLIP) card**
Add a row with `ic_ms_open_in_new` and text "Model source: GitHub (MobileCLIP)" opening `https://github.com/Bn5prS/Photon_Gallery/tree/master/models`.

- [ ] **Step 2: Add clickable Hugging Face source link to Face Recognition (MobileFaceNet) card**
Add a row with `ic_ms_open_in_new` and text "Model source: Hugging Face (InsightFace MobileFaceNet)" opening `https://huggingface.co/deepghs/insightface`.

- [ ] **Step 3: Verify Kotlin compilation and run tests**
Run: `.\gradlew.bat testDebugUnitTest --tests com.inferno.gallery.data.db.SearchOcrClassificationTest`
Expected: BUILD SUCCESSFUL and tests PASS

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/inferno/gallery/ui/SettingsScreen.kt
git commit -m "feat(settings): add open-source repository links to AI model cards"
```

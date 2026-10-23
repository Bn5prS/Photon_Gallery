# Google Lens Button Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Google Lens button at the far-right end of the floating bottom dock in the image viewer to enable 1-tap visual search using the official Google Files icon.

**Architecture:** Create an extracted Material Symbols vector drawable (`ic_ms_google_lens.xml`), implement an isolated `GoogleLensUtils` helper that resolves intents across Google Lens (`com.google.ar.lens`) and Google App (`com.google.android.googlequicksearchbox`) with user-friendly fallbacks, and integrate the action into `DetailScreen.kt`'s bottom floating dock for non-video media.

**Tech Stack:** Jetpack Compose, Material 3 Expressive, Android Vector Drawable, Kotlin Coroutines, JUnit / Robolectric / MockK.

**Spec:** [docs/superpowers/specs/2026-10-23-google-lens-button-design.md](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/docs/superpowers/specs/2026-10-23-google-lens-button-design.md)

## Global Constraints

- Never use raw hex colors in UI composables; resolve colors via `MaterialTheme.colorScheme.*`.
- Use only Material Symbols Rounded drawables in `res/drawable/ic_ms_<snake_case>.xml`.
- Preserve min 48x48dp interactive touch targets (`minimumInteractiveComponentSize`).
- Keep all commit messages concise, plain, and factual per conventional commits (no hype or marketing adjectives).

## Review Focus

1. **Non-installed Google Lens package**: Launching on a device with neither Google Lens nor Google App must not crash; it should display a clean toast.
2. **Video media handling**: When the viewer displays a video, the Lens button must remain hidden and the dock must preserve its standard 4-button layout.
3. **Narrow screen overflow**: Ensure the 5-button dock layout does not clip or overflow on 360dp width screens.
4. **Content URI permission grant**: The intent must grant `FLAG_GRANT_READ_URI_PERMISSION` so external Google Lens can read the target media file.
5. **Trash folder guard**: When browsing the Trash album, the Lens button must be hidden, showing only Restore and Permanently Delete actions.

---

### Task 1: Vector Asset (`ic_ms_google_lens.xml`)

**Files:**
- Create: `app/src/main/res/drawable/ic_ms_google_lens.xml`

**Interfaces:**
- Produces: `R.drawable.ic_ms_google_lens` (24dp baseline, 960x960 viewport).

- [ ] **Step 1: Create vector drawable `ic_ms_google_lens.xml`**

Create `app/src/main/res/drawable/ic_ms_google_lens.xml` using the exact path data extracted from Google Files APK:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960"
    android:tint="?attr/colorControlNormal">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M480,640q-50,0-85-35t-35-85t35-85t85-35t85,35t35,85t-35,85t-85,35ZM663.5,776.5Q640,753 640,720t23.5-56.5T720,640t56.5,23.5T800,720t-23.5,56.5T720,800t-56.5-23.5ZM280,840q-66,0-113-47T120,680V600h80v80q0,33 23.5,56.5T280,760H480v80H280ZM760,520V360q0-33-23.5-56.5T680,280H280q-33,0-56.5,23.5T200,360V480H120V360q0-66 47-113t113-47h80l40-80H560l40,80h80q66,0 113,47t47,113V520H760Z"/>
</vector>
```

- [ ] **Step 2: Verify vector drawable compiles**

Run: `./gradlew :app:processDebugResources`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit vector asset**

```bash
git add app/src/main/res/drawable/ic_ms_google_lens.xml
git commit -m "feat(ui): add Google Lens Material Symbol vector drawable"
```

---

### Task 2: Google Lens Intent Utility & Fallback Logic

**Files:**
- Create: `app/src/main/java/com/inferno/gallery/ui/utils/GoogleLensUtils.kt`
- Create: `app/src/test/java/com/inferno/gallery/ui/utils/GoogleLensUtilsTest.kt`

**Interfaces:**
- Produces: `GoogleLensUtils.createLensIntent(uri: Uri): Intent`
- Produces: `GoogleLensUtils.launch(context: Context, uri: Uri)`

- [ ] **Step 1: Write unit tests for GoogleLensUtils**

Create `app/src/test/java/com/inferno/gallery/ui/utils/GoogleLensUtilsTest.kt` testing:
1. Intent action is `Intent.ACTION_VIEW`.
2. MIME type is `image/*` or detected image type.
3. URI read permission flag `Intent.FLAG_GRANT_READ_URI_PERMISSION` is present.
4. Primary package target is `com.google.ar.lens`.

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.utils.GoogleLensUtilsTest"`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `GoogleLensUtils`**

Implement `app/src/main/java/com/inferno/gallery/ui/utils/GoogleLensUtils.kt`:
```kotlin
package com.inferno.gallery.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object GoogleLensUtils {
    const val LENS_PACKAGE = "com.google.ar.lens"
    const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    fun createLensIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            setPackage(LENS_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun launch(context: Context, uri: Uri) {
        val lensIntent = createLensIntent(uri)
        try {
            context.startActivity(lensIntent)
            return
        } catch (_: Exception) {}

        // Fallback to Google App
        try {
            val googleAppIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                setPackage(GOOGLE_APP_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(googleAppIntent)
            return
        } catch (_: Exception) {}

        Toast.makeText(context, "Google Lens is not installed", Toast.LENGTH_SHORT).show()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.inferno.gallery.ui.utils.GoogleLensUtilsTest"`
Expected: PASS

- [ ] **Step 5: Commit utility**

```bash
git add app/src/main/java/com/inferno/gallery/ui/utils/GoogleLensUtils.kt app/src/test/java/com/inferno/gallery/ui/utils/GoogleLensUtilsTest.kt
git commit -m "feat(viewer): implement GoogleLensUtils for visual search intents"
```

---

### Task 3: Bottom Action Dock Integration in DetailScreen

**Files:**
- Modify: `app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt`

**Interfaces:**
- Consumes: `R.drawable.ic_ms_google_lens`
- Consumes: `GoogleLensUtils.launch(context, uri)`

- [ ] **Step 1: Update Floating Bottom Action Dock in `DetailScreen.kt`**

1. Update dock container width from `fillMaxWidth(0.82f).widthIn(min = 280.dp, max = 340.dp)` to:
   ```kotlin
   modifier = Modifier
       .fillMaxWidth(0.88f)
       .widthIn(min = 280.dp, max = 360.dp)
       .height(54.dp)
   ```
2. In the normal viewer mode (outside Trash), add the Google Lens button after Delete (Order A: Far-Right):
   ```kotlin
   // Google Lens (Images only)
   if (currentItem != null && !currentItem.isVideo) {
       FilledTonalIconButton(
           onClick = {
               val targetUri = resolvedCurrentUri ?: currentItem.uri
               com.inferno.gallery.ui.utils.GoogleLensUtils.launch(context, targetUri)
           },
           enabled = currentItem.localExists,
           colors = IconButtonDefaults.filledTonalIconButtonColors(
               containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
               contentColor = MaterialTheme.colorScheme.onSurface
           ),
           modifier = Modifier.size(40.dp)
       ) {
           Icon(
               imageVector = ImageVector.vectorResource(R.drawable.ic_ms_google_lens),
               contentDescription = "Google Lens",
               modifier = Modifier.size(22.dp)
           )
       }
   }
   ```

- [ ] **Step 2: Run verification and tests**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL and all tests pass

- [ ] **Step 3: Commit DetailScreen changes**

```bash
git add app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt
git commit -m "feat(viewer): add Google Lens button to bottom action dock (fixes #16)"
```

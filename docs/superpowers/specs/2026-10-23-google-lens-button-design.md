# Google Lens Button Feature Design

## 1. Intent & Context
- **Issue Reference**: [GitHub Issue #16](https://github.com/Bn5prS/Photon_Gallery/issues/16)
- **Objective**: Provide quick access to Google Lens visual search directly within the media detail viewer ([DetailScreen.kt](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt)).

---

## 2. Goals & Non-Goals
### Goals
- Add a Google Lens button to the floating bottom action dock in [DetailScreen.kt](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt).
- Place the button on the far-right side of the unified tools pill: `[ Share · Edit · Favorite · Delete · Lens ]`.
- Utilize the exact official Google Lens vector drawable extracted from Google Files (`drawable/gs_google_lens_2_vd_theme_24`), added to `res/drawable/ic_ms_google_lens.xml`.
- Ensure the button is only displayed for static images (`!currentItem.isVideo`) and omitted in Trash view.
- Launch Google Lens using an Android `Intent` with read URI permissions (`FLAG_GRANT_READ_URI_PERMISSION`), falling back to Google App or a user toast if Google Lens is not installed.
- Ensure the dock width dynamically accommodates 5 buttons on images (up to ~340-360dp) and smoothly transitions to 4 buttons on videos.

### Non-Goals
- Cloud processing or third-party web search SDKs (100% offline local intent execution).
- Displaying Lens action for video media items.

---

## 3. Approaches Considered & Selected
1. **Approach 1: Standalone Floating Companion Button Beside Dock**  
   - Description: A separate circular button placed next to the tools pill, similar to the video player's `[ Slider ] ( Play )` layout.  
   - Tradeoff: Creates visual asymmetry and distraction over full-bleed photo content.  
   - Decision: **Rejected**.

2. **Approach 2: Unified Pill with Lens Placed Before Delete**  
   - Description: `[ Share · Edit · Favorite · Lens · Delete ]`.  
   - Tradeoff: Keeps destructive delete at the absolute edge, but does not fulfill the user's explicit request for far-right placement.  
   - Decision: **Rejected**.

3. **Approach 3: Unified Pill with Lens on the Far Right (Selected)**  
   - Description: `[ Share · Edit · Favorite · Delete · Lens ]`.  
   - Rationale: Fully integrates into the existing Material 3 Expressive pill dock without visual fragmentation. Cleanly appends Lens at the far-right end.  
   - Decision: **Selected by user**.

---

## 4. Technical Architecture & Implementation
1. **Vector Asset**:
   - File: `app/src/main/res/drawable/ic_ms_google_lens.xml`
   - Attributes: 24dp baseline, 960x960 viewport, `?attr/colorControlNormal` tinting.
   - Path Data from Google Files:
     `M480,640q-50,0-85-35t-35-85t35-85t85-35t85,35t35,85t-35,85t-85,35ZM663.5,776.5Q640,753 640,720t23.5-56.5T720,640t56.5,23.5T800,720t-23.5,56.5T720,800t-56.5-23.5ZM280,840q-66,0-113-47T120,680V600h80v80q0,33 23.5,56.5T280,760H480v80H280ZM760,520V360q0-33-23.5-56.5T680,280H280q-33,0-56.5,23.5T200,360V480H120V360q0-66 47-113t113-47h80l40-80H560l40,80h80q66,0 113,47t47,113V520H760Z`

2. **UI Dock in [DetailScreen.kt](file:///c:/Users/Deepak/Desktop/Projects/Photon%20Gallery/app/src/main/java/com/inferno/gallery/ui/DetailScreen.kt)**:
   - Surface container width: `fillMaxWidth(0.88f).widthIn(min = 280.dp, max = 360.dp)`.
   - Far-right button: `FilledTonalIconButton` with `ic_ms_google_lens`.
   - Content description: `"Google Lens"`.
   - Visibility: `if (!currentItem.isVideo)`.

3. **Intent Dispatch**:
   - Priority 1: `Intent(Intent.ACTION_VIEW)` with `setDataAndType(uri, "image/*")`, `setPackage("com.google.ar.lens")`.
   - Priority 2 (Fallback): `Intent(Intent.ACTION_VIEW)` with `setDataAndType(uri, "image/*")`, `setPackage("com.google.android.googlequicksearchbox")`.
   - Priority 3 (Fallback): Toast notification informing the user that Google Lens is not installed on their device.

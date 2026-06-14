<p align="center">
  <img src="screenshots/logo.png" width="160" alt="Photon Gallery Logo" />
</p>

<h1 align="center">Photon Gallery</h1>

<p align="center">
  <a href="https://github.com/Bn5prS/Photon_Gallery/stargazers"><img src="https://img.shields.io/github/stars/Bn5prS/Photon_Gallery?style=flat&logo=github&color=blue" alt="GitHub Stars" /></a>
  <a href="https://github.com/Bn5prS/Photon_Gallery/releases"><img src="https://img.shields.io/github/downloads/Bn5prS/Photon_Gallery/total?style=flat&logo=github&color=green" alt="Downloads" /></a>
  <a href="https://github.com/Bn5prS/Photon_Gallery/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat" alt="License" /></a>
  <a href="https://github.com/Bn5prS/Photon_Gallery"><img src="https://profile-counter.glitch.me/Bn5prS-Photon_Gallery/count.svg" alt="Views" /></a>
  <a href="https://github.com/Bn5prS/Photon_Gallery"><img src="https://img.shields.io/github/repo-size/Bn5prS/Photon_Gallery?style=flat&color=orange" alt="Repo Size" /></a>
</p>

<p align="center">
  <strong>A sleek Android media gallery app built with Jetpack Compose and Material 3 design.</strong><br>
  <strong>Equipped with private smart search and integrated cloud backups.</strong>
</p>

<p align="center">
  <a href="https://github.com/Bn5prS/Photon_Gallery/releases/latest/download/app-release.apk"><img src="https://img.shields.io/badge/Download-Latest%20APK-3DDC84?style=flat&logo=android&logoColor=white" alt="Download Latest APK" /></a>
</p>

---

## Features

- **Smooth Media Browsing**: Browse all your local photos and videos smoothly with highly responsive grids, fluid image loading, and fast interactions.
- **Smart On-Device Search**: Secure, offline search powered by on-device MobileCLIP semantic models and optical character recognition (OCR) for prompt-based search.
- **Material 3 Expressive Design**: Modern layout utilizing shape-morphing animations, emphasized variable font weights, and physics-based spring motions.
- **Local Album Management**: Create folders directly in public device storage and manage a dedicated Recycle Bin next to standard folders.
- **Auto-Hiding Pill Dock**: Navigation toolbar that collapses dynamically on down-scroll and re-appears on minor up-scroll to maximize screen space.
- **Telegram Cloud Backup**: Synchronize selected media folders securely to your private Telegram chat using a personal Telegram Bot. Includes Wi-Fi only upload constraints, low-battery pausing, and location metadata stripping for enhanced privacy.
- **Collage Builder**: Select multiple photos to instantly create custom photo collages.

---

## Screenshots

| Home Grid View | Smart Search Results |
| :---: | :---: |
| <img src="screenshots/home.jpg" width="360" alt="Home Grid View" /> | <img src="screenshots/search.jpg" width="360" alt="Smart Search" /> |

| Telegram Cloud Backup | Advanced Settings |
| :---: | :---: |
| <img src="screenshots/cloud.jpg" width="360" alt="Telegram Backup" /> | <img src="screenshots/settings.jpg" width="360" alt="Settings" /> |

---

## Getting Started

### Prerequisites
- Android SDK 31+
- Java JDK 17
- Gradle 8.x+

### Build and Deploy
1. Clone the repository:
   ```bash
   git clone https://github.com/inferno/photon-gallery.git
   ```
2. Compile and check build:
   ```bash
   ./gradlew compileDebugKotlin
   ```
3. Assemble the signed release APK:
   ```bash
   ./gradlew assembleRelease
   ```
4. Deploy the APK to your connected phone:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
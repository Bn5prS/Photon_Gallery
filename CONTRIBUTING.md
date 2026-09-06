# Contributing to Photon Gallery

Thank you for your interest in contributing to **Photon Gallery**! We are building a lightning-fast, privacy-first, on-device Android gallery with a stunning **Material 3 Expressive** design.

Photon Gallery is licensed under the **GNU General Public License v3 (GPLv3)**. By contributing, you agree that your contributions will be licensed under the same terms.

---

## 🛠️ Development Setup

### Prerequisites
- **JDK 17** (Temurin recommended)
- **Android Studio Meerkat** or newer
- **Android SDK** with target API 37 (Android 15+) and min API 31 (Android 12+)

### Clone & Build
```bash
git clone https://github.com/Bn5prS/Photon_Gallery.git
cd Photon_Gallery

# Compile Kotlin sources
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 📐 Architecture & Standards

Photon Gallery follows modern Android development principles and a strict Material 3 Expressive design system:

### 1. Architectural Stack
- **Architecture**: MVVM with reactive UI via Kotlin Coroutines & StateFlow.
- **Database**: Room (SQLite + FTS4/5) with keyset/cursor pagination (`MediaCursorPagingSource`) for $O(\log N)$ scrolling efficiency.
- **Images & Thumbnails**: Coil 3 with hardware bitmap pooling and unified memory caching. Avoid raw unpooled `Bitmap` allocations.
- **Machine Learning**: 100% on-device (ONNX Runtime, ML Kit OCR, ArcFace/GhostFaceNet face clustering). Never make outbound cloud calls for media processing.

### 2. Official Material 3 Expressive Design Rules
Our UI design is governed by strict M3 Expressive tokens:
- **No Raw Hex/Color**: Never use `Color(0x...)` in UI screens. Resolve colors dynamically via `MaterialTheme.colorScheme.<role>`.
- **5-Tier Elevation Hierarchy**: Use `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, and `surfaceContainerHighest`.
- **Official Shape Scale**: Only radii `[0.dp, 4.dp, 8.dp, 12.dp, 16.dp, 20.dp, 28.dp, 32.dp, 48.dp, Full]` are allowed.
- **Typography**: Strictly use the bundled **Google Sans Flex** variable font (`res/font/google_sans_flex.ttf`). No other fonts are permitted.
- **Icons**: Strictly **Material Symbols Rounded** (`res/drawable/ic_ms_*.xml`). Tabler Icons, generic SVGs, or old material icon packs are prohibited.
- **Motion**: Use `MaterialTheme.motionScheme` springs (`fastSpatialSpec`, `defaultSpatialSpec`, `slowSpatialSpec`) or `MotionTokens.kt`.

---

## 🧪 Testing & Quality Assurance

Before submitting any Pull Request, ensure that all checks pass:

```bash
# 1. Run Android Lint
./gradlew lintDebug

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Verify debug build
./gradlew assembleDebug
```

All new features and bug fixes should include unit tests in `app/src/test/` covering ViewModels, DAOs, and algorithmic components.

---

## 📝 Commit & PR Guidelines

### Conventional Commits
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. Format your commit messages as:

```
<type>(<scope>): <short summary>
```

Common types:
- `feat`: A new user-facing feature or enhancement.
- `fix`: A bug fix.
- `perf`: A code change that improves performance (scrolling, memory, queries).
- `refactor`: Code change that neither fixes a bug nor adds a feature.
- `chore`: Maintenance, dependencies, release scripts.
- `test`: Adding or correcting tests.
- `docs`: Documentation changes.

### PR Submission Checklist
1. Create a feature branch from `main`: `git checkout -b feature/my-feature`.
2. Keep PRs focused on a single responsibility.
3. Verify that all unit tests and lint checks pass.
4. Fill out the provided [Pull Request Template](.github/pull_request_template.md).
5. Attach screenshots or screen recordings for any UI changes.

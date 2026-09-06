## Description
<!-- Describe the changes made and the motivation behind them. -->

## Type of Change
- [ ] 🐛 Bug fix (non-breaking change fixing an issue)
- [ ] ✨ New feature (non-breaking change adding functionality)
- [ ] ⚡ Performance improvement (scrolling, memory, queries)
- [ ] 🎨 UI / Theming (Material 3 Expressive polish)
- [ ] ♻️ Code refactoring or architectural cleanup
- [ ] 🧪 Tests (unit, integration, or UI tests)
- [ ] 📚 Documentation update

## Material 3 Expressive Compliance Checklist
- [ ] **Colors**: Zero hardcoded `Color(0x...)` — all colors resolved through `MaterialTheme.colorScheme.<role>`.
- [ ] **Surface Elevation**: Follows the 5-tier elevation hierarchy (`surfaceContainerLowest` to `surfaceContainerHighest`).
- [ ] **Shapes**: Only official radii used (`[0, 4, 8, 12, 16, 20, 28, 32, 48, Full]`).
- [ ] **Typography**: Uses official bundled **Google Sans Flex** variable font hierarchy (`res/font/google_sans_flex.ttf`).
- [ ] **Icons**: Sourced from `res/drawable/ic_ms_*` (Material Symbols Rounded).

## Verification & Testing
- [ ] `./gradlew lintDebug` passes with no new errors.
- [ ] `./gradlew testDebugUnitTest` passes.
- [ ] `./gradlew assembleDebug` compiles successfully.
- [ ] Tested on physical device or emulator.

## Screenshots / Screen Recordings (if applicable)
<!-- Attach screenshots or GIFs showing the change in both Light and Dark themes. -->

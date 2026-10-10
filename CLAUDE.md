# Aviation Watch: notes for contributors and AI assistants

Wear OS app for Samsung Galaxy Watch4 that mirrors the Garmin D2 Air X15 pilot
features. Roadmap and feature map: `docs/FEATURES.md`.

## Commands
- `./gradlew :core:aviation:test`: fast JVM tests (no Android SDK required)
- `./gradlew build`: full build, unit tests, lint (what CI runs)
- `python3 tools/build_airports.py`: generate the airport DB asset (git-ignored) before local builds

## Conventions
- Aviation math and models go in `core/aviation` (pure Kotlin, unit-tested). Keep Android out of it.
- UI is Compose for Wear OS **Material 3** (`androidx.wear.compose.material3`), not phone Material and not Wear M2.5.
  Screens use `ScrollingScreen` / `MessageScreen` from `ui/common`; `AppScaffold` lives in `AviationWatchApp`.
- Dependencies go through `gradle/libs.versions.toml`.
- Keep it zero-cost: no paid APIs, no API keys committed, no Firebase.
- Engine-time files must stay byte-compatible with the Enginetime web app (`EnginetimeFormat`, version 2);
  see docs/ENGINETIME.md. OAuth client ID/secret come from Gradle properties / CI secrets, never the repo.
- Units: SI internally. Display ft / NM / kt / hPa (+inHg) as pilots expect.

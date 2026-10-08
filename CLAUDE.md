# Aviation Watch: notes for contributors and AI assistants

Wear OS app for Samsung Galaxy Watch4 that mirrors the Garmin D2 Air X15 pilot
features. Roadmap and feature map: `docs/FEATURES.md`.

## Commands
- `./gradlew :core:aviation:test`: fast JVM tests (no Android SDK required)
- `./gradlew build`: full build, unit tests, lint (what CI runs)

## Conventions
- Aviation math and models go in `core/aviation` (pure Kotlin, unit-tested). Keep Android out of it.
- UI is Compose for Wear OS (`androidx.wear.compose.*`), not phone Material.
- Dependencies go through `gradle/libs.versions.toml`.
- Keep it zero-cost: no paid APIs, no API keys committed, no Firebase.
- Units: SI internally. Display ft / NM / kt / hPa (+inHg) as pilots expect.

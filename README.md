# Aviation Watch ✈️⌚

An aviation operations app for the **Samsung Galaxy Watch4** (Wear OS), inspired by
the pilot features of the **Garmin D2 Air X15**: Nearest, Direct-To with CDI,
METAR/TAF weather, barometric altimeter, Zulu time, flight timers, automatic
flight logging and pulse/SpO₂ monitoring.

> ⚠️ **Supplemental use only.** This app is meant to help with situational awareness.
> It is not certified and must never be used as a primary means of navigation,
> altitude or weather information.

## Status

| Phase | Scope | State |
|------|-------|-------|
| 1 | Project setup, CI, Zulu clock, baro altimeter | ✅ in place |
| 2 | GPS, Nearest airports, Direct-To / CDI, airport search & info, Material 3 UI | ✅ in place |
| 3 | METAR / TAF, runway wind, density altitude, QNH sync | ✅ in place |
| 4 | Timers, auto flight log, GPX export | planned |
| 5 | Heart rate / SpO₂, hypoxia alerts | planned |
| 6 | Tiles & complications | planned |

Full feature mapping against the D2 Air X15: [docs/FEATURES.md](docs/FEATURES.md).

## Zero cost

Everything here is free: open-source tools, GitHub Actions (free for public repos),
free aviation data sources with no API keys, and sideloading onto your own watch via
ADB instead of a paid store account. Details in [docs/SETUP.md](docs/SETUP.md#costs).

## Project layout

```
aviation-watch/
├── core/aviation/        Pure Kotlin: navigation, Direct-To/CDI, atmosphere, airport CSV, units (JVM unit tests)
├── wear/                 Wear OS app (Compose for Wear OS, Material 3)
├── tools/                Data scripts (airport database builder)
├── docs/                 Features, architecture, setup, data sources
└── .github/              CI workflow, Dependabot, issue templates
```

## Quick start

```bash
./gradlew :core:aviation:test    # fast logic tests, no Android SDK needed
./gradlew build                  # full build: tests, lint, APKs (needs Android SDK)
./gradlew :wear:installDebug     # install on a connected watch (ADB)
```

Each push builds a debug APK in GitHub Actions. Download it from the run's
**Artifacts** section. See [docs/SETUP.md](docs/SETUP.md) to install it on the watch.

## Docs

- [FEATURES.md](docs/FEATURES.md): D2 Air X15 → Galaxy Watch4 feature map and roadmap
- [ARCHITECTURE.md](docs/ARCHITECTURE.md): modules, layers, tech choices
- [SETUP.md](docs/SETUP.md): dev environment, costs, installing on the watch
- [DATA_SOURCES.md](docs/DATA_SOURCES.md): free aviation data and licensing

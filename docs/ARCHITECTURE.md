# Architecture

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:core:aviation` | Kotlin/JVM library | Navigation math, ISA atmosphere, units, flight category, nearest-airport search. No Android deps → fast unit tests. |
| `:wear` | Android application | Wear OS UI (Compose for Wear OS), sensors, location, networking, tiles, complications. |

Planned as the app grows:

| Module | Purpose |
|---|---|
| `:core:data` | Airport DB loader, METAR/TAF client, caching (Room/DataStore) |
| `:wear:tiles` (or package in `:wear`) | Tiles & complication data sources |

## Layers inside `:wear`

```
ui/<feature>/      Composable screens (+ ViewModel when state gets non-trivial)
sensors/           Flows wrapping Android sensors (barometer, later heart rate)
location/          (phase 2) GPS location → Flow<Fix>
data/              (phase 2-3) repositories: airports asset, weather API
services/          (phase 4) foreground service for flight logging
tiles/, complications/  (phase 6)
```

Data flows one way. Sensors and repositories expose Kotlin `Flow`s, ViewModels
combine them using `core` functions, and screens render plain state.

## Tech choices

- **Kotlin + Jetpack Compose for Wear OS** (`androidx.wear.compose`), Google's
  recommended stack for Wear OS 3+. Round-screen components (`ScalingLazyColumn`,
  `TimeText`, swipe-to-dismiss navigation) come out of the box.
- **minSdk 30**, the Wear OS 3 level the Galaxy Watch4 launched with. The watch now
  runs newer Wear OS versions. **targetSdk / compileSdk 36**.
- **Standalone app** (`com.google.android.wearable.standalone=true`): no phone
  companion app needed. Network goes through the phone's Bluetooth link, Wi-Fi or LTE.
- **Version catalog** (`gradle/libs.versions.toml`) for all dependencies.
  Dependabot proposes updates monthly.
- **No paid services**, no API keys, no Firebase. See `DATA_SOURCES.md`.

## Battery considerations

- Show GPS and sensors only while a screen is visible
  (`collectAsStateWithLifecycle`).
- The flight-log service (phase 4) will use a low GPS rate (e.g. 5 s) and batched
  barometer readings.
- Cache weather and refresh it on demand, not by polling.

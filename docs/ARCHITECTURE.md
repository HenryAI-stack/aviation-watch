# Architecture

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:core:aviation` | Kotlin/JVM library | Navigation math, Direct-To solution and CDI scaling, ISA atmosphere, units, flight category, airport CSV parsing, nearest-airport search, display formatting. No Android deps → fast unit tests. |
| `:wear` | Android application | Wear OS UI (Compose for Wear OS), sensors, location, networking, tiles, complications. |

Planned as the app grows:

| Module | Purpose |
|---|---|
| `:core:data` | Airport DB loader, METAR/TAF client, caching (Room/DataStore) |
| `:wear:tiles` (or package in `:wear`) | Tiles & complication data sources |

## Layers inside `:wear`

```
ui/<feature>/      Composable screens (+ ViewModel when state gets non-trivial)
ui/common/         ScrollingScreen (ScreenScaffold + TransformingLazyColumn), MessageScreen
sensors/           Flows wrapping Android sensors (barometer, later heart rate)
location/          LocationManager GPS → Flow<Fix> (kt, magnetic variation), permission gate
data/              AirportRepository (bundled CSV assets), WeatherRepository (aviationweather.gov, 5 min cache),
                   DirectToStore, AltimeterSettings
enginetime/        EngineLogStore (prefs + pending/sent files), EnginetimeAuth (Google device sign-in),
                   DriveClient (appDataFolder upload), EnginetimeSync (WorkManager, network constraint)
services/          (phase 4) foreground service for flight logging
tiles/, complications/  (phase 6)
```

Data flows one way. Sensors and repositories expose Kotlin `Flow`s, ViewModels
combine them using `core` functions, and screens render plain state.

## Tech choices

- **Kotlin + Jetpack Compose for Wear OS, Material 3** (`androidx.wear.compose.material3`),
  Google's recommended stack. `AppScaffold` (one per app, provides `TimeText`) +
  `ScreenScaffold` per screen, `TransformingLazyColumn` lists, swipe-to-dismiss navigation.
- **Location from the platform `LocationManager`** (watch GPS), no Google Play services
  dependency. Directions are shown magnetic using the on-device WMM model
  (`GeomagneticField`), as on aircraft instruments.
- **minSdk 30**, the Wear OS 3 level the Galaxy Watch4 launched with. The watch now
  runs newer Wear OS versions. **targetSdk 36** (Wear OS 6), **compileSdk 37** (needed by current AndroidX).
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
- Weather is fetched when a weather screen opens or on Refresh (5 minute cache), never polled.

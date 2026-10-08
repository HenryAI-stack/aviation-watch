# Setup

## Costs

Nothing in this project needs to cost money:

| Item | Cost | Notes |
|---|---|---|
| GitHub repository | Free | |
| GitHub Actions CI | Free | Unlimited minutes on standard runners for **public** repos. A private repo on the Free plan gets 2,000 min/month, and the default $0 spending limit blocks any overage charge. |
| Android Studio, SDK, Gradle, Kotlin | Free | |
| Airport data (OurAirports) | Free | Public domain |
| Weather (aviationweather.gov) | Free | No API key |
| Installing on your own watch | Free | ADB over Wi-Fi, no store account needed |
| Google Play publishing | $25 one-time | **Optional**, only if you want to publish |
| Samsung Galaxy Store publishing | Free | Optional |

Avoid: paid map tile APIs, paid weather APIs, Firebase paid tiers. None are used.

## Development environment

1. Install [Android Studio](https://developer.android.com/studio) (latest stable).
2. Open the repository folder. Android Studio picks up the Gradle wrapper and
   creates `local.properties` with your SDK path. That file is git-ignored.
3. In SDK Manager, install **Android SDK Platform 35**.
4. Optional: create a **Wear OS Small Round** emulator (Device Manager → Wear OS).

Command line:

```bash
./gradlew :core:aviation:test   # pure Kotlin tests
./gradlew build                 # everything CI runs
```

## Install on the Galaxy Watch4 (free, no store)

1. On the watch, go to **Settings → About watch → Software information** and tap
   **Software version** 5 times. This turns on Developer options.
2. Go to **Settings → Developer options** and turn on **ADB debugging** and
   **Wireless debugging**. Watch and computer must be on the same Wi-Fi.
3. In **Wireless debugging**, tap **Pair new device** and note the IP:port and code. Then:
   ```bash
   adb pair <ip>:<pair-port>        # enter the pairing code
   adb connect <ip>:<port>          # port shown on the Wireless debugging screen
   ```
4. Install:
   ```bash
   ./gradlew :wear:installDebug
   # or, with an APK downloaded from GitHub Actions artifacts:
   adb install -r wear-debug.apk
   ```

`adb` comes with Android Studio (`<sdk>/platform-tools`). It is also available
standalone as "SDK Platform-Tools".

## Release signing (later)

Create your own keystore locally and **never commit it**. `*.jks` and
`keystore.properties` are git-ignored. Signing config will be added when there is
something worth releasing.

## Airport database

The app reads `airports.csv`, `runways.csv` and `frequencies.csv` from
`wear/src/main/assets/`. They are **generated, not committed**:

- **CI** runs `python3 tools/build_airports.py` before every build, so APKs from
  GitHub Actions always contain the current worldwide database.
- **Local builds** (Android Studio): run the script once before building,
  otherwise Nearest shows "No airport database":
  ```bash
  python3 tools/build_airports.py                       # worldwide, ~40k airports
  python3 tools/build_airports.py --countries AT,DE,CH  # smaller regional subset
  ```

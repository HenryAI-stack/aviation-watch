# Feature map: Garmin D2 Air X15 → Galaxy Watch4

The Galaxy Watch4 has GPS, a barometer, an optical heart-rate sensor (also used for
SpO₂), Wi-Fi, Bluetooth and optionally LTE. It has no Garmin aviation database and
cannot talk to Garmin avionics, so some features are substituted or left out.

Legend: ✅ done · 🔜 planned (phase) · 📱 already provided by Wear OS / Samsung · ❌ not feasible

## Aviation features

| D2 Air X15 feature | Our implementation | Phase | Notes |
|---|---|---|---|
| Zulu / UTC time, multiple time zones | Zulu clock screen + local time; UTC complication | ✅ 1 / 🔜 6 | `ui/clock` |
| Barometric altimeter, baro setting | Altimeter with adjustable QNH (hPa / inHg), pressure altitude | ✅ 1 | Cabin altitude in pressurised aircraft |
| Altitude alerts | Vibration when crossing a set altitude | 🔜 4 | |
| Nearest airports ("NRST") | GPS position + bundled OurAirports DB, bearing/distance list | 🔜 2 | Logic ready in `core` (`NearestAirports`) |
| Direct-To navigation | Bearing, distance, ETE, ground speed, track | 🔜 2 | Logic ready in `core` (`Navigation`) |
| HSI / CDI | Course deviation needle from cross-track error | 🔜 2 | `Navigation.crossTrackMeters` |
| Airport info (elevation, runways, frequencies) | From OurAirports runways/frequencies CSV | 🔜 2 | |
| METAR / TAF | aviationweather.gov Data API, colour-coded flight category | 🔜 3 | `FlightCategory` ready in `core` |
| Weather at nearest / destination airport | Combined Nearest + Weather | 🔜 3 | |
| Automatic flight logging | Detect takeoff/landing from GPS speed & baro trend; block/air time | 🔜 4 | Foreground service |
| Flight timer, fuel/tank timer | Count-up and countdown timers with vibration | 🔜 4 | |
| Track log | GPX export to phone / share | 🔜 4 | |
| Pulse Ox with altitude acclimation | Heart rate (Health Services); SpO₂ via Samsung Health Sensor SDK | 🔜 5 | See note below |
| Hypoxia alerts | Alert when SpO₂ is low at altitude | 🔜 5 | |
| Sunrise / sunset | Computed locally from GPS position | 🔜 4 | |
| Aviation tiles / glances | Wear OS Tiles: Nearest, METAR, Zulu | 🔜 6 | |
| Watch-face data | Complications: UTC, altitude, next timer | 🔜 6 | Works with any watch face |
| Moving map | Simple track/airport plot, no basemap | 🔜 later | Offline maps are large; scope later |
| Garmin Connext (avionics link) | — | ❌ | Proprietary Garmin protocol |
| Garmin Pilot flight-plan sync | Manual route entry or import from phone | 🔜 later | |
| Aviation database (Jeppesen/Garmin) | OurAirports (free, public domain) | — | Not an official source |

## General smartwatch features

| D2 Air X15 | Galaxy Watch4 |
|---|---|
| Smart notifications, calls, voice assistant | 📱 Built in |
| Garmin Pay | 📱 Samsung Wallet / Google Wallet |
| Music | 📱 Built in |
| Health / fitness tracking | 📱 Samsung Health |

## SpO₂ note

Wear OS **Health Services** gives continuous heart rate but no on-demand SpO₂.
Samsung's **Health Sensor SDK** does provide SpO₂ on the Galaxy Watch4 and is free.
Personal use with developer mode on is fine. Publishing an app that uses it needs
Samsung partner approval. Phase 5 will do heart rate first and add SpO₂ behind a
capability check.

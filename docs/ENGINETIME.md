# Enginetime integration

The watch records engine times in the same format as the
[Enginetime web app](https://github.com/HenryAI-stack/enginetime) and uploads them to the
same place, so they appear in the web app's list of saved flights.

## How it works

```
 Watch: MOTOR AN → START → MOTOR AUS
        │  current flight saved on every tap (survives app close / reboot)
        ▼
 enginelog_YYYYMMDD_HHMM_REG.json   in app storage "pending/"
        │  WorkManager job, runs when any network is available
        │  (Wi-Fi, LTE or the phone's Bluetooth link), retries with backoff
        ▼
 Google Drive appDataFolder (your account)  ←── same folder the web app reads
        │  file moved to "sent/" on the watch only after Drive confirmed it
        ▼
 Enginetime web app → "Gespeicherte Flugdaten"
```

- **Flight data:** registration (set once in Settings, editable), departure (nearest
  airport by GPS at MOTOR AN), destination (nearest airport at MOTOR AUS, falling back to the
  Direct-To target), note `VFR DEP→DEST`. Every field can be edited on the Engine Time page.
- **Same rules as the web app:** events in order MOTOR AN → START → MOTOR AUS, each once,
  with undo. The flight is saved automatically after MOTOR AUS once all fields are present.
- **File format:** identical to the web app's `version: 2` JSON (`events` with `id`, `type`,
  `label`, `iso`, `lTime`, `lDate`, `uTime`, `uDate`), built and unit-tested in
  `core/.../EngineLog.kt`.
- **No duplicates:** before uploading, the watch checks whether a file with the same name
  already exists in the folder.

## One-time setup (free)

Google only lets apps from the **same Google Cloud project** see that Drive folder. The
watch therefore needs its own OAuth client in the project that holds the Enginetime web
client (client ID starting with `876959021639-`).

1. [Google Cloud Console](https://console.cloud.google.com/) → select that project →
   **APIs & Services → Credentials → Create credentials → OAuth client ID**.
2. Application type **TVs and Limited Input devices**, name e.g. `Aviation Watch`. Copy the
   **client ID** and **client secret**.
3. **OAuth consent screen**: set the publishing status to **In production**. In
   *Testing* status Google expires refresh tokens after 7 days and the watch would have to
   sign in again every week. `drive.appdata` is a non-sensitive scope.
4. GitHub → this repository → **Settings → Secrets and variables → Actions → New repository
   secret**: `ENGINETIME_CLIENT_ID` and `ENGINETIME_CLIENT_SECRET`.
5. The next CI build produces an APK with sync enabled. For local builds put
   `enginetime.clientId=…` and `enginetime.clientSecret=…` in `~/.gradle/gradle.properties`
   (never in the repository).
6. On the watch: **Settings → Sign in with Google**. The watch shows a code; open
   `google.com/device` on the phone, enter it and allow access. Done once.

Without steps 1–5 the app still records and stores every flight on the watch; the
Flight Log shows them as waiting, and they are uploaded after you sign in.

### About the client secret

For device clients Google treats the secret as non-confidential: it ships inside the APK
(and debug APKs from public CI runs can be downloaded by anyone). It cannot be used to read
anyone's data without that person approving the sign-in on their own phone, and access is
limited to this project's app-data folder. It is still kept out of the repository.

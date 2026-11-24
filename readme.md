# TravelTime

An Android app that automatically records how long you spend travelling. It notices when you get into a car, train or bus, onto a bike, or (optionally) start walking or running, and logs each trip's start, end and duration. You can share your trips as a CSV file through the normal Android share sheet (WhatsApp, Gmail, Drive, …).

## Features

- **Automatic trip detection.** Uses Google's Activity Recognition Transition API. No GPS, very low battery use, and it keeps working when the app is closed or after a reboot.
- **Travel modes:** vehicle (car, bus, train, tram), bicycle, running, walking. Choose which ones to detect. Vehicle and bicycle are on by default.
- **Smart merging.** A short stop (a red light, changing trains) continues the same trip instead of splitting it. Adjustable: 0–20 min, default 5.
- **False-positive filter.** Detected trips shorter than a threshold are dropped. Adjustable: 0–15 min, default 3.
- **Manual trips.** Start and stop a trip yourself. Detection never interferes with a manual trip.
- **Live timer.** A silent "Travelling…" notification with a running clock while a trip is in progress.
- **Overview.** Totals for today, this week and this month, a weekly breakdown per mode, and a day-by-day trip list.
- **Edit trips.** Change the mode, start/end date and time, add a note (e.g. "commute", "client visit"), or delete a trip.
- **Share as CSV.** Share icon → *This week* / *This month* / *All trips*. Opens the standard Android share sheet with a `.csv` attachment and a one-line summary.
- **Private.** No account, no network access, data stays on the device (included in Android's own device backup).

### CSV format

```csv
date,start,end,duration_minutes,duration,mode,source,note
2026-11-24,08:10,09:15,65,1:05,Vehicle,auto,
2026-11-24,17:00,17:25,25,0:25,Bicycle,manual,"home, via the park"
2026-11-24,23:30,2026-11-25 00:45,75,1:15,Vehicle,auto,
```

UTF-8 with BOM and CRLF line endings, so it opens correctly in Excel, Google Sheets and Numbers. `end` includes the date when a trip crosses midnight, and reads `ongoing` for a trip still in progress.

## Install on your Android phone

### Option A: from GitHub (recommended)

Once the repo is on GitHub (see [Deploy](#deploy-build-and-host-on-github)), open this on your phone:

- **Install page:** `https://<user>.github.io/<repo>/` (GitHub Pages, with a big install button)
- **Or directly:** `https://github.com/<user>/<repo>/releases/latest/download/TravelTime-release.apk`

Then:

1. Tap the download. The first time, Android asks you to allow installing from your browser: **Settings → Apps → Special app access → Install unknown apps → (your browser) → Allow**.
2. Open the downloaded `TravelTime-release.apk` and tap **Install**. If Play Protect warns about an unknown developer, choose **Install anyway**; this happens for any app not from the Play Store.
3. Open TravelTime and tap **Turn on tracking**. Allow **Physical activity**, and **Notifications** if you want the live timer.

### Option B: over USB from your computer

1. Enable developer options on the phone: **Settings → About phone → tap "Build number" 7×**. Then turn on **Settings → System → Developer options → USB debugging**.
2. Connect the phone and accept the "Allow USB debugging" prompt.
3. Build and install:

   ```bash
   ./gradlew :app:installDebug
   # or, with an APK you already have:
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Option C: copy the APK

Send `TravelTime-release.apk` to the phone however you like (Drive, email, a WhatsApp message to yourself, USB file transfer), open it from the Files app, and follow steps 1–3 of option A.

### Updating

Install a newer APK over the old one; your trips are kept. This only works if both APKs are signed with the **same key**, which is why release signing is set up below. Switching between a debug and a release build means uninstalling first, which deletes your trips, so share a CSV before doing that.

### Make detection reliable

Some phones (Samsung, Xiaomi, Huawei, OnePlus, …) aggressively stop background apps. If trips are missed:

- **Settings → Apps → TravelTime → Battery → Unrestricted** (wording varies per brand).
- Samsung: also remove TravelTime from **Battery → Background usage limits → Sleeping apps**.
- See [dontkillmyapp.com](https://dontkillmyapp.com) for brand-specific steps.

Detection usually reports a transition within about a minute of it happening, so start and end times can be off by a minute or so. Fix them in the trip editor if you need exact times.

## Build locally

Requirements:

- JDK 17. On macOS with Homebrew (`brew install openjdk@17`), put it on your `PATH`, e.g. in `~/.zshrc`:

  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17
  export PATH="$JAVA_HOME/bin:$PATH"
  ```

  Otherwise `java`/`keytool` fail with *"Unable to locate a Java Runtime"* (macOS's `/usr/bin` stubs only find JDKs registered with the system).
- Android SDK with platform 36 (Android Studio installs this; otherwise set `sdk.dir` in `local.properties`)
- Gradle wrapper is included (Gradle 8.14.2)

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease     # → app/build/outputs/apk/release/app-release.apk
```

Or open the folder in Android Studio and press Run.

`assembleRelease` is signed with the debug key unless the `SIGNING_*` environment variables below are set.

## Deploy: build and host on GitHub

The workflow in [.github/workflows/build.yml](.github/workflows/build.yml) does everything.

**Every push to `main`/`master` (and every PR):**
- runs the unit tests
- builds debug and release APKs
- uploads them as workflow artifacts (`TravelTime-debug-apk`, `TravelTime-release-apk`)
- publishes the install page ([deploy/index.html](deploy/index.html)) plus the APKs to GitHub Pages

**Every tag `v*`:** also creates a GitHub Release with both APKs attached.

### One-time setup

1. Create the repo and push:

   ```bash
   git init && git add . && git commit -m "init"
   git branch -M main
   git remote add origin git@github.com:<user>/traveltime.git
   git push -u origin main
   ```

2. Enable Pages: **repo → Settings → Pages → Source: GitHub Actions**.

3. Fix the links in [deploy/index.html](deploy/index.html) if your repo isn't `Marcuzzz/traveltime`.

4. **Release signing** (strongly recommended, so updates install over each other). Create a keystore once and keep it safe. If you lose it, users must uninstall to update.

   `keytool` comes with the JDK (see [Build locally](#build-locally) if it says "Unable to locate a Java Runtime").

   ```bash
   mkdir -p keystore
   keytool -genkeypair -v -keystore keystore/release.jks -alias traveltime \
     -keyalg RSA -keysize 4096 -validity 10000
   base64 -i keystore/release.jks | pbcopy   # macOS: copies it to the clipboard
   ```

   Add these under **repo → Settings → Secrets and variables → Actions**:

   | Secret | Value |
   |---|---|
   | `SIGNING_KEYSTORE_B64` | the base64 output from above |
   | `SIGNING_STORE_PASSWORD` | keystore password |
   | `SIGNING_KEY_ALIAS` | `traveltime` |
   | `SIGNING_KEY_PASSWORD` | key password |

   `keystore/` and `*.jks` are git-ignored; never commit the keystore.

   To sign locally with the same key:

   ```bash
   SIGNING_KEYSTORE_PATH=keystore/release.jks SIGNING_STORE_PASSWORD=… \
   SIGNING_KEY_ALIAS=traveltime SIGNING_KEY_PASSWORD=… ./gradlew :app:assembleRelease
   ```

### Publishing a release

Bump `versionCode` (it must increase) and `versionName` in [app/build.gradle.kts](app/build.gradle.kts), commit, then:

```bash
git tag v1.0.0
git push origin v1.0.0
```

When the workflow finishes, the APK is at `https://github.com/<user>/<repo>/releases/latest/download/TravelTime-release.apk` and the install page always points to it.

## How detection works

```
Play services activity recognition
        │  ENTER / EXIT transition (vehicle, bicycle, running, walking)
        ▼
ActivityTransitionReceiver  (works with the app closed)
        │
        ▼
TripTracker  ── rules ─────────────────────────────────────────────
        │   ENTER mode  → start trip, or resume the last trip of the
        │                 same mode if it ended < "join" minutes ago
        │   ENTER other → end current auto trip, start a new one
        │   EXIT mode   → end the running auto trip of that mode
        │   manual trip → never touched by detection
        │   short trips → pruned once they can no longer be resumed
        ▼
Room database (trips.db)  →  UI, notification, CSV export
```

`BootReceiver` re-registers detection after a reboot or app update, because Android drops the registrations then.

## Project structure

```
app/src/main/java/com/example/traveltime/
├── TravelTimeApp.kt            # Application; creates AppContainer, (re)registers tracking
├── data/                       # Trip entity, DAO, Room DB, repository, settings, AppContainer
├── tracking/                   # TripTracker (rules), TrackingManager (Play services),
│                               # ActivityTransitionReceiver, BootReceiver, TripNotifier
├── util/                       # Duration formatting, totals/grouping, CsvExport (+ share sheet)
└── ui/
    ├── MainActivity.kt
    ├── theme/  nav/  vm/
    └── screens/                # TripListScreen, TripEditScreen, SettingsScreen
app/src/test/                   # JUnit tests: TripTracker rules, stats, CSV format
```

## Permissions

| Permission | Why |
|---|---|
| `ACTIVITY_RECOGNITION` | Detect when you start and stop travelling (asked at runtime on Android 10+) |
| `POST_NOTIFICATIONS` | The "trip in progress" timer (Android 13+, optional) |
| `RECEIVE_BOOT_COMPLETED` | Turn detection back on after a reboot |

No location, internet or storage permissions.

## Known limitations / next ideas

- Detection can't tell car from bus from train; they're all "Vehicle". Add a note or change the mode in the editor if you want to tell them apart.
- No start/end locations (deliberately: that would need background location permission).
- Needs Google Play services, so it won't work on de-Googled phones.
- Possible additions: home-screen widget, weekly summary notification, CSV import, charts per week/month.

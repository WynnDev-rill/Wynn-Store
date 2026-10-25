# IWatchYou

Native Android (Kotlin/JVM, Jetpack Compose, Material 3), Android 11+, Plus Jakarta Sans.
One screen: daily unlocked screen time, actual hourly bars, application usage, date selector,
and a monitoring card fixed at the bottom above gesture navigation.

## Counting rules

- Screen time is the union of interactive, unlocked screen intervals, including Home and IWatchYou.
- App time follows one focused application window. Split-screen does not double count.
  PiP playback, input methods, accessibility overlays and background services do not create independent app time.
- System dialogs and the notification shade pause app time; the screen total continues while unlocked.
- Timers accumulate per app per local calendar day. Midnight archives the previous day and resets the live overlay.
- Elapsed durations use `elapsedRealtime`, with wall time used only for dates and hour labels.
- A bounded event reconciliation handles temporarily unavailable window roots. No foreground-app polling,
  wake locks, exact alarms, foreground-service loop, content-change subscription, or per-second disk writes.
- A worker writes a daily SQLite row on app/screen transitions and at most every minute while unlocked.
  No maintenance loop runs while the screen is off. Notification seconds are rendered by Android's chronometer.
- If Android/vendor firmware kills or force-stops the service, unobserved time is deliberately not invented.
  A sudden kill may lose the current uncommitted interval (up to one minute). Force-stop cannot be bypassed.

## Privacy and setup

Accessibility interactive-window access is required for focus/PiP detection. Only root package names and
window metadata are inspected: no text traversal, screenshots, messages, keystrokes or analytics.
Durations stay inside private app storage; cloud and device-transfer backups are excluded.
Internet access is used only for GitHub update metadata and APK downloads.

Enable Accessibility and notifications once using the bottom card. No separate overlay permission is needed.
For Xiaomi/HyperOS, if the OS stops the service, enable autostart and unrestricted battery in system app settings.
The application cannot silently change these vendor settings.

## Build and verification

From repository root:

```sh
./screenguard-pro/gradlew -p iwatchyou :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:assembleDebugAndroidTest
```

CI also runs native instrumentation/UI checks on API 30 and 36. APK shrinking uses R8 + resource shrinking,
replaces the oversized extended icon dependency, retains all three font weights and all supported ABIs,
and keeps uncompressed DEX/native libraries for efficient memory mapping.

## Production releases

Never publish debug builds as stable. Production signing uses the owner's existing private production
certificate (SHA-256 `eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a`).
Private signing material stays outside this repository and must not be uploaded in workflow artifacts.

1. Increase `versionCode` and `versionName`. CI builds and runs QA, uploads an unsigned optimized release.
2. Sign verified CI output outside the repository using the private backup. Verify v2/v3 signatures,
   identity, release flags, source digest and unchanged ZIP payload. Stage only the public APK and metadata.
3. `scripts/publish.py` requires a successful source-build/emulator run, verifies staged source identity,
   production certificate and artifact hashes, publishes immutable release assets, downloads them for
   verification, then updates `updates/stable.json`.
4. Installed production builds check for updates on launch (metadata cached for six hours); Settings can force
   a check. Downloads are bounded and HTTPS-only, with size, SHA-256, package, exact version and original
   certificate checks. Android installation consent is always required. Unknown-source consent resumes
   installation on return without repeating the download.

**Preview 1.0.0 was debug-signed with a different certificate. Android will reject an in-place transition
from that preview to production. Production-to-production updates preserve the app identity and private data.**

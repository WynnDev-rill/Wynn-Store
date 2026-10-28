# IWatchYou

Native Android (Kotlin/JVM, Jetpack Compose, Material 3), Android 11+, Plus Jakarta Sans.
One screen: daily unlocked screen time, actual hourly bars, application usage, date selector,
and a monitoring card fixed at the bottom above gesture navigation.

The primary outcome is an advancing timer over whichever external app you are using,
so time passing stays visible. See [agent instructions](AGENTS.md) for architecture and
development constraints, and [the focus/bubble audit](docs/BUG-AUDIT.md) for findings,
verification and remaining device checks.

## Counting rules

- Screen time is the union of interactive, unlocked screen intervals, including Home and IWatchYou.
- App time follows one focused application window. Split-screen does not double count.
  PiP playback, input methods, accessibility overlays and background services do not create independent app time.
- System dialogs and the notification shade pause app time; the screen total continues while unlocked.
- Timers accumulate per app per local calendar day. Midnight archives the previous day and resets the live overlay.
- Elapsed durations use `elapsedRealtime`, with wall time used only for dates and hour labels.
- A bounded event reconciliation handles temporarily unavailable window roots. No foreground-app polling,
  wake locks, exact alarms, foreground-service loop, content-change subscription, or per-second disk writes.
- Focused-window metadata takes priority over package events. A bounded in-memory window/package cache
  avoids reading roots when identity is already known; an unresolved selection can query the active root.
  Background pane events therefore cannot directly replace the timer of a known focused app.
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

### Checking the bubble on a phone

After enabling IWatchYou in Accessibility, leave Settings and open two ordinary apps
in turn. The bubble should appear in both and keep advancing while you use each app.
Returning to an app resumes its accumulated time for today. At Home, inside IWatchYou,
or while the screen is locked, there should be no app bubble. The notification shade
and system dialogs pause app time. Dimming after three seconds should keep the timer
readable; tap it to unlock dragging.

If the bubble only appears in Settings, record the installed IWatchYou version, phone
model, Android/HyperOS version and which apps reproduce it. A card saying "Pemantauan
aktif" means the service is connected; it does not prove that foreground detection or
the bubble is working. Check whether the service is still enabled before changing
battery settings. Autostart/unrestricted battery can help when HyperOS stops the
service, but cannot correct a foreground-detection bug. Source changes are not present
in the published APK until a new verified, production-signed release is distributed.

## Build and verification

From repository root:

```sh
python3 -m unittest discover -s iwatchyou/qa -p 'test_*.py' -v
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

## Version 1.4 (1.4.0)

No Uji/preview label or production-preview build variant. The third version
component preserves compatibility with installed update validators.

- Use the upper-right arrow to dock the timer at the closest screen edge. Tap the
  solid edge button to show it again. Hidden state persists across app switches.
- Tap the lower-right grip to shrink in 10% steps, or drag horizontally within
  80–100%. Settings → Atur ulang bubble timer returns to the shipped maximum.
- After 3 seconds the entire timer body passes touches through to the app. Use
  the solid edge button to unlock its controls; tapping the faded body has no effect.
- Setup checks that “Jeda aktivitas apl jika tidak digunakan” is disabled and
  guides enabling “Mulai otomatis di latar belakang”. Settings are owned by Android;
  vendor intents fall back to App info with manual directions.
- All saved daily/hourly/app statistics remain in the existing SQLite database.
  The date menu lists recorded days, comparisons name the previous recorded day,
  and “Rincian per jam” reveals all 24 hourly durations. Unobserved dates are not zeroes.

See `AGENTS.md`, `DESIGN.md`, `docs/AI-DEVELOPMENT.md` and `docs/VERIFICATION-1.4.md`.
Existing separate preview installs have a different package/data directory. An APK
cannot silently move that private history or bypass a missing original signer.
Production signing keys are required before issuing an installable production update.

## Owner-authorized fresh installation, 29 October 2026

The 1.4.0 APK supplied for a fresh installation uses a new dedicated release
certificate, as explicitly accepted by the owner after the older signer could
not be located. It cannot update an installation signed with an older key.
Uninstalling that installation deletes its private history. This delivery does
not change the public stable feed or authorize using the new key to replace
updates for other existing installations. See `docs/VERIFICATION-1.4.md`.

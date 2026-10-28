# IWatchYou: instructions for coding agents

## Product goal

The user should notice time passing whenever they use another app. A readable,
advancing bubble over the currently focused app is the primary outcome. A running
service or a timer visible only in Android Settings does not establish success.
Keep the experience automatic after enabling Accessibility; avoid adding setup
permissions unless evidence shows they are necessary.

## Project and architecture

Native Kotlin/Jetpack Compose, minSdk 30 (Android 11), compile/target SDK 36.
Package identity: `id.wynn.iwatchyou`. Build configuration is in `app/build.gradle.kts`.

- `WatchService.kt`: Accessibility events, foreground selection, screen/lock state,
  notification and lifecycle. Android binds this service; do not manually start it
  as an ordinary background service.
- `ForegroundPolicy.kt`: platform-neutral rules for focused windows, keyboard,
  overlays, split screen and PiP, plus current-root eligibility and fallback priority.
- `OverlayController.kt`: `TYPE_ACCESSIBILITY_OVERLAY` bubble, native Chronometer,
  fading, drag position and rotation. No `SYSTEM_ALERT_WINDOW` permission is needed.
- `TimeLedger.kt`: synchronized monotonic time accounting and daily/hourly splits.
- `HistoryStore.kt`: private SQLite history and legacy migration.
- `MainActivity.kt`: dashboard, setup, monitoring state and updates.
- `UpdateManager.kt`: HTTPS update feed, verified downloads and installation consent.

Preserve the counting rules in `README.md`: one focused app at a time; Home and
IWatchYou count toward unlocked screen time but do not show an app bubble. Lock,
notification shade and system dialogs pause app time. PiP and keyboards must not
steal app time. Resume an app's accumulated duration for the local day; do not
invent time while the process was killed. Use elapsedRealtime for durations.

## Reliability and privacy constraints

Inspect only package names and window metadata. Do not traverse text, record
messages/keystrokes, take screenshots, add analytics, or upload usage history.
Device QA screenshots are test artifacts, not a production monitoring feature.
Do not log sensitive screen contents. Cloud/device-transfer backups stay disabled.

Keep UI operations on the main thread and SQLite work on the ledger worker.
Potentially blocking Accessibility window calls stay off both threads. Reject
stale asynchronous results after a newer focus transition. Window resolution is
event-driven, with bounded reconciliation; do not introduce foreground polling,
per-second database writes, wake locks or a foreground-service loop as a shortcut.

Known focused-window identity is authoritative. Keep the per-window package hints
bounded (32 entries in RAM) and copy them before background reads. These are inferred
associations, not a security attestation. Query the active root only for unresolved
selection, require its ID to match a known selected window, and apply window-role
exclusions before accepting it. An unrelated missing root must not override a known
focused app. Global event hints are a last resort when window identity is unavailable;
background pane state events must never directly commit a new foreground app.

Do not assume a foreground Activity's class ends in `Activity` or that every
external Activity has a launcher entry. Missing Accessibility roots are a real
input condition. Test beyond `com.android.settings`. Accessibility overlays and
input-method events must not replace the app that the user is actually using.

Never promise to bypass force-stop or OEM battery restrictions. Record remaining
physical-device uncertainty (especially Xiaomi/HyperOS) rather than treating an
emulator pass as proof that every phone works.

## Build and test

Run from the repository root with a configured Android SDK and a full JDK 17+
(including `javac`, not just a JRE):

```sh
python3 -m unittest discover -s iwatchyou/qa -p 'test_*.py' -v
./screenguard-pro/gradlew -p iwatchyou :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:assembleDebug :app:assembleDebugAndroidTest
```

IWatchYou uses the sibling Gradle wrapper; it has no independent `gradlew`.
Use `ANDROID_HOME` or an untracked `local.properties` to select the SDK. Keep
the wrapper's pinned distribution/checksum and normal TLS verification.

- JVM tests: `app/src/test/`, covering selection and time accounting.
- Device tests: `app/src/androidTest/`; real separate-APK windows include PiP and
  `FocusProbe` (an Activity without a launcher or an `Activity` suffix).
- On a booted emulator/device, run `bash iwatchyou/qa/run-device.sh` from the root.
  This installs debug/test APKs and changes Accessibility, rotation and font settings;
  use a dedicated test device. The test teardown restores those settings.
- Test missing-window access, Settings → another app → Home, advancing bubble,
  repeated app switches, keyboard, lock/unlock, PiP and rotation when changing focus.
- Keep fullscreen fixtures in a separate task affinity from the PiP fixture and
  verify their window is focused and outside PiP. Use supported intent flags for
  task cleanup and verify the PiP window disappears. Wait for actual Home/Settings
  focus instead of fixed animation sleeps. Apply system-bar/cutout insets to native
  fixtures on API 36; visible screenshots alone do not prove a text node is accessible.
  External test-APK activities run in their own process: keep them self-contained
  (the Java `FocusProbe` intentionally does not depend on Kotlin from the target APK).
  Missing-root tests use the WindowManager focused component as an independent
  oracle; do not make their readiness depend on UiAutomation root-cache availability.
- Reports: `app/build/test-results/`, `app/build/reports/`, `qa/output/` (untracked).
  Record commands and actual results in the audit document. Do not claim unrun tests.

## Releases

Production signing and publishing rules in `README.md` remain authoritative.
Preserve the package ID, production certificate and private history. Never replace
a production APK with a debug-signed build or tell users to uninstall to test an
update, since that discards their history. No private signing material belongs in
the repository, logs, chat, or CI artifacts.

`distribution/` and `updates/stable.json` contain published artifacts, not source
build outputs. A source fix must be tested independently of an already staged APK.
Do not claim an existing APK contains a new fix. Publishing requires a new version,
matching source digest, successful source/device QA, production signature and the
checks enforced by `scripts/publish.py`.

The `preview` build is an optimized, debug-signed, separate app
(`id.wynn.iwatchyou.preview`, "IWatchYou Uji"). Use `:app:assemblePreview` when the
production key is unavailable and a user requests a safe test download. Never
represent it as an in-place production update or advertise it in the stable feed.
`qa/run-preview.sh` is a dedicated-emulator smoke check after native tests: the
production package remains installed while preview launches and shows its bubble.
Only one monitoring service should be enabled when trying it on a real phone.

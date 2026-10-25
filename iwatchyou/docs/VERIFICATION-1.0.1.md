# IWatchYou 1.0.1 verification

Production identity: `id.wynn.iwatchyou`, versionCode `2`, Android 11+ (minSdk 30),
targetSdk 36. Kotlin/JVM, Jetpack Compose, Material 3, Plus Jakarta Sans.

Source: `c2cfaec63c70bb0334c3200769e7895ad5db8070`.
[Source build and native tests](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40152442640).

## Artifact

| Check | Result |
| --- | --- |
| Signed APK | 2,003,492 bytes (2.00 MB decimal) |
| Previous debug preview | 59,754,965 bytes |
| Size reduction | 96.65% from that preview |
| APK SHA-256 | `b0c2a7c5d1c170f7e45e4671703045f0a96008448a4083d169b94a13a1f4cbc5` |
| Production certificate SHA-256 | `eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a` |
| Signature | v3 verified for supported Android versions; v2 independently verified using API 24–27 verifier bounds |
| Payload | Signing preserves every unsigned APK ZIP entry byte-for-byte |
| Distribution | Release asset downloaded and SHA-256 verified before activating stable feed |

The API 24–27 verifier bounds validate the v2 signature only; they do not lower the
application's minimum Android version. All four ABI library variants and all three font
weights remain bundled, together with the font license. R8 and resource shrinking are
enabled. The extended icon dependency was replaced with the core icon set.

## Coverage

| Area | Evidence and scope |
| --- | --- |
| Elapsed durations, rapid transitions, lock/resume, checkpoints, midnight archives, hour bins, clock changes, DST | 14 JVM tests passed |
| Focus selection, split-screen policy, PiP, keyboard, overlays, system shade, launcher, unknown windows | 10 JVM tests passed |
| SQLite history persistence | Native integration test passed on API 30 and 36 |
| Main UI, rotation, large text | Native UI test passed on API 30 and 36 |
| Timer progression, background pause, repeated real app transitions, screen off, notification pause, resume | Native integration test passed on API 30 and 36 |
| Dimmed swipe lock, tap/unlock/drag, portrait position restored after landscape | Native UI checks passed on API 30 and 36 |
| Actual PiP window with another focused app | Native fixture test passed on API 30 and 36 |
| Android Lint | 0 errors; remaining warnings include SDK/dependency currency and stylistic suggestions |
| Signed, optimized production APK | Installed and main UI verified on Android 16; no app-specific fatal exception found in the sampled smoke log |
| Memory | Android 16 emulator sample: UI PSS 27.2 MiB, background/service PSS 28.1 MiB. These are single samples, not physical-device budgets |
| Update download, Android installation consent, retained private data | Passed on Android 16: public stable feed → verified in-app download → installer consent → versionCode 1 to 2; recorded screen seconds 11 → 28 |

The native instrumentation suite contains four methods per API, with several assertions
inside each method. There are 24 JVM tests plus 8 native test executions across both APIs.
Debug instrumentation coverage is distinguished from the optimized production smoke test.

[Final production update verification](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40154464343) passed. The lower-version test fixture has the production payload and certificate, with only its manifest version changed; it is not a separately published stable release.

## Audit changes

Daily history is stored in atomic SQLite rows and migrated from the old preferences.
Midnight is split at the calendar boundary; the live overlay is resynchronized. Durations
use a monotonic clock. Foreground selection uses interactive-window focus and ignores
PiP playback, keyboards and accessibility overlays. Real home activities are resolved
without treating Android's temporary Settings/FallbackHome as a launcher.

Screen/display events stop and restart counting. Database work runs on one worker.
Foreground detection has no polling loop. A minute checkpoint runs only while unlocked;
notification seconds are rendered by Android's chronometer. Main-page updates stop while
the activity is not started. Package icons and labels are loaded off the UI thread.

The overlay uses the bundled font, stable digit widths, adaptive font dimensions,
visible bounds and separate portrait/landscape positions. A gesture beginning while
dimmed cannot drag or unlock it; a tap is required first. Monitoring remains at the
bottom of the single-page UI, with system insets respected.

Update handling restricts HTTPS origins and redirects, bounds response/download sizes,
and checks hash, package, exact version and the installed production certificate before
Android installation. Interrupted downloads are removed. Private signing material is
kept outside source control and workflow artifacts. FileProvider exposes only updates.
No text traversal, screenshots, messages, keystrokes, analytics or external usage uploads
are implemented. Backup and device transfer of the private usage data are excluded.

## Limits

No physical Poco F6/HyperOS device was available. Vendor autostart restrictions, long-term
battery drain, physical refresh-rate/jank measurements, TalkBack and a live midnight
transition were not exercised. Split-screen focus selection has unit coverage, while
the actual multi-app split-screen interaction has not been driven on a device. No MobSF
server or dynamic security scanner was available. Source review and a successful build
do not establish blanket security or an absence of all defects.

If the OS abruptly kills the service, the latest uncommitted interval can be lost (up to
one minute); unobserved intervals are not invented. Force-stop and revoked accessibility
permission cannot be bypassed. HyperOS may require user-controlled autostart/unrestricted
battery settings if it stops the service.

Preview 1.0.0 uses a different debug certificate. Android cannot install this production
APK over that preview with the same package identity. A first uninstall removes preview
data. Subsequent production releases use the same production certificate and app ID.

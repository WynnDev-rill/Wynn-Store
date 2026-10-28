# IWatchYou 1.4.0 verification — fresh installation

29 October 2026, Asia/Jakarta. Source: `b2977b66d43580c043ac2c565817e5e40d4769fc`.

The owner explicitly accepted uninstalling the older APK and installing a fresh
copy. This delivery uses package `id.wynn.iwatchyou`, versionCode 14,
versionName 1.4.0 and label IWatchYou, with a newly generated dedicated RSA-3072
release key. It is not an in-place update signed with the old production or
preview certificate. Uninstalling the old application deletes its private history.
Private signing material is backed up outside the repository and CI artifacts.
The public stable feed and older releases remain unchanged.

## Local verification

- Python QA runner regression: 1 test passed (8 instrumentation/crash outcomes).
- JVM: 49 tests, zero failures/errors.
- Release lint: zero errors, 19 warnings and 1 hint. Warnings are retained rather
  than described as a clean warning-free run.
- Optimized release, legacy unsigned variant and Android test APK: build succeeded.
- APK package, version, label and absence of debuggable flag checked with aapt.
- APK Signature Scheme v3 verification passed; minSdk is 30.
- Every ZIP payload entry matches the optimized unsigned source build.
- Native source digest: `a929d24552218563310e80fd2ca9c91f2553b15533e2592f2b84440544e3c88a`.
- Delivered APK SHA-256: `2e22934018261a8ad0402ecb1b0412685c03f297519edad4eaf40698e9ba7d8f`.
- New public signer SHA-256: `96065f69e2665ad2f264f5229a0d5b765f3b0bc78d677eca3bf3276286c7e4a3`.

## Emulator verification

QA-only follow-up source: `bd9b3d09d05fba47d5173d18c9f723d0015d2323`.
The native release inputs and delivered APK payload have not changed.
[Final run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40741975870):
Android 11/API 30 and Android 16/API 36: **both passed**.
Each API completed all 11 application tests, both older-fixture history-upgrade
checks, and the optimized non-debuggable release smoke check. The build job also
passed unit tests and lint. The production publication job was intentionally
skipped on the feature branch; the older public stable feed was not replaced.

The preceding run passed all 11 application tests on API 36. Its optimized-release
smoke script failed before installation because adb consumed an empty setting
value locally; the remote command is now quoted intact and its argument transport
was verified. API 30 passed 10/11 application cases; setup queried its monitoring
control before Activity/accessibility readiness. That fixture now launches with
-W and waits for the actual control instead of ignoring a boolean wait result.

The earlier failing drag test started 20 physical pixels from the bubble's left
edge. Logcat confirms FLAG_BACK_GESTURE_ANIMATED and Settings closing to Home.
The test now drags from the body midpoint and verifies Settings retains focus.
It still exercises real touch input, movement, position restoration, app switches,
lock/unlock, and accounting. No production touch or fade behavior was weakened.

Graphify was refreshed: 353 nodes, 717 edges, 20 communities. Its Kotlin parser
still partially extracts UpdateManager; compilation and lint are authoritative.
No physical POCO/HyperOS device was available. OEM autostart/background survival
and gesture comfort on the owner's phone remain physical-device checks.

## Optimized release installation

[Dedicated optimized APK verification](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40741966426)
passed on API 30 and API 36. Each emulator installed the non-debuggable optimized
APK, cold-launched the actual MainActivity, displayed its dashboard, bound the
accessibility service and showed the native timer over Settings without app crashes.
The source digest guard matches the already-built artifact to the checked-out
native source. Both API 36 dashboard and overlay captures were reviewed.

The smoke fixture now clears its prior task before cold launch. Earlier
instrumentation deliberately opened Accessibility Settings inside the app's task;
simply bringing that task to the front inspected Settings instead of MainActivity.
The rootless-app timer test now waits for a real Chronometer tick within eight
seconds instead of assuming a loaded emulator dispatches the callback within
1.1 seconds. It also verifies external-app focus remains correct.

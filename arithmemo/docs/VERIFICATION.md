# Verification evidence — ArithMemo 1.0.0

Release package `id.wynn.arithmemo`, version code **2**, minSdk **26**, targetSdk **36**. The optimized production APK is 1,758,882 bytes. The public GitHub download was independently downloaded and compared byte-for-byte with the local signed release.

- APK SHA-256: `908ea224cf9c47b7bcd8fd0b8bc3c38ba201ae68158cf7b0b22232dea688e8a6`
- Production certificate SHA-256: `09e663533c70cbf440f3bef30acb2832355d8f1a5e0c18834b8042f5aa6c5eab`
- APK Signature Scheme v2 verified; exactly one 4096-bit RSA signer. Production preparation rejects a wrong signer, debuggable QA package and nonpositive version code.

## Final automated gates

[Android verification run 44123871251](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/44123871251) tests source `4e81964399e17f3b64c7b9335b13e262b2bb6ebe`:

| Check | Result |
| --- | --- |
| JVM/Robolectric tests | 12 passed, 0 failures |
| Android release lint | 0 errors; 23 advisory warnings, primarily dependency updates/KTX recommendations |
| Debug, release and native-test APK compilation | Passed |
| Android 8 / API 26 | 3 native UI tests + 1 cold-process restoration test passed; crash gate passed |
| Android 16 / API 36 | 3 native UI tests + 1 cold-process restoration test passed; crash gate passed |

The device gate verifies navigation, list/flashcard reveal, digit selection, typed correct-answer feedback, theme/font/orientation changes, actual system-bar appearance flags and saved-session restoration. It asserts actual SQLite contents after force-stop. Native captures include normal, dark at 160% font scale, landscape, recall, feedback and relaunch. Curated raw captures are in [screens](screens/).

JVM coverage includes 10,800 arithmetic samples across operations/digits/groups; exact division and nonnegative subtraction; stable catalog uniqueness; adaptive intervals; local-date streak/statistics; SQLite reopen and idempotent transactional answers; backup round-trip and nested-schema rejection; monotonic speed timing and foreground keypad availability during autosave; strict update URLs/package/version/certificate/minSdk and truncated, oversized or tampered downloads. Data-import and timer/autosave regressions were observed failing before their fixes.

## Real signed in-app upgrade

[Production upgrade run 44090376669](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/44090376669), source `8a90e86d0f95b22d556107aec86c40be806b5062`, passed on API 36. It installed the same-key QA baseline 0.9.9/code 1, seeded one answer and an active session, downloaded the final production APK from GitHub through the actual updater, verified its hash/package/certificate, opened the native installer, confirmed installation and reopened version 1.0.0/code 2. The saved answer, accuracy and resumable session remained present. The test-only baseline is clearly marked in a separate prerelease; it is never served by the stable feed.

Activation verifies the exact workflow identities, successful real-upgrade job, source SHAs, tested candidate manifest and signed-APK/fixture checksums. It rejects application or upgrade-harness changes after their verification. The stable feed is activated only after these gates pass. Earlier failures and fixes are recorded in [DEVICE-FIXES.md](DEVICE-FIXES.md), without treating diagnostic adb installs or failed captures as passes.

## Security, recovery and access limits

Private keystore/credentials remain outside git with restrictive file permissions. An authenticated AES-256-GCM offline backup was restored and compared with the original key; its recovery password is kept separately. No private signing files are tracked. Graphify extraction/query succeeded locally without a cloud/API key.

The current integration returns HTTP 403 when managing GitHub Actions Secrets. The initial signed release is published through the runner's scoped token from verified public APK bytes. The normal future signing workflow and secret-configuration helper are ready; an account with Secrets administration permission must supply the same four production-key secrets once. See [RELEASE.md](RELEASE.md).

No physical handset or manual TalkBack session is claimed. The local software-only Android 30 emulator was unstable and was excluded; passing device evidence comes from hardware-accelerated GitHub emulators. OEM background scheduling and haptics should also be reviewed on the owner's handset.

## Reproduction

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease assembleDebug assembleDebugAndroidTest
bash qa/run-device.sh
python3 tools/prepare-release.py app/build/outputs/apk/release/app-release.apk
bash qa/run-upgrade.sh
bash tools/refresh-graph.sh
```

Use the private signing wrapper for signed builds; never commit generated credentials. `qa/run-device.sh` resets only the debug package. `qa/run-upgrade.sh` resets the production package on a disposable emulator: do not run it on a personal device containing real study history.

## Contrast

WCAG sRGB text/background token ratios: primary white/blue 5.62:1; body 15.53:1; muted body 5.84:1; blue-container text 8.88:1; dark body 13.60:1; dark muted body 10.67:1; dark blue-container text 8.49:1. System bars follow the selected app theme.

Final visual acceptance uses independent adb screencap after warm theme/rotation transitions, confirmed by a second capture one second later. Both dark and landscape captures show correct system icon contrast and complete layout; cold-dark evidence agrees. UiAutomation intermediate rotation frames are excluded.

# Verification evidence

Status is updated during the release gate; see final recorded results below. No physical Android handset is connected in this environment.

## Automated checks
- Arithmetic generation: 10,800 sampled cases across all operations, digit counts and memorization groups, including exact division and nonnegative subtraction.
- Stable catalog uniqueness and fact serialization/validation.
- Adaptive interval expansion, repeat-soon mistakes, and mastery rules.
- SQLite reopen persistence, duplicate-answer idempotence, atomic session progression and backup round-trip.
- Malformed/nested schema import regression: observed failing, fixed root-level validation, then passing without replacing existing settings.
- Speed-clock regression: observed failing when moving past feedback, fixed monotonic accounting, then passing; background time stays paused.
- Update trust: newer version, package/signature/minSdk checks, strict URLs, invalid metadata, truncated/oversize/tampered stream rejection.

The first complete JVM run passed 10 tests. Android lint reported 0 errors and 27 warnings (mostly version suggestions, KTX suggestions and launcher/license polish); later final status is recorded below. The optimized signed APK passed apksigner verification against the pinned production certificate. Source compatibility targets API 26–36.

## Device gate
`qa/run-device.sh` installs the debug app and native Compose tests, verifies navigation, flashcard reveal and typed answer feedback, force-stops/relaunches to assert session restoration, checks AndroidRuntime crashes, and captures normal, dark large-text and landscape images. GitHub Actions runs the same test gate on APIs 26 and 36 with hardware virtualization. Emulator results must be recorded before claiming UI/device verification.

## Access constraints
The current GitHub integration returns HTTP 403 for Actions secrets administration. A signing configuration script is provided for an authorized account. Private signing material remains outside git. Browser automation initialization failed in this environment. Production signing and local APK verification remain possible.

## Reproducible commands
```sh
./gradlew testDebugUnitTest lintRelease assembleRelease assembleDebug assembleDebugAndroidTest
bash qa/run-device.sh
python3 tools/prepare-release.py app/build/outputs/apk/release/app-release.apk
bash tools/refresh-graph.sh
```
Use the private signing wrapper for release builds. Keep device logs out of git; attach curated evidence to the release as needed.

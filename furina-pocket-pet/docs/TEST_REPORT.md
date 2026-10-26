# Verification report — 26 October 2026

Owner development build: Furina Pocket Pet 1.0.0, package `id.wynn.furina`, versionCode 1, minSdk 26, target/compileSdk 36. ARM64-v8a + x86_64. Release APK: 5,353,189 bytes (5.11 MiB).

## Completed locally

- Clean native build: `:app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` — passed.
- Seven JVM tests — 0 failures/errors. Ten-minute behavior simulation at 60 Hz verifies bounds and mode variety; additional simulations verify continuity and disabled behaviors. Archive tests verify safe paths, extraction and traversal rejection. Update versions are compared numerically.
- Android lint — 0 errors, 9 warnings (non-blocking style/tooling warnings; see generated report). No lint baseline suppresses errors.
- `apksigner verify` — passed with certificate SHA-256 `eba3ffafb7169cd8687b46a28a2a27b34ee7fb146a7eb2ac2a710da0ed180a3b`.
- `zipalign -c -P 16 4` — passed. All four native libraries have 16 KB ELF LOAD alignment; verified ARM64 and x86_64 Cubism Core and AndroidX graphics path.
- APK identity and target SDK checked using `aapt dump badging`.
- APK assets inspected: no Furina assets or private signing material. MIT fallback model and original license notice are included.
- SHA-256: `b891d4bfbe71dc2001c009a8c3386f1e0fa7c2582e35e75d31e15b181ace0a2d`.

## Android device verification

**Passed:** [GitHub Actions run 40290409385](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40290409385), verification commit `00c247f8befa664caa710e0bc423ede5362ba3d0`.

All three native instrumentation tests passed on an Android 16 x86_64 KVM emulator:

1. Real model ZIP import with nested Unicode names; unsafe traversal ZIP rejected, no outside file written.
2. Official SDK Haru evaluation fixture renders visible pixels. This fixture exists in debug builds only and is excluded from the release APK.
3. MIT WhaleGirl renders real transparent pixels and animated mesh changes, remains active over Home and Settings, pauses/hides, suspends while screen-off, resumes after unlock, stops, and renders again after stop/restart.

GPU readback: Haru 30,972 visible / 211,652 transparent pixels; WhaleGirl 92,199 visible / 150,425 transparent pixels; restart 92,243 visible / 150,381 transparent pixels. GL error = 0. A screenshot was inspected for actual character display. These are renderer probes, not FPS or battery benchmarks.

The final native ES2 ArtMesh adapter retains the SDK Core, model, motion, expression and physics pipeline. It supports normal/additive/multiplicative blending, standard/inverted clipping and premultiplied alpha. Advanced Cubism 5.3 offscreen blending is explicitly rejected; compatibility with such models is not claimed.

The instrumented build is the debug variant; the delivered APK is an optimized, signed release build from the same application source. Release signing, identity, alignment and compilation were checked locally. The signed release itself was not installed on a physical device.

Local software emulation crashed during emulator boot, before the app ran. This is not counted as an app test pass or failure. The cloud emulator uses KVM.

## Unverified / owner actions

- No physical Poco F6 / HyperOS device was available. Sustained CPU/GPU load, RAM on that hardware, battery drain and OEM background termination remain device-dependent and are not claimed as measured.
- The BOOTH Furina ZIP was not downloaded or tested; the exact model rig, expressions, VTS-specific toggles and compatibility remain unverified. Import an authorized local ZIP, select existing actions, and test. No natural walking or arm waving is promised when a rig lacks suitable controls.
- GitHub update parsing has unit coverage; no approved public Furina APK release exists, so a live production update installation was not tested. Android installation must use the same stable key; the private signing backup is retained for future builds.
- Importing models does not grant their usage rights. The model listing prohibits redistribution. Public SDK-based importer publication requires resolution of Live2D Expandable Application licensing; public Release publishing is gated off.
- Overlay touch passthrough applies outside the bounded rectangular character window. Transparent padding inside the window remains touchable.

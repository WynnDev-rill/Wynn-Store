# Companion life 1.3.0 — verification in progress (26 October 2026)

- Native unit suite: 27 tests, zero failures/errors (19 retained regressions + 8 companion tests). Covers sustained activity interruption/disable, bounded edge movement, no unsupported sitting, head/body/hold/stroke/drag/cancel classification, PCM silence/peaks and voice cooldown, plus previous importer/renderer-independent behavior/update regressions.
- Local `testDebugUnitTest`, `lintRelease`, `assembleDebug`, `assembleDebugAndroidTest` completed. Lint reports zero errors and 12 non-blocking warnings. Native Android runtime checks are being run separately; these build results are not a device-test claim.
- First Android 16 run [40351047025](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40351047025): real audio completion/cancel/bad-file handling, actual Ghost Mode underlying taps/restoration and all four retained regressions passed. The book assertion failed (white pixels 14616 → 14586); white clothing is not a valid baseline for white page coverage. The diagnostic is being replaced with a same-frame GPU comparison immediately before/after props, which must still prove >1000 changed book pixels, >500 cake/fork pixels, and zero prop pixels after disabling activities. This is not yet reported as a renderer pass.
- Second Android 16 run [40353170975](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40353170975): book changed 19,471 framebuffer pixels, cake/fork 12,803, disabled activity zero; all GL probes reported error 0. Screenshot confirmed the book composed in front of the model. Activity stroke interruption, accessible click, real audio and all four retained regressions passed. The Ghost restoration assertion timed out because UIAutomator click opened MainActivity (the product's intentional long-hold behavior), established by ActivityTaskManager logs. The test now keeps real system input injection with a calibrated 60 ms down/up pair after the window update renders; it still requires a body-poke reaction and no underlying click. Production behavior was not weakened to fit the emulator.
- Added Android tests for actual underlying clicks in Ghost Mode and restoration of interaction, visible book pixels and stroke interruption, real MediaPlayer envelope samples/completion, pending-playback cancellation and corrupt/missing audio. Existing real renderer, import, across-app and screen-off regressions are retained.
- Signed owner build: `id.wynn.furina` / versionCode 5 / versionName 1.3.0, 27,587,751 bytes. Original signer SHA-256 `eba3ffafb7169cd8687b46a28a2a27b34ee7fb146a7eb2ac2a710da0ed180a3b` matches the previous release. `apksigner verify`, package/SDK identity and `zipalign -c -P 16 4` passed. APK SHA-256: `55d55f5e5340d04b243a789c5de3a737a753de04aa83e13b3394715a6be3edff`. Embedded model ZIP is byte-for-byte identical to the owner-provided ZIP; all six embedded clips match their manifest hashes and have nonzero envelopes. No signing files, debug probe activity or SDK evaluation fixture are present in the release.
- Private host-side rig probe with official Web Core 5.1.0: 109 parameters, existing mouth-opening range 0..1; 0 → 0.8 moves three drawable meshes (summed vertex delta 1.7695259). This establishes model support, not a claim of signed-owner-APK Android runtime verification.
- Final Android instrumentation outcome is pending. No physical Poco F6/HyperOS measurements or live production update installation are claimed. Authored sitting/swing motions remain unavailable for the supplied model; no keyboard detection was added.

---

# Owner model / reactive pet — 1.2.0 (26 October 2026)

- Owner-only APK: `Furina-Pocket-Pet-1.2.0-Personal.apk`, 27,289,128 bytes, package `id.wynn.furina`, versionCode 4. SHA-256 `a74bc03ffd2479ded6124f866ff655fe7886ac21260e0e9657d2dc0b0a9f2560`. Original signer `eba3ffafb7169cd8687b46a28a2a27b34ee7fb146a7eb2ac2a710da0ed180a3b` verified; APK identity and 16 KiB ZIP alignment passed. Source builds have a separate opt-in private-model asset source set; standard CI contains only licensed test assets.
- `assets/personal/model.zip` compared byte-for-byte with the user-supplied archive. No private keystore/passwords in the APK; no Furina model bytes committed or published as repository artifacts.
- Local native release build, 19 JVM tests (0 failures/errors), lint (0 errors, 11 warnings) passed. Tests cover rate limiting/variation of local dialogue, velocity/density-sensitive dizzy detection and recovery, fixed hip/alternating feet/run amplitude, prior archive and behavior regressions.
- Private QA: unmodified official Cubism Web Core 5.1.0 successfully loaded the exact supplied MOC3. Four labelled leg drawables (left/right legs and shoes) were inspected; four textured phases were software-rendered and viewed. Hip weights remain zero, feet alternate lift. This validates geometry/algorithm against the actual owner model, not Android rendering of the signed personal APK.
- The gait is a small frontal chibi shuffle applied to explicitly labelled leg meshes; run mode at speed >= 1.4 uses larger stride/lift. It is not a full side-view walk/run/sitting rig or a model-author motion. Native body/face physics, wave and expressions remain SDK/model-driven.
- Commentary is entirely local and event-driven (touch, drag, dizzy, hop, roaming/running, nap/stretch, idle, low battery, wake/unlock), with global/per-event cooldowns and no immediate repeated line. No AI, OCR, screen capture, usage access or additional permissions were added. Bubbles are bounded non-touchable windows and hide on pause/hide/screen-off/stop.
- Model selection robustness: position saves write only X/Y, preventing stale service snapshots from rewriting the imported model selection; stale asynchronous model-inspection results are ignored. Owner initial model is Furina; existing explicit selections remain available and can be reset with “Model awal”.
- [Android 16 CI run 40335298741](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40335298741), code `aec9796f4e4a97c0e6e0ab0be5928c89d621d5ed`: all four instrumentation tests passed (`OK (4 tests)`). The new regression replays timed MotionEvents through the actual overlay to trigger DIZZY, verifies a real attached speech TextView with FLAG_NOT_TOUCHABLE, verifies bubble removal on hide, and checks position-only saves preserve model selection. Prior renderer/transparency/mesh, corrupt import, cross-app, pause/hide/screen-off/stop/restart regressions also pass. CI uses licensed test assets, not the private Furina ZIP. Earlier UIAutomator swipe/text assertions were not reliable velocity/non-interactive-window probes; final tests use timed view input and actual window inspection rather than deleting the assertions.
- Physical Poco F6/HyperOS testing, optimized signed personal-APK runtime rendering, actual thumb gesture sensitivity and battery/RAM measurements are not claimed as completed. Public APK/update-channel publication remains gated; no production update installation was tested.

---

# Behavior/update release — 1.1.0 (26 October 2026)

- Package `id.wynn.furina`, versionCode 3. Owner release signed with the original certificate; `apksigner verify`, APK identity and `zipalign -c -P 16 4` passed. Local `assembleRelease` completed successfully offline after build dependencies were restored. Initial local attempts were interrupted by missing JDK/compiler and network/dependency availability, not application test failures.
- APK SHA-256: `62cbb05cd859e631188cf18c2c49988522be4a7bfbee539c481034651eefb800`.
- Fifteen JVM tests passed locally (0 failures/errors): prior archive/behavior checks plus mid-roam disable, bounded drag/touch decay, model-owned motion selection, phased hops, two-axis coverage, and release parsing for missing/current/new/draft/unrelated channels.
- [Android 16 CI run 40326130059](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40326130059), code commit `f5d60da20732646e3ca4281ab2aa40bab66ba3c1`: unit tests, lint and native debug build succeeded; native instrumentation reports `OK (3 tests)`. Renderer, real transparent/deformed meshes, nested Unicode import, overlay across apps and lifecycle stop/pause/hide/screen-off/restart checks all passed. These are the debug variant, not a physical Poco F6 test of the optimized signed release.
- Auto-check runs on entry/foreground with cooldown and availability dialog; manual check shows progress and snackbar result. Parsing is tested with fixtures; a real production upgrade installation/dialog against a newly published APK is not verified because the publication gate remains closed and no public channel APK is published.
- Owner Furina archive was inspected locally: wave motion and four expressions, no walking/sitting cycle or clearly labeled independent leg rig. Its runtime rendering and natural footfalls are NOT claimed as verified. Original model bytes are neither bundled nor committed. This version improves wandering, touch/drag gaze, double-tap hops, jump phasing and model-owned gait selection; it does not create missing articulated legs.
- Reduced repetitive UI instructions and removed on-screen renderer diagnostics; added Sapa/Lompat buttons and a movement-motion selector. Motion discovery now deduplicates canonical paths.

---

# Import-size fix — 1.0.1 (26 October 2026)

- Raised ZIP input and individual extracted-file limits to 500 MiB; aggregate extraction limit to 2 GiB. Previous 160 MiB per-file check produced the reported "File model terlalu besar" even with a ZIP below the old 300 MiB input limit.
- Extraction remains streamed with a 64 KiB buffer; traversal, duplicate paths, entry count, model references and runtime memory validation remain enforced. Existing imported models and preferences are retained on update.
- Added a streamed 255 MiB synthetic-entry extraction regression and oversized stored-entry rejection test. Nine JVM tests passed, zero failures/errors. The fixture is synthetic data, not an actual Furina texture or a full Furina import test.
- `testDebugUnitTest`, `lintRelease`, `assembleRelease` passed. Lint: 0 errors, 10 non-blocking warnings.
- Signed release 1.0.1 / versionCode 2 verified with the same original certificate recorded below; APK identity and 16 KiB ZIP alignment passed.
- APK SHA-256: `3e9ab80219bcf194ef8dfd19a658373a6337fa32b15f58b2e9de67c8cdfa13dd`.
- Android instrumentation was not rerun for this bounded importer change; prior renderer/device results below apply to 1.0.0. Actual BOOTH ZIP and physical Poco F6 import remain owner tests.

---

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

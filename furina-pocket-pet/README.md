# Furina Pocket Pet

Native Kotlin + Jetpack Compose Android pet with **Cubism SDK Java R5** for rig/motion/physics and a bounded native **OpenGL ES 2** mesh renderer. Android 8+, target Android 16, ARM64 for Poco F6 and x86_64 for testing. No WebView, account, AI chat, backend, accessibility service, camera, microphone or broad storage access.

The app starts with the **WhaleGirl MIT test model**, not Furina. It imports a user's licensed VTube Studio/Cubism ZIP locally through Android's document picker. No Furina model is present in this repository or APK. BOOTH availability is not permission to redistribute or embed the model.

## Behavior

- A foreground service keeps one small, transparent overlay above other apps. Drag to reposition; tap for reactions; long-press to open settings. Touches outside the overlay window continue to other apps.
- Smooth time-based roaming, stops, looking around, small hops, blinking and breathing through actual rig parameters, physics, model motions and expressions. Random timing, weighted choices and eased transitions reduce repetition.
- Natural walking is deliberately not synthesized: the default model floats. Waves use an existing compatible motion or expression. Rig support determines what a model can do. VTS hotkeys are not executed; unlisted `.motion3.json` and `.exp3.json` are discovered for manual mapping.
- Standard ArtMesh clipping, inverted masks, premultiplied alpha, normal/additive/multiplicative blending. Advanced Cubism 5.3 offscreen blend modes are explicitly unsupported.
- Size, speed, activity, roaming, hops, reactions, 30/60 fps, favorite actions, pause/hide/stop, and reset position. An ongoing quiet notification exposes controls.
- Screen-off/locked/hidden/paused stops the animation loop and pauses GL. Battery saver caps animation at 15 fps. Textures are decoded at at most 2048 pixels per side, then CPU bitmaps are recycled; motion caching is limited.
- ZIP extraction enforces app-private paths, 500 MiB input / 2 GiB extracted / 500 MiB per file / 4096 entries, plus reference, texture, JSON and motion validation. Import is transactional; a failed ZIP does not replace the current model.
- Optional GitHub Releases update check on app open. It only recognizes `furina-vX.Y.Z` releases with matching APK names. Download/install is handed to the release page and Android installer. No silent installation.

## Build

Install JDK 17 and Android SDK platform 36 / build-tools 36.0.0. Gradle wrapper 8.13 is included with its checksum.

```sh
# Review the official agreements first. This grants no publication approval.
python3 scripts/prepare-deps.py --accept-sdk-license
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest
```

The preparation script verifies an official SDK ZIP checksum and each MIT model file checksum. Dependencies are fetched into ignored `.deps`; proprietary Core is never committed. App-owned source follows the repository MIT license; Cubism remains under Live2D's separate licenses. See [asset audit](docs/ASSET_AUDIT.md).

For release builds, restore the **original private signing backup** in `signing/` or configure `FP_KEYSTORE`, `FP_STORE_PASSWORD`, `FP_KEY_ALIAS=furina`, `FP_KEY_PASSWORD`. Then run `python3 scripts/build-release.py`. Never generate a replacement key for an installed app. Certificate identity is recorded in `docs/signing-sha256.txt`.

## Device tests

With a running Android 16 emulator and built debug/test APKs, run `bash qa/run-device.sh`. Native instrumentation (three tests, including a debug-only official SDK sample) exercises real mesh deformation and transparent GPU pixels, overlay persistence over Home/Settings, pause, hide, screen-off/resume, stop, valid nested Unicode ZIP import and unsafe ZIP rejection. It stores test output and logs in ignored `qa/output`. Pure unit tests cover long behavior simulations, bounds, continuity, disabled behaviors, archive paths and numeric update versions.

Actual verification results are recorded in [TEST_REPORT.md](docs/TEST_REPORT.md); test code alone is not evidence that tests passed. Poco F6/HyperOS hardware and the BOOTH Furina model require owner testing.

## Permissions and Android limits

Grant “Display over other apps.” Notification permission is requested when starting. The foreground service remains visible in Android's active-app controls even if notification permission is denied. HyperOS may require enabling autostart and unrestricted battery use in system settings; the app does not bypass platform restrictions. After reboot, force-stop, or OS termination, start it again from the app. Secure screens and apps that request hiding overlays can hide it. No wake lock is held.

The touchable area is the character's **small rectangular window**, including transparent padding; per-pixel touch passthrough inside that window is not claimed. Android's public overlay APIs do not expose reliable arbitrary silhouette input regions. Status/navigation/cutout margins are reserved.

## Publication status

This is an owner development/testing build. A general local-model importer fits Live2D's published **Expandable Application** definition; public publication requires Live2D approval, including for individuals. No public APK Release is automatically created. The workflow runs build/device checks; its separate publication job requires `FURINA_PUBLICATION_APPROVED=true`, stable signing secrets and an explicit publish dispatch. Do not enable that gate until licensing is resolved. The update checker is ready for an approved release channel.

The BOOTH model also restricts permitted uses and prohibits redistribution. Obtain the creator's permission for use as an Android pet; local import does not itself grant rights. Do not upload its ZIP, textures or runtime files to this repository.

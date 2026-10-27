# Furina Pocket Pet

Native Kotlin + Jetpack Compose Android pet with **Cubism SDK Java R5** for rig/motion/physics and a bounded native **OpenGL ES 2** mesh renderer. Android 8+, target Android 16, ARM64 for Poco F6 and x86_64 for testing. No WebView, account, AI chat, backend, accessibility service, camera, microphone or broad storage access.

The standard public-source/CI build starts with the **WhaleGirl MIT test model**. Owner-only builds can include a locally supplied model ZIP as the initial character, using `python3 scripts/build-personal.py /path/to/model.zip` and the existing signing key. This owner build uses the supplied Furina ZIP. Private model bytes stay under ignored `.deps/personal-assets` and are never published by CI. It imports a user's licensed VTube Studio/Cubism ZIP locally through Android's document picker. No Furina model is present in the repository or standard CI APK. Only the private owner APK includes the supplied model. BOOTH availability is not permission to redistribute or embed the model.

## Behavior

- A foreground service keeps one small, transparent overlay above other apps. Drag to reposition; tap for reactions; long-press to open settings. Touches outside the overlay window continue to other apps.
- Smooth time-based roaming, stops, looking around, small hops, blinking and breathing through actual rig parameters, physics, model motions and expressions. Random timing, weighted choices and eased transitions reduce repetition.
- Models can use their own walk/run motion. For explicitly labelled separate leg meshes, an optional small frontal chibi gait alternates feet using cached vertex weights; this is a procedural stylization, not a full animator-authored walk/run cycle. Waves use an existing compatible motion or expression. Rig support determines what a model can do. VTS hotkeys are not executed; unlisted `.motion3.json` and `.exp3.json` are discovered for manual mapping.
- Standard ArtMesh clipping, inverted masks, premultiplied alpha, normal/additive/multiplicative blending. Advanced Cubism 5.3 offscreen blend modes are explicitly unsupported.
- Fast dragging can trigger a temporary dizzy reaction; nap/stretch, original wave/expressions, and rate-limited local speech bubbles react to touch, dragging, hops, movement, screen wake and battery. Bubbles are non-touchable, disappear on pause/hide/screen-off, and can be disabled. No screen reading or AI calls.
- Model-owned walk/run motion auto-detection and a manual movement action selector; stops locomotion motion when leaving roaming. Unsupported rigs use a subtle bob, never a claimed natural footstep cycle. Double-tap to hop, touch-directed gaze, drag lean, phased hop anticipation/flight/landing.
- Size, speed, activity, roaming, hops, reactions, 30/60 fps, favorite actions, pause/hide/stop, and reset position. An ongoing quiet notification exposes controls.
- Screen-off/locked/hidden/paused stops the animation loop and pauses GL. Battery saver caps animation at 15 fps. Textures are decoded at at most 2048 pixels per side, then CPU bitmaps are recycled; motion caching is limited.
- ZIP extraction enforces app-private paths, 500 MiB input / 2 GiB extracted / 500 MiB per file / 4096 entries, plus reference, texture, JSON and motion validation. Import is transactional; a failed ZIP does not replace the current model.
- GitHub Releases checks on app open/foreground return (60-second cooldown), automatic availability dialog, visible manual-check progress and snackbar results. Empty release channel is distinguished from an up-to-date installed version. It only recognizes `furina-vX.Y.Z` releases with matching APK names. Download/install is handed to the release page and Android installer. No silent installation.

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

With a running Android 16 emulator and built debug/test APKs, run `bash qa/run-device.sh`. Native instrumentation (eight tests, including a debug-only official SDK sample and ES2 activity-scene checks) exercises real mesh deformation and transparent GPU pixels, overlay persistence over Home/Settings, pause, hide, screen-off/resume, stop, valid nested Unicode ZIP import and unsafe ZIP rejection. It stores test output and logs in ignored `qa/output`. Pure unit tests cover long behavior simulations, bounds, continuity, disabled behaviors, archive paths and numeric update versions.

Actual verification results are recorded in [TEST_REPORT.md](docs/TEST_REPORT.md); test code alone is not evidence that tests passed. Poco F6/HyperOS hardware and the BOOTH Furina model require owner testing.

## Permissions and Android limits

Grant “Display over other apps.” Notification permission is requested when starting. The foreground service remains visible in Android's active-app controls even if notification permission is denied. HyperOS may require enabling autostart and unrestricted battery use in system settings; the app does not bypass platform restrictions. After reboot, force-stop, or OS termination, start it again from the app. Secure screens and apps that request hiding overlays can hide it. No wake lock is held.

The touchable area is the character's **small rectangular window**, including transparent padding; per-pixel touch passthrough inside that window is not claimed. Android's public overlay APIs do not expose reliable arbitrary silhouette input regions. Status/navigation/cutout margins are reserved.

## Publication status

This is an owner development/testing build. A general local-model importer fits Live2D's published **Expandable Application** definition; public publication requires Live2D approval, including for individuals. No public APK Release is automatically created. The workflow runs build/device checks; its separate publication job requires `FURINA_PUBLICATION_APPROVED=true`, stable signing secrets and an explicit publish dispatch. Do not enable that gate until licensing is resolved. The update checker is ready for an approved release channel.

The BOOTH model also restricts permitted uses and prohibits redistribution. Obtain the creator's permission for use as an Android pet; local import does not itself grant rights. Do not upload its ZIP, textures or runtime files to this repository.


## Continuing development

See [continuation instructions](AGENTS.md), [five-project experience benchmark](docs/PRODUCT_BENCHMARK.md), [asset decisions](docs/ASSET_AUDIT.md), and [verification evidence/limits](docs/TEST_REPORT.md). Keep private owner assets/signing out of public source and CI.

## Companion life 1.3.0 (previous release)

Quiet autonomous read/cake activities now last long enough to feel intentional. Existing facial rig controls animate focus, chewing and touch expressions; native GPU props add page turns and cake bites. Stroke the head, poke the body, double-tap to hop or hold for settings. Edge mode eases to a side and peeks; Ghost Mode fades the pet and passes taps through, with a notification action to restore interaction. No automatic typing detection is claimed.

The supplied Furina does not have authored sitting or swinging motions. Those controls remain disabled until the selected model provides matching motions or an explicit mapping. Read/eat are native prop-and-face vignettes, not new full-body animation files. The renderer never substitutes another model's curves.

Personal builds include six short original Japanese clips and playback-position RMS mouth animation. Sound follows silent/DND mode, media volume, cooldown and foreground lifecycle; the app never records audio. Standard public-source builds contain no private voices and show that the voice pack is absent. Twemoji book/cake graphics are credited under CC BY 4.0 in the app and notices.


## Body activities 1.4.0

The original owner Furina now has a model-specific frontal puppet adapter: she sits on a blue/gold chair with raised knees and dangling boots, holds her book with bent arms, brings a fork to her mouth, and holds a suspended swing's ropes. Body, furniture and held props share a model-space projection; hands draw in front of the props. These are native procedural 2D poses using the original ArtMeshes, not new author-supplied motion files or side-view artwork. Original face/physics/voice behavior remains active.

“Gerak tubuh aktivitas” controls the adapter. It is enabled only for the exact inspected MOC3 hash; arbitrary imported models retain their own motion mappings and the previous read/cake prop fallback. Matching authored model motions take priority. Disable the adapter to return to the original rig presentation. Read/cake/sit/swing can run autonomously or from their settings buttons; touch preempts the activity and the pose fades out.

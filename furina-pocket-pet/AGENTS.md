# Furina Pocket Pet — continuation guide

Read `README.md`, `docs/TEST_REPORT.md`, `docs/PRODUCT_BENCHMARK.md`, and `docs/ASSET_AUDIT.md` before changing behavior. Do not restart completed work or replace the native stack without evidence.

## Product contract

- Native Kotlin/Compose + Cubism Java R5 rig/motions/physics + ES2 ArtMesh renderer. No WebView, account, AI server or screen surveillance. The user deferred AI; current bubbles are local event-driven dialogue.
- Prioritize the felt experience: cue-driven reactions, bounded smooth movement, recognizable model identity, occasional dialogue and phone navigation that remains usable.
- Asset-first: inspect 3–5 relevant primary-source products and existing compatible model files before custom implementation. Record what is actually reused, license, compatibility and why rejected alternatives do not fit. Do not equate research snippets with usable code/assets.
- The owner supplied a private Furina ZIP. `FP_PERSONAL_MODEL=true` adds ignored `.deps/personal-assets/personal/model.zip`; `scripts/build-personal.py` prepares an owner build. Never commit/upload this ZIP, textures, MOC3, screenshots derived from it or private signing material to a public repository/workflow. Normal CI uses the MIT test assets.
- Preserve user model selections and settings on upgrade. Default (empty model setting) uses the private model if packaged, otherwise the MIT fixture. “Model awal” resets an explicit selection to that default. Save position with `savePosition`, not a stale full settings snapshot.

## Source map

- `BehaviorEngine.kt`: pure time-based states, touch gaze, drag lean, hop phasing, dizzy/nap/stretch and locomotion frame.
- `PetDialogue.kt`: localized causal dialogue/cooldowns and density/time-based fast-drag detector. Does not read apps/screen contents.
- `LegGait.kt`, `NativeMeshRenderer.kt`: optional bounded frontal chibi steps on CDI-labelled leg meshes, cached hip-to-foot weights. Never modify Core's vertex arrays. Apply identical render offsets to mask and visible meshes; prefer original locomotion motion when present.
- `LivePetModel.kt`: official rig/motion/expression/physics flow, standard parameters, native renderer; `PetSurface.kt` owns the GL thread/lifecycle and GPU diagnostics.
- `PetService.kt`, `SpeechBubble.kt`: small overlay, drag/tap, foreground lifecycle, screen-off suspension, separate bounded non-touchable text window. `ModelStore.kt` validates and transactionally installs/imports assets.
- `MainActivity.kt`: settings/import/update feedback. `UpdateChecker.kt`: bounded GitHub release lookup; no public channel is fabricated. Publication workflow stays gated pending SDK/model publication permission.

## Verification-first definition of done

1. Run checks appropriate to the change; record real outcomes in TEST_REPORT, not intended outcomes. Unit suite covers behavior continuity/bounds, bad archives, model motion matching, release parsing, dialogue cooldowns, fast drag and gait anchors.
2. Android 16 instrumentation covers actual GPU pixels/transparency/mesh changes, SDK fixture, Unicode model import/traversal rejection, lifecycle across apps, screen-off/pause/hide/stop/restart, model-selection preservation and time-controlled MotionEvent drag through the real overlay plus bubble window flags/removal. Device tests run in `.github/workflows/furina-pocket-pet.yml` using KVM.
3. Do not lower/delete a test to hide a product failure. UIAutomator gesture injection timing is not a calibrated velocity measurement; the fast-drag regression replays explicit timestamps through the real view. Preserve a physical-phone sensitivity test as unverified until actually performed.
4. Build release with the ORIGINAL private signing backup/environment variables. Never regenerate a key to get a build through. Increment versionCode/versionName for a user update. Verify signer matches `docs/signing-sha256.txt`, package/version, `zipalign -c -P 16 4`, expected owner ZIP bytes and absence of secrets.
5. Distinguish standard CI/debug-model tests, private host-side model/geometry inspection, signed personal-APK testing and actual Poco F6/HyperOS checks. Do not claim natural full-body walking/sitting, live screen understanding, physical battery/FPS/RAM or a production auto-update installation without evidence.
6. Once relevant checks pass, deliver the real signed APK and update docs; do not repeat broad testing for docs-only edits. Preserve unrelated repository work and use safe ref leases when committing through an API.

## Build

JDK 17 (full compiler), Android SDK platform 36, build tools 36/AGP-required tools, Gradle wrapper 8.13. Follow README for pinned development dependency setup and existing key restoration. Standard: unit/lint/debug/androidTest targets; signed owner: `python3 scripts/build-personal.py /absolute/model.zip`. Keep credentials in ignored `signing/` or FP_* environment variables. The public publish gate must not be enabled merely to make the update checker show a newer APK.

## Companion life (1.3.0)

- `ActivityCatalog.kt` capability-matches model-owned seat/swing motions; never enable an unsupported full-body pose by merely adding a prop. `ActivityProps.kt` reuses credited Twemoji textures for native read/cake vignettes in the same GL surface. Keep GL resource release/context recreation intact.
- `PetTouch.kt` consumes **dp** and actual MotionEvent eventTime. A small head stroke must not reposition the window; a normal drag must still work. ACTION_CANCEL never triggers an action or sound.
- Ghost Mode uses FLAG_NOT_TOUCHABLE with window alpha 1.0 (owner requirement since 1.4.1) and suppresses the bubble to avoid stacked obscuring windows. Restore interaction through notification/settings. Never use a full-screen touch catcher. Keyboard visibility is not inferred from unrelated heuristics or screen surveillance.
- `PetVoice.kt` owns one short local Android player and audio-focus lease. `LipEnvelope.kt` samples precomputed PCM energy using playback time; model LipSync group IDs take priority, existing ParamMouthOpenY is the fallback. Muted media/DND/zero volume, pause/hide/screen-off/stop, focus loss and stale prepared callbacks must stop audio/mouth activity. No runtime voice fetches or microphone permission.
- Private audio/envelopes are under `.deps/personal-assets/personal/voice`, alongside the original unchanged model ZIP. Public CI uses generated PCM audio and the MIT model. No private audio or model bytes enter git/workflow artifacts. Preserve original credentials.
- `CompanionDeviceTest` adds real Android audio, activity pixels/interruption and Ghost disabled-input/restoration tests. `OverlayProbeActivity` is debug-only. Do not ship it in release. The activity probe compares the same framebuffer before/after props; white clothing makes a raw white-pixel baseline invalid. Ghost restoration uses real system input with explicit 60 ms down/up timestamps after a rendered window update; a loaded emulator can stretch UIAutomator click into the intentional settings long-hold. Preserve the body-poke and underlying-click assertions.


## Body activities (1.4.0)

- `BodyPose.kt` contains pure two-bone IK, fixed-hip frontal leg projection, swing-pivot composition and bounded hand smoothing. Exact MOC3 SHA-256 recognition is intentional: never infer this adapter from filenames, part labels or similar-looking IDs on another model.
- `ModelInfo.bodyRig` is resolved by streaming the MOC hash during validation. `PetConfig.bodyMotion` defaults on; existing settings and authored motion mappings survive upgrades. Authored activity motions take precedence.
- `NativeMeshRenderer` caches bind weights only for the verified profile. Scene transforms write the render FloatBuffer, never Core arrays. Mask sources use identical transforms. The normal neck mesh is not an arm; do not move it or defer it. Body, furniture, held props and foreground arms use `LivePetModel.stageMatrix`.
- Pass order is furniture → body excluding deferred arms → held props → deferred arms. `StageFurniture.kt` generates app-owned blue/gold textures once; `ActivityProps` retains its credited Twemoji textures and legacy behavior for unsupported rigs. Preserve texture/context cleanup.
- `BodyPoseTest` verifies targets/joints/neutral/hip/projection/profile/interrupts; `StageDeviceTest` uses a real ES2 pbuffer to test scene pixels, swing, held-fork motion, zero-opacity cleanup and texture recreation without private assets. Public tests cannot prove the private owner's visual identity or model-specific joint appearance: inspect the actual owner APK separately and record that evidence.
- Frontal thigh projection is a deliberate 2D stylization. Do not describe this as new side-view art, animator-authored motion or 200 imported VPet animations. Keep previous Ghost/audio/lifecycle/import/input tests.


## Voice and activity correction (1.4.1)
- Voice is media audio: do not gate it on ringer mode. Respect media volume, DND, lifecycle and audio focus; expose blocked/error/completion status. Explicit preview bypasses cooldown and replaces pending playback. START/GREET and the settings preview invoke the actual packaged Japanese clips.
- Test actual bundled OGG/envelope playback in owner builds, not only generated PCM. Public fixtures skip the private-pack test; never upload private audio.
- Opaque Ghost disables pet input. Android 12+ security may block underlying cross-UID taps inside an opaque SAW: do not claim passthrough there or weaken OS protections. Test full opacity, no pet reaction, outside-window input and restoration.
- Page turn and right-hand targets share BodyPose.pageTurn; cake lift/hold/return uses BodyPose.bite. Keep the eating wrist on its own side and attach the fork handle to the final blended wrist.

- 1.4.2: use separate audio preparation (10 s) and started-playback (clip duration + 5 s) guards. Completion tests must await the completion callback/status, not just MediaPlayer.isPlaying becoming false. Original bundled Japanese test passed locally after exposing a too-early combined watchdog.

# Companion life implementation plan

**Goal:** A responsive, unobtrusive Furina companion with sustained activities and short synchronized voice reactions.

**Architecture:** Extend the existing pure behavior engine and native overlay; isolate activity visuals, touch classification and local voice playback. Preserve model/action selection, lifecycle and signing.

**Tech stack:** Kotlin, Compose, Android WindowManager/MediaPlayer/Canvas, Cubism Java R5/OpenGL ES2.

**Spec:** `../specs/2026-10-26-companion-life-design.md`.

## Global constraints

Android 8+/target 36; no WebView, surveillance, microphone permission, replacement signing key or public private-model upload. Capability-gate seated poses. User authorization covers implementation without another design-approval round.

## Review focus

- Ghost Mode must not strand the owner or block underlying apps.
- Canceled gestures, settings changes and stop/screen-off must not emit late voice callbacks.
- Quiet/disabled states and unsupported models must not activate misleading activities.
- Position saves must preserve model/settings; activity changes must not leak props or expressions.
- Audio decode failure, missing files, silent frames and lost audio focus must close the mouth safely.

## Tasks

- [x] Update benchmark and asset decisions from five primary-source products; inspect exact private rig and short Japanese clips.
- [x] Add pure activity/touch/audio-envelope tests, then implement life states, model capability matching, head strokes and body poke.
- [x] Integrate activity visuals and rig feedback, edge/peek and recoverable Ghost Mode. Add focused settings and explicit activity controls.
- [x] Integrate bounded local voice playback with playback-position envelope, focus/silent/cooldown handling and lifecycle cleanup; private owner clips only.
- [ ] Add Android behavior/failure/regression tests. Run unit/lint/build and actual Android instrumentation, fix failures, verify original signer and alignment.
- [ ] Update AGENTS/readme/test evidence, commit source to GitHub, and deliver the signed owner APK. Clearly report unsupported sitting/swing and physical-phone checks if still unavailable.

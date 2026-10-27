# Complete body activities implementation plan

**Goal:** Actual frontal seated/swing postures and arms supporting read/eat actions on the owner's existing Furina.
**Architecture:** Exact-profile recognition, pure bone/presentation math, a bounded render-buffer adapter and model-space props. Preserve Cubism rig/physics, settings and original signing.
**Spec:** `../specs/2026-10-27-body-activities-design.md`.

## Constraints

No private model/art/audio/signing in git or public workflow. No guessed poses on unknown rigs. Core vertices remain untouched. No new surveillance or microphone permission.

## Tasks

- [x] Prove bone math, neutral/interrupt/profile guards with red→green unit tests in `BodyPoseTest.kt` and implement `BodyPose.kt`.
- [x] Recognize the profile in `ModelStore`, integrate cached mesh roles/weights and foreground-arm pass in `NativeMeshRenderer`, and shared model projection/presentation in `LivePetModel`/`PetSurface`.
- [x] Add chair/swing model-space art and hand-aligned book/plate/fork rendering; enable supported actions/settings while retaining unknown-model behavior.
- [x] Exercise actual GPU pixels, masks, transitions and prior Android regressions; inspect private pose renders and signed owner runtime if emulator is available. Review the whole branch once, fix important findings and run relevant checks.
- [x] Verify original signer/owner assets, update AGENTS/research/test evidence, merge source and deliver a signed update.

## Review focus

Unknown/modified models must not be deformed. Scene transitions must not leave missing arms or persistent props. Foreground and mask transforms must agree. Shared swing projection must keep the seat and hands aligned. Pose changes must not break pause/ghost/voice/import/rotation or persist stale settings.

Execution evidence: TEST_REPORT.md, 1.4.0 section. Fresh whole-branch review completed; both Important findings fixed with failing-then-passing regressions. No deferred minors. Private owner update/render/disable/Home/Ghost/Stop checks and final public CI passed. Original signer and all private bytes verified.

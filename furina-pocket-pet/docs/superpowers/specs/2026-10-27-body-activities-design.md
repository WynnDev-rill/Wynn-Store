# Complete native body activities

Continue 1.3 without replacing its native stack, private model, voices or signing identity. The owner explicitly requests completing seated/swing and body-supported read/eat actions rather than stopping at props.

## Asset decision

Existing VPet/Shimeji sprite actions and other Live2D models do not provide curves for this exact MOC3. Recheck found a Furina Shimeji by cartoonation (creator download, attribution/reposting restrictions) and MIT SVG sprites in Ice-teapop/desktop-pet; changing to their artwork/runtime would discard the owner's current identity. Inspecting the original private runtime mesh reveals separate normal arms and legs suitable for a small app-owned frontal puppet rig. Reuse these original textures/meshes without editing the MOC3 or publishing its data. Use two-bone kinematics and projected thigh/shin deformation, not a chair added to an unchanged standing pose. Keep unknown rigs capability-gated.

## Behavior

- Recognize the exact MOC3 by SHA-256. Opt-in setting defaults on for this supported profile; unknown/modified rigs keep model-owned motion selection and legacy prop fallback.
- Chair sit: fixed hips, foreshortened frontal thigh projection, hinged dangling shins/boots and a blue/gold chair. Swing: the same seat posture, hands at the ropes, a shared pivot/phase for ropes, seat and body. This is a frontal 2D pose, not invented side-view artwork.
- Read: seated body, both arms bend to hold the book, hands render in front of the page, gaze follows the page and page turns have a short thinking pause. Cake: left hand supports the plate/cake, right hand/fork travels between cake and mouth; chewing and lip sync use existing face parameters.
- Scene props and the model use one model-to-screen projection. Draw chair/ropes behind the model, book/cake between body and foreground arms. Identical bone offsets apply to visible and clipping meshes. Never mutate Cubism Core vertex arrays.
- Smooth entry/exit and wrist target changes; touch immediately triggers its face reaction while the previous activity fades out. Preserve Ghost Mode, voice, screen-off suspension, model/settings selection and bounded movement.

## Verification

Pure tests prove bone endpoints, joint continuity, exact neutral recovery, fixed hip, bounded swing, profile rejection and interrupted transitions. Android instrumentation must still pass prior regressions and new multi-pass/scene tests with public fixtures. Inspect the private original model through host-side geometry rendering and, if local software emulation works, the actual signed owner APK. Never upload private assets to public CI. Record any physical-phone or unsupported-viewpoint limitations honestly.

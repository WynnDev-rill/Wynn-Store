# Cinematic edition — 27 October 2026

## Intent
Use the supplied 44.7 second reference as a quality benchmark: strong focal light, dark negative space, physically coherent depth, restrained camera acceleration, large quiet typography and connected scenes. Preserve the Indonesian documentary, its evidence and Astro static architecture. The reference is inspiration, not a source of redistributable assets.

## Approach and asset-first decisions
Upgrade the existing Astro/Three.js implementation rather than port to a paid scene service or import an unrelated template. Reuse Three.js official curve/geometry/rendering utilities, the existing local geographic boundaries, licensed fonts, documentary images, Kenney ground props and source-backed data. Research four comparable experiences separately in CINEMATIC-REFERENCE-RESEARCH.md. Surface textures are optional progressive enhancements with local procedural fallback, never a prerequisite for content.

A full film as the hero would deliver stable lighting but lose reversible camera interaction and require new footage. A large imported game environment would increase transfer/GPU budgets and introduce unclear geography. Chosen direction: an authored geographical sculpture opening, then the existing forest rendered with improved vegetation, atmospheric light and continuous camera easing. Original geometry is necessary for the project's specific geography/narrative.

## Scene matrix
| Route / chapter | Composition and purpose | Camera and control | Mobile / failure |
| --- | --- | --- | --- |
| / opening | Warm rim-lit sculptural Kalimantan on a near-black horizon, oversized editorial title; conceptual, not hotspot data | Bounded CatmullRom dolly, scroll progress, slight pointer response | Reframed lower composition; locally generated SVG silhouette fallback; no loading gate |
| /#ruang | Ground-level forest, layered understory, mist and light shafts | Eased dolly and look, stage crossfades, drag/arrows/Escape | Reduced instance counts and pixel ratio; existing static forest fallback |
| /#musim, #cerita | Quiet dark editorial continuation, evidence and chronology | Native scroll with restrained reveals | Fully semantic content; no WebGL dependency |
| /#gambut | Existing explanatory 3D cutaway | Existing stage controls preserved | Existing SVG fallback |
| /#peta | Existing geographic comparison and data controls | Direct chapter entry | All values in DOM; no synthetic hotspot points |
| /#dampak, #pulih | Broad image and calmer rain transition | Existing story and recovery controls | Documentary photo keeps provenance |
| /#wynn, /sumber/ | Creator identity and source documentation | Existing links and keyboard navigation | Full readable content |

## Motion contract
No scroll hijack, mandatory intro, autoplay audio or global preloader. Pause freezes decorative camera and ambient motion; reduced motion uses deliberate fixed framing. Suspended/offscreen canvases do not schedule frames. Context loss reveals fallback, restore resumes. Texture failure leaves base materials usable. Weather remains manually selectable, automatic mode uses a coherent dark visual baseline rather than flashing between white and black.

## Verification before completion
Build/type/static CSP checks plus data tests, production-CSP browser suite, actual screenshots desktop/mobile, camera changes on forward/reverse scroll, stage controls, global pause, reduced motion, blocked WebGL, missing texture, context loss/restore, no-JS content, menu/direct navigation, existing data regression and accessibility audit. Browser emulation is not physical-device performance testing. Record evidence and limits in CINEMATIC-QA.md.

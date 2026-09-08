# Art direction and asset provenance

The design centres on a wind instrument: an ivory bell suspended between two unequal golden petals with jade resonators. It is a symbol of listening, not conquest. The same split-petal shape informs the launcher icon, title mark, beacon architecture and the character's small resonator.

Palette: midnight petrol `#102c33`, warm ivory `#f0ecd9`, worn brass `#e4cb98`, jade `#85e3d0`, sage grass, coral garden foliage and blue-green water. Silhouettes are deliberately readable on a phone: roof clusters mark safety; paired pylons mark beacons; arches lead the eye toward history; Aru's crown identifies the final destination.

All meshes are original procedural constructions in `art.gd` and `world.gd`; no ripped game models, logos, characters, maps or music are included. Static set dressing is converted into shared mesh/material MultiMeshes. Grass is independently culled by spatial cell and displaced by a wind shader. The dynamic sky, water, terrain vertex colours and directional shadows use features suitable for the Compatibility renderer.

The built-in image generation tool generated `assets/branding/icon-art.png`. Packaging only resizes this original for the legacy launcher. The adaptive foreground is a separately authored vector interpretation with transparent padding, paired with a full-bleed background and monochrome mask. The default splash is a matching compact emblem; the title screen is rendered over the actual game world.

Generation prompt, 8 September 2026:

> Use case: logo-brand. Asset type: final Android game launcher icon for an original 3D fantasy adventure named Astra World. Generate one square full-bleed 1024x1024 app icon, premium art-directed hand-painted 3D fantasy artifact, no text or letters. Original emblem: a slender luminous ivory wind-bell with a jade teardrop clapper, suspended inside an asymmetric split crescent made of two upward sweeping champagne-gold metal petals; the left petal taller than the right, engraved shallow flowing wind channels, with a small hovering jade diamond precisely above the bell. This is a crafted sacred wind instrument, distinct silhouette, not a generic star, not a compass. Deep midnight petrol-teal background with a very restrained turquoise atmospheric halo. Center the entire emblem within central 62% safe square suitable for Android adaptive masks, generous edge breathing space, single bold readable shape at 48px, beautifully beveled ivory ceramic and warm brushed gold, bright teal center, studio-like rim lighting, a sense of hopeful mysterious windswept ruined islands. Sophisticated stylized fantasy game aesthetic, original IP. No border frame, no rounded rectangle, no mockup, no text, no watermark, no recognizable game logo. Output includes background, polished ready-to-ship icon.

The tool delivered a 1254×1254 PNG. The vector adaptation intentionally simplifies small details and places the full emblem within Android's central safe zone.

The three music cues and ambient loop are synthesized from original note sequences, harmonic instruments, shaped noise and stereo delays. There are no external audio samples. Twelve WAV effects cover movement, melee, skill, health, telegraph, impact, gathering, menu and beacon feedback.

DejaVu fonts are included under their redistribution terms; see `assets/branding/FONT-LICENSE.txt`. Godot is used under the MIT license; its notice is included in `GODOT-LICENSE.txt`.

# Astra World

Original offline 3D adventure for Android, set in **Lembah Aeralis**. All player-facing text is Indonesian. This project lives entirely in `astra-world/`; the only root addition is its dedicated Android workflow. Existing Wynn Store and Road to Immortal files are not modified.

Nara follows a pocket wind-bell to a valley slowly losing its memories. Forge a tuning blade, restore three beacons, release the keeper Aru from his vigil, and return to Teralun for the Region I ending. The region remains explorable afterward.

## Play

Android: landscape; left thumb moves, right-side drag rotates the camera, and separate buttons attack, dodge, jump/glide, heal and cast Echo. Touch the nearby object's name to interact. The pause menu includes a control guide. The game needs no account, network connection or purchase.

Desktop: WASD, right mouse drag camera, J attack, Q Echo, K dodge, Space jump/glide, Shift sprint, E interact, H heal, M map, I inventory, Esc pause.

## Build from source

Use **Godot 4.6.1 standard**, not the .NET editor. Open `project.godot` and import. All final runtime art, audio and fonts are included. Run the main scene with F6/F5 or `godot --path .`.

Android release export requires OpenJDK 17, Android SDK build-tools 35.0.0 and matching Godot Android export templates. Set Android SDK and Java SDK paths in Editor Settings, then run:

```bash
mkdir -p build
godot --headless --editor --import --path .
godot --headless --path . --export-release Android build/Astra-World-unsigned.apk
```

The unsigned release is deliberately separated from the signing key. Sign it using Android `apksigner` and the private Astra World update key. **Never commit a private keystore or its password.** Use the same key, `com.wynndev.astraworld`, and an increased `version/code` for compatible updates.

`.github/workflows/astra-world-android.yml` performs source import, startup, APK export, package inspection, and an input-driven rendered Region I playthrough. It uploads build logs, screenshots, results and the unsigned APK. A build artifact is not evidence of physical-device performance; consult `docs/VERIFICATION.md` for the actual tested scope.

## Content

- A continuous island roughly 250 metres across, seven named landmarks, Teralun village, an aqueduct, garden, pool, archive ruins, secret garden and boss arena.
- Third-person locomotion, spring-arm camera collision, jump, sprint, unlockable glide, dodge invulnerability, three-hit attack rhythm and an area skill.
- Twelve persistent encounters including an elite and a two-phase boss with timed area attacks.
- Three beacon objectives, opening and ending dialogues, a three-letter side quest, an ordered rune puzzle and eight treasure chests.
- Guaranteed introductory materials plus scattered resources; four crafting recipes; three weapons, a mantle, a charm and healing consumables.
- Level progression, equipment, journal, map with discovery and unlocked fast travel, inventory, pause/help/settings, automatic saves with a previous-good fallback.
- Low–Ultra presets plus independent frame cap, render scale, shadow, particle, foliage, audio and camera controls.
- Original layered procedural music, ambience, action/UI sound design, vector branding and generated icon artwork.

## Source organization

| Path | Responsibility |
| --- | --- |
| `scripts/data.gd` | Authored region, lore, quests, items and recipes |
| `scripts/game.gd` | Session orchestration and progression rules |
| `scripts/world.gd` | Deterministic terrain, dressing, collision and landmarks |
| `scripts/art.gd` | Original shared mesh kit and articulated characters |
| `scripts/player.gd`, `enemy.gd`, `fx.gd` | Movement, combat, animation and effects |
| `scripts/state.gd` | Validated save/load and inventory transactions |
| `scripts/ui.gd`, `hud.gd`, `map.gd` | Menu UI, multi-pointer touch controls and map |
| `scripts/audio.gd` | Score selection and sound playback |
| `tools/generate_assets.py` | Deterministic score/SFX synthesis and icon packaging |
| `tests/playthrough.gd` | Input-driven rendered campaign verification |

Regenerating packaged assets requires Python 3 with NumPy/Pillow, Inkscape, ffmpeg with Vorbis support, and DejaVu fonts. `python3 tools/generate_assets.py` creates the same family of sound and branding assets without a paid service. The original generated icon is retained with its prompt in `docs/ART-DIRECTION.md`.

This is a deliberately compact original indie region. Its small-team procedural visual vocabulary and authored content should not be mistaken for the asset density, cinematics or production scale of a large commercial open-world game.

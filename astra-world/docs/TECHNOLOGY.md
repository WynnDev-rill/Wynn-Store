# Technology decision

## Engine and environment

The available workspace had Java 17 and asset scripting tools, but no installed Unreal Engine, Godot editor, Android SDK, display server or Android device/emulator. Unreal's official Linux setup requires obtaining an Epic account-linked installed/source build; the official recommended hardware includes 32 GB RAM and a dedicated GPU. The available workspace had approximately 15 GB RAM and no hardware graphics device. An Unreal Android end-to-end pipeline was therefore not an available, credible choice in this session.

Godot 4.6.1 standard was selected for deterministic headless imports, prebuilt Android export templates, GDScript iteration and a small self-contained project. The version is intentionally pinned with verified upstream SHA-256 digests. GitHub Actions supplies the build and software-rendered test host. The user's instruction to produce an APK takes precedence over preference for an unavailable engine.

Renderer: OpenGL Compatibility. Priorities are phone coverage, one renderer for desktop CI and Android, shared static geometry, limited shadow range, authored colours and atmosphere. No ray tracing, Lumen, Nanite, screen-space global illumination or desktop-only effect is claimed. Low–Ultra change real render scale, foliage density/culling, shadows, effects and frame caps; a high preset cannot manufacture GPU capacity.

## References consulted

Primary sources, accessed 8 September 2026:

- https://dev.epicgames.com/documentation/en-us/unreal-engine/linux-development-quickstart-for-unreal-engine
- https://docs.godotengine.org/en/4.6/tutorials/export/exporting_for_android.html
- https://docs.godotengine.org/en/4.6/tutorials/rendering/renderers.html
- https://docs.godotengine.org/en/4.6/tutorials/performance/gpu_optimization.html
- https://github.com/godotengine/godot-builds/releases/tag/4.6.1-stable

## Budget choices

The island is authored rather than infinitely generated. Every main progression resource has a guaranteed placement, quest encounters are persistent, and defeated sentries do not respawn to inflate playtime. Materials have no inventory weight or paid replenishment. Resting supplies a minimum of three healing consumables so death does not create a resource soft lock. Fast travel requires finding a safe village or restoring a beacon, and is unavailable near active enemies.

The source can support more content, but Region I is designed to end. Completing the ending does not lock remaining exploration or side activities.

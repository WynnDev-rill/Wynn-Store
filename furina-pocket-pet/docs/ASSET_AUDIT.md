# Asset-first audit — 26 October 2026

| Resource | Findings | Decision |
|---|---|---|
| [Furina BOOTH 5876181](https://booth.pm/en/items/5876181), 狐宮静商店 / @Jackche62136792 | VTS runtime ZIP, 243 MB; no editable CMO3. Listing describes wave, angry, heart and shy actions. Prohibits secondary distribution and commercial live-stream use; permits specified Genshin-related video use under official fan rules. Desktop pet use is not clearly granted. | Do not download/repackage/distribute. Link to creator; import only a user-owned authorized local ZIP. Exact rig compatibility has not been verified. |
| [WhaleGirl Pet](https://github.com/7l-ui/whalegirl-pet), commit `ea296fb06bb7daf68dbd390b7d11e7cf001d0b87` | MIT source; upstream third-party notice explicitly includes `art/` and runtime model under MIT. AI-generated artwork; standard MOC3, texture, physics, Idle/Blink/Nod/Shake. WebView renderer and Operit chat are outside this app's scope. | Reuse the complete licensed runtime model as a test fixture, preserve MIT notice, pin all files by SHA-256. Reuse architecture findings; do not include chat, WebView or auto-rig tooling. Original artwork provenance beyond upstream's representation is not independently verified. |
| [Akimeji](https://github.com/hushino/akimeji-shimeji-and-padorus), commit `8695b5aa7c51d1900ee424d3abaa66e99429eda2` | Compose UI + bounded WindowManager overlays and sprite behavior. README says “MIT (or repository license)” but the checked root has no LICENSE. Sprite renderer does not drive Live2D meshes. | Study platform patterns only. Copy no code or character assets without explicit license confirmation. |
| [Cubism SDK Java R5](https://www.live2d.com/en/sdk/download/java/) | Official Android AAR, ARM64/x86/x86_64; native Core under Live2D Proprietary Software License, Framework under Live2D Open Software License. SDK sample describes loading MOC3, physics, motion, expression and renderer pipeline. | Use official Java Framework and JNI Core, not WebView. Build-time download with exact ZIP checksum `2bccf7b3a6cc6fdaeade2cf1d7a6e544cb4ad71912d0131565648baa6d515bd3`. No SDK binaries in git. No public APK publishing until license approval. |

Architecture: Compose settings → local preferences/model store → user-started `specialUse` foreground service → bounded translucent GLSurfaceView → Cubism Java Framework/JNI/OpenGL. A pure Kotlin state engine drives timing and movement; the model renderer drives existing rig parameters and files. No native walking cycle is invented when a rig lacks one.

Licenses and primary references:

- [Live2D Proprietary Software License](https://www.live2d.com/eula/live2d-proprietary-software-license-agreement_en.html), especially 1.5/1.6, 2.1/2.2 and 6.3. The individual/small business exemption excludes expandable applications; “personal” is not a blanket public release exemption.
- [Live2D Open Software License](https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html).
- [Expandable Application classification](https://www.live2d.com/en/sdk/license/expandable/). Approval and publication agreement required before public release. This application's classification is inferred from the official definition because it loads arbitrary local models; Live2D makes the final determination.
- [Official Java sample](https://github.com/Live2D/CubismJavaSamples) and [Framework](https://github.com/Live2D/CubismJavaFramework). Preserve separate Live2D ownership; repository MIT license applies only to app-owned source.
- [Foreground specialUse service](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).
- [Background service starts](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start): start from the app's visible user action, do not rely on an invisible overlay exemption.
- [Window flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCH_MODAL): pass input outside the window; no full-screen interception.
- [Android 12 overlay touch protections](https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events).

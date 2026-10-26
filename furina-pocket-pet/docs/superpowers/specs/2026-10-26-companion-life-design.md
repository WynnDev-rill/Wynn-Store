# Companion life 1.3

The owner requested implementation, research and verification in this session. Keep Kotlin/Compose, Cubism Java and the native renderer, existing model choices, original signing identity and private asset boundaries.

## Intended experience

Furina should have quiet, sustained activities, react differently to a head stroke and a body poke, occasionally prefer a screen edge, and let the phone remain usable. Brief Japanese interaction clips drive real mouth movement. Controls remain available while Ghost Mode passes touches through.

## Approaches considered

1. Port VPet/Shimeji or a desktop Electron pet: mature behaviors but incompatible WPF/sprite/WebView runtime and character-specific animations. Rejected.
2. Replace the supplied Furina with an unrelated model or generate pose images: loses model identity and existing rig/physics. Rejected.
3. Extend the native engine, reuse the supplied rig/motions/expressions, compose small activity props, and accept compatible model-owned activity motions. Selected engineering approach under the owner's implementation authorization. Authored sitting/swing assets are preferred; unsupported poses must remain explicitly unavailable rather than advertising a standing model as seated.

## Design

- Time-based activity phases and energy/interest influence weighted choices without needy notifications or punishment. Touch preempts any activity. Quiet hours favor rest. User can disable autonomous activities.
- Read: sustained downward eye tracking, focused brow, head tilts and page turns with an open book. Cake: small mouth/eye/chewing reactions with a plate, fork and disappearing bites. These are native vignette activities, not newly authored hand/leg rigging.
- Seat/swing: use only an existing compatible model motion with an explicit seat/swing mapping. A missing sitting pose is shown as unavailable. Do not distort static legs or transplant another character's Cubism curves.
- Head strokes are distinguished from a drag using bounded travel, accumulated path and event timestamps; body taps have a separate brief expression. Double tap keeps hop; hold keeps settings. Canceled touches never emit a tap/voice.
- Screen edge movement eases to a side and can peek with content clipped inside the existing bounded overlay. No screen reading. Keyboard visibility is not reliably exposed to a non-focusable overlay; do not claim automatic typing detection without evidence.
- Ghost Mode uses FLAG_NOT_TOUCHABLE and window alpha below Android's untrusted-touch threshold. Notification and settings restore interaction. Screen-off/pause/hide/stop stop sound and animation; rotation preserves bounded position.
- Local short voice playback, no microphone permission or online runtime requests. Precomputed PCM RMS envelopes sampled at playback position drive model LipSync IDs, with verified ParamMouthOpenY fallback for models whose LipSync group is empty. Respect silent/DND, ducking/focus loss, volume, cooldown and lifecycle.
- Public source and CI contain no private Furina model/audio. Owner assets are packaged only through ignored personal assets. Existing public publication gate remains unchanged.

## Acceptance

Pure tests cover states, interruption, bounded edge movement, stroke/drag/cancel, envelope silence/peaks and cooldown. Android instrumentation must exercise actual overlay flags/passthrough, controls restoring Ghost Mode, activity pixels, model parameters, audio failure/completion/cancellation and existing import/lifecycle regressions. Record observed outcomes separately from physical Poco F6 and unsupported authored pose limitations.

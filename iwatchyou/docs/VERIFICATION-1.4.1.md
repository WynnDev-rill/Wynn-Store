# IWatchYou 1.4.1: timer controls and concise permissions

29 October 2026 (Asia/Jakarta). Version 1.4.1, code 15, package
`id.wynn.iwatchyou`. Native source digest:
`618dcc538412a559b422fd3e69cecf39f9c0ee2d7f20a15a60b8c8bf20a5c20a`.

## Changes

- The timer pill retains its appearance. Replace the stacked hide/resize icons
  with one solid, vertically centered inline arrow. Tap hides; drag moves.
- The screen-edge restore tab appears only after explicit hiding. Automatic
  dimming leaves the timer visible and its body touch-through, without an edge tab.
- Move resizing to Settings → Ukuran timer: one native 50–100% slider with an
  immediate percentage, Reset and Done. Scale and hidden state remain persisted.
- Replace the long setup tutorial with three concise permission rows and a brief
  local-data privacy statement. Keep restriction checks, error-only manual
  confirmation and vendor-settings fallback. Remove redundant dashboard guidance.
- Shorten the Android accessibility-service label to IWatchYou and its description
  to the actual package-name/local-history purpose.
- Time accounting, foreground selection and the SQLite history schema are unchanged.

## Verification

The regression-only commit `3d5da295e2c9f06a069e4cb025f78a35bf92e24d`
failed its two half-size bounds assertions against the older 80% minimum.
The verified candidate commit is `5142f1a72a69d278fb6a102035bce8adfd4ee636`.

[Source/device run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40808771325):

- 49 JVM tests passed, zero failures.
- Python QA outcome regression passed.
- Optimized release, debug, instrumentation and unsigned legacy variant built.
- Release lint: zero errors, 22 warnings and 1 hint; not a warning-free claim.
- API 30 and 36: all 11 native cases passed on each, history-upgrade fixture passed, and optimized release installation/launch/overlay smoke passed.
- Visual review completed: native captures of expanded/faded/hidden controls,
  the half-size timer and permission setup at normal and 200% text. The slider
  physically selected and persisted 50%, producing a half-width timer with a
  separate usable inline control. Dimming alone left the edge tab absent.

Graphify was refreshed locally using the pinned 0.9.97 AST extractor: 360 nodes,
752 edges, 20 communities. The external UI analyzer was not used; manual review
uses the actual native emulator captures. No physical POCO/HyperOS test was run.

## Direct-update APK

The CI release artifact is signed outside CI with the same dedicated owner key
as the delivered 1.4.0 APK. Both certificates were verified against SHA-256
`96065f69e2665ad2f264f5229a0d5b765f3b0bc78d677eca3bf3276286c7e4a3`.
The 1.4.1 APK has v2/v3 signatures, and every unsigned ZIP payload entry matches
CI output. It can update that 1.4.0 installation without uninstalling or deleting
history; it cannot replace older installations signed with another key.

APK size: 2,077,531 bytes.
APK SHA-256: `6bf1c3385fb98c36bda0344ce0f3a8de5b2af150507a87768ce5a7ec39453e27`.
Public stable feeds and older releases are unchanged. Private signing material
remains outside the repository and CI artifacts.

## Follow-up verification findings

Use directional chevron vectors instead of rotating an entire overlay View;
window and touch geometry then stay in screen coordinates. UI tests target
Slider track bounds and the actual Material button enabled state. Native
screenshots confirm the compact permission dialog remains usable at 200% text.

Cold timer creation previously checked isAttachedToWindow before creating the
separate inline control, delaying that control until dimming. Create it with the
registered timer window instead. Use parent registration for layout updates and
remove registered controls even before their first attachment callback, avoiding
orphan windows during rapid switches. The half-size/cold-show case covers this.

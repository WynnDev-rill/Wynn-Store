# ScreenGuard Pro — UI UX Pro Max design system

Native Android / Kotlin / Jetpack Compose. Reference: the supplied seven-screen white-blue dashboard and Orion Store's floating bottom navigation. Only the bottom navigation pattern is adapted from Orion; no store/catalog branding or content is copied.

## Source and deliberate adaptations

UI UX Pro Max is vendored under `.agents/skills/ui-ux-pro-max` with its MIT license and upstream notices. Run from this folder with Python 3; no package installation or network is required.

Searches reviewed:
- `digital wellbeing mobile calm blue --design-system`: enterprise gateway and editorial type did not fit a native wellbeing utility; rejected.
- Retry `wellness productivity dashboard --design-system`: flat mobile UI, Plus Jakarta Sans, fast transitions and simple onboarding fit. The website hero/video layout and teal/orange palette do not fit the user's reference; retained the white-blue palette and native dashboard layout instead.
- `navigation animation typography --stack jetpack-compose`: typography tokens, animation in composition, state/navigation separation.

## Visual tokens

- Bundled Plus Jakarta Sans, static weights 400/500/600/700, SIL OFL 1.1. No downloadable-font dependency. All 15 Material typography roles explicitly use the family.
- Blue #2366D1 on white; pale blue #EAF3FF; ink #142137; secondary text #58677C. Dark counterparts defined separately. Surface container roles are explicit, avoiding default purple accents.
- Body 14/21sp, body large 16/24sp, title 15/22sp semibold, page title 20/28sp bold, metric 32/40sp bold. System font scaling remains enabled.
- Gutters 20–24dp; card radius 24dp; inputs/list rows 20dp; dock capsule; section spacing 12–24dp; touch targets >=48dp.

## Navigation and state

Four persistent dock destinations: Beranda, Bekukan, Jaringan, Pengaturan. Statistics and the full usage list are secondary Beranda destinations. Per-app wait/math challenges, manual freezes and optional automatic limits belong to Bekukan. Unlock/permission flows hide the dock. Back returns to the relevant parent; tab changes preserve search, filters and scroll state with SaveableStateHolder.

Selected dock item shows icon plus text when space allows; at large font sizes/small widths all icons retain accessible labels and selected tab semantics. Content reserves dock and gesture-bar space.

## Motion

- Dock selection: 150–180ms tint, damped native spring for capsule allocation.
- Page change: 220ms fade + 280ms short directional translation, outgoing 120–180ms.
- Chart reveal: one 480ms eased progress, never continuous polling animation.
- Tabs and disclosure: brief color/disclosure transitions. Download progress reflects actual bytes.
- Standard Compose duration scaling honors Android animator-duration/reduced-motion settings. No infinite decorative animation, forced frame loops, blur shaders or animated background.

## Feedback and privacy

Permission failure guides users through Android's Restricted Settings; no claimed automatic bypass. Offline update checks never block opening the app and do not claim success. Updates require version/package/SDK/checksum/signature checks and the Android install confirmation. Usage data stays local.

## Verification

Automated UI interaction, tab-state retention, restricted-settings help, compact dock at 2x text, dark/landscape screens, screenshot captures, JVM update validation, Android lint and optimized APK cold launch. Physical HyperOS remains a separate device check.

## 1.2 reviewed refinements

Relevant UI UX Pro Max queries: `digital wellbeing app freezing four floating tabs interactive usage chart accessible --domain ux`, `bottom sheet lists animation accessibility --stack jetpack-compose`. Native semantics replace web ARIA guidance; choices wrap instead of shrinking text. Charts expose selected-day buttons with date/duration labels; horizontal scrolling retains 48dp day targets on small displays. No slogan in the home header. The user-supplied original raster brand artwork intentionally overrides generic vector-only advice; native adaptive launcher layers apply system masks while preserving the supplied full-color artwork. Gate controls use a native modal sheet, retained per-app state, explicit wait/math progress, and standard system motion scaling.

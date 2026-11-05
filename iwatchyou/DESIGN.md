# Visual system — IWatchYou

Mode: Operate. Platform: native Android/Compose, Indonesian. Follow the supplied
Dribbble references, preserving product truth in PRODUCT.md and all existing flows.

## References actually inspected

Both supplied pages were opened in a live browser on 2026-11-05. Their original
media were downloaded to the research workspace and visually inspected. Main:
https://dribbble.com/shots/11297277-Digital-Wellbeing-App (ilham yoga / Omnicreativora).
Observed: near-white cool ground, indigo type, blue emphasis, purple/cyan/green
segmented circular chart, light chart tracks, restrained rounded white surfaces.
Overlay: https://dribbble.com/shots/24002424-Mobile-Widget-animation (Ronas IT).
Its poster is blank; the actual MP4 was inspected as a 1fps contact sheet covering
12 seconds. Observed: compact black island expands into a rounded task widget,
white time digits, green active indicator, and later a lock-screen widget. This
is an iOS task example, not evidence of Android stopwatch functionality.

Adapt rounded dark compactness and short eased transitions. Do not copy expansion
into task details, task/avatar UI, lock-screen behavior, focus modes, app limits,
notification/unlock counters or imaginary comparative statistics.

## Tokens and assets

- Background #F7F8FD, surface white, ink #20245C, secondary text #646B88.
- Action blue #1769E0; chart purple #8270EA, cyan #008EAA, green #138563;
  muted track #E8ECF6. Chart colors have text duration/name counterparts.
- Bundled Plus Jakarta Sans (regular/medium/bold), existing OFL license retained.
- Existing authored clock/power vectors, Material Settings icon, and actual Android
  app icons resolved off the UI thread. No decorative bitmap/video is shipped.
- Spacing 4/8/12/16/24dp; 28dp summary/dialog corners, 20dp medium controls.
- Minimum interactive target 48dp; use native controls and semantics. Large system
  type moves the central duration outside the ring instead of shrinking user text.

## Layout and behavior

Header/settings → screen-time composition → hourly rhythm → date-selectable app
ranking → timer help. Monitoring control remains visible below the scrolling body.
The ring shows the five leading app durations divided by all recorded app time;
remaining/tiny shares keep the pale track. It is not progress toward an invented
goal. Its center is unlocked screen time, which includes Home and IWatchYou.
Hourly graph remains unlocked screen duration. App rows remain one focused app's
local-day total, sorted descending; lazy list and async icon loading are retained.
Historical date survives rotation. At widths of 600dp and above, the summary uses
a horizontal 112dp ring and a separate text column to fit short landscape screens. Dates, permission/HyperOS advice, notification
permission, update download/consent/error flows, settings and monitoring off stay.

Overlay: dark #141722 pill, white 16sp tabular time, 48dp minimum height, subtle
cool border, readable 76% dimmed / 97% active opacity. Native Chronometer remains
one-second ticking, no Compose runtime or custom timer loop in the overlay. Existing
fade after 3s, tap-to-unlock, drag, rotation positions and screen/PiP/IME rules stay.
200ms decelerated opacity transitions respect ValueAnimator.areAnimatorsEnabled.
Compose native animation scale is respected; no perpetual ring animation, blur,
network assets, additional permissions or per-frame database writes.

## Skill integration

Ponytail minimizes dependencies/complexity; UI UX Pro Max provides native accessibility
and semantics guidance; Impeccable audits hierarchy, typography, states and renders.
Upstream UI UX Pro Max's broad wellbeing query returned a marketing/enterprise
pattern: rejected as unrelated, not persisted as design truth. A narrower Compose
accessibility query returned contentDescription/semantics guidance and was applied.
The user reference direction overrides generic stylistic suggestions.

## Verification gates

Build release/preview/debug/test APKs, JVM tests, release lint, QA-outcome host test,
native API 30/36 regressions, plus populated historical portrait/landscape/2x type
captures and a preview overlay capture. Inspect actual renders; report physical
HyperOS and battery uncertainty. Do not substitute a browser mockup for the app.

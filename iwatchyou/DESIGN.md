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

Adapt the actual compact-to-expanded interaction, not merely a dark color. The
owner explicitly requests an edge handle, a smaller visible timer, an expanded
app identity/duration panel, and real weekly history. System HyperOS islands are
not a dependency: implement our own native accessibility window on Android.
No task/avatar UI, lock-screen timer or invented statistics.

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

Header/settings → screen-time ring with real app icons aligned to segment midpoints
→ top five app rows → Lihat selengkapnya. Detail provides Harian/Mingguan, all app
rows, previous/next seven-day windows and tappable days. Weekly totals sum recorded
local days, including unlocked screen time outside apps; missing dates are zero.
Hourly accounting remains stored but the hourly rhythm UI is removed by request.
Keep permission privacy disclosure in setup, not repeated on the dashboard.

Overlay: three states in one TYPE_ACCESSIBILITY_OVERLAY window. Compact visual
pill is 36dp high inside a 48dp touch area, approximately128dp wide with13sp time;
large fonts grow the window. Tap the clock to expand to app icon/name and24sp time.
The explicit edge button docks a16×36dp handle inside a48dp target. Tapping restores
the clock. Hidden mode persists across app changes/rotation/service reconnect and
never pauses time. Dimmed clocks drag immediately; tap-to-unlock is intentionally
replaced. Expanded panel provides collapse and hide actions. Native Chronometer
stops while hidden. Short220ms geometry and180ms opacity transitions honor disabled
system animation, no perpetual animation, polling, new permission or dependency.

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

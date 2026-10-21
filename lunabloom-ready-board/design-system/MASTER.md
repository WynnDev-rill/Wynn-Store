# Lunabloom — design master

UI/UX Pro Max compatible product-specific direction. Primary visual reference: the approved Lunabloom white-and-mint ready-board mockup from October 2026. This overrides generic dashboard templates.

## Product promise
Tap a half-hour slot, instantly learn who *might* play. One page, no personal information on screen, no account form. Mobile-first; desktop works equally well.

## Visual language
- Warm-off-white background #F8FAF8, pure white cards, ink #203029, muted text #7C8A80, forest-green #2D945B and pale highlight #EDF8F0.
- Modern Manrope + DM Sans; fluid sizing, zero gimmicky neon/glassmorphism/3D. Semantic hierarchy and generous breathing space.
- Rounded 11–22px surfaces with light borders, no excessive shadows or decorative images.
- Icons only as supplementary wayfinding for dayparts and rules, never as the sole label.

## UI behavior
- 48 half-hour slots grouped 05:00–04:30 Jakarta time, sorted in service-day order.
- Limit exactly five selections per anonymous visitor; tapping a selected time deselects it. Selected state uses a soft green card background and check.
- Each cell uses up to five visible green dots and +N overflow beneath. An actual server count is never replaced with fake numbers.
- Peak time is the highest participant count on the active service day, not a pledge to play.
- Counter 0/5; daypart chips available, not required for operation. No yellow certainty tier.
- Reset daily at 05:00 Asia/Jakarta, including rollover while tab is open.
- Transparent connection status; nonconfigured site explicitly says preview and never falsely claims realtime.

## Accessibility and engineering QA
- Focus-visible keyboard states, aria-pressed on each toggle, dialog with semantic labels, touch targets ~44px when practical, reduced motion, meaningful text under visual dots.
- Keyboard/screen reader support, <=375px phone layout, no horizontal overflow, no layout shift on count changes.
- No token other than a public Supabase publishable key is ever bundled client-side. No direct personal selection row exposure. Aggregates only over realtime.
- Never modify the existing Wynn-Store root app to build or deploy this subproject.

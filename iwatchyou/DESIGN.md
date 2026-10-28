# IWatchYou design context

Mode: Operate. Native Android 11+, primarily Xiaomi/POCO/HyperOS; Indonesian copy.
Preserve the existing Plus Jakarta Sans family, white/blue dashboard, light surface
`#F6F8FC`, text `#142033`, secondary text `#637087`, accent `#246ADC`.

The timer is a restrained dark pill, maximum the existing 134×44dp footprint (grows
only when system font scaling needs more room). Supported user scale: 80–100%;
12sp minimum base timer text. Upper-right chevron hides; lower-right grip shrinks
on tap or resizes horizontally on drag. Hidden state and size persist. The solid
48dp edge handle restores editing. After 3 seconds the body dims, removes all
accessibility actions and sets FLAG_NOT_TOUCHABLE, including corner controls.
No invisible hit targets remain over other apps. Settings supplies a full-size reset.
Safe-area clamping applies to both orientations and every size change.

History dates come only from persisted rows or the active live day. Comparisons
name the previous recorded date, never substitute zero for an unobserved date, and
explain that the current day is incomplete. Hourly bars expose actual durations.
No history cutoff, synthetic pre-install records, network upload or destructive
schema migration. Empty, unreadable and not-yet-monitored states are distinct.

Setup requires unused-app pause to be disabled when Android reports it enabled.
An unobservable vendor setting is never claimed as verified. Explain manual
confirmation on API failure, and route autostart to vendor settings with App info
fallback. Setup content scrolls with large system text.

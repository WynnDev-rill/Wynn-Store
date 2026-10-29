# IWatchYou design context

Mode: Operate. Native Android 11+, primarily Xiaomi/POCO/HyperOS; Indonesian copy.
Preserve the existing Plus Jakarta Sans family, white/blue dashboard, light surface
`#F6F8FC`, text `#142033`, secondary text `#637087`, accent `#246ADC`.

The timer retains its restrained dark pill, font and shipped 134×44dp maximum
(growing only for system text size). User scale is 50–100%, persisted through a
single native Slider in Settings → Ukuran timer. A separate solid 32dp circular
chevron follows the pill, centered vertically. Tap hides; drag moves. There are
no stacked corner icons or on-bubble resize grips. The body dims after 3 seconds
and passes touches through; the solid inline control remains explicit and usable.
Only a user collapse produces the 32×48dp edge tab. Restoring removes that tab.
Persist hidden state and separate portrait/landscape positions. The tiny 50%
option is an explicit owner request; its type scales proportionally.

The permission dialog is a compact list: Jeda aplikasi, Mulai otomatis,
Aksesibilitas. Show real status and one short action per row. Keep only a brief
local-data privacy line. No tutorial paragraphs, repeated setup instructions or
long timer guidance in the dashboard. Preserve error recovery and honest unknown
vendor states, and keep the dialog scrollable at 200% system text.

History dates come only from persisted rows or the active live day. Comparisons
name the previous recorded date, never substitute zero for an unobserved date, and
explain that the current day is incomplete. Hourly bars expose actual durations.
No history cutoff, synthetic pre-install records, network upload or destructive
schema migration. Empty, unreadable and not-yet-monitored states are distinct.

Setup requires unused-app pause to be disabled when Android reports it enabled.
An unobservable vendor setting is never claimed as verified. Explain manual
confirmation on API failure, and route autostart to vendor settings with App info
fallback. Setup content scrolls with large system text.

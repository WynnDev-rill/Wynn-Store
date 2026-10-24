# ScreenGuard Pro 1.5.0

Requested change: manual app freezing and battery-conscious operation, with retained and redesigned informational activity statistics.

- Default freeze collection; no All/Open category chips. Empty content has only the bottom-right blue add FAB. Actual Android state drives grayscale icons and the snowflake badge. Opening unfreezes and launches without a challenge or mandatory timer. The app stays in the collection for refreezing.
- Three destinations: Bekukan, Aktivitas, Pengaturan. Remove product controls for screen-time targets, parental PIN, math/wait challenges, daily limits, schedules and internet blocking. Retire the foreground service/notification permissions and hourly rather than 15-minute statistics snapshots. No battery percentage saved is fabricated.
- Upgrade migrates old state non-destructively, preserving app-disable ownership and manually selected apps; retires enforcement and safely restores formerly applied network/scheduled locks when Shizuku is available. Usage history and display settings remain on device.
- Activity periods day/7/30, bounded dates, explicit durations beside daily bars, full application ranking, per-app sessions/longest session/percentage/average, a 24-hour chart plus exact hourly rows. Partial and unavailable history remains labelled, without treating missing days as zero.
- Grayscale applies to icons inside ScreenGuard. Android launchers cannot show an icon for a disable-user package; ScreenGuard does not modify other apps' actual resources.

Validation includes migration, aggregate boundaries, UI empty/managed/disabled states, picker multi-select, retained period navigation, dark/large text/landscape, real shell Shizuku freeze/open/refreeze/remove operations, real usage-duration fixture, original-certificate signed upgrade from 1.4 and live original-client updater installation. Physical HyperOS remains the final user device check.

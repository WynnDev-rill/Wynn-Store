# ScreenGuard Pro 1.4.0

Authorized scope (23 October 2026): user reported inaccurate per-app/screen statistics, intrusive persistent notification, inconsistent icons and missing freeze conveniences. User requested implementation and an in-app production update. This replaces the earlier audit-selection workflow; no unselected audit-wide redesign is included.

## Statistics

- Compute screen time from interactive/unlocked screen events, independently from per-app foreground durations. Split-screen overlap is not double-counted in screen totals. On ROMs without display events, use the foreground interval union and label the fallback.
- Bound app sessions to local midnight, screen-off/keyguard, and known lifecycle events. Discard unknown shutdown tails per Android documentation. Show foreground sessions as sessions, not an asserted count of icon taps.
- Replace legacy expanded UsageStats aggregates with event-derived daily/hourly snapshots. Keep the legacy table on disk; unavailable history is unknown, not invented zero. Partial retention coverage is labelled. Snapshot recent data using existing WorkManager work; no extra permission or foreground service for statistics.
- Refresh while the UI is resumed and offer manual refresh. Show unlock count, apps used, data freshness, and preceding/following dates in app details. Per-app shares use app totals, not physical screen time.

## Freeze and notifications

- Search/filter the curated collection, choose multiple apps on adding, show temporary-lease countdown, open an explicit app action menu, freeze now/all active leases, inspect Android app info, and manage the existing automatic rules.
- Keep optional challenges and four root destinations. Restore direct app context from statistics. Retain safety protections and original signing identity.
- Manual access leases are enforced by the existing Shizuku daemon; they no longer keep a foreground notification alive just for the client. Actual automatic limits/schedules still use a foreground service. Its new quiet notification category has no alert/badge and can be hidden in Android's per-category settings through a direct Settings entry. Android may continue to show active apps in system UI.
- Use one new launcher resource reference and the supplied SG brand image at an adaptive safe inset. Refresh package icons with package update timestamps; load icons for disabled apps explicitly.

## Compared applications (documentation inspected 23 October 2026)

| Reference | Inspected mechanism | Applied / rejected |
|---|---|---|
| StayFree, Android Play listing https://play.google.com/store/apps/details?id=com.burockgames.timeclocker | Daily/detailed usage history and breakdowns | Apply clear daily history/data coverage; omit cloud sync and new tracking permissions. |
| ActionDash https://actiondash.com/ | Usage breakdowns, unlock frequency, daily/weekly/monthly trends | Apply unlock count, real refresh and separate totals; omit unrelated email features. |
| YourHour, Android Play listing https://play.google.com/store/apps/details?id=com.mindefy.phoneaddiction.mobilepe | Daily dashboard, timelines and daily/weekly/monthly reports | Apply detail/date navigation and data freshness; omit floating overlay and accessibility service. |
| Hail https://github.com/aistra0528/Hail | Distinct launch/freeze/unfreeze and group/all operations | Apply explicit app menu, multi-add, freeze all open leases. Keep SG challenges and ownership checks. |

These are qualitative documentary references, not comparative device tests or proof of measured accuracy. No comparator assets or code were copied.

Android sources: https://developer.android.com/reference/android/app/usage/UsageEvents.Event and https://developer.android.com/reference/android/app/usage/UsageStatsManager . queryUsageStats may expand range bounds; shutdown events have an unknown tail. ScreenGuard cannot reconstruct events already deleted by Android or guarantee equality to proprietary HyperOS statistics. Physical HyperOS testing remains the final user test after update.

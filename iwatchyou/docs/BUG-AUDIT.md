# Foreground timer / bubble audit

Date: 2026-10-28. Base: `6d7292e90bd5f691f1c12303a08507a581cee5b4`.
Scope: Accessibility focus detection, bubble lifetime, regression coverage and
the CI path that verifies source changes. Published stable APK: 1.0.2/code 3.

## User-visible acceptance criteria

After enabling Accessibility, leaving Settings and opening another app should
show a readable bubble that advances while that app is used. Switching apps
selects the new app's daily accumulated time. Returning Home or locking the
screen hides the bubble and stops app time. A connected service alone is not
the acceptance criterion.

## Findings

### 1. Foreground fallback depends on Activity names or a launcher (high)

In `WatchService.onAccessibilityEvent`, the immediate transition hint was accepted
only for an excluded package, a class ending in `Activity`/`Launcher`, or a package
for which `getLaunchIntentForPackage` returned a launch intent. Activity names
are arbitrary and can be obfuscated. External activities do not have to provide
a launcher entry, and package visibility can also limit package-manager results.

If window roots are unavailable, a rejected transition cannot be recovered by
the window resolver. The previous hint (including the null hint after Home or
IWatchYou) is retained. The bubble consequently stays absent or displays the
previous app's timer even though the service remains connected. Settings passes
the heuristic, which explains why Settings-only tests did not catch this case.

The regression uses a real separate-APK `FocusProbe` Activity, with neither a
launcher entry nor an `Activity` suffix. It disables interactive-window retrieval
and repeats Settings → external app → Home, checking foreground identity, visible
bubble, advancing time and pause at Home. This reproduces a concrete detection
failure; it does not establish that every OEM failure has the same cause.

### 2. Existing staged APK bypasses source QA (high)

`.github/workflows/iwatchyou-android.yml` previously skipped unit tests, lint,
source builds and native device tests whenever an APK for `versionName` already
existed in `distribution/`. That condition is true for the checked-in 1.0.2 APK.
Editing Kotlin without changing the version could therefore verify the old
binary instead of exercising the new code. There was also no pull-request trigger.

Source QA now runs for pull requests and builds regardless of staged APKs, with
native device jobs on API 30 and 36. Production publication is restricted to
`main` and depends on successful source and device jobs. Existing source-digest,
signature and publishing checks remain in place.

### 3. Service readiness and rendered-bubble readiness are different (medium)

The dashboard derives its active state from `WatchService.instance`, which proves
connection but not successful focus selection or window attachment. Overlay
attachment failures were also silently discarded. Treat the real cross-app
bubble checks as necessary; do not infer success from the dashboard or a Settings
screenshot. Physical OEM overlay restrictions still require a device check.

## Architecture decision

Keep event-driven Accessibility and `TYPE_ACCESSIBILITY_OVERLAY`. These provide
focus/window metadata and the bubble without adding a separate overlay permission,
Usage Access permission, foreground polling or per-second database writes. Changing
permission systems alone would not correct the rejected-transition branch above.

Preserve monotonic time accounting, single-app focus, keyboard/overlay/PiP filtering,
bounded window reconciliation, off-main-thread root reads, daily persistence and
the original production identity/history. Do not invent time after process death.

## Verification

Verification results will be recorded here after the baseline and patched tests.
Local SDK/JDK installation is outside the repository; generated files are ignored.

## Physical-device follow-up

On the affected phone, record model, Android/HyperOS version, installed IWatchYou
version and the two apps used for the check. Test launch from Home, Settings and
IWatchYou; switch apps repeatedly; type with the keyboard; open/close notification
shade; lock/unlock; rotate; and return to each app to check daily accumulation.

If the service disconnects, examine the phone's autostart and battery restrictions.
If it remains connected but the bubble disappears, distinguish missed focus
transitions from failed overlay attachment. Never promise an OEM restriction can
be bypassed. No affected physical phone is attached to this environment.

Source fixes are not installed on a phone or present in the old stable APK merely
because this document exists. A production update still requires an increased
version, successful QA, matching source digest and the owner's production signature.
Do not uninstall an existing production install or substitute a debug certificate.

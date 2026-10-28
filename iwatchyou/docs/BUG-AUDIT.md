# Foreground timer / bubble audit

Date: 2026-10-28. Base: `6d7292e90bd5f691f1c12303a08507a581cee5b4`.
Scope: Accessibility focus detection, bubble lifetime, regression coverage and
the CI path that verifies source changes. Published stable APK: 1.0.2/code 3.
Reported affected phone: Poco F6, HyperOS 3.0.304.0, IWatchYou 1.0.2.
The phone's Android base version was not supplied and is not inferred from HyperOS.
The source candidate is 1.0.3/code 4; it has not been production-signed or published.

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
In particular, ordinary launcher apps normally satisfy the existing launcher
query in the manifest, so this defect alone is not proof of the reported phone's
exact cause. Firmware-specific event/overlay behavior remains unverified.

The fix accepts external window-state package metadata without the launcher/class
suffix gate. Dialogs, popups, the app's own overlay, and standard IME window classes
remain excluded from immediate hints. Real-window keyboard/overlay/PiP filtering
and stale-generation checks remain in place. This also removes a package-manager
lookup from the main-thread event path. The strengthened regression opens a real
keyboard, checks rendered timer text advances, and relaunches the fixture from Home.

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
Overlay attachment exceptions now emit a warning tagged `IWatchYou`, without
logging screen text or usage history. Successful attachment alone still cannot
prove that firmware actually leaves the bubble visible in every app.

## Architecture decision

Keep event-driven Accessibility and `TYPE_ACCESSIBILITY_OVERLAY`. These provide
focus/window metadata and the bubble without adding a separate overlay permission,
Usage Access permission, foreground polling or per-second database writes. Changing
permission systems alone would not correct the rejected-transition branch above.

Preserve monotonic time accounting, single-app focus, keyboard/overlay/PiP filtering,
bounded window reconciliation, off-main-thread root reads, daily persistence and
the original production identity/history. Do not invent time after process death.

## Verification

The [baseline source/native run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40622065270)
uses commit `84f661a61f5a4911dce32d2b551f5f1188130b93` (unmodified production code):

- Source build, 24 JVM tests, release lint and release/debug/test APK builds passed.
- API 30: six native tests executed; the new external-app regression failed while
  waiting for the fixture to become foreground. An existing dashboard pause test
  also failed: Settings gained 145 ms after the first observed null foreground.
- API 36: six native tests executed; only the new external-app regression failed,
  at the same external foreground assertion. This establishes the rejected-event
  defect independently of the affected HyperOS phone.
- The QA runner now rejects `OK (0 tests)` rather than treating it as success.

Patched-source validation is tracked in [draft PR #25](https://github.com/WynnDev-rill/Wynn-Store/pull/25).
The baseline API 30 logs expose a readiness problem in the existing pause test:
MainActivity starts at 02:42:51.672 and resumes at 51.755, but the test launches
Settings again at 52.032, before MainActivity's `Displayed` message at 53.001.
The first null foreground does not establish a settled dashboard. The test now
waits for `am start -W` and a dashboard UI marker before measuring the pause;
the strict unchanged-duration assertion remains. The exact cause of the failing
iteration's extra 145 ms is not established by the available event logs.

The [first patched native run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40622894748)
passed source/JVM/lint/build checks, but the expanded regression exposed fixture
readiness problems. API 30 timed out before the external launch because Settings
was already top when window retrieval was disabled, so no fresh transition hint
was guaranteed. API 36 reached external selection, bubble presence and rendered
advancement, then could not locate a keyboard through the package-based UI query.
These runs are not claimed as a passing native suite. The fixture now makes a
fresh Home → Settings transition, waits for actual Activity display, opens the
IME explicitly, and checks its real window metadata. Teardown waits for service
disconnection before another test enables a new instance.

The [next run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40623892975)
confirmed the external selection and rendered timer assertions on both APIs,
but failed at the real keyboard check. The API 30 screenshot shows the fixture
inside a small PiP task above Settings; logcat reports its button as non-clickable.
The fixture shared its default task affinity with the earlier PiP fixture.
`FocusProbe` now has a distinct task affinity and explicitly disables PiP, so the
cross-app/keyboard check runs against a separate fullscreen application window.
This is test isolation, not a change to production PiP accounting.
The old PiP cleanup used unsupported `am start --activity-new-task`; Android's
[`Intent` command parser](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/content/Intent.java)
does not define that option. Cleanup now uses explicit NEW_TASK/CLEAR_TASK flags
and verifies the PiP window disappears instead of discarding an unsuccessful
command and contaminating the following test.

The [isolated-task run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40625108713)
passed all six native tests on API 30. API 36 failed two readiness checks: the old
rootless test launched Settings before Home's transition finished, and the new
fixture's title was drawn underneath the status bar due to edge-to-edge layout.
The test now waits for actual focused Home and Settings windows while preserving
the strict time-growth assertion. The native fixture applies system-bar/cutout
insets so its controls and title are visible on API 36 as well as API 30.

The [inset-corrected run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40626259316)
passed all six tests on API 30 and five on API 36. The remaining API 36 failure
was a test-APK crash: `NoClassDefFoundError: kotlin.jvm.internal.Intrinsics` in
the fixture's inset callback, not a crash of the production application. The
external test process cannot rely on Kotlin classes supplied only by the target
APK. `FocusProbe` is now a self-contained Java Activity; the QA runner checks
fatal exceptions in both the app and external test package.

A [clean control run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40626623724)
reverts only `WatchService` to the original detection logic while retaining the
isolated fullscreen fixture and focused-window preconditions. On API 30 the
fixture is displayed and focused, then the old service fails the external
foreground assertion (DeviceTest line 190). One selected test executed and failed
at that intended assertion. This removes the earlier PiP/readiness ambiguity
from the evidence that the old detection rejects the external Activity.

The patched local build passed all 24 JVM tests, release lint and release/debug/
test APK compilation. Lint reports zero errors and 17 warnings (dependency
versions, backup rules, SDK/KTX, touch accessibility and RTL), which are recorded
rather than described as a warning-free run.
The device-QA shell runner also passed controlled-output checks: zero tests and
failure output exit nonzero; one or six successful tests exit zero.
Local baseline unit/debug builds also passed. A local software-only API 30 emulator
did not complete reliable boot/installation, so it supplies no behavioral evidence;
CI uses accelerated emulators instead. Local SDK/JDK files and generated artifacts
remain outside version control.

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

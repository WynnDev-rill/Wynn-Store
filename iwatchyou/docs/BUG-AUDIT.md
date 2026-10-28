# IWatchYou bubble audit

Date: 2026-10-28. Repository base: 6d7292e90bd5f691f1c12303a08507a581cee5b4.
Reported phone: Poco F6, HyperOS 3.0.304.0, IWatchYou 1.0.2.
The Android base version was not supplied and is not inferred from HyperOS.

The acceptance criterion is an advancing, readable bubble over the external app
the user is actually using. A connected Accessibility service or a bubble visible
only in Settings does not establish success. The source candidate is 1.0.3/code 4;
the published stable APK/feed remain 1.0.2/code 3.

## Findings and changes

### High: rootless detection rejects valid external Activities

WatchService accepted immediate transitions only for excluded packages, classes
ending in Activity/Launcher, or packages with a launch intent. Activity names can
be arbitrary or obfuscated; an external Activity need not have a launcher entry.
When window data was unavailable, rejected transitions retained the previous hint,
including the null hint left by Home or IWatchYou. The bubble could stay absent
or continue the previous application's timer while the service was connected.

The candidate accepts external window-state package metadata without the launcher
or class-suffix gate. It retains filtering for dialogs, popups, the app's own
overlay and standard IME window classes. Real-window keyboard/overlay/PiP rules
and stale-generation checks remain. A package-manager lookup is also removed
from the main-thread event path.

The [clean control run](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40626623724)
restores only the old WatchService while retaining an isolated fullscreen fixture.
On API 30 the fixture is displayed and independently verified as focused, then
the old service fails the external foreground assertion (DeviceTest line 190).
One selected test executes and fails at that intended assertion. This establishes
the detection defect without relying on earlier ambiguous PiP/readiness failures.

Ordinary launcher apps normally satisfy the old manifest's launcher query.
Consequently this is a confirmed code defect, not proof of the exact cause on the
reported Poco firmware. No physical Poco F6 is attached to this environment.

### High: staged APKs bypassed source verification

The workflow skipped JVM tests, lint, source builds and native device tests when
an APK matching versionName existed in distribution/. That was already true for
1.0.2. A Kotlin edit without a version bump could therefore verify an old binary.

Source QA now runs regardless of staged APKs, including pull-request checks and
native jobs on API 30 and 36. Production publishing is restricted to main and
depends on successful build/device jobs. Existing source-digest, signature and
publishing checks remain authoritative.

### Medium: native fixtures and assertions gave unreliable evidence

The final regression checks repeated Settings/external-app/Home transitions,
independent focused-window identity, bubble presence, rendered text advancement,
per-app time advancement and pause at Home, plus a real keyboard and an injected
standard IME event while IWatchYou's window retrieval is disabled.

Test isolation/readiness fixes preserve the strict duration assertions:

- FocusProbe has its own task affinity and cannot enter PiP. It previously joined
  the earlier PiP fixture's pinned task, making its button non-interactive.
- PiP cleanup uses explicit NEW_TASK/CLEAR_TASK flags and verifies removal.
  The old --activity-new-task option is not supported by Android's
  [Intent command parser](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/content/Intent.java).
- Home, Settings and the fixture must actually receive focus; launch commands use
  -W where a displayed Activity is required. Fixed animation sleeps and the first
  transient null foreground are not readiness signals.
- The fixture applies system-bar/cutout insets for API 36.
- FocusProbe is self-contained Java. A Kotlin inset callback in the standalone
  test process crashed on API 36 with a missing kotlin.jvm.internal.Intrinsics;
  the target APK's Kotlin runtime was not available to that external process.
- Teardown waits for the service to disconnect before another test enables it.

The five existing native methods still cover persistence, lock/resume, PiP,
rotation/large text and overlay gestures.

### Medium: the QA runner could accept incomplete or crashed runs

The runner now rejects zero tests and recognizes fatal exceptions in both the
main and external test package. The crash matcher consumes its entire input:
a quiet grep could exit early, causing SIGPIPE upstream under pipefail and
incorrectly accepting a large crash log.

qa/test_runner.py exercises the real shell runner with controlled adb output:
zero tests, one/six passing tests, failed tests, main/fixture crashes, an unrelated
crash, and 10,000 matching crash blocks. All eight cases pass locally; the same
host test is included in CI before the Android build.

### Remaining: service readiness differs from visible-bubble readiness

The dashboard's active state still means WatchService is connected. It does not
prove successful foreground selection or actual overlay visibility. Attachment
exceptions now emit an IWatchYou warning without recording screen text or usage
history. Successful attachment alone cannot establish visibility on every OEM.

## Verification and architecture

- The production source digest is
  787139a48be0099ac6d328985f9edc9f9e7afc60859d72b6c07b488a0a3f72f3.
- Local source verification passed 24 JVM tests, release lint and release/debug/
  instrumentation APK builds. Lint reports zero errors and 17 warnings
  (dependency versions, backup rules, SDK/KTX, touch accessibility and RTL).
- The host QA-outcome test passes eight controlled cases, including the large-log
  regression that failed before removing the early-exit crash matcher.
- Native acceptance requires all six methods on each of API 30 and 36.
  Exact candidate commits, results and logs are linked from
  [PR #25 checks](https://github.com/WynnDev-rill/Wynn-Store/pull/25/checks).
  Use source-build/native results for the candidate, not an existing staged APK.
- Earlier incomplete runs are diagnostic evidence, not passing candidate QA.
  The local software-only emulator did not supply reliable behavioral evidence;
  CI uses accelerated emulators. No physical-device latency, battery or memory
  guarantee is inferred from these tests.

Keep event-driven Accessibility and TYPE_ACCESSIBILITY_OVERLAY. The fix requires
no additional permission, foreground polling, wake lock or per-second database
write. Preserve monotonic time, one focused app, local-day accumulation, private
SQLite history and the original app identity. Do not invent time after a process
death or promise to bypass force-stop.

## Physical verification and release

On the reported phone, test two ordinary apps in turn, opening them from Home,
Settings and IWatchYou. Confirm the bubble is visible and advances, returning to
each app resumes its daily total, and Home/lock hide it. Exercise keyboard,
notification shade, PiP and rotation as well.

If the service disconnects, investigate autostart/battery restrictions. If it
remains connected, distinguish missed foreground transitions from attachment or
firmware visibility failures before changing permissions.

No production-signed candidate has been published or installed on the phone.
A release still needs successful candidate QA, the new version, matching source
digest and the owner's original production signature. Do not uninstall an
existing production installation or substitute a debug certificate.

See [project instructions](../AGENTS.md) and [repository instructions](../../AGENTS.md)
for the architecture, build commands, privacy constraints and AI handoff rules.

# Floating stopwatch revision1.0.5

Owner feedback rejected1.0.4's fade-only pill. Both Dribbble pages were reopened
in a live browser; the original widget MP4 was viewed as a12-frame contact sheet.
The revision implements the reference's compact/expanded interaction as a native
Android accessibility overlay, with an explicit edge handle. It does not depend
on proprietary HyperOS island APIs. Runtime assets are bundled font/vectors and
local app icons, with no downloaded imagery or new runtime library.

Focus audit found unbounded trailing window-event debounce, roots serialized ahead
of newer metadata reads, and generation checks ending before queued commits.
Identity-only events now trigger immediate reconciliation, metadata/root readers
have separate bounded latest-work queues, and selection generations reach ledger
and overlay callbacks. Physical Poco F6 Notes responsiveness remains unverified.

Overview: five rows, ring-aligned app icons, details with all rows and actual local
seven-day totals. Each day opens its daily history. Hourly data remains stored;
the hourly UI and repeated helper copy were removed as requested. Setup retains
privacy and permission explanations.

Initial source CI41976155275: build/unit/lint passed, Android30 all9 device tests
passed; Android36 failed on a test's immediate click during font reconfiguration.
The test now waits for the recreated clock. An early weekly capture caught the
prior composition; capture now waits for the actual dated bar. Manual renders of
compact/expanded/edge, populated history and rotation were inspected. The edge
chevron was corrected to draw within the dark handle rather than on white.
Impeccable detector returned no findings; native rendered evidence remains primary.
The independent follow-up reviewer could not run because its account usage limit
was reached; implementation/render review continued in the main thread.

Production must reuse the pinned1.0.4 key. Upgrade baseline is the actual published
1.0.4 APK, not a synthetic manifest downgrade. No final release pass is asserted
here until the completed CI and public-update result are recorded below.

Final source CI41977782245 at3de10a903eaab5298b68a9d98c69e3912323a777 passed
build,50 JVM tests, lint, optimized APKs, and all9 device methods on each API30/36.
The final weekly and edge captures on API30 were inspected and show the corrected
states. Production signing uses the same pinned certificate; v2/v3 both verified.

Main release run41979132406 initially failed: API30 input-dispatch timeout during
a drag, with concurrent emulator/system latency, and API36 could not reach the
weekly chip during UI automation. Logs were retained. A single retry, without
changing the APK or test assertions, passed all native tests on both APIs. This
records nondeterministic emulator/test behavior; it does not prove physical Poco
responsiveness or battery performance. Source verification was already green on
both APIs before merge. Production publication/upgrade is checked separately.

Final production run41979132406 attempt2 completed successfully, including production
smoke and actual public-feed/in-app download/Android installation consent from
versionCode5 to6. Private screen history remained10→24 seconds after update.
Public APK and stable-v2 feed were independently downloaded and their SHA256,
version and pinned signer metadata matched the staged production APK.
Release: https://github.com/WynnDev-rill/Wynn-Store/releases/tag/iwatchyou-v1.0.5

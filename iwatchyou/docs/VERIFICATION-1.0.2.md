# IWatchYou 1.0.2 verification

Production identity: `id.wynn.iwatchyou`, versionCode `3`, Android 11+.
Source: `e6fe3ab408a224159dc0f49e85cb08d503adff3e`.
[Source build and native tests](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40156611475).

## Timer fix

Activity transition events now select the app immediately, before accessibility-window
reconciliation. Missing window roots retain the latest activity hint. Window retrieval
runs on a separate executor so slow OEM accessibility calls cannot block the UI or time
ledger. Generation checks discard stale results after another transition. Returning to
the dashboard pauses the external-app timer directly. The overlay is updated before
database persistence.

There is no foreground polling loop. Window reconciliation follows events, with at most
two bounded retries for missing roots. The notification continues to use Android's
chronometer and checkpoints remain one minute apart while the screen is active.

## Source checks

24 JVM tests and five native instrumentation methods on each of API 30 and 36 passed.
The new regression disables retrieval of interactive windows, then repeats Home → Settings
→ Home three times and verifies that time progresses and pauses. Existing coverage for
PiP, lock/resume, rotation, large text, overlay dim/tap/drag and SQLite persistence passed.
Readiness timeouts allow emulator cold starts; these tests do not establish a measured
physical-device response-latency guarantee.

## Signed artifact

- Size: 2,003,492 bytes.
- SHA-256: `4329817577bdd9ce4739ccb676c82e371ab83de1afa75424f6ad1614212de5fb`.
- Production signer: `eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a`.
- v3 and independent v2 verification passed; signing preserved every APK payload entry.
- The production app ID and certificate match 1.0.1, permitting an update without uninstall.

Physical Poco F6/HyperOS and long-term battery measurements remain unavailable. The
[1.0.1 verification](VERIFICATION-1.0.1.md) describes the unchanged architectural coverage
and platform restrictions.

## Production smoke

The optimized signed APK was installed and its home screen verified on API 36.
The captured Settings launch shows the floating stopwatch at `00:00:02`. Sampled
UI PSS was 24,968 KiB and service/background PSS was 32,640 KiB. These are single
emulator samples, not a physical-device memory budget. No app-specific fatal
exception or ANR appears in the sampled smoke log.

The publisher independently verified the source QA run, package, production
signatures, source digest and downloaded release hash before enabling stable feed 3.
[Production smoke and publication](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40157194548).
The separate host-network readiness precheck in that run failed before upgrade UI
testing; Android's actual update flow is tested independently.

## Update verification limit

[Latest upgrade attempt](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/40157635070)
installed the same-signer versionCode 2 fixture, but the automatic update dialog did
not appear. An independent Android Java/TLS request to the public raw feed timed out
after 40 seconds. The host-network preflight in the preceding run also could not
observe the feed within 180 seconds. The precise network cause is undetermined.
Consequently the live 2 → 3 in-app update and retained-data assertions were not reached
and are not claimed as passing for this release. The update implementation is unchanged
from the successfully tested 1 → 2 flow in 1.0.1. The production signer, package and
version checks permit direct installation over production 1.0.1 without uninstall.
The public release asset and stable feed have been verified through the publishing API.

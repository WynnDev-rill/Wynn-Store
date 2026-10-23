# ScreenGuard Pro 1.2: decisions and platform boundaries

Research reviewed 2026-10-23, before implementation. These are source references, not claims that every OEM implements an identical interface.

| Source | Inspected revision / file | Decision |
| --- | --- | --- |
| [Hail](https://github.com/aistra0528/Hail) | `60742d0608f2d0abb4fa7e8bbc1b66b3888d22bc`, `app/src/main/kotlin/com/aistra/hail/utils/HPackages.kt` | Disable an app for its Android user, retain data, verify PackageManager state. A disabled launcher component cannot be launched until enabled. Suspend alone does not meet the requested launcher hiding. |
| [ShizuWall](https://github.com/AhmetCanArslan/ShizuWall) | `8a51d90ec7fed3d2e53e027fe997052403ec6859`, `app/src/main/java/com/arslan/shizuwall/firewall/PerUidFirewall.kt` | Retain existing typed ConnectivityService OEM_DENY_3 per-UID backend, capability checks and safe shared-UID refusal; no VPN or iptables/root dependency. Do not flush the global chain. |
| [Canta](https://github.com/samolego/Canta) | `9b30d1bc1c8627f9866cdf7939f9fa28400b3605`, `composeApp/src/androidMain/kotlin/io/github/samolego/canta/core/shizuku/ShizukuServices.kt`, `packages/.../PackageOperations.kt` | Shizuku identity is shell UID 2000 on the no-root path and user 0. Prefer reversible enable-state changes, not uninstall/debloating. Include disabled applications in catalog and recovery. |
| [Shizuku API](https://github.com/RikkaApps/Shizuku-API) | API 13.1.5 / UserServiceArgs daemon lifecycle | Keep a typed daemon UserService. A bounded refreeze lease lives in this process instead of relying on a screen coroutine or foreground-service polling alone. |
| [Android UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event) | ACTIVITY_RESUMED/PAUSED, SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN/STARTUP | Calculate per-package foreground unions, retain original session opening timestamps, clip at day/hour boundaries, discard unmatched shutdown sessions. Never fabricate hourly data when Android has pruned events. |
| [Android PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager) | COMPONENT_ENABLED_STATE_DISABLED_USER / MATCH_DISABLED_COMPONENTS | Use `cmd package disable-user --user 0` and `enable`, inspect fresh enabled state, preserve ownership journals. |
| [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) | Native foreground/background/monochrome layers and masks | Embed the supplied original SG image (JPEG bytes despite the uploaded .png name) as the launcher artwork in a native adaptive-icon resource. Full-color artwork is preserved; no unrelated monochrome symbol is substituted. No in-app shortcut/web icon. |

## Four destinations

Beranda contains an interactive seven-day chart, app usage rankings, a secondary full usage list and native app-statistics detail. Bekukan owns the installed-app search, download/system filters, per-app challenge editor and optional automatic rules. Jaringan owns the real combined Wi-Fi/cellular block. Pengaturan owns permissions, PIN, appearance, target, recovery and verified updates. There is no separate Focus timer or duplicated global challenge menu.

An app's challenge uses wait, math or both, 1–5 questions, three arithmetic difficulties, a 5–120 second wait and a 1–30 minute access lease. Parent PIN protects configuration/removal/recovery, not every ordinary challenge. Math checks and monotonic wait validation run in the ViewModel, independent of button state. Navigating away cancels a challenge; a new attempt starts over. Configuration changes are not applied to external freezes.

## Temporary access and ownership

The local AtomicFile journals the intended guard, disable ownership and both clock deadlines before privileged mutation. Reconciliation arms the Shizuku timer **before enabling** an application. Failure to arm leaves it closed. Disabling ownership remains recorded through the lease, so an independent daemon timer's change can be safely reconciled. A lease cannot be extended by wall-clock rollback or survive a different boot generation. The timer deadline is monotonic and stable across repeated reconciliations. Removing protection cancels the timer before enabling; recovery restores only ScreenGuard-owned state.

The daemon uses one scheduled executor, no busy loop and no wake lock. Refreeze verifies Android's actual state and retries a failed operation three times; foreground monitoring and WorkManager provide additional reconciliation. A sleeping/Doze device can delay execution until the CPU wakes. Daily limits and schedules use the existing screen-on 15-second / screen-off 60-second service and are not exact alarms.

A foreground access session is necessarily enabled and may reappear in the launcher during that session. It is hidden again when disabled. Keeping a disabled package hidden while also launching it is not possible with ordinary shell privileges. Do not claim universal interception of the Android launcher.

Shizuku daemon survives force-stopping the ScreenGuard client, **not** Shizuku server termination or reboot. If Shizuku stops during an open lease, refreeze waits for reconnection/reconciliation. Closed disabled apps stay disabled. On stock non-root Android the user must restart Shizuku after boot. The UI exposes disconnected/error status; emergency ADB recovery is documented separately. HyperOS battery/launcher caching behavior still needs physical-device verification.

Schema migration preserves network rules, ownership, existing limits/windows and PIN. Legacy manual freezes and Focus selections become per-app guards; legacy automatic-only rules retain automatic behavior. There are no destructive database resets or account/server migrations.

## Signing and release provenance

Production package remains `id.wynn.screenguard`; version code increases to 3 / 1.2.0. The existing production certificate must be retained for Android upgrade compatibility. Private JKS/passwords stay outside git. Actions builds optimized unsigned production plus separately identified preview APK, runs lint/unit/device tests, and can sign with protected environment secrets. If these secrets are absent, an unsigned artifact is signed outside git with the same existing private key, committed as a fixed-version distribution APK with checksum/certificate manifest, then installed and validated by Actions. No debug key fallback is allowed for the production package.

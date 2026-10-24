# ScreenGuard Pro

Version 1.5.0: manual app freezing with informational activity statistics. [Release notes](docs/RELEASE-1.5.md).

Native Kotlin/Jetpack Compose, Android 11+, Shizuku without root or VPN. Freezing uses Android disable-user and preserves app data. Opening a frozen app restores it and launches it; it remains in your collection for later refreezing. App icons inside ScreenGuard show their verified Android state.

The app has three destinations: Bekukan, Aktivitas, Pengaturan. Statistics are local, optional, and do not enforce time limits. No foreground monitoring notification is used. Use Settings → Restore all applications before uninstalling ScreenGuard. Shizuku must be started again after a non-root device restart.

Production updates require the original signing certificate, matching package/version/SDK/hash, and Android's installation confirmation. Private signing material never belongs in git.

GPL-3.0. See bundled dependency/font license notices and prior release documentation for provenance.

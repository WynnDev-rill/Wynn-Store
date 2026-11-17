# First emulator gate findings

Run 44073454259 built successfully in GitHub Actions. API 36 passed typed recall and persistence within the UI, then the navigation test failed because it tried to find a flashcard action before the LazyColumn composed the offscreen item. The fix scrolls the owning list to the matching node before interacting; this is a test harness fix, not a skipped assertion.

API 26 installed both APKs but its logcat clear command failed before instrumentation. Clearing old log buffers is best effort; native UI tests and crash checks remain mandatory. The gate was corrected and rerun. No device pass is inferred from a successful installation alone.

The local software-only Android 30 emulator booted but Settings/system services became unresponsive under CPU load and APK installation returned a package-service broken pipe. It is not counted as an app/device pass. GitHub's KVM-backed API 26 and 36 runners provide the device gate.

The second gate passed both Compose interactions on API 26 and all checks on API 36. API 26 then returned the launcher instead of the app while its night/font-scale configuration was still being applied. Captures now retry until the actual app's loaded home semantics appear, with a bounded timeout; screenshots are never accepted from the launcher or a null accessibility root. No assertion was removed.

Screenshot review also caught the keypad's disabled appearance during periodic session persistence. Background autosave now uses the same serialized database path without toggling foreground busy state, preserving number entry during saving. A runnable regression checks this behavior before the fix.

The initial real-upgrade test downloaded and verified the APK and obtained the installer intent, but the installer later showed a URI parsing error after instrumentation ended. The test now starts a visible baseline activity and stays alive until Android's real confirmation is present, matching a user's foreground install action. The install intent also includes explicit ClipData for its temporary read grant. Failure evidence now includes PackageInstaller/device logs and a separate diagnostic adb installation without counting that diagnostic as an in-app upgrade pass.

The large-text visual review found the four-tab label “Pengaturan” wrapping poorly at 160%; the shorter native label “Setelan” retains system font scaling and keeps navigation legible.

The next run confirmed native installation of version 1.0.0/code 2 and showed the preserved answer and accuracy behind Android's completed-install dialog. The verifier now explicitly chooses Open/Done before reading application semantics. The app itself did not lose data.

API 26's remaining failure was an Android 8 framework NPE in ViewRootImpl's accessibility-state listener during repeated standalone UiAutomator connections while windows were being recreated. Theme/font/orientation checks now stay within one active native instrumentation session. A separate cold-process RestorationTest verifies the SQLite answer and session after force-stop. App-process crash detection remains enabled, including framework exceptions in the app process.

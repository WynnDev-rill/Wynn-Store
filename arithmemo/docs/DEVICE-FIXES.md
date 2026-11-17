# First emulator gate findings

Run 44073454259 built successfully in GitHub Actions. API 36 passed typed recall and persistence within the UI, then the navigation test failed because it tried to find a flashcard action before the LazyColumn composed the offscreen item. The fix scrolls the owning list to the matching node before interacting; this is a test harness fix, not a skipped assertion.

API 26 installed both APKs but its logcat clear command failed before instrumentation. Clearing old log buffers is best effort; native UI tests and crash checks remain mandatory. The gate was corrected and rerun. No device pass is inferred from a successful installation alone.

The local software-only Android 30 emulator booted but Settings/system services became unresponsive under CPU load and APK installation returned a package-service broken pipe. It is not counted as an app/device pass. GitHub's KVM-backed API 26 and 36 runners provide the device gate.

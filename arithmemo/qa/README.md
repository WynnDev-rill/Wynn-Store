# Device verification

Run from a fresh emulator with animations enabled. `run-device.sh` resets **only the .debug package**, never production app data. It installs compiled debug and instrumentation APKs, requires explicit `OK (N tests)` evidence, and treats crashes as failure. Captures dark/large text and landscape without claiming these were physically tested. Restore system settings after manual checks.

For production upgrade verification, use a locally signed baseline with lower versionCode, seed a practice answer, then install the final signed APK with the Android package installer. Verify the installed certificate and version, and that the answer/session remain. Never use a different signing key or clear production data to make an upgrade pass. Keep test-only baselines out of public stable feeds.

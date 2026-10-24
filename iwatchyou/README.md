# IWatchYou
Native Android application using Kotlin and Jetpack Compose, Android 11+.

- Counts screen-on duration and per-app foreground sessions with Android monotonic clock.
- Fades and locks the draggable overlay after three seconds; tap to reactivate it.
- Screen-time-only chronometer notification.
- Local data, day reset at midnight, no user account or tracking backend.
- Secure release manifest for verified in-app updates; user approves installation.
- AccessibilityService requires explicit activation and can be interrupted by Android / HyperOS.

Build from the repository root:
```
./screenguard-pro/gradlew -p iwatchyou :app:testDebugUnitTest :app:assembleDebug
```
Production signing uses the existing protected ScreenGuard release environment credentials, sharing its signing certificate while retaining a different application package. Never commit private keys. Future updates require the same signer.

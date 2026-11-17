# Release signing and updates
Package `id.wynn.arithmemo`, first version `1.0.0`, code `1`. Android 8.0 / API 26 minimum, target 36. Every subsequent version must increase versionCode and retain the same signing key.

Private release key is generated outside the repository in `/workspace/private/arithmemo/arithmemo-release.p12`; credentials in sibling `signing.json`, directory mode 700 and files mode 600. Back up both files to a password manager or encrypted offline archive. Do not commit either, paste passwords into issue comments, or regenerate this key for upgrades. Loss of the key prevents seamless upgrades.

Local signed build:
```sh
python3 tools/with-signing.py ./gradlew testDebugUnitTest lintRelease assembleRelease
```
On another workstation set `ARITHMEMO_SIGNING_FILE` to the private JSON and update only its `AM_KEYSTORE_PATH`. Secret values never appear in the command line or logs.

Configure the dedicated GitHub Actions secrets with an authenticated account that has permission to manage secrets:
```sh
python3 tools/configure-github-secrets.py
```
This sets `AM_KEYSTORE_BASE64`, `AM_STORE_PASSWORD`, `AM_KEY_ALIAS`, `AM_KEY_PASSWORD` over stdin. The current GitHub integration returns HTTP 403 on Actions secrets/public-key; it cannot perform this account administration step. Browser automation is unavailable in the current environment. Source and locally signed artifacts do not depend on that permission.

CI runs tests/lint and instrumentation. Manual release workflow uses the same four secrets, verifies package/version/certificate against `signing-certificate.sha256`, publishes `arithmemo-v<version>` with `ArithMemo.apk`, `SHA256SUMS`, and update metadata, then activates `arithmemo/updates/stable.json`. No debug-key fallback is permitted. Manual locally signed publication is also supported by the release script.

The app reads the public stable manifest only. Network is used only for GitHub updates. WorkManager checks approximately daily with network available; Android may defer background work. Settings offers an immediate check. Android 13+ notification permission is optional, with foreground update status available when denied. Downloads enforce HTTPS GitHub hosts, metadata and file size limits, hash, package, higher version code and installed signing certificate. Package installer requires user's explicit confirmation; silent installation is not attempted.

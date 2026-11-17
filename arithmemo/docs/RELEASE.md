# Release signing and updates
Package `id.wynn.arithmemo`, first version `1.0.0`, code `2`. Android 8.0 / API 26 minimum, target 36. Every subsequent version must increase versionCode and retain the same signing key.

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

## First-release upgrade fixture
`-PqaBaseline -PtestProduction -PreleaseCode=1 -PreleaseName=0.9.9` builds a lower-version, debuggable **test baseline only** using the private production key. It is not a production release. The conditional instrumentation test seeds a completed answer, downloads the final APK from its official GitHub release URL, verifies it, and opens Android's installer. `qa/run-upgrade.sh` confirms installation and the saved session afterwards on an API 36 emulator. Temporary QA APKs are stored in an explicitly marked prerelease with checksums; no signing key is uploaded with them. The default release remains optimized and non-debuggable.

Publication uses two explicit phases: `python3 tools/publish-release.py --stage` uploads a prerelease without changing the stable feed; only after device and real-upgrade checks pass, `--activate` updates the feed and promotes the release. Retrying activation is safe only when existing feed metadata exactly matches the candidate and the downloaded release APK has the expected hash. A published final release cannot be replaced with different APK bytes. Production preparation rejects debuggable QA APKs and version code zero.

## Encrypted offline recovery backup
An authenticated AES-256-GCM `.amkey` backup and a separate recovery-password file are stored in `/workspace/private/`, both mode 600. Copy them separately to safe storage. The archive contains the original keystore and signing credentials; it is never committed. A decryption round-trip was tested and restored identical keystore bytes. Restore with:
```sh
uv run --with cryptography python tools/signing-backup.py restore /safe/ArithMemo-signing-backup.amkey /safe/empty-directory
```
Enter the recovery password at the prompt; do not place it in a shell command. The restored JSON updates the local keystore path automatically. Point `ARITHMEMO_SIGNING_FILE` at it. The helper refuses to restore signing material anywhere inside this repository.

## Initial distribution without exposing the signing key
The local integration cannot upload release assets (`uploads.github.com` returns HTTP 401), although repository writes work. The initial verified signed APK and frozen signed upgrade fixtures are therefore stored as public binary artifacts in `distribution/` and `qa/fixtures/`. They contain no keystore or credentials. `arithmemo-bootstrap.yml` verifies the production certificate and hashes, uploads through the runner's scoped GitHub token, then runs the real upgrade gate. A separate activation phase requires successful API 26/36 verification and successful upgrade verification, and rejects any unverified app-source differences. This supports the first release while Actions Secrets administration remains blocked. The normal future release workflow builds from the same private key once its four secrets are configured.

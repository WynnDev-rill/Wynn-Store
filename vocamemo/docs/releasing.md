# Signing, release and update operations

Application ID `id.wynn.vocamemo`; build 1.2.0 / versionCode 3. Update identity is anchored in `signing-certificate.sha256`. A new RSA-4096 production key was created for VocaMemo, outside Git. The key and recovery credentials must be kept in a private vault and secure secondary backup. Never generate a replacement key for an update.

## Actions Secrets
Repository WynnDev-rill/Wynn-Store needs:
- VOCA_KEYSTORE_BASE64: base64 of private release.p12
- VOCA_STORE_PASSWORD
- VOCA_KEY_ALIAS: vocamemo-release
- VOCA_KEY_PASSWORD

Use `python3 scripts/configure-signing-secrets.py /private/path/signing.json` with a token permitted to manage Actions Secrets, or add through GitHub Settings. Do not paste secrets in workflow YAML, issues or chat. Initial environment token returned 403 for the Actions Secrets public-key endpoint; the user chose a private recovery package and setup guide instead of changing that permission. The recovery package is outside Git. A workflow without secrets can validate an unsigned rebuild and separately verify/install the signed APK staged in distribution/; it cannot manufacture a production signature. This fallback is explicitly reported in logs.

## Local signed build
Load the private JSON into environment through a trusted local helper (do not print its contents); run `./gradlew testDebugUnitTest lintRelease assembleRelease`, copy app/build/outputs/apk/release/app-release.apk to dist/VocaMemo.apk, then `python3 scripts/release-manifest.py`. This pins signer, calculates checksum/size and emits stable.json. Compare package/version with `aapt dump badging` and verify `apksigner verify --verbose --print-certs`.

## Workflow
`.github/workflows/vocamemo-android.yml` audits dataset, runs unit tests/lint, builds, verifies signing, and executes offline instrumentation and signed-release install smoke on API 26 and 35. Tags `vocamemo-v*` publish only after all jobs pass. A signed APK in distribution/VocaMemo-VERSION.apk bootstraps validation when the integration cannot set secrets; automatic signing of new source requires the four secrets above. Do not substitute an old staged binary for a new version. Manifest script resolves the exact current version and verifies the APK package, version code/name, minimum SDK and pinned certificate against the project before emitting a feed. An embedded SHA-256 fingerprint over production source/resources/build configuration must match the checkout, so the staging fallback cannot silently use an older build with the same version number.

## Stable feed
`vocamemo/updates/stable.json` on main is the application feed. It must be copied from the verified release artifact only after publishing that exact APK. Uses the ScreenGuard convention: versionCode/name, package, minSDK, official GitHub Releases asset URL, byte size, SHA-256, signer SHA-256 and notes. Do not touch another app's feed.

The app checks on launch/manual action and through WorkManager every 24 hours with network constraints. Background notification requires permission on Android 13+. Before installer handoff it verifies URL allowlist, increasing version, installed package/signature, min SDK, declared size, downloaded SHA, archive package/version and archive signing identity. Android handles the final installation consent and unknown-source permission. Failed network checks preserve all offline functions.

## Next release checklist
Increment versionCode, versionName and tag; update notes, dataset provenance if changed; provide Room migrations; run all tests; use same private key; inspect signed artifact, publish through workflow, update only VocaMemo stable feed; test upgrade over previous signed APK with existing Memo/progress. Never distribute a debug APK as VocaMemo.apk.

Initial upload endpoint rejected the workspace integration credential with HTTP 401. The signed APK is therefore staged in Git following the ScreenGuard distribution pattern; no signing key or password enters Git. GitHub Actions publishes with its own scoped GITHUB_TOKEN after all checks pass.

Dictionary refresh: bump the versioned copied asset filename in Dictionary.kt (currently dictionary-v2.db) whenever changing the bundled database; otherwise existing installs retain their prior cached dictionary. Verify/copy the new generation atomically, preserve the separate personal Room database and retain source licences. Never use destructive Room migrations for an update.

Initial or staged publication can use the separate `VocaMemo Publish Verified Release` workflow with a successful VocaMemo Android run ID. It requires successful build/API26/API35 jobs, rejects changed production/test inputs, rechecks the pinned signature/source fingerprint/package/version, publishes using Actions' own token, and activates only the VocaMemo stable feed. Documentation-only changes may be included in the release tag after verification. Public releases are never silently replaced by this workflow.

## ArithMemo-style private signing recovery
Use `python3 scripts/with-signing.py ./gradlew testDebugUnitTest lintRelease assembleRelease`. Set `VOCAMEMO_SIGNING_FILE` to a private signing.json on another workstation. Credentials are passed through environment variables, never printed.

The original VocaMemo key is backed up with authenticated AES-256-GCM in `/workspace/private/VocaMemo-signing-backup.vmkey`; the separate password is `/workspace/private/VocaMemo-recovery-password.txt`. Both are mode 600, outside Git. Restore was tested against the original keystore bytes. Store archive and recovery password separately in secure storage. Restore:
```
uv run --with cryptography python scripts/signing-backup.py restore /safe/VocaMemo-signing-backup.vmkey /safe/empty-directory
```
Enter the password at the prompt. The helper refuses repository destinations and updates the restored keystore path. Never replace this key for an update.

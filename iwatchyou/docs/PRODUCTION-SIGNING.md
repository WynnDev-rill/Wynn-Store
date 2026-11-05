# Permanent production signing

Owner-authorized rotation: 2026-11-05. Alias: `iwatchyou-production`. Type: PKCS12.
Certificate SHA-256: `04ed3a280377b2671ad1f6fb6f7fd1c335684302ab35d50d0000ca2b23b585f5`. Package: `id.wynn.iwatchyou`.

Reuse this key for EVERY future production update. Never create another key merely
because a workspace was replaced. Public APKs cannot recover private signing keys.

Private workspace path: `/workspace/private/iwatchyou/production.jks`, password file
`/workspace/private/iwatchyou/store-password`. Use keytool/apksigner password-file
arguments; never print passwords or commit keys, passwords or the recovery bundle.
The owner receives `IWatchYou-production-key-backup.zip` privately, containing the
encrypted keystore, password file and restore instructions. Store it in a private
password manager or encrypted backup outside this disposable workspace. If the
workspace no longer has the key, restore that bundle; do not generate a replacement.

The current GitHub connection returns403 for Actions secret administration. No claim
is made that secrets are stored there. Future CI signing requires the owner to
restore these credentials securely or enable secret administration. Existing staged
APK publication needs only public signed APKs and metadata, never private keys.

The legacy certificate was
`eca17e8afa84775e941e0e490741b01636e7941da545c508f85b8a256a4f346a`.
Legacy installations require manual reinstall as expressly accepted by the owner;
old history cannot survive uninstall without an independent supported backup.
New production builds use stable-v2.json and the normal verified in-app updater.
Legacy stable.json remains frozen.

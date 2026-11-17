# VocaMemo 1.0.0 design
The user's full brief authorizes implementation and release. Native Android, min API 26, target 36. App lives exclusively in vocamemo/; only repository project index and new CI workflow touch root.

## Architecture and alternatives
Use a bundled indexed SQLite dictionary and a separate Room personal database. This ships ready offline, avoids a compulsory large first-run download, and keeps dictionary replacement from destroying learning progress. Runtime cloud dictionaries violate offline requirements; a small hand-written word list fails coverage and provenance. No translation generation.

Use Compose + ViewModel + coroutines + Room Flow, native TTS, WorkManager update checks. Avoid dependency injection framework and a server. All disk/network work runs on IO. Dictionary opens lazily with an atomic asset copy. Room transactions save review event and scheduling state together. Rotation retains state through ViewModel/SavedStateHandle; process death retains database.

## Workflows
Kamus searches indexed prefix and exact words, then opens senses with contextual English gloss, Indonesian choices, POS, IPA where available and source link. Each translation can be saved as a specific memo. Memo supports filtering, groups, favourites, mastery and editing/deletion with confirmation. Latihan selects mode, direction and optional group; spaced mode selects due, unmastered cards. Unlimited modes cycle until user exits. Typed recall uses Unicode/whitespace/case normalization with exact selected answers, never vague substring matching. Speed mode uses monotonic time and pauses when backgrounded. Self grading offers Again/Hard/Good/Easy after reveal. Statistics use actual review events by local calendar date. Goal defaults to 20 reviews.

## Update and signing
Feed vocamemo/updates/stable.json points to vocamemo-vX.Y.Z GitHub release asset. Validate trusted HTTPS URL, package, minSDK, increasing version, same certificate and SHA-256 before handing APK to Android Package Installer. OS retains final install consent. WorkManager daily check with connected network; launch and manual check. No core network dependency.

## Quality floor
Dictionary structural audit, source flag filtering, stable build fingerprint, actual counts, sample polysemy checks. Unit tests scheduling, Unicode answers, update trust boundaries; instrumented Room, search, UI and backup. Build release, lint, APK certificate verification and install smoke. Record physical/emulator limitations honestly. Backups exclude signing material.

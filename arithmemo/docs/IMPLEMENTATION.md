# Design and execution plan
Authorized scope: complete native Android app in this monorepo, signed v1.0.0 APK and release automation. Decisions delegated by the supplied detailed brief are recorded here.

1. Lock visual tokens and offline architecture; reuse mature assets. Alternatives: Room adds code generation without needed relational complexity; JSON snapshots rewrite growing history. Chosen native SQLite transactions with small explicit schema, typed Kotlin models and StateFlow.
2. Verify arithmetic bounds, nonnegative subtraction, exact integer division, stable bounded catalog, adaptive intervals and strict update metadata with JVM tests before implementation.
3. Build persistence for settings, progress, attempts and interrupted sessions. Commit answers and next session snapshot atomically. Export/import versioned JSON via Android document picker with size and schema validation; imports replace data only after confirmation.
4. Implement Compose home, fact list, interactive flashcards, typed recall, 60-second speed, due-card review, random drills, group/digit/operation settings, daily goal, stats and history. Sessions are ten cards except speed; every answer immediately persists. Pause speed and answer timing when app backgrounds.
5. Adapt ScreenGuard update verification; WorkManager daily checks with notification permission requested only from the settings control. Install only after checksum, package, version and signing identity validation.
6. Generate a new private release key outside repository, configure CI secrets if account permissions allow, build and verify signature. CI runs JVM/lint/build and API 26/36 device tests; release only after checks pass.
7. Install Graphify, build a reproducible local AST graph and query it; document app use, backup, release recovery, test evidence and all remaining access constraints.

Review focus: division always terminates across groups/digits; no duplicate answer writes; last answer survives process recreation; malformed import never replaces good data; update cert/hash mismatch fails closed. No account, ad, formula or sibling-app change.

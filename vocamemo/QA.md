# VocaMemo 1.1.0 verification

- Compared the 1.1.0 bundled data against the signed 1.0.0 APK. All 20,230 original senses and 15,589 attributed examples match, including lexical IDs and contextual text.
- Audited the full browse cursor sequence, which returned all 15,725 headwords once. Verified WikDict and WordNet English aliases, source-backed Indonesian reverse lookup, asset integrity and checksums.
- SQLite query plans use the sort index for first and late pages and the lookup primary key for exact and prefix ranges. On the build host, 64-row pages averaged about 0.25 ms, exact reverse lookup about 0.01 ms and English prefix-range retrieval about 0.12 ms. These measure warm SQLite queries on the host.
- The signed 1.0.0 APK remains the upgrade fixture. On each emulator, the script seeds a Memo and a completed review in 1.0.0, installs the signed 1.1.0 APK with `adb install -r`, and checks both records in the production SQLite database. The Android 15 signed-app smoke test also opens the retained Memo in the UI. Compose tests exercise navigation, browse controls, lookup, large text, practice modes, backup and update validation offline on both emulator versions.
- Physical Android hardware and audible TTS output are outside the emulator evidence.

## VocaMemo 1.2.0 verification

- The dictionary audit passed. The bundled dictionary matches 1.1.0 and contains 15,725 headwords, 20,230 senses, and 15,589 examples.
- The signed Android 8 upgrade from 1.1.0 preserved a seeded Memo and graded review. The signed APK also passed the offline lookup smoke test. Ten of eleven tests passed in the initial instrumentation run. The dark theme at 150% font scale rendered and passed the visibility assertion before the test failed while restoring font scale.
- The final patch dismisses the numeric keyboard after saving the daily goal and makes the font-scale test wait for Android's configuration change. The signed build, lint, and JVM tests passed again. A completed device rerun for this last patch is unavailable.

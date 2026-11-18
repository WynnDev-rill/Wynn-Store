# VocaMemo 1.1.0 verification

- Compared the 1.1.0 bundled data against the signed 1.0.0 APK. All 20,230 original senses and 15,589 attributed examples match, including lexical IDs and contextual text.
- Audited the full browse cursor sequence, which returned all 15,725 headwords once. Verified WikDict and WordNet English aliases, source-backed Indonesian reverse lookup, asset integrity and checksums.
- SQLite query plans use the sort index for first and late pages and the lookup primary key for exact and prefix ranges. On the build host, 64-row pages averaged about 0.25 ms, exact reverse lookup about 0.01 ms and English prefix-range retrieval about 0.12 ms. These measure warm SQLite queries on the host.
- The signed 1.0.0 APK remains the upgrade fixture. On each emulator, the verification script seeds a Memo and a completed review in 1.0.0, runs `adb install -r` for the signed 1.1.0 APK, and checks both the visible Memo and the persisted review. The device job also exercises lookup, browse controls, large text, core practice modes, backup and update validation offline.
- Physical Android hardware and audible TTS output are outside the emulator evidence.

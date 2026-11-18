# VocaMemo 1.1.0 verification

- Compared the 1.1.0 bundled data against the signed 1.0.0 APK. All 20,230 original senses and 15,589 attributed examples match, including lexical IDs and contextual text.
- Audited the full browse cursor sequence, which returned all 15,725 headwords once. Verified WikDict and WordNet English aliases, source-backed Indonesian reverse lookup, asset integrity and checksums.
- SQLite query plans use the sort index for first and late pages and the lookup primary key for exact and prefix ranges. On the build host, 64-row pages averaged about 0.25 ms, exact reverse lookup about 0.01 ms and English prefix-range retrieval about 0.12 ms. These measure warm SQLite queries on the host.
- The signed 1.0.0 APK remains the upgrade fixture. On each emulator, the script seeds a Memo and a completed review in 1.0.0, installs the signed 1.1.0 APK with `adb install -r`, and checks both records in the production SQLite database. The Android 15 signed-app smoke test also opens the retained Memo in the UI. Compose tests exercise navigation, browse controls, lookup, large text, practice modes, backup and update validation offline on both emulator versions.
- Physical Android hardware and audible TTS output are outside the emulator evidence.

## VocaMemo 1.2.0 redesign

- The local Room database, dictionary search and browse paths, vocabulary source tables, backup format, scheduler and update policy retain their 1.1 behavior. The offline corpus audit passed at 15,725 words, 20,230 senses and 15,589 examples.
- Reviewed all 12 Behance reference panels and the four incumbent screenshots. The Dribbble site challenged direct and embed attempts.
- Checked contrast ratios for seven actual foreground/background role pairs per theme. The measured minimum was 7.47:1 across those pairs.
- Verified the nine Commons resources visually and against their exact headwords. Thumbnails are bundled, with per-file credits in the in-app attribution document.
- The release APK uses the original package and pinned production certificate. The instrumentation workflow uses the exact signed 1.1.0 APK as its upgrade fixture, seeds a Memo and completed review, installs 1.2.0 over it and checks both records in the production database.
- Credential Manager's configured Google account flow requires the project's Google Web Client ID and a matching Android OAuth client. The offline suite covers onboarding, the guest path and the unconfigured state. It does not claim a live Google sign-in or physical-device verification.
- Final build, lint, unit tests, API 26 and API 35 emulator jobs passed in [GitHub Actions run 44329916263](https://github.com/WynnDev-rill/Wynn-Store/actions/runs/44329916263). Each emulator passed the clean-install guest journey, 10 offline instrumentation cases and the signed upgrade smoke. Production SQLite retained the seeded Memo and review on both.
- Reviewed the final Android 15 captures for onboarding, dictionary browsing, photos, detail, keyboard search, practice, profile and dark mode at large text. The keyboard search keeps results visible down to the keyboard. The public direct download matches the signed build byte for byte.

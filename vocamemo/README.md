# VocaMemo

Native Android English–Indonesian dictionary and personal vocabulary practice. Kotlin, Jetpack Compose, Material 3, Room and SQLite. Version **1.1.0** (code 2), Android **8.0/API 26+**.

## Install
Download **VocaMemo.apk** from [VocaMemo Releases](https://github.com/WynnDev-rill/Wynn-Store/releases/tag/vocamemo-v1.1.0), open it on Android and allow installation from your download app when Android asks. All core features work from first launch without login or internet. Install an offline English voice in Android's Text-to-speech settings for pronunciation. Keep the same package and signing key when updating.

## Daily use
1. **Kamus**: browse immediately, jump A–Z, filter unsaved words or explore a random selection. Search English headwords or Indonesian meanings, with labelled spelling and base-form suggestions. Open the result, read the part of speech and source context, then save an individual Indonesian meaning. IPA and Tatoeba examples appear where available; context remains English from the source.
2. **Memo**: search personal vocabulary; filter by collection, favourites, learning or mastered. Tap a memo to edit the selected meaning, collection and personal note, mark mastered or delete with confirmation.
3. **Latihan**: choose EN → ID or ID → EN and an optional collection. Use due-card spaced repetition, flashcards, unlimited mechanical loops, typed answers, a 60-second speed test, or a parallel word/meaning list. All modes exclude words marked mastered.
4. **Statistik**: actual review counts, accuracy, last seven local calendar days, difficult words and daily goal. Untested automatic loops do not create fictitious progress.
5. **Setelan**: daily goal, system/light/dark theme, JSON export/merge restore, TTS settings and verified GitHub updates. Export a backup before uninstalling; automatic Android cloud backup is disabled. JSON backup has shared validation limits of 50 MB, 50,000 memos and 100,000 review records; export rejects files beyond those limits instead of producing an unrestorable archive.

Typed answers match the exact selected meaning with case/Unicode/whitespace normalization. A different synonym can be linguistically valid yet fail this specific card; edit the selected meaning in Memo when needed. Self-rating: Ulang lagi, Sulit, Ingat, Mudah. The scheduler is a documented SM-2 adaptation, not a claimed FSRS implementation. Failed cards return after a minute; successful intervals increase with performance.

## Trusted offline data
- 15,725 normalized English headwords / 20,230 contextual senses, official WikDict 2_2026-06. Source `is_good=0` rows are excluded (13,556).
- 15,589 Tatoeba English–Indonesian sentence pairs, including sentence IDs and contributor attribution. Examples are independent usage, not guaranteed sense alignment.
- Dictionary derivative CC BY-SA 4.0; sentences CC BY 2.0 France; font Nunito OFL. See [attribution](docs/ATTRIBUTION.txt), [research](docs/research.md), [manifest](app/src/main/assets/dictionary.json).

This is a substantial community dictionary, not an assertion that every English word or sense is covered or infallible. No AI-generated translations. Kaikki was evaluated; no unreconciled merge was shipped.

## Build and test
Use JDK 17+, Android SDK platform/build tools 36, and the checked-in Gradle 8.13 wrapper:

```sh
cd vocamemo
./gradlew testDebugUnitTest lintRelease assembleDebug assembleDebugAndroidTest
python3 scripts/audit-dictionary.py
./gradlew connectedDebugAndroidTest
```

Release build reads `VOCA_KEYSTORE_PATH`, `VOCA_STORE_PASSWORD`, `VOCA_KEY_ALIAS`, `VOCA_KEY_PASSWORD` from the environment. Missing keys produce an unsigned artifact, never a debug-signed production APK. See [release instructions](docs/releasing.md) and [QA evidence](QA.md). Room schema is exported under app/schemas; future versions require explicit non-destructive migrations.

Rebuild dataset after downloading the exact source files in the manifest:

```sh
python3 scripts/build-dictionary.py --sources /path/to/source-downloads
python3 scripts/audit-dictionary.py
```

No runtime dictionary download is needed. SQLite asset is copied atomically on first use, then opened read-only; personal progress is in a separate Room database.

## Continue development
Read [AGENTS.md](AGENTS.md), [PRODUCT.md](PRODUCT.md), [DESIGN.md](DESIGN.md), and [tooling](docs/tooling.md). Ponytail Full, UI UX Pro Max and Impeccable are vendored for persistent use. Graphify provides a local AST knowledge graph; use scripts/refresh-graph.sh and query graphify-out/graph.json. No cloud AI key is required.

# VocaMemo 1.1 dictionary browsing

| Product reviewed | Pattern adopted |
| --- | --- |
| [Oxford Learner’s Dictionaries](https://www.oxfordlearnersdictionaries.com/browse/english/) | Direct alphabet jumps and an ordered list available immediately. Keep browsing continuous after a jump. |
| [WordUp](https://www.wordupapp.co/) | Make discovery a distinct entry point. Use sourced meanings and examples already bundled in VocaMemo. |
| [WordWeb](https://www.wordwebsoftware.com/android/WordWeb.html) | Local lookup with spelling suggestions, inflected forms and random words. Distinguish suggestions from exact hits. |
| [AnkiDroid](https://docs.ankidroid.org/manual.html) | Browsing and studying remain separate destinations. Retain existing cards, scheduling and backup. |

Reuse the VocaMemo icon, Nunito, native Material 3 search field, filter chips and list items. Put the headword list immediately below compact controls. Give the alphabet picker targets large enough for touch, and keep it reachable while scrolling. Keep each original sense and example accessible from the list. Put corpus counts and attribution in Setelan.

Preserve the WikDict senses, lexical IDs, contexts and Tatoeba attribution. Build an English headword index and an Indonesian index derived from the existing reviewed corpus. Import attested English inflections from WikDict and WordNet 3.0 exceptions. Indonesian WikDict had no usable inflection rows matched to the retained glosses. Use explicitly labelled affix suggestions only when the candidate is already an indexed translation. Prefixes and spelling suggestions must resolve to real entries. The release checks cover list ordering and paging, both lookup directions, typo and inflection retrieval, and installing the signed APK over 1.0.0 with existing personal data.

## Data expansion decision

The native WikDict Indonesian → English 2_2026-06 file has four records flagged as high quality. Reverse lookup instead indexes each existing, source-backed Indonesian gloss. [WordNet 3.0](https://wordnet.princeton.edu/license-and-commercial-use) supplies English lexical exceptions such as `went → go` and `running → run`. Its permissive terms allow redistribution with the complete bundled Princeton notice. WordNet supplies aliases only. No extra definitions or translations are shipped. English and Indonesian word statistics therefore refer to the same vetted translations as 1.0.0. Unverified Indonesian affix guesses are explicitly marked as possibilities and resolve to existing glossary entries.

Sources for reconstruction are listed with SHA-256 in `dictionary.json`. Download the official WikDict files from the original snapshot and the WordNet 3.0 archive from the [NLTK corpus mirror](https://github.com/nltk/nltk_data/blob/gh-pages/packages/corpora/wordnet.zip). Keep all input versions pinned to the recorded hashes. Paging reads 64 indexed rows at a time and uses a sort cursor. Spelling suggestions use edit distance with transpositions and WikDict scores for ties. The detail page retains original senses and attribution.

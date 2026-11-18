# Research and data acceptance — 2026-11-17

## Comparable products
| Product / primary source | Strength applied | Limitation / design response |
|---|---|---|
| [AnkiDroid manual](https://docs.ankidroid.org/manual.html) | Offline personal flashcards, scheduled review, explicit grading and export | Broad configuration can overwhelm; VocaMemo exposes four clear grades and six study modes |
| [WordWeb Android](https://www.wordwebsoftware.com/android/WordWeb.html) | Fast local dictionary, no adverts, direct lookup | English dictionary rather than a contextual Indonesian personal study flow; selected bilingual senses bridge lookup to practice |
| [Oxford pronunciation guide](https://www.oxfordlearnersdictionaries.com/about/english/pronunciation_english) | POS, IPA, contextual lexical structure | Proprietary text cannot be bundled without a licence; use licensed WikDict, clearly attributed and with limitations |
| [Quizlet study modes](https://help.quizlet.com/hc/en-us/articles/360030841732-Studying-on-Quizlet) | Familiar flip/typed/test interaction and short sessions | Some learning modes have subscription limits; VocaMemo's core modes work without accounts or payment |

This is documentary feature research, not a claim that these apps were all installed or benchmarked on a physical device.

## UI reference audit
- [Math Learning, Terabit Projects](https://dribbble.com/shots/26974846-Math-Learning-Mobile-App-UI-Daily-Challenges-Skill-Builder): source palette includes #EDEEF6, #4A8DFA, #0725D5, #2B63EC. Soft blue, rounded cards, daily practice and bottom navigation suit the brief.
- [Learn Mode, UI Ants](https://dribbble.com/shots/24138576-Learn-Mode-Mobile-App-Ui): learning-focused composition and component hierarchy; proprietary UI kit not redistributed.
- [Flashcards education app](https://www.behance.net/gallery/111025041/Flashcards-education-mobile-app): requested reference; page retrieval returned 403. No claim of a complete visual inspection.
- User icon supplies layered flashcard metaphor, blue/white and yellow. Nunito font bundled with OFL licence. Material rounded icons unify native controls.

## Dictionary selection
[WikDict official SQLite](https://www.wikdict.com/page/download) prioritized. Pair en-id.sqlite3 and lexical en.sqlite3 version 2_2026-06 downloaded from official server. Exact lexical IDs join POS/IPA, avoiding cross-language/homonym mismatches. Include only is_good=1, yielding 20,230 rows / 15,725 casefolded headwords. The source's flag is a heuristic, not a human guarantee; 13,556 rejected rows are excluded, not silently presented as reliable.

Polysemy samples: book → buku/kitab (noun), pesan (verb); light → cahaya (noun), ringan (adjective); run includes movement, machine operation and management senses. Context remains English to avoid inventing an Indonesian context translation. Source translation strings are split only at their ` | ` separator. Memorization stores one explicitly selected meaning; manually edited values are labelled personal notes.

[Kaikki](https://kaikki.org/dictionary/English/index.html) investigated as alternative/enrichment: current complete English JSONL is about 3.2 GB, with a different schema requiring sense reconciliation. No unverified merge shipped. Primary WikDict coverage and Tatoeba supply the requested licensed offline base. Additional coverage should be evaluated using a reproducible import and benchmark before a later dataset update; no claim of exhaustive English coverage.

[Tatoeba through ManyThings](https://www.manythings.org/anki/) provides 15,589 paired English–Indonesian sentences, CC BY 2.0 France. Each pair retains both sentence IDs and contributor names. FTS token/phrase matching retrieves examples independently; the interface says their sense may differ.

## Licences and transformation
Dictionary derivative: CC BY-SA 4.0, with Wiktionary contributor, DBnary and WikDict attribution in-app and repo. Examples: separate CC BY 2.0 France attribution per row. Font: OFL. Code: repository MIT. Dataset licences do not change to MIT. Reproduction command and SHA-256 input hashes in app/src/main/assets/dictionary.json. No machine-generated translations.

## Verification gates
SQLite integrity check, nonempty fields, word/sense counts, no duplicate IDs, indexed prefix query plan, licensed example credit, source sample equivalence, strict selected-answer matching, review transaction, invalid update rejection. Automated passing checks and device limitations are recorded separately in QA.md.

## 1.2.0 interface research

| Reference | Decision in VocaMemo |
|---|---|
| [Dictionary (translate & memorize), Alisa Zymovets on Behance](https://www.behance.net/gallery/124842629/Dictionary-(translate-memorize)-Mobile-App) | Keep looking up a word and saving a meaning within the same lexical flow. |
| [Learn Mode, UI Ants on Dribbble](https://dribbble.com/shots/24138576-Learn-Mode-Mobile-App-Ui) | Give the daily learning action priority, with other modes easy to scan beneath it. |
| [Word.ws A–Z](https://word.ws/browse/) | Browsing starts with real entries, and a direct letter jump stays close to the list. |
| [Strava Progress Summary Chart](https://support.strava.com/en-us/articles/15401618-progress-summary-chart) | Group real activity by time period, with readable values and totals. |
| [Apple HIG Materials](https://developer.apple.com/design/human-interface-guidelines/materials) and [Android touch target guidance](https://support.google.com/accessibility/android/answer/7101858) | Restrict glass to the control layer and keep Android-native touch, state and text scaling. |
| [The Design of Everyday Things, Don Norman](https://mitpress.mit.edu/9780262525671/the-design-of-everyday-things/) and [Don't Make Me Think, Steve Krug](https://www.pearson.com/en-au/media/zollwzef/9780321965516.pdf) | Give letter jumps, save actions and theme controls explicit signifiers and feedback. Name sections so readers can find settings and statistics by scanning. |

The design references informed information hierarchy and interaction patterns. VocaMemo uses original Compose layouts and the existing supplied icon.

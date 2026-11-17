# VocaMemo design system
Native Android Operate surface. Explicit brief pins soft blue, rounded education components and layered cards. Read PRODUCT.md first.

## Direction contract
THESIS: a word's selected meaning travels intact from lookup to daily recall.
OWN-WORLD: cool pale blue ground, white surfaces, blue type accents, yellow bookmark highlights, dark navy body text. Nunito, Material rounded icons, 20–28dp cards, 20dp outer gutters, 8dp rhythm.
STORY: search a real word, inspect contextual senses, save one meaning, practise and see actual progress.
FIRST VIEWPORT: compact brand bar; large two-line purpose heading; prominent labelled search; offline dictionary source/count panel; real quick-word shortcuts; daily practice invitation.
FORM: code-led native Material 3 with bottom navigation and focused practice surface; seed: none (pinned), user requested supplied blue icon and education references.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Tokens and accessibility
Primary #245CCA on white; text #172842; secondary text #475770; background #F4F7FD; card #FFFFFF; primary container #DEE9FF; yellow container #FFE8A6; success #006C58. Dark theme maps semantic tokens, never raw per-screen colours. All Compose typography roles use bundled Nunito, IPA falls back through Android font fallback. Body 16sp with native scaling. Every icon action has a label; touch targets use Material 48dp controls. Content scrolls at large font sizes and is constrained to 840dp on tablets. No fixed-height text regions.

## Interaction
Native ripples and back semantics, query debounce 180ms, lazy lists, loading/error/empty states; flashcard answer uses Compose animation following system duration scale. Offline copying, search and network all on IO. Practice timer only advances while resumed; automatic repetition pauses in background and does not fabricate statistics. Delete and exiting practice require explicit confirmation. TTS buttons use offline English voices.

## Skill integration
Ponytail Full controls code simplicity; UI UX Pro Max supplies accessibility/touch/Compose checks; Impeccable supplies visual quality and final rendered review. User brief and native conventions win conflicts. Generic web marketing/AI-purple recommendations were rejected as unrelated. Skill copies are versioned under .agents/skills; upstream versions/hashes in docs/tooling.md.

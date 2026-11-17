# Research before implementation — 2026-11-17
Primary sources inspected before product code:
- Magoosh Mental Math flashcards: https://gre.magoosh.com/flashcards/mental-math — ready-made arithmetic groups and one clear prompt. Retain grouped facts and reveal; omit exam content and formulas.
- AnkiDroid: https://ankidroid.org/ — offline flashcards and spaced repetition. Retain per-fact due dates and explicit self-rating; simplify deck management for a personal daily routine.
- Mental Math Trainer: https://www.mathtrainer.xyz/ — configurable operand digits and recorded solve times. Retain digit controls and timed recall, restrict scope to 1–3 digits.
- Math Workout: https://play.google.com/store/apps/details?id=io.ts.mathworkout — four operations and short arithmetic drills. Retain quick entry and brief sessions, omit ads/game clutter.
- Numbio: https://apps.apple.com/us/app/mental-math-games-numbio/id1551025256 — adaptable practice and progress tracking. Retain daily continuity; omit paid plans and formula topics.
These are source/listing reviews, not claims of hands-on testing of competitor binaries.

Visual references fetched and inspected: Dribbble 26974846 (blue palette, light surfaces, broad pill buttons), 24138576 (quiet Learn modes, progress and green feedback), Behance 111025041 (tactile card stack and direct reveal/review). The supplied icon defines the identity. Launcher artwork is a generation-assisted production adaptation of the image supplied in chat; the original chat file was not mounted as a downloadable file.

Asset-first decisions: reuse existing repository Gradle wrapper, pinned Compose/AndroidX dependencies, Material icons, bundled Plus Jakarta Sans with OFL, and the hardened ScreenGuard updater. Use Android SQLite transactions and Storage Access Framework rather than a custom sync service. No runtime network dependency for learning. User-requested skills vendored from official repos; versions recorded in tools/SOURCES.json.

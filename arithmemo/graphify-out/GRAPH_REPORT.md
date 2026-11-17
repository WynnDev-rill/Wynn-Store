# Graph Report - arithmemo  (2026-11-17)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 296 nodes · 568 edges · 12 communities (4 shown, 8 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 23 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f3ecc98e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MemoApp.kt
- Fact
- AppUpdater.kt
- MemoStore
- StudySession
- UpdateManifest
- TimerTest.kt
- MemoViewModel
- AppUpdater
- NativeUiTest.kt
- Theme.kt
- MainActivity.kt

## God Nodes (most connected - your core abstractions)
1. `MemoViewModel` - 30 edges
2. `MemoStore` - 19 edges
3. `AppUpdater` - 18 edges
4. `Fact` - 16 edges
5. `MemoApp()` - 14 edges
6. `PracticeConfig` - 13 edges
7. `StudySession` - 12 edges
8. `Attempt` - 11 edges
9. `Snapshot` - 11 edges
10. `UserSettings` - 11 edges

## Surprising Connections (you probably didn't know these)
- `MemoViewModel` --calls--> `Snapshot`  [INFERRED]
  main/java/id/wynn/arithmemo/ui/MemoViewModel.kt → main/java/id/wynn/arithmemo/core/Models.kt
- `MemoApp()` --calls--> `ArithMemoTheme()`  [INFERRED]
  main/java/id/wynn/arithmemo/ui/MemoApp.kt → main/java/id/wynn/arithmemo/ui/Theme.kt
- `Attempt` --references--> `Fact`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt
- `StudySession` --references--> `Fact`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt
- `EquationCard()` --references--> `Fact`  [EXTRACTED]
  main/java/id/wynn/arithmemo/ui/MemoApp.kt → main/java/id/wynn/arithmemo/core/Core.kt

## Import Cycles
- None detected.

## Communities (12 total, 8 thin omitted)

### Community 0 - "MemoApp.kt"
Cohesion: 0.07
Nodes (20): ActionIcon(), EquationCard(), Filters(), Heading(), HomeScreen(), LibraryScreen(), MemoApp(), Metric() (+12 more)

### Community 1 - "Fact"
Cohesion: 0.09
Nodes (20): Fact, FactGroup, ALL, LOWER, UPPER, Facts, Operation, ADD (+12 more)

### Community 4 - "StudySession"
Cohesion: 0.14
Nodes (12): Attempt, Backup, Snapshot, Stats, StudyMode, ADAPTIVE, FLASHCARD, RANDOM (+4 more)

### Community 5 - "UpdateManifest"
Cohesion: 0.13
Nodes (4): branded(), ApkTransfer, UpdateManifest, UpdateTest

## Knowledge Gaps
- **20 isolated node(s):** `HOME`, `LIBRARY`, `SETTINGS`, `STATS`, `ALL` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 106 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MemoViewModel` connect `MemoViewModel` to `MemoApp.kt`, `AppUpdater.kt`, `MemoStore`, `StudySession`, `TimerTest.kt`, `AppUpdater`?**
  _High betweenness centrality (0.256) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `MemoStore` (e.g. with `.answersAndSessionSurviveCloseAndDuplicateCommit()` and `.invalidImportCannotOverwriteExistingData()`) actually correct?**
  _`MemoStore` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `HOME`, `LIBRARY`, `SETTINGS` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `MemoApp.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06966618287373004 - nodes in this community are weakly interconnected._
- **Why does `AppUpdater` connect `AppUpdater` to `MemoApp.kt`, `AppUpdater.kt`, `MemoViewModel`?**
  _High betweenness centrality (0.190) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Fact` (e.g. with `.factKeyRoundTripsAndRejectsInvalidOperands()` and `.answersAndSessionSurviveCloseAndDuplicateCommit()`) actually correct?**
  _`Fact` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Fact` be split into smaller, more focused modules?**
  _Cohesion score 0.09413067552602436 - nodes in this community are weakly interconnected._
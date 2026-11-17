# Graph Report - arithmemo  (2026-11-17)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 333 nodes · 664 edges · 12 communities (7 shown, 5 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 57 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `47da2236`
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
1. `MemoViewModel` - 31 edges
2. `MemoStore` - 21 edges
3. `AppUpdater` - 19 edges
4. `Fact` - 19 edges
5. `StudySession` - 14 edges
6. `MemoApp()` - 14 edges
7. `PracticeConfig` - 13 edges
8. `Attempt` - 13 edges
9. `Snapshot` - 12 edges
10. `UserSettings` - 11 edges

## Surprising Connections (you probably didn't know these)
- `MemoViewModel` --calls--> `Snapshot`  [INFERRED]
  main/java/id/wynn/arithmemo/ui/MemoViewModel.kt → main/java/id/wynn/arithmemo/core/Models.kt
- `MemoApp()` --calls--> `ArithMemoTheme()`  [INFERRED]
  main/java/id/wynn/arithmemo/ui/MemoApp.kt → main/java/id/wynn/arithmemo/ui/Theme.kt
- `UserSettings` --references--> `PracticeConfig`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt
- `Backup` --references--> `Progress`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt
- `Snapshot` --references--> `Progress`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt

## Import Cycles
- None detected.

## Communities (12 total, 5 thin omitted)

### Community 0 - "MemoApp.kt"
Cohesion: 0.09
Nodes (15): HomeScreen(), LibraryScreen(), MemoApp(), SessionScreen(), SettingsScreen(), StatsScreen(), ActionIcon(), EquationCard() (+7 more)

### Community 1 - "Fact"
Cohesion: 0.09
Nodes (19): FactGroup, ALL, LOWER, UPPER, Facts, Operation, ADD, DIVIDE (+11 more)

### Community 2 - "AppUpdater.kt"
Cohesion: 0.08
Nodes (3): AppUpdater, State, UpdateManifest

### Community 3 - "MemoStore"
Cohesion: 0.12
Nodes (14): Fact, Attempt, Backup, Snapshot, Stats, StudyMode, ADAPTIVE, FLASHCARD (+6 more)

### Community 4 - "StudySession"
Cohesion: 0.08
Nodes (3): MainActivity, MemoApplication, UpdateWorker

### Community 5 - "UpdateManifest"
Cohesion: 0.09
Nodes (4): ArithMemoTheme(), branded(), ApkTransfer, UpdateTest

### Community 9 - "NativeUiTest.kt"
Cohesion: 0.12
Nodes (5): Page, HOME, LIBRARY, SETTINGS, STATS

## Knowledge Gaps
- **20 isolated node(s):** `ALL`, `LOWER`, `UPPER`, `ADD`, `DIVIDE` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 111 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AppUpdater` connect `AppUpdater.kt` to `MemoApp.kt`, `MemoStore`, `StudySession`, `TimerTest.kt`, `AppUpdater`?**
  _High betweenness centrality (0.225) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `MemoStore` (e.g. with `.answersAndSessionSurviveCloseAndDuplicateCommit()` and `.invalidImportCannotOverwriteExistingData()`) actually correct?**
  _`MemoStore` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ALL`, `LOWER`, `UPPER` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `MemoApp.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08974358974358974 - nodes in this community are weakly interconnected._
- **Why does `MemoViewModel` connect `AppUpdater` to `MemoApp.kt`, `AppUpdater.kt`, `MemoStore`, `TimerTest.kt`, `MemoViewModel`?**
  _High betweenness centrality (0.224) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `AppUpdater` (e.g. with `.verifiedDownloadOpensAndroidInstallerAndRetainsStudyData()` and `.doWork()`) actually correct?**
  _`AppUpdater` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `Fact` be split into smaller, more focused modules?**
  _Cohesion score 0.09024390243902439 - nodes in this community are weakly interconnected._
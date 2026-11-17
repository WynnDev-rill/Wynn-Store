# Graph Report - arithmemo  (2026-11-17)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 328 nodes · 652 edges · 12 communities (5 shown, 7 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 53 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `46bd9a9c`
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
2. `MemoStore` - 21 edges
3. `AppUpdater` - 19 edges
4. `Fact` - 18 edges
5. `MemoApp()` - 14 edges
6. `PracticeConfig` - 13 edges
7. `Attempt` - 13 edges
8. `StudySession` - 13 edges
9. `Snapshot` - 11 edges
10. `UserSettings` - 11 edges

## Surprising Connections (you probably didn't know these)
- `MemoViewModel` --calls--> `Snapshot`  [INFERRED]
  main/java/id/wynn/arithmemo/ui/MemoViewModel.kt → main/java/id/wynn/arithmemo/core/Models.kt
- `MemoViewModel` --calls--> `AppUpdater`  [EXTRACTED]
  main/java/id/wynn/arithmemo/ui/MemoViewModel.kt → main/java/id/wynn/arithmemo/update/AppUpdater.kt
- `SettingsScreen()` --references--> `AppUpdater`  [EXTRACTED]
  main/java/id/wynn/arithmemo/ui/SettingsScreen.kt → main/java/id/wynn/arithmemo/update/AppUpdater.kt
- `State` --references--> `UpdateManifest`  [EXTRACTED]
  main/java/id/wynn/arithmemo/update/AppUpdater.kt → main/java/id/wynn/arithmemo/update/UpdateManifest.kt
- `Attempt` --references--> `Fact`  [EXTRACTED]
  main/java/id/wynn/arithmemo/core/Models.kt → main/java/id/wynn/arithmemo/core/Core.kt

## Import Cycles
- None detected.

## Communities (12 total, 7 thin omitted)

### Community 0 - "MemoApp.kt"
Cohesion: 0.07
Nodes (16): HomeScreen(), LibraryScreen(), MemoApp(), SessionScreen(), SettingsScreen(), StatsScreen(), ArithMemoTheme(), ActionIcon() (+8 more)

### Community 2 - "AppUpdater.kt"
Cohesion: 0.09
Nodes (20): Fact, FactGroup, ALL, LOWER, UPPER, Facts, Operation, ADD (+12 more)

### Community 4 - "StudySession"
Cohesion: 0.12
Nodes (13): Attempt, Backup, Snapshot, Stats, StudyMode, ADAPTIVE, FLASHCARD, RANDOM (+5 more)

### Community 5 - "UpdateManifest"
Cohesion: 0.13
Nodes (4): branded(), ApkTransfer, UpdateManifest, UpdateTest

### Community 8 - "AppUpdater"
Cohesion: 0.13
Nodes (5): Page, HOME, LIBRARY, SETTINGS, STATS

## Knowledge Gaps
- **20 isolated node(s):** `ALL`, `LOWER`, `UPPER`, `ADD`, `DIVIDE` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 106 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MemoViewModel` connect `MemoViewModel` to `MemoApp.kt`, `Fact`, `MemoStore`, `StudySession`, `TimerTest.kt`?**
  _High betweenness centrality (0.232) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `MemoStore` (e.g. with `.answersAndSessionSurviveCloseAndDuplicateCommit()` and `.invalidImportCannotOverwriteExistingData()`) actually correct?**
  _`MemoStore` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ALL`, `LOWER`, `UPPER` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `MemoApp.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06696428571428571 - nodes in this community are weakly interconnected._
- **Why does `AppUpdater` connect `Fact` to `MemoApp.kt`, `Theme.kt`, `MemoStore`, `MemoViewModel`?**
  _High betweenness centrality (0.227) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `AppUpdater` (e.g. with `.verifiedDownloadOpensAndroidInstallerAndRetainsStudyData()` and `.doWork()`) actually correct?**
  _`AppUpdater` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Should `Fact` be split into smaller, more focused modules?**
  _Cohesion score 0.07087486157253599 - nodes in this community are weakly interconnected._
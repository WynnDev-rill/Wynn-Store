# Graph Report - arithmemo  (2026-11-17)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 341 nodes · 684 edges · 13 communities (6 shown, 7 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 57 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f35abab8`
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
- MainActivity.kt

## God Nodes (most connected - your core abstractions)
1. `MemoViewModel` - 31 edges
2. `MemoStore` - 23 edges
3. `Fact` - 19 edges
4. `AppUpdater` - 19 edges
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

## Communities (13 total, 7 thin omitted)

### Community 0 - "MemoApp.kt"
Cohesion: 0.06
Nodes (20): HomeScreen(), LibraryScreen(), MemoApp(), Page, HOME, LIBRARY, SETTINGS, STATS (+12 more)

### Community 1 - "Fact"
Cohesion: 0.09
Nodes (20): Fact, FactGroup, ALL, LOWER, UPPER, Facts, Operation, ADD (+12 more)

### Community 3 - "MemoStore"
Cohesion: 0.12
Nodes (13): Attempt, Backup, Snapshot, Stats, StudyMode, ADAPTIVE, FLASHCARD, RANDOM (+5 more)

### Community 4 - "StudySession"
Cohesion: 0.09
Nodes (4): ArithMemoTheme(), branded(), ApkTransfer, UpdateTest

### Community 6 - "TimerTest.kt"
Cohesion: 0.16
Nodes (3): NativeUiTest, RestorationTest, saveDeviceCapture()

### Community 7 - "MemoViewModel"
Cohesion: 0.17
Nodes (3): AppUpdater, State, UpdateManifest

## Knowledge Gaps
- **20 isolated node(s):** `HOME`, `LIBRARY`, `SETTINGS`, `STATS`, `ALL` (+15 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 114 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MemoStore` connect `UpdateManifest` to `MemoApp.kt`, `MemoStore`, `TimerTest.kt`, `AppUpdater`, `NativeUiTest.kt`, `Theme.kt`, `MainActivity.kt`?**
  _High betweenness centrality (0.260) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `MemoStore` (e.g. with `.answersAndSessionSurviveCloseAndDuplicateCommit()` and `.invalidImportCannotOverwriteExistingData()`) actually correct?**
  _`MemoStore` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `HOME`, `LIBRARY`, `SETTINGS` to the rest of the system?**
  _20 weakly-connected nodes found - possible missing edges._
- **Should `MemoApp.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06095481670929241 - nodes in this community are weakly interconnected._
- **Why does `MemoViewModel` connect `AppUpdater` to `MemoApp.kt`, `MemoStore`, `UpdateManifest`, `MemoViewModel`, `NativeUiTest.kt`?**
  _High betweenness centrality (0.221) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Fact` (e.g. with `.verifiedDownloadOpensAndroidInstallerAndRetainsStudyData()` and `.factKeyRoundTripsAndRejectsInvalidOperands()`) actually correct?**
  _`Fact` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Fact` be split into smaller, more focused modules?**
  _Cohesion score 0.09413067552602436 - nodes in this community are weakly interconnected._
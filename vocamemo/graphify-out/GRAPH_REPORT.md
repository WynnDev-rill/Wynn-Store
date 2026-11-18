# Graph Report - vocamemo  (2026-11-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 470 nodes · 811 edges · 35 communities (11 shown, 24 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 36 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a297b5b4`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 6
- Community 7
- Community 8
- Community 10
- Community 11
- Community 12
- Community 14
- Community 15
- Community 16
- Community 17
- Community 19
- Community 22
- Community 23
- Community 24
- Community 28
- Community 29
- Community 31

## God Nodes (most connected - your core abstractions)
1. `VocaModel` - 41 edges
2. `Memo` - 25 edges
3. `VocaScreen()` - 19 edges
4. `Dictionary` - 17 edges
5. `MemoScreen()` - 15 edges
6. `Updates` - 13 edges
7. `VocaDao` - 12 edges
8. `UiTest` - 11 edges
9. `WordScreen()` - 10 edges
10. `Review` - 10 edges

## Surprising Connections (you probably didn't know these)
- `VocaScreen()` --calls--> `WelcomeScreen()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/AccountScreen.kt
- `VocaScreen()` --calls--> `DictionaryScreen()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt
- `VocaScreen()` --calls--> `ProfileScreen()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/ProfileScreen.kt
- `VocaScreen()` --calls--> `VocaNavigation()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/VocaUi.kt
- `VocaScreen()` --calls--> `VocaRail()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/VocaUi.kt

## Import Cycles
- None detected.

## Communities (35 total, 24 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.09
Nodes (18): clearGoogleAccount(), dictionaryKey(), FlowRowCompat(), GroupPicker(), Hint(), MainActivity, MemoEditor(), MemoScreen() (+10 more)

### Community 1 - "Community 1"
Cohesion: 0.04
Nodes (40): graphify, VocaMemo development contract, Direction, Layout and assets, Screens, VocaMemo visual system, Google sign-in, Implementation audit (+32 more)

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (8): ImportResult, json(), Memo, memoFrom(), PersonalStore, Review, VocaDao, VocaDatabase

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (3): Release, Updates, UpdateWorker

### Community 7 - "Community 7"
Cohesion: 0.06
Nodes (10): memoKey(), Answers, Practice, VocaApp, VocaModel, WordFilter, All, Illustrated (+2 more)

### Community 10 - "Community 10"
Cohesion: 0.27
Nodes (3): Scheduler, UpdatePolicy, CoreTest

### Community 11 - "Community 11"
Cohesion: 0.33
Nodes (5): Architecture and alternatives, Quality floor, Update and signing, VocaMemo 1.0.0 design, Workflows

### Community 12 - "Community 12"
Cohesion: 0.40
Nodes (4): Global constraints, Review focus, Tasks, VocaMemo Implementation Plan

### Community 14 - "Community 14"
Cohesion: 0.17
Nodes (6): Dictionary, Example, indonesianRoots(), Sense, TypoDistance, WordResult

### Community 17 - "Community 17"
Cohesion: 0.13
Nodes (3): VocaNavigation(), VocaRail(), VocaSearch()

### Community 29 - "Community 29"
Cohesion: 0.14
Nodes (3): DictionaryScreen(), WordPhoto, WordTile()

## Knowledge Gaps
- **4 isolated node(s):** `All`, `Illustrated`, `Random`, `Unsaved`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 186 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VocaModel` connect `Community 7` to `Community 0`, `Community 3`, `Community 14`, `Community 19`, `Community 28`, `Community 29`?**
  _High betweenness centrality (0.130) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `Memo` (e.g. with `.persistentRoomReopens()` and `.progressIsAtomicBackupIsValidated()`) actually correct?**
  _`Memo` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `All`, `Illustrated`, `Random` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.09292929292929293 - nodes in this community are weakly interconnected._
- **Why does `Memo` connect `Community 2` to `Community 0`, `Community 6`, `Community 7`, `Community 8`, `Community 10`, `Community 28`, `Community 29`?**
  _High betweenness centrality (0.121) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `VocaScreen()` (e.g. with `clearGoogleAccount()` and `WelcomeScreen()`) actually correct?**
  _`VocaScreen()` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.04081632653061224 - nodes in this community are weakly interconnected._
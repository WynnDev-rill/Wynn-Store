# Graph Report - vocamemo  (2026-11-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 418 nodes · 696 edges · 32 communities (10 shown, 22 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 25 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e292296c`
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
- Community 25

## God Nodes (most connected - your core abstractions)
1. `VocaModel` - 39 edges
2. `Memo` - 25 edges
3. `Dictionary` - 16 edges
4. `VocaScreen()` - 15 edges
5. `MemoScreen()` - 13 edges
6. `Updates` - 13 edges
7. `VocaDao` - 12 edges
8. `Review` - 11 edges
9. `UiTest` - 10 edges
10. `Design System: VocaMemo` - 10 edges

## Surprising Connections (you probably didn't know these)
- `VocaScreen()` --calls--> `DictionaryScreen()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt
- `VocaApp` --calls--> `Dictionary`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/VocaModel.kt → app/src/main/java/id/wynn/vocamemo/Dictionary.kt
- `VocaApp` --calls--> `PersonalStore`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/VocaModel.kt → app/src/main/java/id/wynn/vocamemo/Data.kt
- `DictionaryScreen()` --calls--> `dictionaryKey()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt → app/src/main/java/id/wynn/vocamemo/Dictionary.kt
- `VocaScreen()` --references--> `VocaModel`  [EXTRACTED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/VocaModel.kt

## Import Cycles
- None detected.

## Communities (32 total, 22 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (19): FlowRowCompat(), GroupPicker(), Hint(), MainActivity, MemoEditor(), MemoScreen(), Pill(), posLabel() (+11 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (31): graphify, VocaMemo development contract, Actions Secrets, ArithMemo-style private signing recovery, Local signed build, Next release checklist, Signing, release and update operations, Stable feed (+23 more)

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (8): ImportResult, json(), Memo, memoFrom(), PersonalStore, Review, VocaDao, VocaDatabase

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (3): Release, Updates, UpdateWorker

### Community 7 - "Community 7"
Cohesion: 0.07
Nodes (7): Practice, VocaApp, VocaModel, WordFilter, All, Random, Unsaved

### Community 10 - "Community 10"
Cohesion: 0.17
Nodes (5): memoKey(), Answers, Scheduler, UpdatePolicy, CoreTest

### Community 11 - "Community 11"
Cohesion: 0.33
Nodes (5): Architecture and alternatives, Quality floor, Update and signing, VocaMemo 1.0.0 design, Workflows

### Community 12 - "Community 12"
Cohesion: 0.40
Nodes (4): Global constraints, Review focus, Tasks, VocaMemo Implementation Plan

### Community 14 - "Community 14"
Cohesion: 0.17
Nodes (7): Dictionary, dictionaryKey(), Example, indonesianRoots(), Sense, TypoDistance, WordResult

### Community 17 - "Community 17"
Cohesion: 0.08
Nodes (25): Buttons, Cards / Containers, Chips and pills, Colors, Components, Design System: VocaMemo, Direction contract, Do: (+17 more)

## Knowledge Gaps
- **3 isolated node(s):** `All`, `Random`, `Unsaved`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 172 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **22 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Memo` connect `Community 2` to `Community 0`, `Community 6`, `Community 7`, `Community 8`, `Community 10`, `Community 19`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `Memo` (e.g. with `.persistentRoomReopens()` and `.progressIsAtomicBackupIsValidated()`) actually correct?**
  _`Memo` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `All`, `Random`, `Unsaved` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.0783744557329463 - nodes in this community are weakly interconnected._
- **Why does `VocaModel` connect `Community 7` to `Community 0`, `Community 10`, `Community 19`, `Community 14`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Dictionary` (e.g. with `.browsePagingAndSearchDirections()` and `.dictionarySearchSensesExamples()`) actually correct?**
  _`Dictionary` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.05128205128205128 - nodes in this community are weakly interconnected._
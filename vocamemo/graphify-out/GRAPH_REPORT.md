# Graph Report - vocamemo  (2026-11-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 457 nodes · 820 edges · 32 communities (12 shown, 20 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 46 edges (avg confidence: 0.85)
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

## God Nodes (most connected - your core abstractions)
1. `VocaModel` - 38 edges
2. `Memo` - 24 edges
3. `VocaScreen()` - 16 edges
4. `Dictionary` - 16 edges
5. `Updates` - 13 edges
6. `MemoScreen()` - 12 edges
7. `VocaDao` - 12 edges
8. `UiTest` - 11 edges
9. `DictionaryScreen()` - 11 edges
10. `WordScreen()` - 10 edges

## Surprising Connections (you probably didn't know these)
- `DictionaryScreen()` --calls--> `dictionaryKey()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt → app/src/main/java/id/wynn/vocamemo/Dictionary.kt
- `DictionaryScreen()` --calls--> `Hint()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt → app/src/main/java/id/wynn/vocamemo/MainActivity.kt
- `VocaScreen()` --calls--> `DictionaryScreen()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt
- `DictionaryEntry()` --calls--> `entryType()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt → app/src/main/java/id/wynn/vocamemo/Theme.kt
- `MemoScreen()` --calls--> `VocaSearch()`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/MemoScreen.kt → app/src/main/java/id/wynn/vocamemo/DictionaryScreen.kt

## Import Cycles
- None detected.

## Communities (32 total, 20 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (7): DictionaryEntry(), dictionaryLetter(), DictionaryScreen(), VocaSearch(), MainActivity, posLabel(), type()

### Community 1 - "Community 1"
Cohesion: 0.04
Nodes (41): graphify, VocaMemo development contract, Colors, Components, Design System: VocaMemo, Do's and Don'ts, Elevation & Depth, Layout (+33 more)

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (8): ImportResult, json(), Memo, memoFrom(), PersonalStore, Review, VocaDao, VocaDatabase

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (5): GoalDialog(), readLimited(), SettingRow(), SettingsGroup(), SettingsScreen()

### Community 6 - "Community 6"
Cohesion: 0.19
Nodes (14): FlowRowCompat(), Hint(), Pill(), Pronouncer, Title(), VocaDock(), VocaScreen(), WordScreen() (+6 more)

### Community 7 - "Community 7"
Cohesion: 0.06
Nodes (8): dictionaryKey(), Practice, VocaApp, VocaModel, WordFilter, All, Random, Unsaved

### Community 10 - "Community 10"
Cohesion: 0.13
Nodes (5): memoKey(), Answers, Scheduler, UpdatePolicy, CoreTest

### Community 11 - "Community 11"
Cohesion: 0.33
Nodes (5): Architecture and alternatives, Quality floor, Update and signing, VocaMemo 1.0.0 design, Workflows

### Community 12 - "Community 12"
Cohesion: 0.40
Nodes (4): Global constraints, Review focus, Tasks, VocaMemo Implementation Plan

### Community 14 - "Community 14"
Cohesion: 0.12
Nodes (7): OfflineTest, Dictionary, Example, indonesianRoots(), Sense, TypoDistance, WordResult

### Community 17 - "Community 17"
Cohesion: 0.21
Nodes (3): Release, Updates, UpdateWorker

### Community 23 - "Community 23"
Cohesion: 0.11
Nodes (5): glass(), ReadingBackdrop(), usesDarkTheme(), VocaTheme(), windowColor()

## Knowledge Gaps
- **3 isolated node(s):** `All`, `Random`, `Unsaved`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 166 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **20 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VocaModel` connect `Community 7` to `Community 0`, `Community 3`, `Community 6`, `Community 10`, `Community 14`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `Memo` (e.g. with `.persistentRoomReopens()` and `.progressIsAtomicBackupIsValidated()`) actually correct?**
  _`Memo` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `All`, `Random`, `Unsaved` to the rest of the system?**
  _3 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05505952380952381 - nodes in this community are weakly interconnected._
- **Why does `Memo` connect `Community 2` to `Community 0`, `Community 6`, `Community 7`, `Community 8`, `Community 10`, `Community 14`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `VocaScreen()` (e.g. with `DictionaryScreen()` and `MemoScreen()`) actually correct?**
  _`VocaScreen()` has 6 INFERRED edges - model-reasoned connections that need verification._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.04081632653061224 - nodes in this community are weakly interconnected._
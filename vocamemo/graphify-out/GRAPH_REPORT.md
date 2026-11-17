# Graph Report - vocamemo  (2026-11-17)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 345 nodes · 572 edges · 27 communities (10 shown, 17 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 19 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7b0dcc05`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 5
- Community 6
- Community 7
- Community 8
- Community 10
- Community 11
- Community 12
- Community 14
- Community 15
- Community 16
- Community 25

## God Nodes (most connected - your core abstractions)
1. `VocaModel` - 35 edges
2. `Memo` - 24 edges
3. `VocaScreen()` - 15 edges
4. `MemoScreen()` - 13 edges
5. `Updates` - 13 edges
6. `Review` - 12 edges
7. `VocaDao` - 12 edges
8. `DictionaryScreen()` - 10 edges
9. `Dictionary` - 10 edges
10. `UiTest` - 9 edges

## Surprising Connections (you probably didn't know these)
- `VocaApp` --calls--> `PersonalStore`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/VocaModel.kt → app/src/main/java/id/wynn/vocamemo/Data.kt
- `VocaApp` --calls--> `Dictionary`  [INFERRED]
  app/src/main/java/id/wynn/vocamemo/VocaModel.kt → app/src/main/java/id/wynn/vocamemo/Dictionary.kt
- `VocaScreen()` --references--> `VocaModel`  [EXTRACTED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/VocaModel.kt
- `DictionaryScreen()` --references--> `Review`  [EXTRACTED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/Data.kt
- `DictionaryScreen()` --references--> `VocaModel`  [EXTRACTED]
  app/src/main/java/id/wynn/vocamemo/MainActivity.kt → app/src/main/java/id/wynn/vocamemo/VocaModel.kt

## Import Cycles
- None detected.

## Communities (27 total, 17 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (22): DictionaryScreen(), FlowRowCompat(), GroupPicker(), Hint(), MainActivity, MemoEditor(), MemoScreen(), Pill() (+14 more)

### Community 1 - "Community 1"
Cohesion: 0.05
Nodes (34): graphify, VocaMemo development contract, Direction contract, Interaction, Skill integration, Tokens and accessibility, VocaMemo design system, Actions Secrets (+26 more)

### Community 2 - "Community 2"
Cohesion: 0.12
Nodes (8): ImportResult, json(), Memo, memoFrom(), PersonalStore, Review, VocaDao, VocaDatabase

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (3): Release, Updates, UpdateWorker

### Community 5 - "Community 5"
Cohesion: 0.26
Nodes (4): Dictionary, Example, Sense, WordResult

### Community 7 - "Community 7"
Cohesion: 0.09
Nodes (3): Practice, VocaApp, VocaModel

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
Cohesion: 0.40
Nodes (4): Device checks, Host checks completed, Practical limits, VocaMemo 1.0.0 verification

## Knowledge Gaps
- **17 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Memo` connect `Community 2` to `Community 0`, `Community 6`, `Community 7`, `Community 8`, `Community 10`?**
  _High betweenness centrality (0.131) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `Memo` (e.g. with `.persistentRoomReopens()` and `.progressIsAtomicBackupIsValidated()`) actually correct?**
  _`Memo` has 5 INFERRED edges - model-reasoned connections that need verification._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05711849957374254 - nodes in this community are weakly interconnected._
- **Why does `VocaModel` connect `Community 7` to `Community 0`, `Community 10`, `Community 3`, `Community 5`?**
  _High betweenness centrality (0.122) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.04878048780487805 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.12436974789915967 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.06829268292682927 - nodes in this community are weakly interconnected._
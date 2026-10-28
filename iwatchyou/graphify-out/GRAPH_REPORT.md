# Graph Report - iwatchyou  (2026-10-29)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 353 nodes · 717 edges · 20 communities (5 shown, 15 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 21 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b2977b66`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16

## God Nodes (most connected - your core abstractions)
1. `ForegroundPolicyTest` - 35 edges
2. `TimeLedger` - 32 edges
3. `WatchService` - 30 edges
4. `OverlayController` - 27 edges
5. `DeviceTest` - 25 edges
6. `MainActivity` - 15 edges
7. `TimeLedgerTest` - 15 edges
8. `HistoryStore` - 15 edges
9. `ForegroundPolicy` - 7 edges
10. `Window` - 6 edges

## Surprising Connections (you probably didn't know these)
- `WatchService` --references--> `OverlayController`  [EXTRACTED]
  app/src/main/java/id/wynn/iwatchyou/WatchService.kt → app/src/main/java/id/wynn/iwatchyou/OverlayController.kt
- `WatchService` --references--> `TimeLedger`  [EXTRACTED]
  app/src/main/java/id/wynn/iwatchyou/WatchService.kt → app/src/main/java/id/wynn/iwatchyou/TimeLedger.kt
- `WatchService` --references--> `HistoryStore`  [EXTRACTED]
  app/src/main/java/id/wynn/iwatchyou/WatchService.kt → app/src/main/java/id/wynn/iwatchyou/HistoryStore.kt

## Import Cycles
- None detected.

## Communities (20 total, 15 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.07
Nodes (6): ClockGlyph(), dayLabel(), HourChart(), MainActivity, PowerGlyph(), typography()

### Community 1 - "Community 1"
Cohesion: 0.13
Nodes (3): ForegroundPolicy, Window, ForegroundPolicyTest

### Community 3 - "Community 3"
Cohesion: 0.15
Nodes (3): Snapshot, TimeLedger, TimeLedgerTest

### Community 4 - "Community 4"
Cohesion: 0.11
Nodes (11): DeviceQaOutcomeTest, adb(), capture(), duration(), find(), shell(), tap(), tree() (+3 more)

### Community 9 - "Community 9"
Cohesion: 0.15
Nodes (3): connection(), parse(), Release

## Knowledge Gaps
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `WatchService` connect `Community 5` to `Community 0`, `Community 2`, `Community 3`, `Community 6`?**
  _High betweenness centrality (0.339) - this node is a cross-community bridge._
- **Are the 14 inferred relationships involving `TimeLedger` (e.g. with `.backgroundAppsAreNotCounted()` and `.backgroundGapAfterProcessRestartIsNeverInvented()`) actually correct?**
  _`TimeLedger` has 14 INFERRED edges - model-reasoned connections that need verification._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.0653061224489796 - nodes in this community are weakly interconnected._
- **Why does `HistoryStore` connect `Community 6` to `Community 0`, `Community 5`, `Community 7`?**
  _High betweenness centrality (0.170) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.13131313131313133 - nodes in this community are weakly interconnected._
- **Why does `TimeLedger` connect `Community 3` to `Community 5`, `Community 6`?**
  _High betweenness centrality (0.139) - this node is a cross-community bridge._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.12162162162162163 - nodes in this community are weakly interconnected._
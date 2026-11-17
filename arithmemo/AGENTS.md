# ArithMemo — durable coding instructions
Read the parent AGENTS.md. Only edit this app and its dedicated workflows. Native Kotlin + Compose; offline SQLite is the source of truth. Never add accounts, ads, formula lessons or remote analytics.

## Skills and priority
User requirements and supplied visual references > correctness, security, data durability and accessibility > `DESIGN.md` > project skills. Read `.agents/skills/ponytail/SKILL.md` (Full default, Ultra only when justified), `.agents/skills/ui-ux-pro-max/SKILL.md` for searchable Compose guidance, and `.agents/skills/impeccable/SKILL.md` for visual review. These skills complement one another: Ponytail limits unnecessary code, UI UX Pro Max informs native interaction, Impeccable checks rendered quality. Generic aesthetic rules never override the supplied references. Skill workflows must not reduce the user's requested feature scope.

## Verification first
Write meaningful failing checks for arithmetic, scheduler, storage and update trust boundaries, then implement. Run `./gradlew testDebugUnitTest lintRelease assembleRelease assembleDebugAndroidTest`. Run instrumentation on API 26 and 36 (CI supports KVM). Inspect real screenshots at compact width, large text, landscape and dark mode. Report physical-device limits honestly.

## Release
Version 1.0.0 / code 1. Never use a debug key for release. Signing lives outside git, passed through AM_KEYSTORE_PATH / AM_STORE_PASSWORD / AM_KEY_ALIAS / AM_KEY_PASSWORD. Preserve the first release certificate for every upgrade. No credentials, keystores, passwords, private backups or device logs in git. Release workflow verifies checksum, certificate and package before activating updates/stable.json. APK installation always requires an explicit tap and Android confirmation.

## Knowledge graph
Graphify is pinned in `tools/requirements-graphify.txt`. Run `bash tools/refresh-graph.sh` after structural changes. Start with `graphify query 'question' --graph graphify-out/graph.json`, `graphify explain 'MemoStore' --graph graphify-out/graph.json`, then verify against source. Graph paths are navigation aids, never evidence of runtime correctness. Do not index secrets, build outputs, vendored skills or unrelated sibling apps.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. When the answer is under a Markdown heading, `graphify section "<heading>"` prints that section's text, so the .md need not be opened. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

Graph node source paths are relative to `app/src/`, the extraction root. Public graph outputs are committed; machine-specific hidden Graphify state and extraction caches are ignored. Refresh before relying on an old graph.

# Durable AI development workflow

Use the project root (`iwatchyou/`) as cwd. The repository also contains unrelated apps.

## Installed skills and precedence

The complete project-local skills are in `.agents/skills/` and committed with the app.
Read `AGENTS.md`, then the applicable skill's `SKILL.md`.

1. User requirements, privacy, Android behavior and existing release safeguards win.
2. Ponytail defaults to **full** (ultra only for unnecessary complexity). Reuse existing
   native components; never remove requested behavior, validation or verification.
3. UI UX Pro Max supplies platform guidance. Query the actual Jetpack Compose stack,
   or the specific UX outcome. Android full-size controls use 48dp touch targets.
4. Impeccable refines hierarchy, wording, states and rendered quality. This is an
   **Operate** surface: preserve the existing light/blue Jakarta identity. Its web
   recommendations do not replace Android APIs or the user's compact bubble brief.
   The corner shortcuts are intentionally compact; the solid 48dp edge tab and
   Settings → Atur ulang bubble timer provide full-size alternatives.
5. Graphify maps code. Query before broad file scans; source remains authoritative.

Ponytail: https://github.com/DietrichGebert/ponytail @ 09c434ab73ca43761c5d8b0c5d341926c2fba710 (MIT).
Impeccable: https://github.com/pbakaus/impeccable @ 7510e233b228864606888d252abacf5e7148bf87 (Apache-2.0).
UI UX Pro Max: installed from the user's existing skill, including its local search
scripts, data and references; do not silently replace that custom version.
Graphify: https://github.com/Graphify-Labs/graphify @ 0a8cd4e1e342235fe53e881fb5d591bf0460071f,
package version 0.9.97 (Apache-2.0/MIT notices upstream).

## Graphify

```sh
uv tool install 'graphifyy==0.9.97'
graphify query 'OverlayController' --budget 1200
graphify explain 'HistoryStore'
graphify affected 'BubbleSize'
# After source changes; local AST only, no API keys or cloud upload:
graphify extract . --code-only --no-label --no-viz
graphify cluster-only . --no-label
```

The graph lives in `graphify-out/graph.json`; its report and HTML are checked in.
`.graphifyignore` excludes downloaded skills, build outputs, binary distribution,
private local settings and generated graph data. Graphify indexes Kotlin/Java and
QA Python/shell. Android XML and some Kotlin constant syntax are not fully modeled;
compiler/lint and direct source reads decide correctness. No user statistics enter
this development graph. Rebuild before relying on line numbers. CLI integration
through `.agents/skills/graphify` and these instructions works without an MCP server.

## Asset-first decisions for 1.4

Retained the existing app, audited foreground policy, SQLite history schema,
Chronometer, Material 3, Jakarta font files, native window manager and AndroidX Core.
AndroidX Core already supplies unused-app restriction status and settings intents:
https://developer.android.com/reference/androidx/core/content/PackageManagerCompat
https://developer.android.com/reference/androidx/core/content/IntentCompat

An external floating-window library would duplicate accessibility window ownership,
introduce another permission and risk focus regressions. Native window flags provide
actual touch pass-through; alpha alone does not. Small chevron/grip vector assets
follow the existing two-stroke native icon style. No runtime dependency was added.

## Verification first

Before release, run the commands in `AGENTS.md`, exercise the device suite and
review screenshots, including failure/permission states. Keep a truthful report in
`docs/VERIFICATION-1.4.md`. A successful APK build alone does not satisfy release.
Never update the stable feed until source/device QA, production signing and source
identity checks pass. Missing signing material must remain a release blocker.

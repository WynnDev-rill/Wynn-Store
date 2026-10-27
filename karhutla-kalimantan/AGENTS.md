# Kalimantan: Di balik asap — instructions for contributors and AI agents

## Scope and runtime
This directory is an isolated Astro static site in the Wynn-Store monorepo. Work here unless the user requests another app. Keep Astro, TypeScript, npm lockfile and Cloudflare Pages deployment conventions. Node 24; run commands from this directory. No backend, keys or paid runtime is required.

## Start here
Read README.md, docs/CINEMATIC-DESIGN.md, docs/CINEMATIC-REFERENCE-RESEARCH.md and docs/CINEMATIC-QA.md for current direction and validation. Earlier WEATHER/SPATIAL/EXPERIENCE documents describe earlier editions, not necessarily current acceptance criteria. Do not delete their provenance.

## Content integrity
- Indonesian semantic HTML must work without canvas or JavaScript. Keep all chapters, sources, citations and download access.
- Historical burned area (2019–2023) and October 2026 hotspot snapshots are different metrics. Never sum them, relabel hotspots as fires/hectares, or replace unreported province values with zero.
- Preserve dates, uncertainty and source notes. This is not live monitoring or a prediction model.
- Decorative 3D, lights and particles are conceptual illustrations, never evidence or actual hotspot coordinates.
- Do not use generated imagery as documentary evidence. Keep image/font/model licensing and source attribution; list new assets and modifications under docs/licenses/.

## Visual and architecture rules
Prioritize cinematic composition, coherent light/material response and bounded camera motion over adding effects. Inspect the supplied reference frames/research before making visual changes. Apply asset-first development: evaluate mature existing libraries, components and licensed assets before custom work. Reuse existing Three.js and local assets; keep runtime requests same-origin. Never import another site's protected artwork/code.
Keep semantic content separate from Three.js renderers. Do not change the main app's dependencies. Use native scrolling, reversible camera paths, fixed mobile framing, and visible keyboard/touch controls. Honor prefers-reduced-motion and the cinematic pause control. Audio must start only on user action. Pause work offscreen/hidden, cap DPR and instance counts, dispose resources, handle failed assets and WebGL context loss. Changes must respect public/_headers CSP without executable inline scripts or third-party runtime requests.

## Required verification
```sh
npm ci
npm test
npm run build
BROWSER_EXECUTABLE_PATH=/usr/bin/chromium npm run test:cinematic
BROWSER_EXECUTABLE_PATH=/usr/bin/chromium npm run test:weather
BROWSER_EXECUTABLE_PATH=/usr/bin/chromium npm run test:browser
BROWSER_EXECUTABLE_PATH=/usr/bin/chromium npm run test:accessibility
```
Use an installed Playwright Chromium if system Chromium is unavailable. Build success alone does not establish visual quality. Open screenshots at desktop and 390px/320px, inspect hero/forest/midpage/footer, compare the reference's composition and motion. Test reverse/rapid scroll, resize, keyboard, source route, manual/automatic weather, reduced motion, pause, no-JS, asset failure and WebGL unavailable/lost/restored. Preserve meaningful data regressions. Store transient output under ignored test-results/, keep concise release evidence and selected screenshots in docs/. Report browser emulation and any untested physical-device performance honestly.

## Delivery
Use a feature branch; document what changed, commands/results and known limits. Do not claim a deployed site unless deployment was actually observed. Keep unrelated monorepo projects untouched.

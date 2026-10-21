# Lunabloom Ready Board

Minimalis, mobile-first, real-time Mobile Legends planning board. It lives independently at `Wynn-Store/lunabloom-ready-board/`, without modifying existing projects.

## Features
- One page and one-click toggles; **maximum 5 half-hour slots per member**.
- 24-hour service day 05:00–04:30 **Asia/Jakarta**; server-enforced rollover every morning at 05:00.
- At most **35 anonymous participants per day**. Display 5 green dots at most, then `+N lainnya`.
- Adaptive 2/3/4/6 column grid; five time-of-day filters; 12-hour friendly labels below 24-hour time.
- Realtime counts from Supabase aggregate table, poll-on-refocus, anonymous device tokens, transactional anti-duplication.
- No profile, email/password form, chat, or yellow 'maybe' markers.

## Run locally
`python3 -m http.server 8000` then open `http://localhost:8000/lunabloom-ready-board/` if launched at the monorepo root, or `http://localhost:8000/` if launched here. No npm dependencies needed for the site itself.

## Backend provisioning
1. Create a **dedicated Supabase project** (do not attach to unrelated existing production apps).
2. Run `supabase/schema.sql` once using Supabase SQL editor / migration. The DB stores random visitor UUIDs but only publishes time-slot aggregates.
3. No Auth provider configuration is necessary. The app creates a random browser-local UUID. This is not proof of a unique human: people can still use another device/browser to evade casual limits.
4. Fill `config.js` with **project URL and publishable key** from Supabase. These are public browser values, not the service-role key.
5. Host folder as static site: build command empty, output directory `.`. Vercel root directory `lunabloom-ready-board`, no framework, no build command.
6. Test with two separate browsers or devices; check counts and slot changes appear in both.

**Security**: SQL functions enforce 5 slots, 35 distinct participants per day, and advisory locks to prevent simultaneous overbooking. Direct selection records have RLS enabled and no client policies. Only aggregate counts are included in the Realtime publication. A malicious user can create a new browser-local UUID on a different browser/device. Anonymous boards cannot reliably guarantee unique humans without login or a group-issued identifier.

**Reset**: Uses logical service-day filtering based on database time in Asia/Jakarta, not browser local clocks. Old data is deleted after seven days when writes occur.

## What happens before the backend is configured?
The site displays an honest **Mode pratinjau** banner. Clicks are local-only and demo sample counts are shown for visual evaluation. It is *not* a live group board until `config.js` is populated and Supabase tables/auth are provisioned.

## QA
`node --test tests/core.test.mjs` and test manually with 375px and 1440px viewports, 2 devices, 05:00 WIB reset edge case.

## UI/UX Pro Max
Project design rules: `design-system/MASTER.md`. To install the latest official **UI UX Pro Max** Codex project skill in this folder (when npm is available):

```
cd lunabloom-ready-board
npx --yes ui-ux-pro-max-cli init --ai codex
```

Official project: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill. The skill is a *design-time agent resource*; it is not a runtime website dependency.

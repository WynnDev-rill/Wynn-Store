# Data contracts and provenance

Research and live endpoint verification began 2026-09-05. Public endpoint availability is not a service-level guarantee. No account/private API or credential extraction is used.

| Content | Primary | Fallback / limits |
|---|---|---|
| Hero identity, artwork, Indonesian skills, roles/lanes and guide relations | Public MLBB website GMS source 2756564 | Documented Rone Arena hero endpoints, then last-good snapshot; missing fields stay missing |
| Win/pick/ban, opponent and teammate pairs | MLBB public GMS 2756569, 7-day window | Rone Arena rank endpoint for rates; incomplete pair fields are omitted; preserved old snapshot retains timestamps |
| Item identities and Indonesian metadata | Moonton Academy through public Rone Arena | Current public MLBBHub item catalog supplements facts, recipes/cost and effects, citing Liquipedia; old metadata has its original date |
| Emblems, talents, spells, rank logos | Moonton Academy through Rone Arena | Previous successful response or normalized snapshot |
| Ranked builds | Rone Arena Academy hero builds, rank Mythic, first listed lane | Three actual core combinations per hero where available; only three core items per combination; no invented six-item order |
| Current season reset | MLBBHub visible current-season statement | Last successfully parsed date only; an expired date disables daily calculations |
| Patch label | Current community catalog label | No promotion of Academy's stale version list to a current patch |

Primary references:

- https://www.mobilelegends.com/
- https://www.mobilelegends.com/rank
- https://arena.rone.dev/web/heroes
- https://arena.rone.dev/web/academy
- https://github.com/ridwaanhall/rone-arena-api
- https://github.com/ridwaanhall/rone-arena-api/blob/main/HOSTED_API_TERMS.md
- https://mlbbhub.com/items
- https://mlbbhub.com/server-time/season-schedule

## Official public website interface

The public website configuration and client scripts identify the GMS app source IDs. POST `https://api.gms.moontontech.com/api/gms/source/2669606/{source}` uses page size 500. Rank filters: `all=101`, `epic=5`, `legend=6`, `mythic=7`, `honor=8`, `glory=9`; match type 0 opponents, 1 teammates. The app labels the highest published bucket Glory+ rather than inventing an Immortal-only bucket. Filters by lane/role select heroes; source rates remain aggregate hero rates for that rank.

Raw rates are fractions converted to percentages exactly once. In the opponent table, nested `hero_win_rate` belongs to the **opponent**; positive `increase_win_rate` is relative to that opponent's own baseline. Counter cards show the opponent's win rate. Favorable-matchup cards show its complement. This is whole-match context, not a lane duel. Teammate rows describe pair outcomes. Neither source publishes per-pair sample sizes; no confidence interval or sample threshold is fabricated.

## Normalization and failures

Raw responses are cached by endpoint with the original successful check timestamp. Invalid success envelopes, truncated pages, missing identities, duplicate IDs, rates outside 0–100, non-HTTPS artwork, retired blank item identities and dangling build references fail validation. A hero catalog shrinking over 5% is rejected. Cache never stores HTTP error bodies as a successful envelope.

The public MLBBHub page embeds JSON data in Next RSC string chunks; these are decoded as JSON, never evaluated as code. Season parsing uses visible text only, requires an explicit current-season assertion, rejects ambiguous dates and implausibly distant resets, and ignores predictions hidden in scripts. The next season is never extrapolated by a fixed cycle.

Snapshot assembly time (`generatedAt`), source revision (`updatedAt`), and successful fetch time (`checkedAt`) are different. A recent assembly time does not make every embedded field fresh. API metadata is not guaranteed to equal gameplay patch publication time. Image availability is separately dependent on source CDNs.

The Android repository validates and atomically persists a last-good snapshot, uses a bundled real snapshot for first-run/offline startup, and falls back between GitHub Raw and jsDelivr. Remote configuration is restricted to the project's HTTPS public repository/CDN paths. Background refresh uses WorkManager with network constraints. No remote code is executed.

## Recommendation limits

Draft scoring is transparent local ordering: bounded matchup deltas, pair synergies, lane coverage and general meta strength. It is not a trained predictor or team win probability. Multi-lane heroes use a small backtracking assignment to avoid an early flexible pick blocking coverage. Selected and banned heroes cannot reappear.

Item counter categories derive from the current effect text and attributes in English/Indonesian, not an immutable item list. This is mechanical guidance, not measured effectiveness against every hero. Users still inspect the effect and match their role.

The Road formula counts remaining net stars and divides by remaining playable local calendar dates, rounding up. Today and a partial reset day count; the exact reset instant is exclusive. Before Mythic it uses basic division stars, excluding unpredictable placement, protection and bonus stars. These assumptions are shown under data sources and on pre-Mythic selection.

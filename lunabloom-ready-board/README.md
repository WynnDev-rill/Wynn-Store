# Lunabloom Ready Board

A one-page, anonymous Mobile Legends availability board. Deploys independently from the existing Wynn-Store apps.

## Live deployment
- GitHub repository: Wynn-Store, folder: lunabloom-ready-board/
- Vercel project: lunabloom-ready-board (root directory: lunabloom-ready-board)
- **Storage**: a dedicated Vercel Private Blob store, attached to this Vercel project. No Supabase required.
- Backend: same-origin Node serverless functions in api/, never expose Vercel storage credentials to the browser.
- Refresh: the browser polls every 120 seconds only while visible; the aggregate route uses a max-40-second Vercel CDN cache and up-to-20-second warm function cache. Your own selection changes the on-screen count immediately, and the saving status stays visible until server confirmation.

## Rules
- 48 half-hour choices beginning at 05:00 WIB and ending at 04:30 the following morning.
- Maximum 5 selected slots per anonymous browser.
- Up to 35 participating anonymous browsers per service day.
- A user may replace or remove previously chosen slots.
- Automatically resets logically at 05:00 WIB; old member data is replaced on the next update.
- Green only: max 5 dots per time card, with a full numeric total (0–35) underneath. No redundant friendly time label or “Semua” filter.
- Participants identified by a secure, HttpOnly, SameSite cookie. We store a SHA-256 hash of the random UUID in a private Blob. **Without accounts, people can circumvent per-person limits by clearing cookies or switching browsers.**

## Availability and limits
Vercel Hobby includes limited Blob operations (2,000 advanced writes and 10,000 simple reads per month, subject to plan changes). Client saves are debounced for 2.3 seconds to reduce writes; aggregate GET responses are globally cached to reduce read costs. Service becomes temporarily unavailable if its free quota is exhausted.

The state is stored in a single private JSON blob with ETag conditional writes and retries to protect against concurrent overwrites. Private reads set useCache:false to obtain strong read-after-write consistency on Vercel Blob >=2.6.1. The server returns only aggregated counts and the current browser's chosen slots, never other browsers' identities.

## Run tests
node --test tests/*.test.mjs

## Local development
npm install
vercel dev

Vercel Blob requires the project's private BLOB_READ_WRITE_TOKEN environment variable for local use. Production gets this from the linked Vercel storage integration.

## Architecture
GET /api/board - shared aggregate only, CDN cache ~60 seconds
GET /api/me - browser's slots using server-managed anonymous cookie; uncached
POST /api/slots - atomic whole-selection replacement, max 5 slots, max 35 contributors, ETag concurrency checks

## UI/UX Pro Max
The official Codex design skill is installed under .agents/skills, with project rules in design-system/MASTER.md.

## Legacy
The supabase/schema.sql migration and previous demo fallback are retained for reference but are not required for the new Vercel Blob live configuration.

## QA and operational notes
- Every visible selection has an optimistic count; status is pending until persisted, and failures roll back with a warning.
- Board readings are served through a short shared public CDN cache and a small in-memory per-function cache. This reduces (not eliminates) Blob read costs. Actual monthly usage depends on visitors and CDN hit rate.
- Pending selections include their service day; after 05:00 WIB, stale writes are rejected rather than counted toward a new day.
- The default view shows all five time categories; tapping a category filters it, tapping it again restores all.
- Data store migration to Cloudflare D1 should be done only after validating Cloudflare permissions and data migration, not by changing the production site blindly.

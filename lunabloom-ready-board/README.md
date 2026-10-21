# Lunabloom Ready Board

A one-page, anonymous Mobile Legends availability board. Deploys independently from the existing Wynn-Store apps.

## Live deployment
- GitHub repository: Wynn-Store, folder: lunabloom-ready-board/
- Vercel project: lunabloom-ready-board (root directory: lunabloom-ready-board)
- **Storage**: a dedicated Vercel Private Blob store, attached to this Vercel project. No Supabase required.
- Backend: same-origin Node serverless functions in api/, never expose Vercel storage credentials to the browser.
- Refresh: shared board is CDN cached up to ~60–75 seconds; personal slot changes appear immediately after the server confirms the save.

## Rules
- 48 half-hour choices beginning at 05:00 WIB and ending at 04:30 the following morning.
- Maximum 5 selected slots per anonymous browser.
- Up to 35 participating anonymous browsers per service day.
- A user may replace or remove previously chosen slots.
- Automatically resets logically at 05:00 WIB; old member data is replaced on the next update.
- Green only: max 5 dots per time card, then +N under the dots.
- Participants identified by a secure, HttpOnly, SameSite cookie. We store a SHA-256 hash of the random UUID in a private Blob. **Without accounts, people can circumvent per-person limits by clearing cookies or switching browsers.**

## Availability and limits
Vercel Hobby includes limited Blob operations (2,000 advanced writes and 10,000 simple reads per month, subject to plan changes). Client saves are debounced to reduce writes; aggregate GET responses are globally cached to reduce read costs. Service becomes temporarily unavailable if its free quota is exhausted.

The state is stored in a single private JSON blob with ETag conditional writes and retries to protect against concurrent overwrites. The server returns only aggregated counts and the current browser's chosen slots, never other browsers' identities.

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

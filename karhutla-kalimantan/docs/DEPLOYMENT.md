# Deployment

Project: karhutla-kalimantan, Cloudflare Pages Git integration.
Repository: WynnDev-rill/Wynn-Store, production main.
Root: karhutla-kalimantan. Build: npm test && npm run build. Output: dist. Node:24.
Only karhutla-kalimantan/* triggers this project's production builds. Other Pages projects have not been reconfigured.

The first build was successful. A production smoke test then found the map's small script was inlined by Astro and blocked by CSP. The final revision emits external scripts, and the browser regression server now reproduces production CSP. No unsafe-inline script exception was added.

The project requires no server, API token in frontend, paid service, third-party tracker or visitor account. Source updates are deployed automatically from main. Credentials remain in the connected services, not the repository.

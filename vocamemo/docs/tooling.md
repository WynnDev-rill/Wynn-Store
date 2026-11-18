# Permanent development tooling

- Ponytail: https://github.com/DietrichGebert/ponytail, commit `96608e078b2d5ea607c5c3b8b5a54d1c1fba6846`. Locally installed and vendored skill instructions where applicable.
- UI UX Pro Max: https://github.com/nextlevelbuilder/ui-ux-pro-max-skill, commit `e59ff376a41f7ca47b7541ad5c116495cbade3ee`. Locally installed and vendored skill instructions where applicable.
- Impeccable: https://github.com/pbakaus/impeccable, commit `d41d9860b0094dcb68030657da63461212835aaa`. Locally installed and vendored skill instructions where applicable.
- Graphify: https://github.com/Graphify-Labs/graphify, commit `340268a70a83588d6b3c6de1fffb05326350ef1d`. Locally installed and vendored skill instructions where applicable.

Ponytail defaults to Full; Ultra is available but does not remove explicitly requested features. Skill priority: user brief → repository instructions → native Android conventions → Ponytail code economy + UI UX Pro Max interaction/accessibility + Impeccable visual craft. No third-party skill can authorize unrelated repository or account operations.

Graphify 0.9.112 installed in an isolated uv environment, Codex skill installed globally and in .codex/skills/graphify. Run `uv tool install graphifyy==0.9.112`, `graphify install --project --platform codex`, then `scripts/refresh-graph.sh`. Local AST extraction needs no AI API or cloud upload. Query: `graphify query "How does grading persist review and schedule?"`. Generated graph is committed after final source verification.

Impeccable shipped role prompts are vendored under .agents/roles for later independent finish reviews and documentation. Plus Jakarta Sans and Literata static weights are reproduced from checksummed Google Fonts sources with fontTools 4.61.1 via scripts/prepare-fonts.py; this is a development-only dependency.

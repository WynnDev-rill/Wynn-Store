#!/usr/bin/env bash
set -euo pipefail
version=$(python3 -c 'import re,pathlib; print(re.search(r"versionName = \"([0-9.]+)\"",pathlib.Path("iwatchyou/app/build.gradle.kts").read_text()).group(1))')
# A signed immutable version is only published after its production smoke test.
# When retrying the following upgrade test, preserve that completed verification.
if gh release view "iwatchyou-v$version" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
 python3 iwatchyou/scripts/publish.py
else
 bash iwatchyou/qa/run-production.sh
 python3 iwatchyou/scripts/publish.py
fi
python3 iwatchyou/qa/upgrade_driver.py

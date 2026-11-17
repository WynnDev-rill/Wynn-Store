#!/usr/bin/env bash
set -euo pipefail
export PATH="$HOME/.local/bin:$PATH"
cd "$(dirname "$0")/.."
graphify extract app/src --code-only --out . --exclude '**/res/**'
graphify cluster-only .
graphify query 'MemoStore record StudySession Scheduler' --graph graphify-out/graph.json --budget 700

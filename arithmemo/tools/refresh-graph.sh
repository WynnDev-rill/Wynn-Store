#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
graphify extract app/src --code-only --out . --exclude '**/res/**'
graphify cluster-only .
graphify query 'MemoStore record StudySession Scheduler' --graph graphify-out/graph.json --budget 700

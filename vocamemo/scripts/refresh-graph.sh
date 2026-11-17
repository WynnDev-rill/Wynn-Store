#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
graphify update . --no-cluster
graphify cluster-only . --no-label

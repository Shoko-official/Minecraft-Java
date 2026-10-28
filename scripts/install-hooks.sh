#!/usr/bin/env bash
# Points git at the versioned hooks in .githooks.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
chmod +x .githooks/* scripts/*.sh
git config core.hooksPath .githooks
echo "hooks installed: core.hooksPath=.githooks"

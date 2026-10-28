#!/usr/bin/env bash
# Creates a throwaway worktree for an agent, so that its view of the repository is limited
# by construction instead of by instructions.
#
# Usage: scripts/agent-worktree.sh DIR [REF] [--docs-only]
#
#   REF          commit or branch to check out detached (default HEAD). A blind test author
#                gets the commit that holds only the API skeleton, before any implementation.
#   --docs-only  sparse checkout limited to docs/ (spec authors never see source code).
#
# Remove with: git worktree remove --force DIR
set -euo pipefail

dir="${1:?target directory required}"
ref="${2:-HEAD}"
docs_only="${3:-}"

[ "$ref" = "--docs-only" ] && { ref=HEAD; docs_only="--docs-only"; }

cd "$(git rev-parse --show-toplevel)"
git worktree add --detach "$dir" "$ref"

if [ "$docs_only" = "--docs-only" ]; then
  # Git Bash on Windows would rewrite the leading slashes as filesystem paths.
  (cd "$dir" && MSYS_NO_PATHCONV=1 git sparse-checkout set --no-cone '/docs/' '/AGENTS.md')
fi
echo "worktree ready: $dir ($ref${docs_only:+, docs only})"

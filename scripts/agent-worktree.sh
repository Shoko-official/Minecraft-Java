#!/usr/bin/env bash
# Creates a throwaway detached worktree for an agent that needs a working copy at a precise commit.
#
# Usage: scripts/agent-worktree.sh DIR [REF]
#
# A worktree shares the object database of the main repository, so it reduces accidental
# exposure (the agent starts from the right files) but is not isolation: later commits and
# other branches stay reachable with git commands. The case where this matters is the blind
# test author, who runs before the implementation of the unit under test exists at all.
# For a view without history, use scripts/agent-export.sh.
#
# Remove with: git worktree remove --force DIR
set -euo pipefail

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || { echo "usage: agent-worktree.sh DIR [REF]" >&2; exit 2; }
dir="$1"
ref="${2:-HEAD}"

cd "$(git rev-parse --show-toplevel)"
git worktree add --detach "$dir" "$ref"
echo "worktree ready: $dir ($ref)"

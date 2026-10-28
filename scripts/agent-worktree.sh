#!/usr/bin/env bash
# Creates a throwaway detached worktree for an agent that needs a working copy at a precise commit.
#
# Usage: scripts/agent-worktree.sh DIR [REF]
#
# DIR is resolved from the current directory, must not exist yet (or be empty) and must be
# outside the repository. A worktree shares the object database of the main repository, so it
# reduces accidental exposure (the agent starts from the right files) but is not isolation: later
# commits and other branches stay reachable with git commands. The case where this matters is the
# blind test author, who runs before the implementation of the unit under test exists at all.
# For a view without history, use scripts/agent-export.sh.
#
# Remove with: git worktree remove --force DIR
set -euo pipefail

[ "$#" -ge 1 ] && [ "$#" -le 2 ] || { echo "usage: agent-worktree.sh DIR [REF]" >&2; exit 2; }
ref="${2:-HEAD}"

parent="$(cd "$(dirname "$1")" 2>/dev/null && pwd -P)" || { echo "parent directory of $1 must exist" >&2; exit 2; }
dir="$parent/$(basename "$1")"
root="$(cd "$(git rev-parse --show-toplevel)" && pwd -P)"

case "$dir/" in
  "$root"/*) echo "refusing a target inside the repository: $dir" >&2; exit 2 ;;
esac
if [ -e "$dir" ] && [ -n "$(ls -A "$dir")" ]; then
  echo "refusing a non-empty target: $dir" >&2
  exit 2
fi

git rev-parse --verify --quiet "$ref^{commit}" >/dev/null || { echo "unknown revision: $ref" >&2; exit 2; }
git worktree add --detach "$dir" "$ref"
echo "worktree ready: $dir ($ref)"

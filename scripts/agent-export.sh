#!/usr/bin/env bash
# Exports a snapshot of the repository to a plain directory, with no .git and no history.
# This is the actual isolation primitive: what is not exported cannot be read.
#
# Usage: scripts/agent-export.sh DIR [REF] [--docs-only] [--with-reference]
#
#   DIR               resolved from the current directory; must not exist yet (or be empty)
#                     and must be outside any git work tree
#   REF               commit or branch to export (default HEAD)
#   --docs-only       export only docs/ and AGENTS.md (spec authors never see source code)
#   --with-reference  also copy the local research reports from docs/reference/ (ignored by git)
#
# The exported directory is not a repository: the orchestrator copies the agent's deliverables
# back into the task branch. Local reference data in .cache/ is read through its absolute path,
# given in the brief.
if (( BASH_VERSINFO[0] < 4 || (BASH_VERSINFO[0] == 4 && BASH_VERSINFO[1] < 4) )); then
  echo "bash 4.4 or newer is required (found ${BASH_VERSION})" >&2
  exit 2
fi
set -euo pipefail

target=""; ref=HEAD; docs_only=0; with_reference=0; positional=0
for arg in "$@"; do
  case "$arg" in
    --docs-only) docs_only=1 ;;
    --with-reference) with_reference=1 ;;
    --*) echo "unknown option: $arg" >&2; exit 2 ;;
    *) positional=$((positional + 1))
       if [ "$positional" -eq 1 ]; then target="$arg"
       elif [ "$positional" -eq 2 ]; then ref="$arg"
       else echo "too many arguments" >&2; exit 2; fi ;;
  esac
done
[ -n "$target" ] || { echo "usage: agent-export.sh DIR [REF] [--docs-only] [--with-reference]" >&2; exit 2; }

parent="$(cd "$(dirname "$target")" 2>/dev/null && pwd -P)" || { echo "parent directory of $target must exist" >&2; exit 2; }
dir="$parent/$(basename "$target")"
root="$(git rev-parse --show-toplevel)"

# Ask git rather than comparing path strings, which differ by case and by mount point on Windows.
# A target inside any work tree (this one, another worktree, another repository) would expose a
# history through git commands.
if git -C "$parent" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "refusing a target inside a git work tree: $dir" >&2
  exit 2
fi
if [ -e "$dir" ] && [ -n "$(ls -A "$dir")" ]; then
  echo "refusing a non-empty target: $dir" >&2
  exit 2
fi

git rev-parse --verify --quiet "$ref^{commit}" >/dev/null || { echo "unknown revision: $ref" >&2; exit 2; }

mkdir -p "$dir"
paths=()
[ "$docs_only" -eq 1 ] && paths=(docs AGENTS.md)
git -C "$root" archive "$ref" -- "${paths[@]}" | tar -x -C "$dir"

# The export must be a plain directory: a repository here would defeat its purpose.
if git -C "$dir" rev-parse --git-dir >/dev/null 2>&1; then
  echo "export unexpectedly lies inside a repository: $dir" >&2
  exit 1
fi

if [ "$with_reference" -eq 1 ] && [ -d "$root/docs/reference" ]; then
  mkdir -p "$dir/docs/reference"
  cp -R "$root/docs/reference/." "$dir/docs/reference/"
fi

label="$ref"
[ "$docs_only" -eq 1 ] && label="$label, docs only"
[ "$with_reference" -eq 1 ] && label="$label, with reference"
echo "export ready: $dir ($label)"

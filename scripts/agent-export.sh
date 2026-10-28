#!/usr/bin/env bash
# Exports a snapshot of the repository to a plain directory, with no .git and no history.
# This is the actual isolation primitive: what is not exported cannot be read.
#
# Usage: scripts/agent-export.sh DIR [REF] [--docs-only] [--with-reference]
#
#   REF               commit or branch to export (default HEAD)
#   --docs-only       export only docs/ and AGENTS.md (spec authors never see source code)
#   --with-reference  also copy the local research reports from docs/reference/ (ignored by git)
#
# The exported directory is not a repository: the orchestrator copies the agent's deliverables
# back into the task branch. Local reference data in .cache/ is read through its absolute path,
# given in the brief.
set -euo pipefail

dir=""; ref=HEAD; docs_only=0; with_reference=0; positional=0
for arg in "$@"; do
  case "$arg" in
    --docs-only) docs_only=1 ;;
    --with-reference) with_reference=1 ;;
    --*) echo "unknown option: $arg" >&2; exit 2 ;;
    *) positional=$((positional + 1))
       if [ "$positional" -eq 1 ]; then dir="$arg"
       elif [ "$positional" -eq 2 ]; then ref="$arg"
       else echo "too many arguments" >&2; exit 2; fi ;;
  esac
done
[ -n "$dir" ] || { echo "usage: agent-export.sh DIR [REF] [--docs-only] [--with-reference]" >&2; exit 2; }

root="$(git rev-parse --show-toplevel)"
cd "$root"
mkdir -p "$dir"

paths=()
[ "$docs_only" -eq 1 ] && paths=(docs AGENTS.md)
git archive "$ref" -- "${paths[@]}" | tar -x -C "$dir"

if [ "$with_reference" -eq 1 ] && [ -d docs/reference ]; then
  mkdir -p "$dir/docs/reference"
  cp -R docs/reference/. "$dir/docs/reference/"
fi
label="$ref"
[ "$docs_only" -eq 1 ] && label="$label, docs only"
[ "$with_reference" -eq 1 ] && label="$label, with reference"
echo "export ready: $dir ($label)"

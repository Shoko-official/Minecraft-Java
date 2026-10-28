#!/usr/bin/env bash
# Repository hygiene checks, shared by the pre-commit hook, the commit-msg hook and CI.
#
# Usage:
#   scripts/check-hygiene.sh                 scan tracked files (working tree)
#   scripts/check-hygiene.sh --staged        scan the index instead of the working tree (pre-commit)
#   scripts/check-hygiene.sh --message FILE  scan a git commit message
#   scripts/check-hygiene.sh --text FILE     scan raw text (PR title and body, commit log)
#
# Exceptions live in scripts/hygiene-exceptions.txt, one "check:path" per line
# (check is "size" or "path"), followed by "# reason".
#
# The path deny-list is deliberately partial: it catches the usual ways reference game data
# enters a repository, it does not prove that none did.
set -euo pipefail
# The size loop runs last in a pipeline and must update $fail in the current shell.
shopt -s lastpipe

# Perl regexes: tool configuration paths (.claude/, CLAUDE.md) are not attribution.
ATTRIBUTION='co-authored-by|generated (with|by) (claude|ai)|ai[- ]assisted|(?<![.\w])claude(?![\w]|\.md)|\banthropic\b'
# Same characters, as raw bytes for grep and as code points for git grep.
DASHES='\xe2\x80[\x93\x94]'
DASHES_GIT='\x{2013}|\x{2014}'
JAVA_TARGET=400
JAVA_MAX=600
EXCEPTIONS=scripts/hygiene-exceptions.txt

# Files that legitimately contain the patterns above.
SELF_EXCLUDED=':(exclude)scripts/check-hygiene.sh'

fail=0
report() { echo "hygiene: $1" >&2; fail=1; }

scan_stream() { # $1 label, stdin: text
  local text
  text="$(cat)"
  if LC_ALL=C grep -iqP -- "$ATTRIBUTION" <<<"$text"; then
    report "forbidden attribution string in $1"
  fi
  if LC_ALL=C grep -qP -- "$DASHES" <<<"$text"; then
    report "en dash or em dash in $1 (use '-')"
  fi
}

mode="${1:-tree}"
case "$mode" in
  --message)
    file="${2:?message file required}"
    # Git keeps '#' lines of messages given with -m or -F, so only the verbose-diff part is cut.
    tr -d '\r' < "$file" | sed '/^# -* >8 -*$/,$d' | scan_stream "commit message"
    exit "$fail"
    ;;
  --text)
    file="${2:?text file required}"
    tr -d '\r' < "$file" | scan_stream "$file"
    exit "$fail"
    ;;
  --staged) grep_flags=(--cached) ;;
  tree) grep_flags=() ;;
  *) echo "usage: check-hygiene.sh [--staged | --message FILE | --text FILE]" >&2; exit 2 ;;
esac

cd "$(git rev-parse --show-toplevel)"

excepted() {
  [ -f "$EXCEPTIONS" ] && sed 's/[[:space:]]*#.*$//' "$EXCEPTIONS" | grep -qxF -- "$1:$2"
}

if [ "$mode" = "--staged" ]; then
  list_files() { git ls-files --cached -z; }
else
  list_files() { git ls-files -z; }
fi

shopt -s nocasematch
while IFS= read -r -d '' f; do
  excepted path "$f" && continue
  case "$f" in
    .cache/*|run/*|reports/*) report "ignored directory content is tracked: $f" ;;
    docs/reference/README.md|gradle/wrapper/gradle-wrapper.jar) ;;
    docs/reference/*) report "local research report must not be versioned: $f" ;;
    assets/minecraft/*|data/minecraft/*|*/assets/minecraft/*|*/data/minecraft/*)
      report "reference game data must not be versioned: $f" ;;
    *.jar|*.mca|*.mcr|*.ogg|*.nbt|*.dat|*.zip|*.mcmeta) report "reference binary must not be versioned: $f" ;;
    docs/images/*|*/src/test/resources/*) ;;
    *.png) report "image outside docs/images and test resources: $f" ;;
  esac
done < <(list_files)
shopt -u nocasematch

# git grep skips binary files (-I) and reads the index with --cached.
hits() { # $1 label, remaining args: git grep options
  local label="$1"; shift
  local out
  out="$(LC_ALL=C git grep "${grep_flags[@]}" -I -l "$@" -- . "$SELF_EXCLUDED" || true)"
  [ -z "$out" ] || while IFS= read -r p; do report "$label in $p"; done <<<"$out"
}
hits "forbidden attribution string" -i -P "$ATTRIBUTION"
hits "en dash or em dash (use '-')" -P "$DASHES_GIT"

LC_ALL=C git grep "${grep_flags[@]}" -I -c '' -- '*.java' | while IFS=: read -r path lines; do
  if [ "$lines" -gt "$JAVA_MAX" ] && ! excepted size "$path"; then
    report "$path has $lines lines (max $JAVA_MAX)"
  elif [ "$lines" -gt "$JAVA_TARGET" ]; then
    echo "hygiene: warning: $path has $lines lines (target $JAVA_TARGET)" >&2
  fi
done || true

exit "$fail"

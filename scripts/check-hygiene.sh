#!/usr/bin/env bash
# Repository hygiene checks, shared by the pre-commit hook, the commit-msg hook and CI.
#
# Usage:
#   scripts/check-hygiene.sh                 scan tracked files (working tree)
#   scripts/check-hygiene.sh --staged        scan the staged content only (pre-commit)
#   scripts/check-hygiene.sh --message FILE  scan a git commit message
#   scripts/check-hygiene.sh --text FILE     scan raw text (PR title and body, commit log)
#
# Exceptions live in scripts/hygiene-exceptions.txt, one "check:path" per line
# (check is "size" or "path"), followed by "# reason".
set -euo pipefail
# scan_text runs last in pipelines and must update $fail in the current shell.
shopt -s lastpipe

ATTRIBUTION='co-authored-by|generated (with|by) (claude|ai)|ai[- ]assisted|claude|anthropic'
# En dash and em dash, as raw UTF-8 bytes so the check works under any locale.
DASHES=$'\xe2\x80\x93|\xe2\x80\x94'
JAVA_TARGET=400
JAVA_MAX=600
EXCEPTIONS=scripts/hygiene-exceptions.txt

# Files that legitimately contain the patterns above.
SELF_EXCLUDED='^scripts/check-hygiene\.sh$'

fail=0
report() { echo "hygiene: $1" >&2; fail=1; }

# Tool configuration paths are not attribution: documenting them must stay possible.
neutralize() { sed -E 's#\.claude/#X/#g; s#CLAUDE\.md#X#g'; }

# scan_text LABEL < text
scan_text() {
  local text
  text="$(neutralize)"
  if LC_ALL=C grep -iqE -- "$ATTRIBUTION" <<<"$text"; then
    report "forbidden attribution string in $1"
  fi
  if LC_ALL=C grep -qE -- "$DASHES" <<<"$text"; then
    report "en dash or em dash in $1 (use '-')"
  fi
}

mode="${1:-tree}"
case "$mode" in
  --message)
    file="${2:?message file required}"
    comment_char="$(git config --get core.commentChar 2>/dev/null || true)"
    case "$comment_char" in "" | auto) comment_char='#' ;; esac
    tr -d '\r' < "$file" \
      | sed '/^# -* >8 -*$/,$d' \
      | awk -v c="$comment_char" 'index($0, c) != 1' \
      | scan_text "commit message"
    exit "$fail"
    ;;
  --text)
    file="${2:?text file required}"
    tr -d '\r' < "$file" | scan_text "$file"
    exit "$fail"
    ;;
  --staged) mode=staged ;;
  tree) ;;
  *) echo "usage: check-hygiene.sh [--staged | --message FILE | --text FILE]" >&2; exit 2 ;;
esac

cd "$(git rev-parse --show-toplevel)"

is_excepted() {
  [ -f "$EXCEPTIONS" ] && sed 's/[[:space:]]*#.*$//' "$EXCEPTIONS" | grep -qxF -- "$1:$2"
}

if [ "$mode" = staged ]; then
  list_files() { git diff --cached --name-only --diff-filter=ACMR -z; }
  read_file() { git show ":$1"; }
  is_text() { [ "$(git diff --cached --numstat -- "$1" | cut -f1)" != "-" ]; }
else
  list_files() { git ls-files -z; }
  read_file() { cat -- "$1"; }
  is_text() { [ -f "$1" ] && grep -qI . -- "$1" 2>/dev/null; }
fi

while IFS= read -r -d '' f; do
  if ! is_excepted path "$f"; then
    case "$f" in
      .cache/*|run/*|reports/*) report "ignored directory content is tracked: $f" ;;
      docs/reference/README.md|gradle/wrapper/gradle-wrapper.jar) ;;
      docs/reference/*) report "local research report must not be versioned: $f" ;;
      assets/minecraft/*|data/minecraft/*|*/assets/minecraft/*|*/data/minecraft/*)
        report "reference game data must not be versioned: $f" ;;
      *.jar|*.mca|*.ogg|*.nbt) report "reference binary must not be versioned: $f" ;;
      docs/*|*/src/test/resources/*) ;;
      *.png) report "image outside docs/ and test resources: $f" ;;
    esac
  fi

  is_text "$f" || continue

  if ! [[ "$f" =~ $SELF_EXCLUDED ]]; then
    read_file "$f" | scan_text "$f"
  fi

  case "$f" in
    *.java)
      lines=$(read_file "$f" | wc -l)
      if [ "$lines" -gt "$JAVA_MAX" ] && ! is_excepted size "$f"; then
        report "$f has $lines lines (max $JAVA_MAX)"
      elif [ "$lines" -gt "$JAVA_TARGET" ]; then
        echo "hygiene: warning: $f has $lines lines (target $JAVA_TARGET)" >&2
      fi
      ;;
  esac
done < <(list_files)

exit "$fail"

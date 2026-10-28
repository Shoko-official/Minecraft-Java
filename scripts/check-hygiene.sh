#!/usr/bin/env bash
# Repository hygiene checks, shared by the pre-commit hook, the commit-msg hook and CI.
#
# Usage:
#   scripts/check-hygiene.sh                 scan the working tree
#   scripts/check-hygiene.sh --message FILE  scan a commit message for attribution only
set -euo pipefail

ATTRIBUTION='co-authored-by|generated (with|by) (claude|ai)|ai[- ]assisted|claude|anthropic'
# En dash and em dash, as raw UTF-8 bytes so the check works under any locale.
DASHES=$'\xe2\x80\x93|\xe2\x80\x94'
JAVA_TARGET=400
JAVA_MAX=600

# Files that legitimately contain the patterns above.
SELF_EXCLUDED='^(scripts/check-hygiene\.sh|\.githooks/commit-msg)$'

fail=0
report() { echo "hygiene: $1" >&2; fail=1; }

if [ "${1:-}" = "--message" ]; then
  msg_file="${2:?message file required}"
  if tr -d '\r' < "$msg_file" | grep -v '^#' | LC_ALL=C grep -iqE -- "$ATTRIBUTION"; then
    report "commit message contains automatic attribution"
  fi
  exit "$fail"
fi

cd "$(git rev-parse --show-toplevel)"

mapfile -t files < <(git ls-files --cached --others --exclude-standard | while read -r f; do [ -f "$f" ] && echo "$f"; done)

for f in "${files[@]}"; do
  case "$f" in
    .cache/*|run/*|reports/*) report "ignored directory content is tracked: $f" ;;
    docs/reference/README.md) ;;
    docs/reference/*) report "local research report must not be versioned: $f" ;;
    *.jar|*.mca) report "reference binary must not be versioned: $f" ;;
  esac
done

for f in "${files[@]}"; do
  if [[ "$f" =~ $SELF_EXCLUDED ]]; then continue; fi
  grep -qI . "$f" 2>/dev/null || continue
  if LC_ALL=C grep -iqE -- "$ATTRIBUTION" "$f"; then
    report "forbidden attribution string in $f"
  fi
  if LC_ALL=C grep -qE -- "$DASHES" "$f"; then
    report "en dash or em dash in $f (use '-')"
  fi
done

for f in "${files[@]}"; do
  case "$f" in
    *.java)
      lines=$(wc -l < "$f")
      if [ "$lines" -gt "$JAVA_MAX" ]; then
        report "$f has $lines lines (max $JAVA_MAX)"
      elif [ "$lines" -gt "$JAVA_TARGET" ]; then
        echo "hygiene: warning: $f has $lines lines (target $JAVA_TARGET)" >&2
      fi
      ;;
  esac
done

exit "$fail"

#!/usr/bin/env bash
# Repository hygiene checks, shared by the pre-commit hook, the commit-msg hook and CI.
#
# Usage:
#   scripts/check-hygiene.sh                 scan tracked files (working tree)
#   scripts/check-hygiene.sh --staged        scan the index instead of the working tree (pre-commit)
#   scripts/check-hygiene.sh --message FILE  scan a git commit message
#   scripts/check-hygiene.sh --text FILE     scan raw text (PR title and body, commit log)
#
# Requires bash 4.4 or newer (macOS ships 3.2: brew install bash).
# Exceptions live in scripts/hygiene-exceptions.txt, one "check:path" per line
# (check is "size" or "path"), followed by "# reason".
#
# The path deny-list is deliberately partial: it catches the usual ways reference game data
# enters a repository, it does not prove that none did.
if (( BASH_VERSINFO[0] < 4 || (BASH_VERSINFO[0] == 4 && BASH_VERSINFO[1] < 4) )); then
  echo "hygiene: bash 4.4 or newer is required (found ${BASH_VERSION})" >&2
  exit 2
fi
set -euo pipefail
# The size loop runs last in a pipeline and must update $fail in the current shell.
shopt -s lastpipe

# Perl regexes. Structured attribution is looked for on every line of a message; bare tool names
# only outside comment lines, because git's editor template quotes branch names and paths.
# Tool configuration paths (.claude/, CLAUDE.md) are not attribution.
STRUCTURED='co-?authored[- ]by|assisted[- ]by|generated (with|by|using) (an? )?(claude|ai)|ai[- ]assisted|ai[- ]generated'
BARE='(?<![.\w])claude(?![\w]|\.md)|claude\.ai|claude[_-]code|\banthropic\b'
ATTRIBUTION="$STRUCTURED|$BARE"
# En dash and em dash, as fixed byte strings: PCRE code points need a UTF-8 locale, which the
# C locale used here (stable byte semantics) is not.
EN_DASH=$'\xe2\x80\x93'
EM_DASH=$'\xe2\x80\x94'
JAVA_TARGET=400
JAVA_MAX=600
EXCEPTIONS=scripts/hygiene-exceptions.txt

# Files that legitimately contain the patterns above.
SELF_EXCLUDED=':(exclude)scripts/check-hygiene.sh'

fail=0
report() { echo "hygiene: $1" >&2; fail=1; }

# scan_text LABEL < text (comment lines are not removed here)
scan_text() {
  local text
  text="$(cat)"
  if LC_ALL=C grep -iqP -- "$ATTRIBUTION" <<<"$text"; then
    report "forbidden attribution string in $1"
  fi
  if LC_ALL=C grep -qF -e "$EN_DASH" -e "$EM_DASH" <<<"$text"; then
    report "en dash or em dash in $1 (use '-')"
  fi
}

# scan_message < message : structured attribution everywhere, the rest outside '#' lines.
scan_message() {
  local text body
  text="$(cat)"
  body="$(grep -v '^#' <<<"$text" || true)"
  if LC_ALL=C grep -iqP -- "$STRUCTURED" <<<"$text"; then
    report "forbidden attribution string in commit message"
  fi
  if LC_ALL=C grep -iqP -- "$BARE" <<<"$body"; then
    report "forbidden attribution string in commit message"
  fi
  if LC_ALL=C grep -qF -e "$EN_DASH" -e "$EM_DASH" <<<"$body"; then
    report "en dash or em dash in commit message (use '-')"
  fi
}

mode="${1:-tree}"
case "$mode" in
  --message)
    file="${2:?message file required}"
    # Git keeps '#' lines of messages given with -m or -F, hence the structured scan on all
    # lines; only the verbose-diff part below the scissors line is cut.
    tr -d '\r' < "$file" | sed '/^# -* >8 -*$/,$d' | scan_message
    exit "$fail"
    ;;
  --text)
    file="${2:?text file required}"
    tr -d '\r' < "$file" | scan_text "$file"
    exit "$fail"
    ;;
  --staged) grep_flags=(--cached) ;;
  tree) grep_flags=() ;;
  *) echo "usage: check-hygiene.sh [--staged | --message FILE | --text FILE]" >&2; exit 2 ;;
esac

cd "$(git rev-parse --show-toplevel)"

# A check that cannot run must fail the build, never pass silently.
git_grep() { # $1 pathspec, then git grep options; status 1 (no match) is fine, above is fatal
  local pathspec="$1" status=0
  shift
  LC_ALL=C git -c core.quotePath=false grep "${grep_flags[@]}" -I "$@" -- "$pathspec" "$SELF_EXCLUDED" || status=$?
  if [ "$status" -gt 1 ]; then
    echo "hygiene: git grep failed with status $status" >&2
    exit 2
  fi
}

excepted() { # $1 check, $2 path ; awk reads the whole file, so no SIGPIPE under pipefail
  [ -f "$EXCEPTIONS" ] && awk -v key="$1:$2" '{ sub(/[ \t]*#.*$/, "") } $0 == key { found = 1 } END { exit !found }' "$EXCEPTIONS"
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

# .gitattributes can mark a text file as binary, which hides it from git grep -I.
if [ -f .gitattributes ]; then
  while IFS= read -r line; do
    case "$line" in
      *"*.png "*|*"*.ogg "*|*"*.jar "*|*"*.mca "*|*"*.nbt "*) ;;
      *binary*|*-text*|*-diff*) report ".gitattributes hides files from the content checks: $line" ;;
    esac
  done < .gitattributes
fi

hits() { # $1 label, remaining arguments: git grep options and patterns
  local label="$1" out
  shift
  out="$(git_grep . -l "$@")"
  [ -z "$out" ] || while IFS= read -r p; do report "$label in $p"; done <<<"$out"
}
hits "forbidden attribution string" -i -P "$ATTRIBUTION"
hits "en dash or em dash (use '-')" -F -e "$EN_DASH" -e "$EM_DASH"

git_grep '*.java' -c '' | while IFS=: read -r path lines; do
  if [ "$lines" -gt "$JAVA_MAX" ] && ! excepted size "$path"; then
    report "$path has $lines lines (max $JAVA_MAX)"
  elif [ "$lines" -gt "$JAVA_TARGET" ]; then
    echo "hygiene: warning: $path has $lines lines (target $JAVA_TARGET)" >&2
  fi
done

exit "$fail"

#!/usr/bin/env bash
# Repository hygiene checks, shared by the pre-commit hook, the commit-msg hook and CI.
#
# Usage:
#   scripts/check-hygiene.sh                 scan the index (staged content)
#   scripts/check-hygiene.sh --message FILE  scan a git commit message
#   scripts/check-hygiene.sh --text FILE     scan raw text (PR title and body, commit log)
#
# The index is scanned in every mode, never the working tree: it is what a commit records, and
# after a clean checkout (CI) it equals HEAD. Encodings, line-ending filters and attributes of the
# working tree therefore cannot hide content. --staged is accepted as a synonym of the default.
#
# Requires bash 4.4 or newer (macOS ships 3.2: brew install bash) and GNU grep with -P.
# Exceptions live in scripts/hygiene-exceptions.txt (read from the index), one "check:path" per
# line (check is "size" or "path"), followed by "# reason".
#
# The path deny-list is deliberately partial: it catches the usual ways reference game data
# enters a repository, it does not prove that none did.
if (( BASH_VERSINFO[0] < 4 || (BASH_VERSINFO[0] == 4 && BASH_VERSINFO[1] < 4) )); then
  echo "hygiene: bash 4.4 or newer is required (found ${BASH_VERSION})" >&2
  exit 2
fi
set -euo pipefail
# scan_message and scan_text run last in a pipeline and must update $fail in the current shell.
shopt -s lastpipe

# Perl regexes, case-insensitive.
# TRAILER is the form tools actually inject; it is searched on every line of a message, including
# '#' lines, because git keeps those with -m and -F. PROSE and BARE are searched everywhere
# except '#' lines of a message, since git's editor template quotes branch names and paths.
# Tool configuration paths (.claude/, CLAUDE.md) are not attribution.
TOOLS='claude\w*|chatgpt|openai|codex|copilot|cursor|gemini'
TRAILER='^(#\s*)?\s*(co-?authored[- ]by|assisted[- ]by)\s*:|^(#\s*)?\s*signed-off-by\s*:.*('"$TOOLS"')|^(#\s*)?\s*generated (with|by|using) (an? )?(claude|ai)(?![\w-])'
PROSE='(generated|written|made|built|created|authored|assisted|co-?written) (with|by|using|via) (an? )?('"$TOOLS"')\b|(generated|assisted|written) (with|by|using) (an? )?ai(?![\w-])|ai[- ]assisted'
BARE='(?<![.\w-])claude(?![\w]|\.(local\.)?md)|claude\.ai|claude[_-]?code|\banthropic\b'
ATTRIBUTION="$TRAILER|$PROSE|$BARE"
# En dash and em dash, as fixed byte strings: PCRE code points need a UTF-8 locale, which the
# C locale used here (stable byte semantics) is not.
EN_DASH=$'\xe2\x80\x93'
EM_DASH=$'\xe2\x80\x94'
JAVA_TARGET=400
JAVA_MAX=600
EXCEPTIONS_PATH=scripts/hygiene-exceptions.txt

# Files that legitimately contain the patterns above.
SELF_EXCLUDED=':(exclude)scripts/check-hygiene.sh'

fail=0
report() { echo "hygiene: $1" >&2; fail=1; }

# matches OPTION PATTERN TEXT : grep status 1 is "no match", anything above is a broken tool.
matches() {
  local status=0
  LC_ALL=C grep -q "$1" -- "$2" <<<"$3" || status=$?
  if [ "$status" -gt 1 ]; then
    echo "hygiene: grep failed with status $status" >&2
    exit 2
  fi
  [ "$status" -eq 0 ]
}
has_dash() {
  local status=0
  LC_ALL=C grep -qF -e "$EN_DASH" -e "$EM_DASH" <<<"$1" || status=$?
  if [ "$status" -gt 1 ]; then
    echo "hygiene: grep failed with status $status" >&2
    exit 2
  fi
  [ "$status" -eq 0 ]
}
matches_ci() { matches -iP "$@"; }

# scan_text LABEL < text (comment lines are not removed here)
scan_text() {
  local text
  text="$(cat)"
  if matches_ci "$ATTRIBUTION" "$text"; then
    report "forbidden attribution string in $1"
  fi
  if has_dash "$text"; then
    report "en dash or em dash in $1 (use '-')"
  fi
}

# scan_message < message : trailers everywhere, the rest outside '#' lines.
scan_message() {
  local text body
  text="$(cat)"
  body="$(grep -v '^#' <<<"$text" || true)"
  if matches_ci "$TRAILER" "$text"; then
    report "forbidden attribution string in commit message"
  fi
  if matches_ci "$PROSE|$BARE" "$body"; then
    report "forbidden attribution string in commit message"
  fi
  if has_dash "$body"; then
    report "en dash or em dash in commit message (use '-')"
  fi
}

mode="${1:-index}"
case "$mode" in
  --message)
    file="${2:?message file required}"
    # Git keeps '#' lines of messages given with -m or -F, hence the trailer scan on all lines;
    # only the verbose-diff part below the scissors line is cut.
    tr -d '\r' < "$file" | sed '/^# -* >8 -*$/,$d' | scan_message
    exit "$fail"
    ;;
  --text)
    file="${2:?text file required}"
    tr -d '\r' < "$file" | scan_text "$file"
    exit "$fail"
    ;;
  --staged | index) ;;
  *) echo "usage: check-hygiene.sh [--staged | --message FILE | --text FILE]" >&2; exit 2 ;;
esac

cd "$(git rev-parse --show-toplevel)"

exceptions="$(git show ":$EXCEPTIONS_PATH" 2>/dev/null || true)"
excepted() { # $1 check, $2 path ; awk reads everything, so no SIGPIPE under pipefail
  awk -v key="$1:$2" '{ sub(/[ \t]*#.*$/, "") } $0 == key { found = 1 } END { exit !found }' <<<"$exceptions"
}

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

# Each listing goes to a file first, so that a failing git command fails the script instead of
# silently producing an empty list through a process substitution.
git ls-files --cached -z > "$workdir/files"
empty_tree="$(git hash-object -t tree /dev/null)"
git diff --cached --numstat -z "$empty_tree" > "$workdir/numstat"

shopt -s nocasematch
while IFS= read -r -d '' f; do
  excepted path "$f" && continue
  case "$f" in
    .cache/*|run/*|reports/*) report "ignored directory content is tracked: $f" ;;
    docs/reference/README.md|gradle/wrapper/gradle-wrapper.jar) ;;
    docs/reference/*) report "local research report must not be versioned: $f" ;;
    assets/minecraft/*|data/minecraft/*|*/assets/minecraft/*|*/data/minecraft/*)
      report "reference game data must not be versioned: $f" ;;
    net/minecraft/*|*/net/minecraft/*|*.class) report "compiled or source code of the reference game: $f" ;;
    *.jar|*.mca|*.mcr|*.ogg|*.nbt|*.dat|*.zip|*.mcmeta) report "reference binary must not be versioned: $f" ;;
    docs/images/*|*/src/test/resources/*) ;;
    *.png) report "image outside docs/images and test resources: $f" ;;
  esac
done < "$workdir/files"

# Binary content hides a file from every content check below (git grep -I skips it), whether it is
# marked by an attribute or recognised by git from its bytes (UTF-16, NUL bytes). Only a few
# image and archive types may be binary.
while IFS= read -r -d '' record; do
  case "$record" in
    $'-\t-\t'*)
      binary_path="${record#$'-\t-\t'}"
      case "$binary_path" in
        *.png|*.jpg|*.ogg|*.jar) ;;
        *) excepted path "$binary_path" || report "file is binary or not valid text, content checks cannot read it: $binary_path" ;;
      esac
      ;;
  esac
done < "$workdir/numstat"
shopt -u nocasematch

# A check that cannot run must fail the build, never pass silently.
git_grep() { # $1 pathspec, then git grep options; status 1 (no match) is fine, above is fatal
  local pathspec="$1" status=0
  shift
  LC_ALL=C git -c core.quotePath=false -c color.grep=never grep --cached -I "$@" -- "$pathspec" "$SELF_EXCLUDED" || status=$?
  if [ "$status" -gt 1 ]; then
    echo "hygiene: git grep failed with status $status" >&2
    exit 2
  fi
}

hits() { # $1 label, remaining arguments: git grep options and patterns
  local label="$1" out
  shift
  out="$(git_grep . -l "$@")"
  [ -z "$out" ] || while IFS= read -r p; do report "$label in $p"; done <<<"$out"
}
hits "forbidden attribution string" -i -P "$ATTRIBUTION"
hits "en dash or em dash (use '-')" -F -e "$EN_DASH" -e "$EM_DASH"

# Line counts: NUL after the path, so that ':' in names and any color setting cannot break parsing.
git_grep '*.java' -z -c '' > "$workdir/lines"
while IFS= read -r -d '' path && IFS= read -r lines; do
  if [ "$lines" -gt "$JAVA_MAX" ] && ! excepted size "$path"; then
    report "$path has $lines lines (max $JAVA_MAX)"
  elif [ "$lines" -gt "$JAVA_TARGET" ]; then
    echo "hygiene: warning: $path has $lines lines (target $JAVA_TARGET)" >&2
  fi
done < "$workdir/lines"

exit "$fail"

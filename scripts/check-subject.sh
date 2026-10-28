#!/usr/bin/env bash
# Validates a commit subject or pull request title: "[KEYWORD] : Description".
set -euo pipefail

# Character counting and [[:upper:]] need a UTF-8 locale; take the first one that counts "e acute"
# as a single character (printf with its UTF-8 bytes keeps this file ASCII).
accent="$(printf '\xc3\xa9')"
locale_found=""
for candidate in C.UTF-8 en_US.UTF-8 fr_FR.UTF-8 en_US.utf8; do
  if [ "$(LC_ALL="$candidate" bash -c 'printf %s "${#1}"' _ "$accent" 2>/dev/null)" = "1" ]; then
    locale_found="$candidate"
    break
  fi
done
if [ -z "$locale_found" ]; then
  echo "check-subject: no UTF-8 locale available, cannot count characters reliably" >&2
  exit 2
fi
export LC_ALL="$locale_found"

subject="${1:?subject required}"
keywords='FEAT|FIX|REFACTOR|TEST|DOCS|CHORE|PERF|BUILD|CI|REVERT'
pattern="^\[(${keywords})\] : [[:upper:]].{3,}$"
dashes=$'\xe2\x80\x93|\xe2\x80\x94'

fail() { echo "invalid subject: \"$subject\" ($1)" >&2; exit 1; }

[[ "$subject" =~ $pattern ]] || fail "expected [KEYWORD] : Description, KEYWORD in ${keywords//|/, }, description starting with an uppercase letter"
nbsp="$(printf '\302\240')"
[[ "$subject" =~ [[:space:]]$ || "$subject" == *"$nbsp" ]] && fail "trailing whitespace"
[[ "$subject" == *$'\t'* ]] && fail "tab character"
[[ "$subject" == *"." ]] && fail "must not end with a period"
LC_ALL=C grep -qE -- "$dashes" <<<"$subject" && fail "en dash or em dash (use '-')"
[ "${#subject}" -gt 100 ] && fail "longer than 100 characters (${#subject})"
exit 0

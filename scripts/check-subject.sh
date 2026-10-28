#!/usr/bin/env bash
# Validates a commit subject or pull request title: "[KEYWORD] : Description".
set -euo pipefail

# Character counting and [[:upper:]] must not depend on the caller's locale.
export LC_ALL=C.UTF-8

subject="${1:?subject required}"
keywords='FEAT|FIX|REFACTOR|TEST|DOCS|CHORE|PERF|BUILD|CI|REVERT'
pattern="^\[(${keywords})\] : [[:upper:][:digit:]].{3,}$"
dashes=$'\xe2\x80\x93|\xe2\x80\x94'

fail() { echo "invalid subject: \"$subject\" ($1)" >&2; exit 1; }

[[ "$subject" =~ $pattern ]] || fail "expected [KEYWORD] : Description, KEYWORD in ${keywords//|/, }, description starting with an uppercase letter"
[[ "$subject" =~ [[:space:]]$ ]] && fail "trailing whitespace"
[[ "$subject" == *$'\t'* ]] && fail "tab character"
[[ "$subject" == *"." ]] && fail "must not end with a period"
LC_ALL=C grep -qE -- "$dashes" <<<"$subject" && fail "en dash or em dash (use '-')"
[ "${#subject}" -gt 100 ] && fail "longer than 100 characters (${#subject})"
exit 0

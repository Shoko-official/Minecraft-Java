#!/usr/bin/env bash
# Validates a commit subject or pull request title: "[KEYWORD] : description".
set -euo pipefail

subject="${1:?subject required}"
keywords='FEAT|FIX|REFACTOR|TEST|DOCS|CHORE|PERF|BUILD|CI|REVERT'
pattern="^\[(${keywords})\] : [^ ].{4,}$"

if ! [[ "$subject" =~ $pattern ]]; then
  echo "invalid subject: \"$subject\"" >&2
  echo "expected: [KEYWORD] : short description (KEYWORD in ${keywords//|/, })" >&2
  exit 1
fi

if [ "${#subject}" -gt 100 ]; then
  echo "subject too long (${#subject} > 100 characters)" >&2
  exit 1
fi

if [[ "$subject" == *"." ]]; then
  echo "subject must not end with a period" >&2
  exit 1
fi

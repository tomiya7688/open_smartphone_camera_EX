#!/usr/bin/env bash
set -euo pipefail

roots=(
  "capture-api/src"
  "processing-api/src"
)

for root in "${roots[@]}"; do
  if grep -REn     --include='*.kt'     '^[[:space:]]*import[[:space:]]+(android\.|androidx\.camera\.)'     "$root"; then
    echo
    echo "ERROR: Android camera/platform imports must not cross into project-owned core APIs."
    exit 1
  fi
done

echo "Architecture boundary check passed."

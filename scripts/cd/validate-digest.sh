#!/usr/bin/env bash
# Fails unless every argument is an image digest of the form sha256:<64 hex characters>.
# A digest, not a tag, is the release identity: tags move, a digest names exactly one image.
# Usage: validate-digest.sh sha256:... [sha256:...]
set -euo pipefail
[ "$#" -ge 1 ] || { echo "usage: $0 <digest>..." >&2; exit 2; }
for d in "$@"; do
  [[ "$d" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "not a digest (expected sha256:<64 hex>): $d" >&2; exit 1; }
done

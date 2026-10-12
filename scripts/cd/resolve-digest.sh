#!/usr/bin/env bash
# Prints the digest that a tag points to in GHCR (needs a prior docker login).
# Used by the tag path of cd.yaml: the Image workflow tags the images that passed for a commit as passed-<commit>.
# Usage: resolve-digest.sh ghcr.io/<owner>/<image>:<tag>
set -euo pipefail
ref="${1:?image reference with tag}"
digest="$(docker buildx imagetools inspect "$ref" --format '{{.Manifest.Digest}}' 2>/dev/null || true)"
[[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "no image found for $ref (did the Image workflow pass for this commit?)" >&2; exit 1; }
echo "$digest"

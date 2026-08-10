#!/usr/bin/env bash
set -euo pipefail
operation=${1:-}; image=${2:-}; tag=${3:-}; expected_digest=${4:-}
[[ "$operation" == require-absent || "$operation" == require-digest ]] || { echo "invalid registry verification operation" >&2; exit 2; }
[[ "$image" =~ ^docker\.io/[a-z0-9_-]+/[a-z0-9._-]+$ ]] || { echo "invalid Docker Hub image" >&2; exit 2; }
[[ "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9._-]{0,127}$ ]] || { echo "invalid Docker tag" >&2; exit 2; }
[[ "$operation" != require-digest || "$expected_digest" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "invalid expected digest" >&2; exit 2; }
: "${DOCKER_USERNAME:?DOCKER_USERNAME is required}"
: "${DOCKER_PASSWORD:?DOCKER_PASSWORD is required}"
curl_bin=${CURL_BIN:-curl}
repository=${image#docker.io/}
auth_response=$("$curl_bin" --fail --silent --show-error --user "$DOCKER_USERNAME:$DOCKER_PASSWORD" "https://auth.docker.io/token?service=registry.docker.io&scope=repository:$repository:pull")
token=$(ruby -rjson -e 'token = JSON.parse(STDIN.read).fetch("token"); abort unless token.is_a?(String) && !token.empty?; print token' <<<"$auth_response")
headers=$(mktemp)
cleanup() { rm -f "$headers"; }
trap cleanup EXIT
status=$("$curl_bin" --silent --show-error --output /dev/null --dump-header "$headers" --write-out '%{http_code}' --header "Authorization: Bearer $token" --header 'Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json' "https://registry-1.docker.io/v2/$repository/manifests/$tag")
if [[ "$operation" == require-absent ]]; then
  case "$status" in
    404) exit 0 ;;
    200) echo "Refusing to overwrite existing image tag $tag" >&2; exit 1 ;;
    *) echo "Registry check failed closed with HTTP $status for $tag" >&2; exit 1 ;;
  esac
fi
[[ "$status" == 200 ]] || { echo "Published tag lookup failed with HTTP $status for $tag" >&2; exit 1; }
actual_digest=$(awk 'BEGIN { IGNORECASE=1 } /^Docker-Content-Digest:/ { gsub("\r", "", $2); print $2 }' "$headers" | tail -1)
[[ "$actual_digest" == "$expected_digest" ]] || { echo "Published tag digest mismatch for $tag" >&2; exit 1; }


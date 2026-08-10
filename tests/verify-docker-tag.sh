#!/usr/bin/env bash
set -euo pipefail
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
cat > "$fixture/curl" <<'FIXTURE'
#!/usr/bin/env bash
set -euo pipefail
for argument in "$@"; do
  if [[ "$argument" == https://auth.docker.io/* ]]; then printf '{"token":"fixture-token"}'; exit 0; fi
done
header_file=""
while (( $# )); do
  [[ "$1" != --dump-header ]] || { header_file=$2; shift; }
  shift
done
[[ -z "$header_file" ]] || printf 'Docker-Content-Digest: %s\r\n' "${FAKE_REGISTRY_DIGEST:-sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa}" > "$header_file"
printf '%s' "${FAKE_REGISTRY_STATUS:-404}"
FIXTURE
chmod +x "$fixture/curl"
export CURL_BIN="$fixture/curl" DOCKER_USERNAME=fixture DOCKER_PASSWORD=fixture
image=docker.io/dundunts/wage-app
digest=sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
FAKE_REGISTRY_STATUS=404 bash "$repo_root/scripts/verify-docker-tag.sh" require-absent "$image" 0.0.1
if FAKE_REGISTRY_STATUS=200 bash "$repo_root/scripts/verify-docker-tag.sh" require-absent "$image" 0.0.1; then echo "existing tag was not rejected" >&2; exit 1; fi
if FAKE_REGISTRY_STATUS=503 bash "$repo_root/scripts/verify-docker-tag.sh" require-absent "$image" 0.0.1; then echo "registry failure did not fail closed" >&2; exit 1; fi
FAKE_REGISTRY_STATUS=200 FAKE_REGISTRY_DIGEST="$digest" bash "$repo_root/scripts/verify-docker-tag.sh" require-digest "$image" 0.0.1 "$digest"
if FAKE_REGISTRY_STATUS=200 FAKE_REGISTRY_DIGEST=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb bash "$repo_root/scripts/verify-docker-tag.sh" require-digest "$image" 0.0.1 "$digest"; then echo "mismatched published digest was accepted" >&2; exit 1; fi
echo "Docker tag verifier fails closed and binds exact digests"

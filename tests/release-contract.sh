#!/usr/bin/env bash

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

ruby -ryaml -e '
  root = ARGV.fetch(0)
  ci = YAML.safe_load(File.read(File.join(root, ".github/workflows/ci.yaml")), aliases: true)
  release = YAML.safe_load(File.read(File.join(root, ".github/workflows/release.yaml")), aliases: true)

  ci_triggers = ci.fetch(true)
  abort "CI must cover pull requests, main and release branches" unless
    ci_triggers.key?("pull_request") && ci_triggers.dig("push", "branches") == ["main", "release/**"]

  triggers = release.fetch(true)
  abort "release must trigger only for version tags" unless triggers == {"push" => {"tags" => ["v*.*.*"]}}
  release_yaml = File.read(File.join(root, ".github/workflows/release.yaml"))
  abort "release must reject duplicate Docker tags before build" unless
    release_yaml.index("Refuse an existing version tag") < release_yaml.index("Build and publish image once")
  abort "release must publish version and commit-SHA tags" unless
    release_yaml.include?("type=raw,value=${{ steps.version.outputs.version }}") &&
    release_yaml.include?("type=sha,prefix=git-,format=long")
  abort "release must capture the registry digest" unless release_yaml.include?("steps.publish.outputs.digest")
  abort "release must require an annotated Git tag" unless release_yaml.include?(%q{git cat-file -t "refs/tags/$GITHUB_REF_NAME"})
  abort "release must use the narrowly scoped GitHub App" unless
    release_yaml.include?("secrets.INFRA_APP_ID") &&
    release_yaml.include?("secrets.INFRA_APP_PRIVATE_KEY") &&
    release_yaml.include?("repositories: wage-app-infr")
  abort "release workflow must not receive Kubernetes credentials" if release_yaml.match?(/KUBECONFIG|kubectl/)

  app = File.read(File.join(root, "src/main/resources/application.yaml"))
  abort "Liquibase must use runtime database credentials" unless
    app.include?("user: ${POSTGRES_USER:user}") && app.include?("password: ${POSTGRES_PASSWORD:password}")
  abort "Keycloak client secret must not have a committed default" unless app.include?("client-secret: ${KEYCLOAK_CLIENT_SECRET:}")
' "$repo_root"

echo "backend release contract is valid"

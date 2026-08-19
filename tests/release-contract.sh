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
  abort "release must support immutable tag pushes and explicit recovery of an existing tag" unless
    triggers.dig("push", "tags") == ["v*.*.*"] &&
    triggers.dig("workflow_dispatch", "inputs", "tag", "required") == true &&
    triggers.dig("workflow_dispatch", "inputs", "tag", "type") == "string"
  release_yaml = File.read(File.join(root, ".github/workflows/release.yaml"))
  abort "release must reject duplicate Docker tags before build" unless
    release_yaml.index("Refuse an existing version tag") < release_yaml.index("Build and publish image once")
  abort "release must publish version and commit-SHA tags" unless
    release_yaml.include?("type=raw,value=${{ steps.version.outputs.version }}") &&
    release_yaml.include?("type=raw,value=git-${{ steps.version.outputs.source_sha }}")
  abort "release must capture the registry digest" unless release_yaml.include?("steps.publish.outputs.digest")
  abort "release image must use the canonical repository, not the login credential as a namespace" unless
    release_yaml.scan("IMAGE: docker.io/dundunts/wage-app").length == 1 &&
    release_yaml.include?(%q{images: ${{ env.IMAGE }}}) &&
    !release_yaml.include?(%q{docker.io/${{ secrets.DOCKER_USERNAME }}/wage-app})
  abort "release must verify the remote annotated tag outside checkout-managed refs" unless
    release_yaml.include?(%q{verified_tag_ref="refs/release-tags/$RELEASE_TAG"}) &&
    release_yaml.include?(%q{refs/tags/$RELEASE_TAG:$verified_tag_ref}) &&
    release_yaml.include?(%q{git cat-file -t "$verified_tag_ref"})
  abort "release must build and attest the peeled release source, including recovery runs" unless
    release_yaml.include?(%q{source_sha=$(git rev-parse "$verified_tag_ref^{commit}")}) &&
    release_yaml.include?(%q{git checkout --detach "$source_sha"}) &&
    release_yaml.include?(%q{type=raw,value=git-${{ steps.version.outputs.source_sha }}}) &&
    release_yaml.include?(%q{org.opencontainers.image.revision=${{ steps.version.outputs.source_sha }}}) &&
    !release_yaml.include?(%q{git-$GITHUB_SHA})
  abort "release must use the narrowly scoped GitHub App" unless
    release_yaml.include?("secrets.INFRA_APP_ID") &&
    release_yaml.include?("secrets.INFRA_APP_PRIVATE_KEY") &&
    release_yaml.include?("repositories: wage-app-infr")
  abort "release workflow must not receive Kubernetes credentials" if release_yaml.match?(/KUBECONFIG|kubectl/)
  abort "workflows must invoke the non-executable Gradle wrapper through bash" unless
    File.read(File.join(root, ".github/workflows/ci.yaml")).include?("bash ./gradlew check bootJar") &&
    release_yaml.include?("bash ./gradlew test")

  dockerfile = File.read(File.join(root, "Dockerfile"))
  abort "release image build must produce bootJar without running the verification lifecycle" unless
    dockerfile.include?("RUN gradle :bootJar --no-daemon") &&
    !dockerfile.match?(/RUN gradle :build\b/)

  app = File.read(File.join(root, "src/main/resources/application.yaml"))
  abort "Liquibase must use runtime database credentials" unless
    app.include?("user: ${POSTGRES_USER:user}") && app.include?("password: ${POSTGRES_PASSWORD:password}")
  abort "Keycloak client secret must not have a committed default" unless app.include?("client-secret: ${KEYCLOAK_CLIENT_SECRET:}")
' "$repo_root"

bash "$repo_root/tests/verify-docker-tag.sh"

echo "backend release contract is valid"

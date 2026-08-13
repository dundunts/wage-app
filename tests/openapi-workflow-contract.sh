#!/usr/bin/env bash

set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

ruby -ryaml -e '
  root = ARGV.fetch(0)
  workflow_path = File.join(root, ".github/workflows/api-compatibility.yaml")
  abort "pull-request API compatibility workflow is missing" unless File.file?(workflow_path)

  workflow = YAML.safe_load(File.read(workflow_path), aliases: true)
  triggers = workflow.fetch(true)
  abort "API compatibility diff must run for pull requests" unless triggers.keys == ["pull_request"]

  workflow_yaml = File.read(workflow_path)
  abort "API compatibility diff must compare the exact pull-request base" unless
    workflow_yaml.include?(%q{github.event.pull_request.base.sha}) &&
    workflow_yaml.include?(%q{git show "$BASE_SHA:openapi/bundled/openapi.yaml"})
  abort "oasdiff image version and digest must be pinned" unless workflow_yaml.include?(
    "tufin/oasdiff:v1.28.0@sha256:86830f988eaafcf589acb2794ee5ab78e3300ded071d6517bf085469300cbf36"
  )
  abort "API compatibility diff must publish a Markdown changelog" unless
    workflow_yaml.include?("changelog") && workflow_yaml.include?("--format markdown") &&
    workflow_yaml.include?(%q{$GITHUB_STEP_SUMMARY})
  abort "incompatible API changes must remain informational" if workflow_yaml.include?("--fail-on")

  build = File.read(File.join(root, "build.gradle.kts"))
  abort "temporary Springdoc dependency remains" if build.match?(/org\.springdoc|springdoc-openapi/)
  abort "temporary extraction task remains" if build.include?("extractCompanyOpenApi")
  abort "temporary extraction test remains" if
    File.exist?(File.join(root, "src/test/kotlin/org/turter/wageapp/openapi/CompanyOpenApiExtractionTest.kt"))
  abort "temporary extraction profile remains" if
    File.exist?(File.join(root, "src/test/resources/application-openapi-docs.yaml"))

  application = File.read(File.join(root, "src/main/resources/application.yaml"))
  abort "runtime documentation configuration remains" if application.match?(/springdoc|swagger-ui/)
' "$repo_root"

echo "OpenAPI publication workflow contract is valid"

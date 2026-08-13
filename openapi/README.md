# Wage App OpenAPI workflow

This directory contains the canonical, manually reviewed OpenAPI 3.0.3 contract
for all 34 inbound `/api/v1` Company, Employee, Shift Session, Checkpoint,
Shift Result Draft, Shift Result, and Payroll operations. The outbound Telegram
notification API is intentionally not part of this service-owned contract.
`openapi.yaml` and its referenced `paths/` and `components/` files are the authored
sources. `bundled/openapi.yaml` is the deterministic, self-contained document for
consumers and can be read directly from `main`:

```text
https://raw.githubusercontent.com/dundunts/wage-app/main/openapi/bundled/openapi.yaml
```

All subsequent HTTP API changes must begin in the authored OpenAPI sources and
update the implementation and behavioral tests in the same pull request.

Run the complete contract verification through Gradle:

```shell
./gradlew openApiCheck
```

`check` also depends on `openApiCheck`. Both the modular source and generated
bundle are validated with all references resolved, Spectral applies the lint
policy, operation identifiers and complete-baseline guarantees are tested, and
a fresh bundle is compared byte-for-byte with the committed bundle. After an
intentional source change, rebuild it with:

```shell
./gradlew openApiUpdateBundle
```

Pull requests also publish an informational oasdiff changelog against the exact
base commit in the GitHub Actions job summary. Incompatible findings are visible
to reviewers but do not block coordinated backend and web changes.

The service has no Springdoc dependency, Swagger UI, or runtime API documentation
endpoint. The committed bundle is the only published documentation surface.

## Pinned toolchain

| Responsibility | Tool | Version |
| --- | --- | --- |
| Validation | swagger-parser | 12.1.0 |
| Linting | Spectral CLI | 6.16.3 |
| Bundling | Redocly CLI | 2.46.1 |
| Compatibility diff | oasdiff container | 1.28.0 |
| CLI runtime | Node.js / npm | 22.14.0 / 10.9.2 |

The Gradle Node plugin is pinned to 7.1.0 and downloads the declared Node.js and
npm versions. Exact npm dependencies and transitives are recorded in
`package-lock.json`.

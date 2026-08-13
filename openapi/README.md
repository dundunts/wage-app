# Wage App OpenAPI workflow

This directory contains the manually reviewed OpenAPI 3.0.3 Company baseline.
`openapi.yaml` and its referenced `paths/` and `components/` files are the authored
sources. `bundled/openapi.yaml` is the deterministic, self-contained document for
consumers.

Run the complete contract verification through Gradle:

```shell
./gradlew openApiCheck
```

`check` also depends on `openApiCheck`. Validation resolves every reference,
Spectral applies the lint policy, the public Company guarantees are tested, and a
fresh bundle is compared byte-for-byte with the committed bundle. After an
intentional source change, rebuild it with:

```shell
./gradlew openApiUpdateBundle
```

## Temporary extraction

The original Spring contract can be re-extracted as a review aid:

```shell
./gradlew extractCompanyOpenApi
```

The result is written to `build/openapi/extracted-company.json`. Springdoc is a
test-only dependency, and the endpoint is enabled only by the `openapi-docs` test
profile. The Swagger UI dependency is not present and the UI remains disabled.
The extracted file is a starting point only; the modular sources are manually
reviewed against validation, security, exception handling, pagination, and
controller integration tests.

## Pinned toolchain

| Responsibility | Tool | Version |
| --- | --- | --- |
| Extraction | springdoc-openapi WebFlux API | 2.8.14 |
| Validation | swagger-parser | 12.1.0 |
| Linting | Spectral CLI | 6.16.3 |
| Bundling | Redocly CLI | 2.46.1 |
| CLI runtime | Node.js / npm | 22.14.0 / 10.9.2 |

The Gradle Node plugin is pinned to 7.1.0 and downloads the declared Node.js and
npm versions. Exact npm dependencies and transitives are recorded in
`package-lock.json`.

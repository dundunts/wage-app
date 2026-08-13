import assert from "node:assert/strict";
import fs from "node:fs";
import process from "node:process";
import YAML from "yaml";

const bundlePath = process.argv[2];
const document = YAML.parse(fs.readFileSync(bundlePath, "utf8"));

assert.equal(document.openapi, "3.0.3");
assert.deepEqual(
  Object.fromEntries(
    Object.entries(document.paths)
      .filter(([path]) => path.startsWith("/api/v1/company/"))
      .map(([path, item]) => [
        path,
        Object.keys(item).filter((key) => ["get", "post", "put", "delete"].includes(key)),
      ]),
  ),
  {
    "/api/v1/company/get/{id}": ["get"],
    "/api/v1/company/get/for-user": ["get"],
    "/api/v1/company/get/page": ["get"],
    "/api/v1/company/create": ["post"],
    "/api/v1/company/update/{id}": ["put"],
    "/api/v1/company/delete/{id}": ["delete"],
  },
);

assert.deepEqual(document.security, [{ BearerAuth: [] }]);
assert.equal(document.components.securitySchemes.BearerAuth.scheme, "bearer");

const operations = Object.entries(document.paths)
  .filter(([path]) => path.startsWith("/api/v1/company/"))
  .flatMap(([, path]) =>
  Object.entries(path)
    .filter(([method]) => ["get", "post", "put", "delete"].includes(method))
    .map(([, operation]) => operation),
  );
for (const operation of operations) {
  assert.ok(operation.responses["401"], `${operation.operationId} must document 401`);
  assert.ok(operation.responses["403"], `${operation.operationId} must document 403`);
}
assert.deepEqual(Object.keys(document.paths["/api/v1/company/get/{id}"].get.responses), [
  "200",
  "400",
  "401",
  "403",
  "404",
]);
assert.deepEqual(Object.keys(document.paths["/api/v1/company/create"].post.responses), [
  "201",
  "400",
  "401",
  "403",
  "409",
]);
assert.deepEqual(Object.keys(document.paths["/api/v1/company/update/{id}"].put.responses), [
  "204",
  "400",
  "401",
  "403",
  "404",
  "409",
]);
assert.equal(document.components.parameters.Page.schema.default, 0);
assert.equal(document.components.parameters.Size.schema.default, 100);
assert.equal(document.components.schemas.CompanyPage.additionalProperties, true);
assert.deepEqual(document.components.schemas.CompanyPage.required, [
  "content",
  "number",
  "size",
  "totalElements",
]);
assert.equal(document.components.schemas.Company.properties.id.format, "uuid");
assert.equal(document.components.schemas.CompanyCreateOrUpdateRequest.properties.title.minLength, 1);
assert.equal(document.components.schemas.CompanyCreateOrUpdateRequest.properties.title.maxLength, 255);
assert.equal(
  document.components.schemas.CompanyCreateOrUpdateRequest.properties.defaultShiftStartTime.pattern,
  "^([0-1][0-9]|2[0-3]):[0-5][0-9]$",
);

const references = [];
const visit = (value) => {
  if (Array.isArray(value)) {
    value.forEach(visit);
  } else if (value && typeof value === "object") {
    for (const [key, child] of Object.entries(value)) {
      if (key === "$ref") references.push(child);
      visit(child);
    }
  }
};
visit(document);
assert.ok(references.length > 0, "the bundle should retain reusable internal components");
assert.ok(
  references.every((reference) => reference.startsWith("#/")),
  `the bundle contains an external reference: ${references.find((reference) => !reference.startsWith("#/"))}`,
);

console.log("Company OpenAPI contract guarantees are satisfied.");

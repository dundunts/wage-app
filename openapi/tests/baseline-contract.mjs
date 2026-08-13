import assert from "node:assert/strict";
import fs from "node:fs";
import process from "node:process";
import YAML from "yaml";

const bundlePath = process.argv[2];
const document = YAML.parse(fs.readFileSync(bundlePath, "utf8"));
const httpMethods = ["get", "post", "put", "patch", "delete"];

const expectedOperations = {
  "/api/v1/company/get/{id}": "get",
  "/api/v1/company/get/for-user": "get",
  "/api/v1/company/get/page": "get",
  "/api/v1/company/create": "post",
  "/api/v1/company/update/{id}": "put",
  "/api/v1/company/delete/{id}": "delete",
  "/api/v1/employee/get/{id}": "get",
  "/api/v1/employee/get/all": "get",
  "/api/v1/employee/get/by-companies": "get",
  "/api/v1/employee/get/coworkers": "get",
  "/api/v1/employee/create": "post",
  "/api/v1/employee/update/{id}": "put",
  "/api/v1/employee/bind-user/{employeeId}": "put",
  "/api/v1/employee/delete/{id}": "delete",
  "/api/v1/session/get/opened": "get",
  "/api/v1/session/get/available/{sessionId}": "get",
  "/api/v1/session/get/available/all": "get",
  "/api/v1/session/open": "post",
  "/api/v1/session/recalculating": "post",
  "/api/v1/session/{sessionId}/close": "put",
  "/api/v1/session/update/time": "put",
  "/api/v1/checkpoint/create": "post",
  "/api/v1/checkpoint/update": "post",
  "/api/v1/checkpoint/{checkpointId}/delete": "delete",
  "/api/v1/calculation/draft/for-session/{sessionId}": "get",
  "/api/v1/calculation/draft/{id}/confirm": "post",
  "/api/v1/calculation/draft/{id}/delete": "delete",
  "/api/v1/shift-result/{resultId}/get/detailed": "get",
  "/api/v1/shift-result/get/detailed/by-period/page": "get",
  "/api/v1/shift-result/save": "post",
  "/api/v1/shift-result/{resultId}/delete": "delete",
  "/api/v1/salary/own/get": "get",
  "/api/v1/salary/staff/get": "get",
  "/api/v1/salary/reports-table": "get",
};

const actualOperations = Object.fromEntries(
  Object.entries(document.paths).flatMap(([path, pathItem]) =>
    Object.keys(pathItem)
      .filter((method) => httpMethods.includes(method))
      .map((method) => [path, method]),
  ),
);
assert.deepEqual(actualOperations, expectedOperations);
assert.equal(Object.keys(actualOperations).length, 34);
assert.ok(Object.keys(actualOperations).every((path) => path.startsWith("/api/v1/")));
assert.ok(Object.keys(actualOperations).every((path) => !path.toLowerCase().includes("telegram")));

const operations = Object.entries(expectedOperations).map(
  ([path, method]) => document.paths[path][method],
);
const operationIds = operations.map(({ operationId }) => operationId);
assert.equal(new Set(operationIds).size, operationIds.length, "operationId values must be unique");

for (const operation of operations) {
  assert.ok(operation.operationId, "every operation must have an operationId");
  assert.ok(operation.summary, `${operation.operationId} must have a summary`);
  assert.ok(operation.description, `${operation.operationId} must have a description`);
  assert.ok(operation.tags?.length, `${operation.operationId} must have a tag`);
  assert.ok(operation.responses["401"], `${operation.operationId} must document 401`);
  assert.ok(operation.responses["403"], `${operation.operationId} must document 403`);
  assert.ok(
    Object.keys(operation.responses).some((status) => status.startsWith("2")),
    `${operation.operationId} must document a successful response`,
  );
}

const resolveLocalReference = (value) => {
  if (!value?.$ref) return value;
  assert.match(value.$ref, /^#\//, `bundle contains an external reference: ${value.$ref}`);
  return value.$ref
    .slice(2)
    .split("/")
    .reduce((current, segment) => current[segment], document);
};
for (const operation of operations) {
  for (const status of ["400", "404", "409"]) {
    if (!operation.responses[status]) continue;
    const response = resolveLocalReference(operation.responses[status]);
    const problemMediaType = response.content?.["application/problem+json"];
    assert.ok(problemMediaType, `${operation.operationId} ${status} must use application/problem+json`);
    assert.equal(
      problemMediaType.schema.$ref,
      "#/components/schemas/ProblemDetail",
      `${operation.operationId} ${status} must use ProblemDetail`,
    );
  }
}

assert.equal(document.openapi, "3.0.3");
assert.match(document.info.description, /complete.*canonical.*\/api\/v1/is);

console.log("Complete OpenAPI baseline guarantees are satisfied.");

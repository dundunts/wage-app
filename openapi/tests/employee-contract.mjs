import assert from "node:assert/strict";
import fs from "node:fs";
import process from "node:process";
import YAML from "yaml";

const bundlePath = process.argv[2];
const document = YAML.parse(fs.readFileSync(bundlePath, "utf8"));
const resolveLocalReference = (value) => {
  if (!value?.$ref) return value;
  return value.$ref
    .slice(2)
    .split("/")
    .reduce((current, segment) => current[segment], document);
};

const employeePaths = Object.fromEntries(
  Object.entries(document.paths)
    .filter(([path]) => path.startsWith("/api/v1/employee/"))
    .map(([path, item]) => [
      path,
      Object.keys(item).filter((key) => ["get", "post", "put", "delete"].includes(key)),
    ]),
);

assert.deepEqual(employeePaths, {
  "/api/v1/employee/get/{id}": ["get"],
  "/api/v1/employee/get/all": ["get"],
  "/api/v1/employee/get/by-companies": ["get"],
  "/api/v1/employee/get/coworkers": ["get"],
  "/api/v1/employee/create": ["post"],
  "/api/v1/employee/update/{id}": ["put"],
  "/api/v1/employee/bind-user/{employeeId}": ["put"],
  "/api/v1/employee/delete/{id}": ["delete"],
});

const employeeOperations = Object.entries(document.paths)
  .filter(([path]) => path.startsWith("/api/v1/employee/"))
  .flatMap(([, path]) =>
    Object.entries(path)
      .filter(([method]) => ["get", "post", "put", "delete"].includes(method))
      .map(([, operation]) => operation),
  );

assert.equal(employeeOperations.length, 8);
for (const operation of employeeOperations) {
  assert.match(operation.operationId, /employee|coworker/i);
  assert.ok(operation.summary);
  assert.ok(operation.description);
  assert.deepEqual(operation.tags, ["Employee"]);
  assert.ok(operation.responses["401"], `${operation.operationId} must document 401`);
  assert.ok(operation.responses["403"], `${operation.operationId} must document 403`);
}

const responseStatuses = (path, method) => Object.keys(document.paths[path][method].responses);
assert.deepEqual(responseStatuses("/api/v1/employee/get/{id}", "get"), [
  "200",
  "400",
  "401",
  "403",
  "404",
]);
assert.deepEqual(responseStatuses("/api/v1/employee/create", "post"), [
  "201",
  "400",
  "401",
  "403",
]);
assert.deepEqual(responseStatuses("/api/v1/employee/update/{id}", "put"), [
  "204",
  "400",
  "401",
  "403",
  "404",
  "409",
]);
assert.deepEqual(responseStatuses("/api/v1/employee/bind-user/{employeeId}", "put"), [
  "204",
  "400",
  "401",
  "403",
  "404",
  "409",
]);

const schemas = document.components.schemas;
assert.deepEqual(schemas.Position.enum, ["MANAGER", "WAITER_ACTIVE", "WAITER_INACTIVE"]);
assert.deepEqual(schemas.Employee.required, [
  "id",
  "companyIds",
  "userId",
  "firstName",
  "lastName",
  "patronymic",
  "simpleName",
  "position",
]);
assert.equal(schemas.Employee.properties.id.format, "uuid");
assert.equal(schemas.Employee.properties.companyIds.items.format, "uuid");
assert.equal(schemas.Employee.properties.userId.nullable, true);
assert.equal(schemas.Employee.properties.simpleName.nullable, true);
assert.deepEqual(schemas.CreateEmployeeRequest.required, [
  "firstName",
  "lastName",
  "patronymic",
  "position",
]);
assert.deepEqual(schemas.CreateEmployeeRequest.properties.companyIds.default, []);
assert.equal(schemas.CreateEmployeeRequest.properties.companyIds.items.format, "uuid");
assert.equal(schemas.CreateEmployeeRequest.properties.firstName.minLength, 1);
assert.equal(schemas.CreateEmployeeRequest.properties.firstName.pattern, ".*\\S.*");
assert.equal(schemas.UpdateEmployeeRequest.properties.userId.nullable, true);
assert.deepEqual(schemas.CompanyEmployees.required, ["companyId", "data"]);
assert.equal(schemas.CompanyEmployees.properties.companyId.format, "uuid");
assert.equal(schemas.CompanyEmployees.properties.data.minItems, 1);

const companyIds = document.components.parameters.CompanyIds;
assert.equal(companyIds.required, true);
assert.equal(companyIds.schema.minItems, 1);
assert.equal(companyIds.schema.items.format, "uuid");
const userId = document.components.parameters.UserId;
assert.equal(userId.required, true);
assert.equal(userId.schema.nullable, false);

for (const status of ["400", "404", "409"]) {
  const matchingResponses = employeeOperations.flatMap((operation) =>
    operation.responses[status] ? [operation.responses[status]] : [],
  );
  assert.ok(matchingResponses.length > 0, `Employee operations must document applicable ${status}`);
  for (const response of matchingResponses) {
    const resolvedResponse = resolveLocalReference(response);
    assert.equal(
      resolvedResponse.content["application/problem+json"].schema.$ref,
      "#/components/schemas/ProblemDetail",
    );
  }
}

console.log("Employee OpenAPI contract guarantees are satisfied.");

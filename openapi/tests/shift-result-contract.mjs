import assert from "node:assert/strict";
import fs from "node:fs";
import process from "node:process";
import YAML from "yaml";

const bundlePath = process.argv[2];
const document = YAML.parse(fs.readFileSync(bundlePath, "utf8"));

const expectedOperations = {
  "/api/v1/calculation/draft/for-session/{sessionId}": {
    method: "get",
    statuses: ["200", "400", "401", "403", "404", "409"],
  },
  "/api/v1/calculation/draft/{id}/confirm": {
    method: "post",
    statuses: ["200", "400", "401", "403", "404", "409"],
  },
  "/api/v1/calculation/draft/{id}/delete": {
    method: "delete",
    statuses: ["204", "400", "401", "403", "409"],
  },
  "/api/v1/shift-result/{resultId}/get/detailed": {
    method: "get",
    statuses: ["200", "400", "401", "403", "404", "409"],
  },
  "/api/v1/shift-result/get/detailed/by-period/page": {
    method: "get",
    statuses: ["200", "400", "401", "403", "404", "409"],
  },
  "/api/v1/shift-result/save": {
    method: "post",
    statuses: ["201", "400", "401", "403", "404", "409"],
  },
  "/api/v1/shift-result/{resultId}/delete": {
    method: "delete",
    statuses: ["204", "400", "401", "403", "409"],
  },
};

for (const [path, { method, statuses }] of Object.entries(expectedOperations)) {
  assert.ok(document.paths[path], `${path} must be documented`);
  assert.deepEqual(
    Object.keys(document.paths[path]).filter((key) => ["get", "post", "put", "delete"].includes(key)),
    [method],
  );
  assert.deepEqual(Object.keys(document.paths[path][method].responses), statuses);
}

const operation = (path, method) => document.paths[path][method];

const operations = Object.entries(expectedOperations).map(([path, { method }]) => operation(path, method));
assert.equal(new Set(operations.map(({ operationId }) => operationId)).size, operations.length);
for (const currentOperation of operations) {
  assert.match(currentOperation.operationId, /^[a-z][A-Za-z0-9]+$/);
  assert.ok(currentOperation.summary);
  assert.ok(currentOperation.description);
  assert.ok(currentOperation.responses["401"]);
  assert.ok(currentOperation.responses["403"]);
}

assert.match(
  operation("/api/v1/calculation/draft/{id}/confirm", "post").description,
  /confirm.*Shift Result Draft.*Shift Result/is,
);
assert.match(
  operation("/api/v1/shift-result/save", "post").description,
  /Manual Override/is,
);
const responseComponent = (path, method, status) => {
  const responseReference = operation(path, method).responses[status].$ref;
  return document.components.responses[responseReference.split("/").at(-1)];
};
assert.match(
  responseComponent("/api/v1/calculation/draft/{id}/confirm", "post", "409").description,
  /existing Shift Result/,
);
assert.match(
  responseComponent("/api/v1/shift-result/save", "post", "404").description,
  /Employee/,
);

const schemas = document.components.schemas;
const requiredSchemas = [
  "ShiftResultDraft",
  "ShiftResultDraftPayment",
  "ShiftResultDraftEmployee",
  "ConfirmShiftResultDraftResponse",
  "ShiftResult",
  "Payment",
  "PaymentEmployee",
  "CalculationSource",
  "SaveManualOverrideShiftResultRequest",
  "ManualOverridePaymentRequest",
  "SaveManualOverrideShiftResultResponse",
  "ShiftResultResponse",
  "ShiftResultPage",
  "ShiftSession",
  "Checkpoint",
];
for (const schemaName of requiredSchemas) {
  assert.ok(schemas[schemaName], `${schemaName} must be documented`);
}

const assertRequired = (schemaName, fields, nullableFields = []) => {
  assert.deepEqual(schemas[schemaName].required, fields);
  for (const field of fields) {
    assert.equal(
      schemas[schemaName].properties[field].nullable,
      nullableFields.includes(field),
      `${schemaName}.${field} must explicitly document nullability`,
    );
  }
};

assertRequired("ShiftResultDraft", ["id", "payments", "date", "sessionId"]);
assertRequired("ShiftResultDraftPayment", [
  "id", "employee", "percentFromRevenue", "tips", "workSeconds",
]);
assertRequired("ShiftResultDraftEmployee", [
  "id", "firstName", "lastName", "patronymic", "simpleName",
], ["simpleName"]);
assertRequired("ConfirmShiftResultDraftResponse", ["resultId"]);
assertRequired("ShiftResult", ["id", "payments", "date", "sessionId", "calculationSource"], ["sessionId"]);
assertRequired("Payment", ["id", "employee", "percentFromRevenue", "tips", "workSeconds"]);
assertRequired("PaymentEmployee", [
  "id", "firstName", "lastName", "patronymic", "simpleName",
], ["simpleName"]);
assertRequired("SaveManualOverrideShiftResultRequest", ["companyId", "payments", "date"]);
assertRequired("ManualOverridePaymentRequest", [
  "employeeId", "percentFromRevenue", "tips", "workSeconds",
]);
assertRequired("SaveManualOverrideShiftResultResponse", ["resultId"]);
assertRequired("ShiftResultResponse", ["shiftResult", "session"], ["session"]);

assert.equal(schemas.ShiftResult.properties.sessionId.nullable, true);
assert.equal(schemas.ShiftResultResponse.properties.session.nullable, true);
assert.equal(schemas.SaveManualOverrideShiftResultRequest.properties.replacementId.nullable, true);
assert.equal(schemas.ShiftResultDraftEmployee.properties.simpleName.nullable, true);
assert.equal(schemas.PaymentEmployee.properties.simpleName.nullable, true);

for (const [schemaName, property] of [
  ["ShiftResultDraft", "id"],
  ["ShiftResultDraft", "sessionId"],
  ["ShiftResultDraftPayment", "id"],
  ["ConfirmShiftResultDraftResponse", "resultId"],
  ["ShiftResult", "id"],
  ["ShiftResult", "sessionId"],
  ["Payment", "id"],
  ["SaveManualOverrideShiftResultRequest", "replacementId"],
  ["SaveManualOverrideShiftResultRequest", "companyId"],
  ["ManualOverridePaymentRequest", "employeeId"],
  ["SaveManualOverrideShiftResultResponse", "resultId"],
]) {
  assert.equal(schemas[schemaName].properties[property].format, "uuid");
}

for (const [schemaName, property] of [
  ["ShiftResultDraft", "date"],
  ["ShiftResult", "date"],
  ["SaveManualOverrideShiftResultRequest", "date"],
]) {
  assert.equal(schemas[schemaName].properties[property].format, "date");
}

for (const schemaName of ["ShiftResultDraftPayment", "Payment", "ManualOverridePaymentRequest"]) {
  assert.equal(schemas[schemaName].properties.percentFromRevenue.format, "int32");
  assert.equal(schemas[schemaName].properties.tips.format, "int32");
  assert.equal(schemas[schemaName].properties.workSeconds.format, "int64");
}

assert.deepEqual(schemas.CalculationSource.enum, ["CHECKPOINTS", "MANUAL_OVERRIDE"]);
assert.deepEqual(document.components.parameters.PeriodType.schema.enum, ["CUSTOM", "CURRENT", "PREVIOUS"]);
assert.equal(document.components.parameters.StartDate.schema.format, "date");
assert.equal(document.components.parameters.EndDate.schema.format, "date");
assert.equal(document.components.parameters.CurrentDate.schema.format, "date");
assert.equal(schemas.ShiftResultPage.additionalProperties, true);
assert.deepEqual(schemas.ShiftResultPage.required, ["content", "number", "size", "totalElements"]);
assert.equal(schemas.LocalDateTime.format, "local-date-time");

console.log("Shift Result OpenAPI contract guarantees are satisfied.");

import assert from "node:assert/strict";
import fs from "node:fs";
import process from "node:process";
import YAML from "yaml";

const bundlePath = process.argv[2];
const document = YAML.parse(fs.readFileSync(bundlePath, "utf8"));

const methodsByPath = Object.fromEntries(
  Object.entries(document.paths)
    .filter(([path]) => path.startsWith("/api/v1/session/") || path.startsWith("/api/v1/checkpoint/"))
    .map(([path, item]) => [
      path,
      Object.keys(item).filter((key) => ["get", "post", "put", "delete"].includes(key)),
    ]),
);

assert.deepEqual(methodsByPath, {
  "/api/v1/session/get/opened": ["get"],
  "/api/v1/session/get/available/{sessionId}": ["get"],
  "/api/v1/session/get/available/all": ["get"],
  "/api/v1/session/open": ["post"],
  "/api/v1/session/recalculating": ["post"],
  "/api/v1/session/{sessionId}/close": ["put"],
  "/api/v1/session/update/time": ["put"],
  "/api/v1/checkpoint/create": ["post"],
  "/api/v1/checkpoint/update": ["post"],
  "/api/v1/checkpoint/{checkpointId}/delete": ["delete"],
});

const expectedResponses = {
  getOpenedShiftSession: ["200", "400", "401", "403", "404", "409"],
  getAvailableShiftSession: ["200", "400", "401", "403", "404", "409"],
  getAvailableShiftSessions: ["200", "400", "401", "403", "404", "409"],
  openShiftSession: ["201", "400", "401", "403", "404", "409"],
  openShiftSessionRecalculation: ["201", "400", "401", "403", "404", "409"],
  closeShiftSession: ["204", "400", "401", "403", "404", "409"],
  updateShiftSessionStart: ["204", "400", "401", "403", "404", "409"],
  createCheckpoint: ["201", "400", "401", "403", "404", "409"],
  updateCheckpoint: ["200", "400", "401", "403", "404", "409"],
  deleteCheckpoint: ["204", "400", "401", "403", "404", "409"],
};
const operations = Object.values(document.paths)
  .flatMap((path) => Object.entries(path))
  .filter(([method]) => ["get", "post", "put", "delete"].includes(method))
  .map(([, operation]) => operation)
  .filter((operation) => expectedResponses[operation.operationId]);
for (const operation of operations) {
  assert.deepEqual(
    Object.keys(operation.responses),
    expectedResponses[operation.operationId],
    `${operation.operationId} response contract drifted`,
  );
}
assert.equal(operations.length, Object.keys(expectedResponses).length);

const schemas = document.components.schemas;
assert.deepEqual(schemas.ShiftSession.required, [
  "id",
  "companyId",
  "startWorkTime",
  "date",
  "status",
  "checkpoints",
]);
assert.equal(schemas.ShiftSession.properties.id.format, "uuid");
assert.equal(schemas.ShiftSession.properties.companyId.format, "uuid");
assert.equal(schemas.ShiftSession.properties.date.format, "date");
assert.equal(schemas.ShiftSession.properties.startWorkTime.format, "time");
assert.deepEqual(schemas.ShiftSessionStatus.enum, [
  "OPENED",
  "CLOSED",
  "RECALCULATING",
  "OPENED_DRAFT",
  "RECALCULATING_DRAFT",
]);
assert.equal(schemas.LocalDateTime.format, "local-date-time");
assert.ok(schemas.LocalDateTime.pattern);
assert.equal(schemas.ShiftSessionStart.allOf[0].$ref, "#/components/schemas/LocalDateTime");
assert.equal(schemas.OpenShiftSessionRequest.properties.startWorkAt.$ref, "#/components/schemas/ShiftSessionStart");
assert.deepEqual(schemas.OpenShiftSessionRequest.required, ["companyId", "startWorkAt"]);
assert.deepEqual(schemas.OpenShiftSessionRecalculationRequest.required, ["closedSessionId"]);
assert.deepEqual(schemas.UpdateShiftSessionStartRequest.required, ["sessionId", "startWorkTime"]);

assert.deepEqual(schemas.Checkpoint.oneOf, [
  { $ref: "#/components/schemas/RegularCheckpoint" },
  { $ref: "#/components/schemas/FinalCheckpoint" },
]);
assert.equal(schemas.Checkpoint.discriminator.propertyName, "type");
assert.deepEqual(schemas.CheckpointFields.required, [
  "id",
  "tips",
  "revenue",
  "employees",
  "dateTime",
  "type",
  "metricRecords",
]);
assert.equal(schemas.CheckpointFields.properties.id.format, "uuid");
assert.equal(
  schemas.CheckpointFields.properties.dateTime.allOf[0].$ref,
  "#/components/schemas/LocalDateTime",
);
assert.equal(schemas.CheckpointFields.properties.revenue.format, "int32");
assert.equal(schemas.CheckpointFields.properties.tips.format, "int32");
assert.deepEqual(schemas.CheckpointType.enum, ["REGULAR", "FINAL"]);
assert.ok(schemas.RegularCheckpoint.description.includes("Regular Checkpoint"));
assert.ok(schemas.FinalCheckpoint.description.includes("Final Checkpoint"));
assert.equal(schemas.CheckpointEmployee.properties.userId.nullable, true);
assert.equal(schemas.CheckpointEmployee.properties.simpleName.nullable, true);

for (const requestName of ["CreateCheckpointRequest", "UpdateCheckpointRequest"]) {
  const request = schemas[requestName];
  assert.equal(request.properties.revenue.format, "int32");
  assert.equal(request.properties.tips.format, "int32");
  assert.equal(request.properties.dateTime.allOf[0].$ref, "#/components/schemas/LocalDateTime");
  assert.equal(request.properties.employeeIds.uniqueItems, true);
  assert.equal(request.properties.employeeIds.items.format, "uuid");
  assert.equal(request.properties.fieldRecords.type, "array");
}

console.log("Shift Session and Checkpoint OpenAPI contract guarantees are satisfied.");

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

const payrollEntries = Object.entries(document.paths)
  .filter(([path]) => path.startsWith("/api/v1/salary/"));
const payrollPaths = Object.fromEntries(
  payrollEntries.map(([path, item]) => [
      path,
      Object.keys(item).filter((key) => ["get", "post", "put", "delete"].includes(key)),
    ]),
);

assert.deepEqual(payrollPaths, {
  "/api/v1/salary/own/get": ["get"],
  "/api/v1/salary/staff/get": ["get"],
  "/api/v1/salary/reports-table": ["get"],
});

const operations = payrollEntries.map(([, path]) => path.get);

assert.deepEqual(
  operations.map((operation) => operation.operationId),
  ["getOwnPayroll", "getStaffPayroll", "exportStaffPayroll"],
);
for (const operation of operations) {
  assert.deepEqual(operation.tags, ["Payroll"]);
  assert.ok(operation.summary);
  assert.ok(operation.description);
  assert.deepEqual(Object.keys(operation.responses), ["200", "400", "401", "403", "404", "409"]);

  const parameters = operation.parameters.map(resolveLocalReference);
  assert.deepEqual(
    parameters.map((parameter) => parameter.name),
    ["companyId", "periodType", "start", "end", "now"],
  );
}

const parameters = document.components.parameters;
assert.equal(parameters.PayrollCompanyId.schema.format, "uuid");
assert.equal(parameters.PayrollCompanyId.required, true);
assert.deepEqual(parameters.PeriodType.schema.enum, ["CUSTOM", "CURRENT", "PREVIOUS"]);
assert.equal(parameters.PeriodType.required, true);
for (const name of ["StartDate", "EndDate", "CurrentDate"]) {
  assert.equal(parameters[name].required, false);
  assert.equal(parameters[name].schema.format, "date");
}
assert.match(parameters.PeriodType.description, /CUSTOM.*start.*end/is);
assert.match(parameters.PeriodType.description, /CURRENT.*PREVIOUS.*now/is);
assert.match(parameters.StartDate.description, /inclusive/i);
assert.match(parameters.EndDate.description, /inclusive/i);

const schemas = document.components.schemas;
assert.deepEqual(schemas.PayrollAggregation.enum, ["BY_DAY", "BY_MONTH", "BY_YEAR"]);
assert.deepEqual(schemas.Payroll.required, ["type", "elements", "summaries"]);
assert.equal(schemas.Payroll.properties.type.$ref, "#/components/schemas/PayrollAggregation");
assert.equal(schemas.Payroll.properties.elements.items.$ref, "#/components/schemas/PayrollElement");
assert.equal(schemas.Payroll.properties.summaries.items.$ref, "#/components/schemas/EmployeePayrollSummary");
assert.deepEqual(schemas.PayrollElement.required, ["date", "payments"]);
assert.equal(schemas.PayrollElement.properties.date.format, "date");
assert.equal(schemas.PayrollElement.properties.payments.items.$ref, "#/components/schemas/PayrollPayment");
assert.deepEqual(schemas.PayrollPayment.required, ["employee", "percentFromRevenue", "tips"]);
assert.equal(schemas.PayrollPayment.properties.employee.allOf[0].$ref, "#/components/schemas/PayrollEmployee");
assert.equal(schemas.PayrollPayment.properties.percentFromRevenue.format, "int32");
assert.match(schemas.PayrollPayment.properties.tips.description, /Employee Tips/);
assert.equal(schemas.PayrollPayment.properties.tips.format, "int32");
assert.deepEqual(schemas.EmployeePayrollSummary.required, [
  "employee",
  "totalPercentFromRevenue",
  "totalTips",
]);
assert.equal(schemas.EmployeePayrollSummary.properties.employee.allOf[0].$ref, "#/components/schemas/PayrollEmployee");
assert.equal(schemas.PayrollEmployee.properties.id.format, "uuid");
assert.equal(schemas.PayrollEmployee.properties.simpleName.nullable, true);

for (const operation of operations) {
  for (const status of ["400", "404", "409"]) {
    const response = resolveLocalReference(operation.responses[status]);
    assert.equal(
      response.content["application/problem+json"].schema.$ref,
      "#/components/schemas/ProblemDetail",
    );
  }
}

for (const path of ["/api/v1/salary/own/get", "/api/v1/salary/staff/get"]) {
  const response = document.paths[path].get.responses["200"];
  assert.equal(response.content["application/json"].schema.$ref, "#/components/schemas/Payroll");
}

const exportResponse = document.paths["/api/v1/salary/reports-table"].get.responses["200"];
assert.equal(
  exportResponse.content["application/octet-stream"].schema.type,
  "string",
);
assert.equal(exportResponse.content["application/octet-stream"].schema.format, "binary");
assert.equal(exportResponse.headers["Content-Disposition"].schema.type, "string");
assert.match(exportResponse.headers["Content-Disposition"].example, /attachment/);
assert.match(exportResponse.headers["Content-Disposition"].example, /reports\.xlsx/);

// Payroll routes are not paginated today; the stable shared pagination contract
// established for current paged routes must remain reusable and unchanged.
assert.equal(parameters.Page.schema.default, 0);
assert.equal(parameters.Size.schema.default, 100);
assert.equal(parameters.Sort.style, "form");

console.log("Payroll OpenAPI contract guarantees are satisfied.");

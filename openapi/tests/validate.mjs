import process from "node:process";
import SwaggerParser from "@apidevtools/swagger-parser";

const sources = process.argv.slice(2);
if (sources.length === 0) {
  throw new Error("Provide at least one OpenAPI document to validate.");
}

for (const source of sources) {
  await SwaggerParser.validate(source);
  console.log(`${source} is a valid OpenAPI document with resolved references.`);
}

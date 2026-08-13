import process from "node:process";
import SwaggerParser from "@apidevtools/swagger-parser";

const source = process.argv[2];
await SwaggerParser.validate(source);
console.log(`${source} is a valid OpenAPI document with resolved references.`);

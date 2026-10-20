#!/usr/bin/env node
/**
 * Ensures the Android library module targets Java 21, matching Capacitor 8 core plugins.
 */

import fs from "node:fs";
import path from "node:path";
import { validateCompileOptionsJava21 } from "./strip-gradle-comments.mjs";

const pluginDir = process.argv.includes("--dir")
  ? process.argv[process.argv.indexOf("--dir") + 1]
  : process.cwd();

const gradlePath = path.join(pluginDir, "android", "build.gradle");
const content = fs.readFileSync(gradlePath, "utf8");
const result = validateCompileOptionsJava21(content);

if (!result.ok) {
  console.error(`${gradlePath}: ${result.reason}.`);
  process.exit(1);
}

console.log(`${gradlePath}: compileOptions Java 21 OK`);

#!/usr/bin/env node
/**
 * Ensures the Android library module targets Java 21, matching Capacitor 8 core plugins.
 */

import fs from "node:fs";
import path from "node:path";

const pluginDir = process.argv.includes("--dir")
  ? process.argv[process.argv.indexOf("--dir") + 1]
  : process.cwd();

const gradlePath = path.join(pluginDir, "android", "build.gradle");
const content = fs.readFileSync(gradlePath, "utf8");

const blockMatch = content.match(/compileOptions\s*\{[\s\S]*?\}/);
if (!blockMatch) {
  console.error(
    `${gradlePath}: missing compileOptions block (expected Java 21 source/target).`,
  );
  process.exit(1);
}

const block = blockMatch[0];
const hasSource = /sourceCompatibility\s+JavaVersion\.VERSION_21/.test(block);
const hasTarget = /targetCompatibility\s+JavaVersion\.VERSION_21/.test(block);

if (!hasSource || !hasTarget) {
  console.error(
    `${gradlePath}: compileOptions must set sourceCompatibility and targetCompatibility to JavaVersion.VERSION_21.`,
  );
  process.exit(1);
}

console.log(`${gradlePath}: compileOptions Java 21 OK`);

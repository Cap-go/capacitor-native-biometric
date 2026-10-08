#!/usr/bin/env bash
set -euo pipefail

DESTINATION="$(
  xcrun simctl list devices available -j \
    | node -e "
const devicesByRuntime = JSON.parse(require('fs').readFileSync(0, 'utf8')).devices;
const runtimes = Object.keys(devicesByRuntime).filter((runtime) => runtime.includes('iOS')).sort().reverse();

for (const runtime of runtimes) {
  for (const device of devicesByRuntime[runtime]) {
    if (device.isAvailable && /iPhone/.test(device.name)) {
      process.stdout.write('platform=iOS Simulator,id=' + device.udid);
      process.exit(0);
    }
  }
}

process.exit(1);
"
)"

xcodebuild test -scheme CapgoCapacitorNativeBiometric -destination "$DESTINATION" -quiet

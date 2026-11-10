#!/usr/bin/env bash
set -euo pipefail


assert_packed_consumer_rules_applied() {
  local tarball="$1"
  if [ ! -f "$tarball" ]; then
    echo "Packed tarball not found: $tarball"
    exit 1
  fi

  if ! tar -tzf "$tarball" | grep -qE '(^|/)android/consumer-rules\.pro$'; then
    echo "Packed package is missing android/consumer-rules.pro"
    exit 1
  fi

  local config_file=""
  if [ -f "app/build/outputs/mapping/release/configuration.txt" ]; then
    config_file="app/build/outputs/mapping/release/configuration.txt"
  else
    config_file="$(find app/build -name configuration.txt 2>/dev/null | head -n 1 || true)"
  fi
  if [ -z "${config_file}" ] || [ ! -f "${config_file}" ]; then
    echo "R8 configuration.txt not found after assembleRelease"
    exit 1
  fi

  local extract_dir
  extract_dir="$(mktemp -d)"
  tar -xzf "$tarball" -C "$extract_dir"
  local rules_file
  rules_file="$(find "$extract_dir" -name consumer-rules.pro | head -n 1 || true)"
  if [ -z "${rules_file}" ]; then
    echo "Could not extract android/consumer-rules.pro from packed package"
    exit 1
  fi

  local missing=0
  local tokens_checked=0
  local token
  while IFS= read -r token; do
    [ -z "${token}" ] && continue
    case "${token}" in
      android.*|androidx.*|com.getcapacitor.*|java.*|javax.*|kotlin.*)
        continue
        ;;
    esac
    token="${token%.**}"
    token="${token%'$*'}"
    token="${token%\*}"
    if [ -z "${token}" ]; then
      continue
    fi
    tokens_checked=$((tokens_checked + 1))
    if ! grep -qF "${token}" "${config_file}"; then
      echo "Merged R8 configuration is missing consumer-rules token: ${token}"
      missing=1
    fi
  done < <(grep -E '^[[:space:]]*-keep' "${rules_file}" | grep -oE '[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z*][A-Za-z0-9_$*]*)+' | sort -u)

  if [ "${tokens_checked}" -eq 0 ]; then
    echo "No plugin keep tokens found in packed consumer-rules.pro"
    exit 1
  fi
  if [ "${missing}" -ne 0 ]; then
    echo "Packed consumer-rules.pro was not applied in ${config_file}"
    exit 1
  fi
  echo "Packed consumer-rules.pro is present and applied in merged R8 configuration"
}


platform="${1:-}"
case "$platform" in
  android | ios | web) ;;
  *)
    echo "Usage: $0 <android|ios|web>"
    exit 1
    ;;
esac

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
tmp_root="${RUNNER_TEMP:-$(mktemp -d)}"
pack_dir="$tmp_root/plugin-package"
test_app="$tmp_root/plugin-example-app"

cd "$repo_root"

bun run build

rm -rf "$pack_dir" "$test_app"
mkdir -p "$pack_dir" "$test_app"
bun pm pack --destination "$pack_dir" --quiet

shopt -s nullglob
packed_packages=("$pack_dir"/*.tgz)
shopt -u nullglob
if [ "${#packed_packages[@]}" -ne 1 ]; then
  echo "Expected exactly one package tarball, found ${#packed_packages[@]}"
  exit 1
fi

plugin_name="$(bun -e 'console.log(require("./package.json").name)')"
cp -R example-app/. "$test_app/"
cd "$test_app"
# example-app pins the plugin as file:../ for local dev; that path is invalid once
# copied under RUNNER_TEMP, and bun.lock can leave a broken @.. install behind.
rm -rf node_modules bun.lock
bun remove "$plugin_name"
bun add "${packed_packages[0]}"
bun run build

ensure_platform() {
  local platform="$1"
  if [ -e "$platform" ]; then
    if [ ! -d "$platform" ]; then
      echo "Expected $platform to be a directory"
      exit 1
    fi
    return
  fi

  local output
  if ! output="$(bunx cap add "$platform" 2>&1)"; then
    printf '%s\n' "$output"
    if [[ "$output" == *"$platform platform already exists"* ]]; then
      return
    fi
    exit 1
  fi
  printf '%s\n' "$output"
}

case "$platform" in
  android)
    ensure_platform android
    bunx cap sync android
    cd android
    ./gradlew build test
    ./gradlew :app:assembleRelease
    assert_packed_consumer_rules_applied "${packed_packages[0]}"
    mapping_file="$test_app/android/app/build/outputs/mapping/release/mapping.txt"
    if [ ! -f "$mapping_file" ]; then
      echo "Expected R8 mapping at $mapping_file"
      exit 1
    fi
    if ! grep -qE '^ee\.forgr\.biometric\.NativeBiometric -> ee\.forgr\.biometric\.NativeBiometric:' "$mapping_file"; then
      echo "NativeBiometric missing from R8 mapping; consumer rules may be wrong"
      exit 1
    fi
    ;;
  ios)
    ensure_platform ios
    bunx cap sync ios
    rm -rf "$HOME/Library/Caches/org.swift.swiftpm/artifacts"/https___github_com_ionic_team_capacitor_swift_pm_releases_download_*
    xcodebuild \
      -project ios/App/App.xcodeproj \
      -scheme App \
      -destination generic/platform=iOS \
      -clonedSourcePackagesDirPath "$tmp_root/plugin-example-swiftpm" \
      -derivedDataPath "$tmp_root/plugin-example-derived-data" \
      CODE_SIGNING_ALLOWED=NO
    ;;
  web)
    ;;
esac

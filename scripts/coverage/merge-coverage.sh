#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
OUTPUT_DIR="${1:-${ROOT_DIR}/app/build/reports/jacoco/full}"\

printf '[merge-coverage] Working directory: %s\n' "${ROOT_DIR}"
./gradlew -p "${ROOT_DIR}" jacocoFullReport

if [ -d "${OUTPUT_DIR}" ]; then
  printf '[merge-coverage] Coverage report available at %s\n' "${OUTPUT_DIR}"
else
  printf '[merge-coverage] Coverage report not generated at %s\n' "${OUTPUT_DIR}" >&2
  exit 1
fi

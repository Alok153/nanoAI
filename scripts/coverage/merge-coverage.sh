#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
OUTPUT_DIR="${1:-${ROOT_DIR}/app/build/reports/jacoco/full}"

REPORT_DIR="${ROOT_DIR}/app/build/reports/jacoco/full"
EXEC_SOURCES=(
  "${ROOT_DIR}/app/build/jacoco"
  "${ROOT_DIR}/app/build/outputs/unit_test_code_coverage"
  "${ROOT_DIR}/app/build/outputs/code_coverage"
)

printf '[merge-coverage] Working directory: %s\n' "${ROOT_DIR}"
"${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" \
  testDebugUnitTest \
  connectedDebugAndroidTest \
  jacocoFullReport \
  coverageMarkdownSummary \
  verifyCoverageThresholds

if [ ! -d "${REPORT_DIR}" ]; then
  printf '[merge-coverage] Coverage report not generated at %s\n' "${REPORT_DIR}" >&2
  exit 1
fi

printf '[merge-coverage] Copying artifacts into %s\n' "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}/html" "${OUTPUT_DIR}/execution-data"

if [ -f "${REPORT_DIR}/jacocoFullReport.xml" ]; then
  cp "${REPORT_DIR}/jacocoFullReport.xml" "${OUTPUT_DIR}/jacocoFullReport.xml"
fi

if [ -d "${REPORT_DIR}/html" ]; then
  cp -R "${REPORT_DIR}/html/." "${OUTPUT_DIR}/html/"
fi

if [ -f "${ROOT_DIR}/app/build/reports/jacoco/full/summary.md" ]; then
  cp "${ROOT_DIR}/app/build/reports/jacoco/full/summary.md" "${OUTPUT_DIR}/summary.md"
fi

if [ -f "${ROOT_DIR}/app/build/coverage/thresholds.md" ]; then
  cp "${ROOT_DIR}/app/build/coverage/thresholds.md" "${OUTPUT_DIR}/thresholds.md"
fi

if [ -f "${ROOT_DIR}/app/build/coverage/summary.json" ]; then
  cp "${ROOT_DIR}/app/build/coverage/summary.json" "${OUTPUT_DIR}/summary.json"
fi

for source_root in "${EXEC_SOURCES[@]}"; do
  if [ -d "${source_root}" ]; then
    while IFS= read -r -d '' file; do
      cp "${file}" "${OUTPUT_DIR}/execution-data/"
    done < <(find "${source_root}" -type f \( -name '*.exec' -o -name '*.ec' \) -print0)
  fi
done

printf '[merge-coverage] Coverage artifacts ready at %s\n' "${OUTPUT_DIR}"
printf '[merge-coverage] Summary files:\n'
[ -f "${OUTPUT_DIR}/summary.md" ] && printf '  - summary.md: %s bytes\n' "$(wc -c < "${OUTPUT_DIR}/summary.md")"
[ -f "${OUTPUT_DIR}/summary.json" ] && printf '  - summary.json: %s bytes\n' "$(wc -c < "${OUTPUT_DIR}/summary.json")"
[ -f "${OUTPUT_DIR}/jacocoFullReport.xml" ] && printf '  - jacocoFullReport.xml: %s bytes\n' "$(wc -c < "${OUTPUT_DIR}/jacocoFullReport.xml")"

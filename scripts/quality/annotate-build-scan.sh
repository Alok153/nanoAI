#!/usr/bin/env bash
set -euo pipefail

ENDPOINT_DEFAULT="https://build.nanoai.dev/build-scans/annotate"
ENDPOINT="${NANOAI_SCAN_ENDPOINT:-$ENDPOINT_DEFAULT}"
SCAN_URL=""
CI_URL=""
GATE_SUMMARY_PATH=""
SLOW_TASKS_PATH=""
DRY_RUN=false

print_usage() {
  cat <<'EOF'
Usage: scripts/quality/annotate-build-scan.sh --scan-url <url> [--ci-url <url>] \
       [--gate-summary <path>] [--slow-tasks <path>] [--dry-run]

Posts custom metadata to the build-scan annotation endpoint. Set NANOAI_SCAN_ENDPOINT to
override the default API base.

  --scan-url       Build scan URL to annotate (required)
  --ci-url         CI job URL associated with the build
  --gate-summary   Path to JSON file with quality gate results (e.g., app/build/coverage/thresholds.json)
  --slow-tasks     Path to JSON file describing slow Gradle tasks (task -> duration millis)
  --dry-run        Print payload without executing the HTTP POST
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scan-url)
      SCAN_URL="$2"
      shift 2
      ;;
    --ci-url)
      CI_URL="$2"
      shift 2
      ;;
    --gate-summary)
      GATE_SUMMARY_PATH="$2"
      shift 2
      ;;
    --slow-tasks)
      SLOW_TASKS_PATH="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      print_usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$SCAN_URL" ]]; then
  echo "ERROR: --scan-url is required." >&2
  print_usage >&2
  exit 1
fi

BRANCH_NAME="$(git rev-parse --abbrev-ref HEAD)"
GIT_SHA="$(git rev-parse HEAD)"

if [[ -n "$GATE_SUMMARY_PATH" && ! -f "$GATE_SUMMARY_PATH" ]]; then
  echo "ERROR: Gate summary file not found at $GATE_SUMMARY_PATH" >&2
  exit 1
fi

if [[ -n "$SLOW_TASKS_PATH" && ! -f "$SLOW_TASKS_PATH" ]]; then
  echo "ERROR: Slow task file not found at $SLOW_TASKS_PATH" >&2
  exit 1
fi

PAYLOAD="$(SCAN_URL="$SCAN_URL" \
  BRANCH_NAME="$BRANCH_NAME" \
  GIT_SHA="$GIT_SHA" \
  CI_URL="$CI_URL" \
  GATE_SUMMARY_PATH="$GATE_SUMMARY_PATH" \
  SLOW_TASKS_PATH="$SLOW_TASKS_PATH" \
  python - <<'PY'
import json
import os

scan_url = os.environ["SCAN_URL"]
branch = os.environ["BRANCH_NAME"]
sha = os.environ["GIT_SHA"]
ci_url = os.environ.get("CI_URL")
summary_path = os.environ.get("GATE_SUMMARY_PATH") or ""
slow_tasks_path = os.environ.get("SLOW_TASKS_PATH") or ""

def read_json(path: str):
    if not path:
        return None
    with open(path, "r", encoding="utf-8") as handle:
        text = handle.read().strip()
        if not text:
            return None
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return {"raw": text}

data = {
    "scanUrl": scan_url,
    "branch": branch,
    "commit": sha,
}

if ci_url:
    data["ciUrl"] = ci_url

gate_summary = read_json(summary_path)
if gate_summary is not None:
    data["qualityGates"] = gate_summary

slow_tasks = read_json(slow_tasks_path)
if slow_tasks is not None:
    data["slowTasks"] = slow_tasks

print(json.dumps(data))
PY
)"

if $DRY_RUN; then
  echo "Payload preview:"
  echo "$PAYLOAD"
  echo "POST $ENDPOINT"
  exit 0
fi

curl --fail --silent --show-error \
  -X POST "$ENDPOINT" \
  -H 'Content-Type: application/json' \
  -d "$PAYLOAD"

echo "\nSUCCESS: Build scan annotated at $SCAN_URL"

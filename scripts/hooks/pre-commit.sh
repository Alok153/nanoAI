#!/usr/bin/env bash
set -euo pipefail

# Enforce quality gates prior to commit.
# Supports --dry-run to print commands without executing them.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

DRY_RUN=false
VERBOSE=false

print_usage() {
  cat <<'EOF'
Usage: scripts/hooks/pre-commit.sh [--dry-run] [--verbose]

Runs formatting, static analysis, unit tests, and coverage verification before allowing commits.
  --dry-run   Print the commands that would run without executing them
  --verbose   Show Gradle output without suppressing logging
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      ;;
    --verbose|-v)
      VERBOSE=true
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
  shift
done

# Gradle arguments shared by all commands. Use --console=plain to keep hook output readable.
GRADLE_ARGS=(--console=plain)
if ! $VERBOSE; then
  GRADLE_ARGS+=(--quiet)
fi

commands=(
  "spotlessCheck"
  "detekt detektMain detektTest"
  "testDebugUnitTest"
  ":app:verifyCoverageThresholds"
)

run_command() {
  local task_string="$1"
  echo "\n→ ./gradlew ${GRADLE_ARGS[*]} $task_string"
  if $DRY_RUN; then
    echo "(dry run)"
    return 0
  fi
  read -r -a task_parts <<< "$task_string"
  if ! ./gradlew "${GRADLE_ARGS[@]}" "${task_parts[@]}"; then
    echo "\n✖ Command failed: ./gradlew ${GRADLE_ARGS[*]} $task_string" >&2
    case "$task_string" in
      spotlessCheck*)
        echo "Hint: Run ./gradlew spotlessApply to auto-format sources." >&2
        ;;
      detekt*)
        echo "Hint: Review detekt reports under build/reports/detekt." >&2
        ;;
      testDebugUnitTest*)
        echo "Hint: Inspect failing tests via ./gradlew testDebugUnitTest --stacktrace." >&2
        ;;
      :app:verifyCoverageThresholds*)
        echo "Hint: Open app/build/coverage/summary.md to find under-covered layers." >&2
        ;;
    esac
    exit 1
  fi
}

for cmd in "${commands[@]}"; do
  run_command "$cmd"
done

echo "\n✔ All quality gates passed. Proceed with git commit."

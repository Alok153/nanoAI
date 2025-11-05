#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOOKS_DIR="$REPO_ROOT/.git/hooks"
SOURCE="$REPO_ROOT/scripts/hooks/pre-commit.sh"
TARGET="$HOOKS_DIR/pre-commit"
FORCE=false

print_usage() {
  cat <<'EOF'
Usage: scripts/hooks/install-hooks.sh [--force]

Symlinks the repository pre-commit hook to .git/hooks/pre-commit.
  --force   Replace an existing pre-commit hook
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE=true
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

if [[ ! -d "$HOOKS_DIR" ]]; then
  echo "ERROR: .git/hooks directory not found. Did you clone the repository?" >&2
  exit 1
fi

if [[ ! -f "$SOURCE" ]]; then
  echo "ERROR: Source hook not found at $SOURCE" >&2
  exit 1
fi

chmod +x "$SOURCE"

if [[ -e "$TARGET" || -L "$TARGET" ]]; then
  if ! $FORCE; then
    echo "WARNING: A pre-commit hook already exists at $TARGET." >&2
    echo "         Re-run with --force to replace it." >&2
    exit 1
  fi
  rm -f "$TARGET"
fi

ln -s "$SOURCE" "$TARGET"

if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]]; then
  echo "INFO: Detected Windows environment; if symlinks are disabled, copy the hook manually." >&2
fi

echo "SUCCESS: Pre-commit hook installed. Run scripts/hooks/pre-commit.sh --dry-run to verify."

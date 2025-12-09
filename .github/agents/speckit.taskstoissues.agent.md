---
description: Convert existing tasks into actionable, dependency-ordered GitHub issues for the feature based on available design artifacts.
tools: ['github/github-mcp-server/issue_write']
---

## Context
- Only proceed for GitHub remotes; never create issues elsewhere.

## Preconditions
1. Run `.specify/scripts/bash/check-prerequisites.sh --json --require-tasks --include-tasks`; parse `FEATURE_DIR`, `TASKS` path.
2. Get git remote: `git config --get remote.origin.url`. Abort unless it is a GitHub URL.
3. Load `tasks.md`.

## Workflow
1. For each task, create a GitHub issue via the MCP server with clear title, description, dependencies (if implied by task order), and file paths.
2. Preserve task IDs and `[USx]` context in issue text; link back to `tasks.md` path.
3. Do not modify repository files.

## Output
- Summary of issues created and any tasks skipped due to validation errors.

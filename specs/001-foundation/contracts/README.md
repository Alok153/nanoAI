# Contracts Directory Index

This directory contains all operational contracts, API specifications, schemas, and test definitions for the nanoAI application.

## 📋 Contract Files Overview

### API Specifications (OpenAPI)

| File | Purpose | Scope | Stakeholders |
|------|---------|-------|--------------|
| [`openapi.yaml`](openapi.yaml) | Infrastructure/management APIs | Model manifests, credential rotation, verification | Backend, DevOps, Security |
| [`llm-gateway.yaml`](llm-gateway.yaml) | Cloud LLM API gateway/proxy | OpenAI/Gemini API integration | Frontend, API consumers |
| [`import-export-openapi.yaml`](import-export-openapi.yaml) | Local import/export functionality | Backup/restore operations | Frontend, Data migration |

### Data Schemas (JSON Schema)

| File | Purpose | Usage |
|------|---------|-------|
| [`model-manifest.json`](model-manifest.json) | Model package metadata schema | Validates downloadable model manifests |
| [`coverage-report.schema.json`](coverage-report.schema.json) | Test coverage reporting schema | Structures CI coverage reports |
| [`sample-backup.json`](sample-backup.json) | Import/export data example | Documents backup JSON structure |

### Behavioral Contracts (Markdown)

| File | Purpose | Scope |
|------|---------|-------|
| [`ui-shell-contracts.md`](ui-shell-contracts.md) | Unified UI shell & command palette | Navigation, offline handling, progress management |
| [`contract-tests.md`](contract-tests.md) | API contract test definitions | Infrastructure API testing requirements |

## 🔗 Relationships

### File Dependencies
```
openapi.yaml
├── contract-tests.md (test requirements)
└── model-manifest.json (schema reference)

llm-gateway.yaml
└── Frontend integration (direct API consumption)

import-export-openapi.yaml
├── sample-backup.json (example data)
└── Frontend backup/restore features

ui-shell-contracts.md
└── Frontend UI implementation
```

### Cross-References
- `ui-shell-contracts.md` references connectivity and progress concepts from broader offline handling
- `contract-tests.md` validates `openapi.yaml` endpoints
- Schema files (`*.json`) are referenced by their corresponding OpenAPI specs

## 📚 Usage Guidelines

### For API Consumers
- **Cloud APIs**: Use `llm-gateway.yaml` for external LLM integration
- **Import/Export**: Follow `import-export-openapi.yaml` for backup operations
- **Infrastructure**: Reference `openapi.yaml` for internal model management

### For Frontend Developers
- **UI Behavior**: Follow `ui-shell-contracts.md` for shell interactions
- **Data Formats**: Use schemas in `*.json` files for validation
- **Offline UX**: Reference offline scenarios in `ui-shell-contracts.md`

### For Backend/Test Developers
- **Contract Tests**: Implement tests defined in `contract-tests.md`
- **API Validation**: Use OpenAPI specs for request/response validation
- **Data Integrity**: Validate against JSON schemas

## 🔧 Maintenance Notes

- **OpenAPI files** serve different stakeholders and are intentionally separate
- **UI contracts** were consolidated from multiple files to reduce redundancy
- **Schema files** are referenced by OpenAPI specs and should be kept in sync
- **Test definitions** specify contract validation requirements

## 📝 File Change History

- **2025-01-XX**: Consolidated UI contracts from `command-palette-ui-tests.md` + `shell-interactions.md` → `ui-shell-contracts.md`
- **2025-01-XX**: Merged offline progress scenarios into `ui-shell-contracts.md`
- **2025-01-XX**: Created this index for better organization

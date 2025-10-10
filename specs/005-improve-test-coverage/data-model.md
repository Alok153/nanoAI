# Data Model: Improve Test Coverage for nanoAI

## Entities

### CoverageSummary
- **Purpose**: Represents aggregated coverage metrics shared with stakeholders after each CI run.
- **Attributes**:
  - `buildId` (String, unique): CI build identifier.
  - `timestamp` (Instant): Generation time.
  - `layerMetrics` (Map<TestLayer, CoverageMetric>): Coverage value per layer (ViewModel, UI, Data).
  - `thresholds` (Map<TestLayer, Double>): Target thresholds (VM 75, UI 65, Data 70).
  - `trendDelta` (Map<TestLayer, Double>): Change vs previous build in percentage points.
  - `riskItems` (List<RiskRegisterItemRef>): Unresolved risks scoped to this summary.
- **Relationships**: Owns many `CoverageTrendPoint`; references multiple `RiskRegisterItem` entries.
- **Validation Rules**: Coverage values must be 0–100; deltas computed from chronological ordering; buildId unique per branch.

### CoverageTrendPoint
- **Purpose**: Captures historical coverage values for visualization and regression detection.
- **Attributes**:
  - `buildId` (String)
  - `layer` (TestLayer)
  - `coverage` (Double)
  - `threshold` (Double)
  - `recordedAt` (Instant)
- **Relationships**: Belongs to `CoverageSummary` timeline (one-to-many).
- **Validation Rules**: `recordedAt` monotonic; threshold must match summary threshold for the same layer.

### TestSuiteCatalogEntry
- **Purpose**: Catalog of automated suites tied to business journeys.
- **Attributes**:
  - `suiteId` (String, unique)
  - `owner` (String)
  - `layer` (TestLayer)
  - `journey` (String, e.g., "Chat send message")
  - `coverageContribution` (Double): percentage points added when passing.
  - `riskTags` (Set<String>): Flags such as `offline`, `ai`, `accessibility`.
- **Relationships**: Each entry may mitigate one or more `RiskRegisterItem`s.
- **Validation Rules**: `coverageContribution` ≥ 0; `owner` must map to accountable team in risk register.

### RiskRegisterItem
- **Purpose**: Tracks known coverage gaps and mitigation plans.
- **Attributes**:
  - `riskId` (String, unique)
  - `layer` (TestLayer)
  - `description` (String)
  - `severity` (Enum: Low/Medium/High/Critical)
  - `targetBuild` (String): build where mitigation expected.
  - `status` (Enum: Open/InProgress/Resolved/Deferred)
  - `mitigation` (String): summary of planned or completed fix.
- **Relationships**: Linked to zero or more `TestSuiteCatalogEntry` via mitigation references.
- **Validation Rules**: Critical severity requires non-null `targetBuild`; `status=Resolved` requires linked suite or documented rationale.

### TestLayer (Enum)
- **Values**: `VIEW_MODEL`, `UI`, `DATA`
- **Description**: Canonical layer vocabulary shared across reports and dashboards.

### CoverageMetric
- **Attributes**:
  - `coverage` (Double)
  - `threshold` (Double)
  - `status` (Enum: `BELOW_TARGET`, `ON_TARGET`, `EXCEEDS_TARGET`)
- **Validation Rules**: Status derived from coverage vs threshold.

### HuggingFaceAuthCoordinator
- **Purpose**: Manages OAuth authentication flow with Hugging Face, handling state transitions and token storage.
- **Attributes**:
  - `authState` (HuggingFaceAuthState): Current authentication status.
  - `oauthService` (HuggingFaceOAuthService): Handles OAuth requests.
- **Relationships**: Interacts with `HuggingFaceCredentialRepository` for secure storage.

### RefreshModelCatalogUseCase
- **Purpose**: Refreshes the local model catalog from remote sources, ensuring up-to-date model availability.
- **Attributes**:
  - `repository` (ModelCatalogRepository): Data access for catalog operations.
- **Relationships**: Triggers updates in `ModelCatalogLocalDataSource`.

### ModelCatalogRepository
- **Purpose**: Repository for model catalog data, providing access to local and remote model information.
- **Attributes**:
  - `localDataSource` (ModelCatalogLocalDataSource): Local storage.
  - `remoteDataSource` (ModelCatalogRemoteDataSource): Remote fetching.
- **Relationships**: Supplies data to `ModelLibraryViewModel`.

## Data Lifecycle
- Coverage summaries generated post-CI, persisted as artifacts for 90 days, optionally stored in analytics bucket.
- Risk register reviewed weekly; resolved items archived after 30 days of stability.
- Catalog entries updated when new suites land; coverage contributions recalculated via JaCoCo diff.

## Assumptions & Notes
- All identifiers stored as lowercase kebab-case strings.
- Historical data retention subject to storage policy; cleaning job purges artifacts beyond retention window.
- Access to reports gated by CI permissions; no PII stored in any artifact.

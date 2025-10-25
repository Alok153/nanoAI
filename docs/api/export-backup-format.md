# Export Backup Format

## Schema

The export backup is a JSON file containing all user data for migration/backup.

```typescript
interface ExportBackup {
  version: string;           // Export format version (e.g., "1.0")
  timestamp: string;         // ISO 8601 timestamp
  device: string;            // Device model/identifier
  conversations: Thread[];   // All chat threads
  personas: Persona[];       // All persona profiles
  apiProviders: ApiProvider[]; // All API configurations
  privacy: PrivacySettings;  // Privacy preferences
  modelCatalog: Model[];     // Installed model references
}

interface Thread {
  id: string;                // UUID
  title: string | null;
  personaId: string;         // UUID reference
  createdAt: string;
  updatedAt: string;
  isArchived: boolean;
  messages: Message[];
  switches: PersonaSwitch[];
}

interface Message {
  id: string;
  role: "user" | "assistant" | "system";
  content: string;
  timestamp: string;
  latencyMs: number | null;
  errorCode: string | null;
}

interface PersonaSwitch {
  id: string;
  fromPersonaId: string | null;
  toPersonaId: string;
  timestamp: string;
  action: "CONTINUE_THREAD" | "START_NEW_THREAD";
}

interface Persona {
  id: string;
  name: string;
  systemPrompt: string;
  temperature: number;
  topP: number;
  modelPreference: string | null;
  createdAt: string;
}

interface ApiProvider {
  id: string;
  name: string;
  baseUrl: string;
  apiKey: string | null;  // REDACTED in export by default
  isDefault: boolean;
  status: string;
  quotaResetAt: string | null;
  createdAt: string;
}

interface PrivacySettings {
  telemetryOptIn: boolean;
  retentionDays: number;
  consentTimestamp: string | null;
}

interface Model {
  id: string;
  name: string;
  version: string;
  provider: string;
  localPath: string;
}
```

## Example Export Structure

```json
{
  "version": "1.0.0",
  "timestamp": "2025-01-01T00:00:00Z",
  "device": {
    "model": "Pixel 8",
    "osVersion": "Android 14"
  },
  "conversations": [],
  "personas": [
    {
      "personaId": "11111111-1111-1111-1111-111111111111",
      "name": "Default",
      "systemPrompt": "You are a helpful assistant.",
      "defaultModelPreference": null
    }
  ],
  "apiProviders": [
    {
      "id": "openai-test",
      "name": "OpenAI Test",
      "baseUrl": "https://api.openai.com/v1",
      "apiKey": "REDACTED",
      "isDefault": true,
      "status": "ACTIVE",
      "quotaResetAt": "2025-01-02T00:00:00Z",
      "createdAt": "2024-12-01T12:00:00Z"
    }
  ],
  "privacy": {
    "telemetryOptIn": false,
    "shareCrashReports": false
  },
  "modelCatalog": [],
  "settings": {
    "theme": "system",
    "telemetryOptIn": false
  }
}
```

## Export Options

When exporting, users can choose:

1. **Full Export** (default)
   - All conversations and messages
   - All personas
   - API providers (keys REDACTED)
   - Privacy settings
   - Model references (not files)

2. **Minimal Export**
   - Only personas
   - Privacy settings
   - API provider configs (no keys)

3. **Conversations Only**
   - All threads and messages
   - Referenced personas
   - No API configs or models

## Security Considerations

- **API Keys**: REDACTED by default (user must manually re-enter after import)
- **Model Files**: Not included (too large; references only)
- **Encryption**: Export file is plain JSON (user can encrypt externally)
- **PII**: Contains user message content (warn before export)

## File Naming

Exports are saved with timestamp:

```
nanoai-backup-2025-10-01-153000.json

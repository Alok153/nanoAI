This guide defines the essential commenting standards for the nanoAI codebase. Focus on **why**, not **what** - the code should be self-documenting for the "what."

---

## Core Principles

1. **Write for Humans** - Explain why, not what
2. **Be Concise** - Skip lengthy explanations in implementation details
3. **Keep Comments Updated** - Outdated comments are worse than no comments
4. **Use Standard Markers** - Use TODO/FIXME/NOTE/HACK/OPTIMIZE for searchable comments
5. **Scale with File Size** - Less detailed comments in larger files (>200 lines)

---

## Standard Comment Markers

Use these searchable markers consistently:

- `// TODO:` - Work to be done
- `// FIXME:` - Known bugs/issues that need fixing
- `// NOTE:` - Critical context or important information
- `// HACK:` - Temporary workaround to be replaced
- `// OPTIMIZE:` - Performance improvement opportunity

### Examples:
```kotlin
// TODO: Add retry logic with exponential backoff
suspend fun fetchData() { ... }

// FIXME: Race condition when multiple threads access cache
private val cache = mutableMapOf<String, Data>()

// NOTE: Must run on Main thread due to DataStore constraints
suspend fun savePreference() { ... }

// HACK: Delaying 100ms to avoid race condition in DataStore
delay(100)

// OPTIMIZE: Cache compiled regex patterns (saves ~15ms per call)
val pattern = Regex("\\d+")
```

---

## KDoc Guidelines

Required for all **public** APIs only:

```kotlin
/**
 * Brief one-line summary ending with period.
 *
 * Optional detailed description if needed.
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 */
```

### File Size Guidelines:
- **<200 lines**: Full KDoc OK
- **200-500 lines**: Brief KDoc (1-2 sentences), focus on markers
- **>500 lines**: Minimal KDoc (one-liner), heavy use of markers

---

## What to Comment

✅ **DO comment:**
- Public APIs (always use KDoc)
- Non-obvious design decisions
- Complex algorithms
- Business rules
- Platform-specific quirks
- Performance/security/privacy considerations
- Workarounds and magic numbers

---

## What NOT to Comment

❌ **DON'T comment:**
- Obvious code
- What code does (the code shows this)
- Every variable (only non-obvious ones)
- Generated code
- Closing braces
- Version history (use Git)
- Author names (use Git)

---

## Quick Reference

| Marker | Purpose | Example |
|--------|---------|---------|
| `// TODO:` | Work to be done | `// TODO: Add pagination support` |
| `// FIXME:` | Known bug | `// FIXME: Memory leak in observer` |
| `// NOTE:` | Critical info | `// NOTE: Must run on Main thread` |
| `// HACK:` | Workaround | `// HACK: Working around Retrofit bug` |
| `// OPTIMIZE:` | Performance | `// OPTIMIZE: Cache results` |

### Priority Order:
1. Action markers (TODO/FIXME/NOTE/HACK/OPTIMIZE) - Always add
2. Public API KDoc - Brief summaries required
3. Critical context - Only when non-obvious
4. Everything else - Skip, keep files lean

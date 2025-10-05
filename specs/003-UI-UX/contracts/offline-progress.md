# Contract: Offline Handling & Progress Center Integration

## Purpose
Describe expectations for how offline state, queued jobs, and reconnect flows behave across the unified UI.

## Domains Covered
- Connectivity banner rendering and dismissal
- Progress center queue updates
- Offline job enqueue + retry semantics

## Scenarios
| Scenario | Given | When | Then |
|----------|-------|------|------|
| `OfflineBanner_showsWithQueuedActions` | Device transitions to OFFLINE, there are 2 queued jobs | Connectivity monitor emits `OFFLINE` | Banner visible with message "Working offline" and CTA "View queue (2)"; right drawer badge increments |
| `OfflineBanner_dismissPersists` | Banner shown, user dismisses within session | User taps dismiss | Banner hidden for 30 minutes; DataStore records `lastDismissedAt`; reappears immediately if more jobs added |
| `ProgressCenter_listsQueuedJobs` | Two downloads paused due to offline | User opens progress center | List displays each job with progress 0%, status "Waiting for connection", retry button disabled |
| `Reconnect_flushesQueue` | Offline jobs exist, network restores | Connectivity emits `ONLINE` | WorkManager enqueues pending jobs, banner replaced with "Syncing..." state, eventually hidden once queue empty |
| `Retry_failedJob` | Job failed due to quota | User taps retry in progress center | ViewModel checks quota (via repository). If allowed, re-enqueue and show toast "Retry scheduled"; otherwise display inline error |

## Metrics Tracking
- Log connectivity transition timestamps for macrobenchmark.
- Record queue length when palette opens to enable quick commands (non-blocking).

## Testing Hooks
- Provide fake `ConnectivityMonitor` + `ProgressRepository` to deterministically emit job states.
- Compose semantics IDs: `"connectivity_banner"`, `"progress_list"`, `"progress_retry_button"`.

## Edge Cases
- Banner MUST not appear when command palette is visible to avoid focus trap; palette invocation hides banner temporarily.
- Ensure queue updates do not block UI thread; snapshot flows run on `Dispatchers.Default`.

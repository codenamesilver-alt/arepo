# Tasks 17 & 18 Implementation Report

## Task 17: Grace Period + Dark Mode

### Files Modified
- `core/domain/model/DomainModels.kt` — Changed `isInGracePeriod` from hardcoded `false` to `fun isInGracePeriod(gracePeriodDays: Int): Boolean` that checks if current date falls within the grace window after expiry.
- `ui/theme/Theme.kt` — Added `darkModeEnabled: Boolean?` parameter to `GarmentoTheme`. When non-null it overrides system dark theme setting.

### Files Created
- `feature/dashboard/GracePeriodBanner.kt` — Composable banner showing warning when subscription is in grace period with countdown.
- `feature/settings/SettingsRepository.kt` — DataStore-backed repository for grace period days, dark mode, username, and logout.
- `feature/settings/SettingsViewModel.kt` — ViewModel exposing SettingsUiState with grace period slider, dark mode toggle, and logout action.
- `feature/settings/SettingsScreen.kt` — Full settings screen with: username display, grace period slider (0-7 days), dark mode toggle switch, and logout button.

### Files Modified (wiring)
- `navigation/NavGraph.kt` — Replaced placeholder `PlaceholderSettingsScreen` with real `SettingsScreen`. Added logout navigation to Login. Removed unused placeholder composables and imports.
- `MainActivity.kt` — Injected `DataStore<Preferences>`, reads `DARK_MODE_KEY` via `collectAsStateWithLifecycle`, passes `darkModeEnabled` to `GarmentoTheme`.

## Task 18: Offline Order Entry

### Files Modified
- `feature/orders/OrdersViewModel.kt` — Added `ConnectivityManager` and `PendingSyncDao` injection. `placeOrder()` now checks `isNetworkAvailable()`: if offline, serializes order as JSON and saves to `PendingSync` table with `table_name="orders"`, returns success with offline message. If online, proceeds normally.

### Files Created
- `core/sync/SyncWorker.kt` — `@HiltWorker` extending `CoroutineWorker`. Processes all pending sync items on network restoration, inserting into appropriate Supabase tables by `table_name`. Marks synced items. Retries on failure.

### Files Modified (wiring)
- `GarmentoApp.kt` — Added `scheduleSyncWorker()` with 15-minute `PeriodicWorkRequest` requiring network connectivity. Called in `onCreate()` after `scheduleReminderWorker()`.

## Build Result
`./gradlew assembleDebug` — **BUILD SUCCESSFUL** (only deprecation warnings for statusBarColor)

## Commit
`feat: add grace period, dark mode, and offline order entry`

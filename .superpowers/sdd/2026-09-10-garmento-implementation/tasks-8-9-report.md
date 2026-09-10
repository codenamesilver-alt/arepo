# Tasks 8 & 9 Implementation Report

**Date:** 2026-09-10
**Commit:** `344cb13` — `feat: add login screen and plans CRUD feature`

## Task 8: Auth Feature (Login)

### Files Created

| File | Purpose |
|------|---------|
| `feature/auth/AuthRepository.kt` | DataStore session management (login/logout/isLoggedIn/getUsername) with hardcoded admin/admin |
| `feature/auth/AuthViewModel.kt` | LoginUiState, onUsernameChange, onPasswordChange, login with loading/error/success |
| `feature/auth/LoginScreen.kt` | Compose screen: username/password fields, login button, error text, loading spinner |

### Key Details
- Session persisted via DataStore Preferences (SESSION_KEY, USERNAME_KEY)
- Validation: blank username/password → error message
- Invalid credentials → "Invalid credentials" error
- Success triggers `onLoginSuccess` callback → navigates to Dashboard, pops Login
- Focus management: password field auto-receives focus on Next, keyboard dismisses on Done

## Task 9: Plans Feature

### Files Created

| File | Purpose |
|------|---------|
| `feature/plans/PlanRepository.kt` | CRUD with Room + Supabase sync, domain↔entity mapping, SupabasePlan DTO |
| `feature/plans/PlansViewModel.kt` | PlansUiState, PlanEditUiState, loadPlans, toggleActive, save with validation |
| `feature/plans/PlansListScreen.kt` | LazyColumn of PlanCards with active toggle Switch, FAB for add |
| `feature/plans/PlanEditScreen.kt` | Form fields (name, duration, amount, clothes, active), validation, save button |

### Key Details
- Supabase sync: `syncFromRemote()` fetches all plans, replaces local cache
- Offline fallback: all operations save to Room first, Supabase call is best-effort
- Validation: name required, duration/amount/clothes must be positive numbers
- Toggle active: updates Room + Supabase in one call
- Domain↔Entity mapping via extension functions in PlanRepository

## Build Fix
- Added `material-icons-core` dependency (missing from build.gradle.kts)
- Changed `selectList()` → `select()` for supabase-kt v3 compatibility

## NavGraph Updates
- Replaced `PlaceholderLoginScreen` → `LoginScreen`
- Replaced `PlaceholderPlansListScreen` → `PlansListScreen`
- Replaced `PlaceholderPlanEditScreen` → `PlanEditScreen`
- Removed unused placeholder composable functions

## Verification
- `./gradlew assembleDebug` → BUILD SUCCESSFUL (42 tasks, 17 executed)

# Tasks 6 + 7 Report — MainActivity Shell, Theme, Navigation

Date: 2026-09-10

## Task 6: MainActivity Shell + Theme

Files created/modified:
- `app/src/main/java/com/example/garmento/ui/theme/Theme.kt` — replaced template `GARMENTOTheme` with `GarmentoTheme` using Material 3 branded light/dark color schemes (blue primary `0xFF1565C0` light, `0xFF9ECAFF` dark), dynamic color on Android 12+, status bar/icon tint via `WindowCompat`.
- `app/src/main/java/com/example/garmento/MainActivity.kt` — `@AndroidEntryPoint`, `enableEdgeToEdge()`, `setContent { GarmentoTheme { ... } }`. Navigation host added in Task 7.

## Task 7: Navigation Setup

Files created/modified:
- `app/src/main/java/com/example/garmento/navigation/Screen.kt` — sealed class with all 12 routes: Login, Dashboard, PlansList, PlanEdit (optional `planId` query arg), CustomerList, CustomerDetail, NewSubscription, PlaceOrder, OrderEdit, Reports, Reminders, Settings. `createRoute` helpers for parameterized destinations.
- `app/src/main/java/com/example/garmento/navigation/NavGraph.kt` — `NavHost` registering all destinations with correct `navArgument` setup (nullable `planId` with default null; required `customerId`/`orderId`). Each destination wired to a private placeholder composable whose parameter signature mirrors the contract the real feature screens must expose (Tasks 8+), making swap-in drop-in.
- `app/src/main/java/com/example/garmento/MainActivity.kt` — now hosts `rememberNavController()` + `NavGraph`.

## Verification

`./gradlew assembleDebug` → BUILD SUCCESSFUL.

## Deviations

- Feature screens (LoginScreen, DashboardScreen, etc.) do not exist yet (Tasks 8+). Places placeholder composables inside `NavGraph.kt` instead of creating feature package files, matching the sub-task instruction "placeholder composables for screens not yet built". When real screens land, only the import + call site swap is needed.
- Deprecation warning on `window.statusBarColor` in Theme.kt (plan-mandated code, Task 17 may revisit).

## Commit

`feat: add MainActivity, theme, and navigation graph`
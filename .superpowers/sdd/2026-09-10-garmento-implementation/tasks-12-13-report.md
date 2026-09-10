# Tasks 12-13 Report: Orders & Dashboard Features

**Date:** 2026-09-10
**Commit:** feb4952

## Task 12: Orders Feature

### Files Created
- `app/src/main/java/com/example/garmento/feature/orders/OrderRepository.kt`
- `app/src/main/java/com/example/garmento/feature/orders/OrdersViewModel.kt`
- `app/src/main/java/com/example/garmento/feature/orders/PlaceOrderScreen.kt`
- `app/src/main/java/com/example/garmento/feature/orders/OrderEditScreen.kt`

### What Was Built
- **OrderRepository**: `placeOrder` with quota deduction (remaining = quota - used, fromQuota = min(N, remaining), overage = N - fromQuota), `updateOrder` with quota reversal, `voidOrder` with quota reversal
- **OrdersViewModel**: Single ViewModel handling both PlaceOrder and OrderEdit via `SavedStateHandle` args. `PlaceOrderUiState` with live quota preview computed properties. `OrderEditUiState` for edit/void
- **PlaceOrderScreen**: Order number + clothes count inputs, live quota preview card (primary/error container based on overage), save button with loading state
- **OrderEditScreen**: Shows current order details, edit clothes count, void button with AlertDialog for reason input

### Key Business Logic
```
remaining = clothesQuota - clothesUsed
clothesFromQuota = min(clothesCount, maxOf(remaining, 0))
clothesOverage = clothesCount - clothesFromQuota
clothesUsed += clothesCount  // never capped
```

---

## Task 13: Dashboard Feature

### Files Created
- `app/src/main/java/com/example/garmento/feature/dashboard/DashboardRepository.kt`
- `app/src/main/java/com/example/garmento/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/example/garmento/feature/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/example/garmento/feature/dashboard/ExpiringSoonSection.kt`

### What Was Built
- **DashboardRepository**: `getActiveSubscriptionCount`, `getExpiringSoon(daysAhead=7)`, `getRevenueThisMonth`, `getOrdersTodayCount`, `getRecentTransactions`, `syncFromRemote` (pulls all three tables)
- **DashboardViewModel**: `DashboardUiState` with all stats, loads on init with remote sync
- **DashboardScreen**: 3-column stat cards (Active Subs, Revenue, Orders Today), ExpiringSoonSection, recent transactions list
- **ExpiringSoonSection**: Reusable composable showing subscriptions expiring within 7 days with days remaining and clothes left

---

## NavGraph Changes
- Dashboard placeholder → real `DashboardScreen`
- PlaceOrder placeholder → real `PlaceOrderScreen`
- OrderEdit placeholder → real `OrderEditScreen`
- Removed 3 unused placeholder functions, kept Reports/Reminders/Settings placeholders

## Build
- `./gradlew assembleDebug` — **SUCCESS** (42 tasks, 11 executed)

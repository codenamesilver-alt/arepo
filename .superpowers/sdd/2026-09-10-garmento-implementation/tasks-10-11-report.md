# Tasks 10-11 Report: Customers & Subscriptions

**Commit:** `feat: add customers and subscriptions features`
**Build:** `assembleDebug` — SUCCESS
**Date:** 2026-09-10

## Files Created

| File | Description |
|------|-------------|
| `feature/customers/CustomerRepository.kt` | CRUD, search, subscriptions-per-customer, domain↔entity mapping, Supabase sync |
| `feature/customers/CustomersViewModel.kt` | CustomersUiState, CustomerDetailUiState, search via `flatMapLatest`, detail load |
| `feature/customers/CustomerListScreen.kt` | Searchable list with `FilterChip`-style search bar, click-to-detail |
| `feature/customers/CustomerDetailScreen.kt` | Contact info, active sub card, subscription history, nav buttons |
| `feature/customers/SubscriptionRepository.kt` | Create with plan snapshot, renew (marks old as renewed + links), transactions |
| `feature/customers/NewSubscriptionViewModel.kt` | Plan selection, payment method, create confirmation |
| `feature/customers/NewSubscriptionScreen.kt` | Plan picker cards, payment method chips, confirm button |

## Files Modified

| File | Change |
|------|--------|
| `navigation/NavGraph.kt` | Replaced `PlaceholderCustomerListScreen`, `PlaceholderCustomerDetailScreen`, `PlaceholderNewSubscriptionScreen` with real composables. Removed unused placeholder functions. |

## Key Decisions

- **Search reactive flow:** `_searchQuery` StateFlow → `flatMapLatest` → repo search/all. UIState only holds the text; `customers` StateFlow drives the list. Clean separation.
- **Supabase sync:** `.select().decodeList<>()` matches existing `PlanRepository` pattern (not `.selectList()`).
- **Flow.first()** in `CustomerRepository.getSubscriptions()` — Room Flow collected once, list returned. Avoids keeping subscription list as a Flow in detail screen.
- **Subscription creation:** Transaction logged alongside subscription. Renewal marks old sub as "renewed" then creates new one.

## Plan Deviation (fixes)

1. `observeCustomers()` in plan returned `MutableStateFlow(emptyList())` — replaced with proper `flatMapLatest` StateFlow pattern.
2. `observePlans()` in `NewSubscriptionViewModel` — used `stateIn()` to return proper StateFlow instead of raw Flow.

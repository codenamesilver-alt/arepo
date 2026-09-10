# GARMENTO — Laundry Subscription Manager

## Design Spec

Date: 2026-09-10
Status: Approved for implementation

---

## 1. Overview

Native Android app (Kotlin + Jetpack Compose) for managing laundry customer subscriptions, orders, and renewals. Backed by Supabase (Postgres + Auth + Realtime).

---

## 2. Tech Stack

| Layer | Technology |
|-------|-----------|
| Platform | Native Android, Kotlin, Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository pattern |
| Backend | Supabase (Postgres, RLS, Auth, Realtime) |
| Networking | supabase-kt (Ktor) |
| Local cache | Room DB (offline order entry + sync queue) |
| DI | Hilt |
| Background | WorkManager (daily reminders, sync queue flush) |
| Charts | Vico |
| QR codes | ZXing |
| Session | DataStore |
| Notifications | Android local notifications |

---

## 3. Supabase Configuration

- **URL:** `https://kfxldnebbqkwkzsxirny.supabase.co`
- **Anon key:** stored in `BuildConfig.SUPABASE_ANON_KEY` via `local.properties`
- **Service role key:** stored in `BuildConfig.SUPABASE_SERVICE_ROLE_KEY` via `local.properties` (dev only, never in source control)

---

## 4. Database Schema

```sql
-- Admin users / staff
create table admins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id),  -- linked after Supabase Auth migration
  username text unique not null,
  password_hash text not null,              -- removed after Auth migration
  role text default 'staff',               -- admin | staff
  created_at timestamptz default now()
);

-- Customers
create table customers (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text not null,
  address text,
  notes text,
  public_code text unique not null,         -- short code for self-service portal
  created_at timestamptz default now()
);

-- Subscription plan templates
create table subscription_plans (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  duration_days int not null,
  amount numeric(10,2) not null,
  total_clothes int not null,
  is_active boolean default true,
  created_at timestamptz default now()
);

-- Customer subscription instances
create table subscriptions (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references customers(id) on delete cascade,
  plan_id uuid references subscription_plans(id),
  start_date timestamptz not null,
  end_date timestamptz not null,
  clothes_quota int not null,
  clothes_used int default 0,
  status text default 'active',            -- active | expired | renewed
  renewed_from_id uuid references subscriptions(id),
  created_at timestamptz default now()
);

-- Laundry orders
create table orders (
  id uuid primary key default gen_random_uuid(),
  subscription_id uuid references subscriptions(id),
  order_number text not null,
  clothes_count int not null,
  clothes_from_quota int not null,
  clothes_overage int default 0,
  order_date timestamptz default now(),
  is_voided boolean default false,
  void_reason text,
  grace_period boolean default false
);

-- Payment/transaction log
create table transactions (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid references customers(id),
  subscription_id uuid references subscriptions(id),
  amount numeric(10,2) not null,
  type text not null,                       -- new | renewal
  payment_method text,
  transaction_date timestamptz default now()
);

-- Order audit trail
create table order_audit (
  id uuid primary key default gen_random_uuid(),
  order_id uuid references orders(id),
  action text not null,                     -- edit | void
  old_clothes_count int,
  new_clothes_count int,
  reason text,
  performed_by text,
  timestamp timestamptz default now()
);

-- Offline sync queue
create table pending_sync (
  id uuid primary key default gen_random_uuid(),
  table_name text not null,
  payload_json text not null,
  created_at timestamptz default now(),
  synced boolean default false
);
```

### Indexes

```sql
create index idx_orders_subscription_id on orders(subscription_id);
create index idx_subscriptions_customer_id on subscriptions(customer_id);
create index idx_subscriptions_end_date on subscriptions(end_date);
create index idx_subscriptions_status on subscriptions(status);
create index idx_transactions_transaction_date on transactions(transaction_date);
create index idx_customers_public_code on customers(public_code);
create index idx_pending_sync_synced on pending_sync(synced);
```

### Row Level Security

```sql
-- Enable RLS on all tables
alter table admins enable row level security;
alter table customers enable row level security;
alter table subscription_plans enable row level security;
alter table subscriptions enable row level security;
alter table orders enable row level security;
alter table transactions enable row level security;
alter table order_audit enable row level security;
alter table pending_sync enable row level security;

-- MVP: authenticated users can do everything (admin-only tool)
create policy "Authenticated full access" on admins for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on customers for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on subscription_plans for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on subscriptions for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on orders for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on transactions for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on order_audit for all using (auth.role() = 'authenticated');
create policy "Authenticated full access" on pending_sync for all using (auth.role() = 'authenticated');
```

---

## 5. Architecture

### 5.1 Package Structure

```
com.example.garmento/
├── core/
│   ├── data/
│   │   ├── local/          # Room DB, DAOs, entities
│   │   ├── remote/         # Supabase data sources
│   │   └── repository/     # Abstract interfaces + impls
│   ├── domain/
│   │   └── model/          # Domain models
│   ├── di/                 # Hilt modules
│   └── util/               # Extensions, constants
├── feature/
│   ├── auth/               # Login screen + AuthViewModel
│   ├── dashboard/          # DashboardScreen + DashboardViewModel
│   ├── plans/              # PlansList, PlanEdit screens + VM
│   ├── customers/          # CustomerList, CustomerDetail, NewSubscription + VMs
│   ├── orders/             # PlaceOrder, OrderHistory, OrderEdit + VMs
│   ├── reports/            # ReportsScreen + ReportsViewModel
│   ├── reminders/          # RemindersScreen + ReminderViewModel
│   └── settings/           # SettingsScreen
└── GarmentoApp.kt          # Application class
```

### 5.2 Data Flow

```
UI (Compose) ←collectAsStateWithLifecycle← ViewModel ←usecases← Repository
                                                                    ↕
                                                          Supabase (remote)
                                                          Room (local cache)
```

- Repository interfaces defined in `core/data/repository/`
- Feature modules depend only on interfaces, not on Supabase/Room directly
- Domain models: Kotlin data classes, mapped at repository boundary
- Remote entity → Domain model → Room entity (and reverse)

### 5.3 Navigation

Bottom nav bar for main sections. Nested nav for detail/edit screens.

```
Login ──→ Main Scaffold (bottom nav)
              ├── Dashboard
              ├── Plans (list → edit)
              ├── Customers (list → detail → new sub / order history / payments)
              ├── Orders (place order → confirmation preview)
              ├── Reports
              ├── Reminders (also accessible from Dashboard)
              └── Settings
```

---

## 6. Business Logic

### 6.1 Clothes Quota Deduction

```kotlin
val remaining = subscription.clothesQuota - subscription.clothesUsed
val clothesFromQuota = min(order.clothesCount, max(remaining, 0))
val clothesOverage = order.clothesCount - clothesFromQuota
subscription.clothesUsed += order.clothesCount  // never capped
```

- Order always accepted, even past quota
- UI shows `"+N over quota"` badge when `clothesOverage > 0`

### 6.2 Low-Quota Warning (Sub-project 3)

During order entry, before save:
- If `clothesUsed + N > clothesQuota`: red warning card "This order will push customer X clothes over quota"
- Two buttons: "Proceed anyway" / "Cancel"
- If under: green card "X clothes remaining after this order"

### 6.3 Renewal Logic

```
new_start_date = now()
new_end_date = now() + plan.duration_days
```

- New subscription row: fresh `clothes_quota` from plan, `clothes_used = 0`
- Old subscription: `status = 'renewed'`, linked via `renewed_from_id`
- Transaction: `type = 'renewal'`, `amount = plan.amount`
- Unused days forfeited — not stacked
- Plan picker at renewal time (defaults to current plan, can upgrade)

### 6.4 Order Edit/Void (Sub-project 3)

- **Edit:** Adjust clothes count, re-calculate quota. Audit trail row stored.
- **Void:** Mark order as voided, reverse quota deduction. Audit trail with reason.
- Both create `order_audit` entry: `{ order_id, action, old/new values, reason, timestamp }`

### 6.5 Grace Period (Sub-project 3)

- Configurable in Settings (0-7 days, default 0)
- Stored in DataStore (local setting)
- During grace period: subscription stays "active", UI shows "Grace period" badge
- Orders during grace: accepted with `grace_period: true` flag
- After grace period: subscription fully locked, orders rejected

### 6.6 Expiry & Reminders

- "Expiring soon" = `end_date` within configurable days (default 7 and 1) AND `status = 'active'`
- WorkManager daily job (9am): queries Supabase for expiring subscriptions
- Fires local notification per subscriber: "Ramesh's plan expires in 3 days — 12 clothes remaining"
- "Send reminder" button opens WhatsApp/SMS with pre-filled message

### 6.7 Self-Service Portal (Sub-project 4)

- Each customer gets `public_code` (e.g. `RM-4829`)
- Web page at Supabase-hosted URL shows: plan name, days remaining, clothes used/remaining
- No login required, code in URL
- QR code generated in-app (ZXing) encodes the portal URL

---

## 7. Screens (15 total)

### 7.1 Login
- Username + password fields
- Hardcoded admin/admin for MVP
- Session flag in DataStore once logged in

### 7.2 Dashboard
- Active subscriptions count (this month / last 30 days)
- Expiring in 7 days — count + tappable list (name, days left, clothes remaining)
- Recent transactions — last 10 (customer, amount, type, date)
- Quick stats: revenue this month, total customers, orders today
- Pull-to-refresh; Supabase Realtime for live updates

### 7.3 Plans List
- All plans: name, duration, amount, total clothes, active/inactive toggle
- Create/edit/deactivate
- Deactivating hides from "new subscription" pickers, keeps history

### 7.4 Plan Edit
- Form: name, duration (days), amount, total clothes, active toggle
- Create or update

### 7.5 Customer List
- Searchable by name/phone
- Shows active plan badge if subscribed

### 7.6 Customer Detail
- Contact info (name, phone, address, notes)
- Current subscription: plan, days left, clothes used/remaining/overage
- Past subscription history
- Order history
- Payment history
- "Renew" button, "New Subscription" button
- QR code generation for self-service

### 7.7 New Subscription
- Pick customer (or create new)
- Pick plan (only active plans)
- Confirm payment method + amount
- Creates subscription + transaction rows

### 7.8 Place Order
- Search/select customer
- Enter order number + clothes count
- Date auto-fills to now (editable for backdated entry)
- Live preview: "8 from quota, 2 over quota" breakdown
- Low-quota warning before save (Sub-project 3)
- Save → deducts from quota

### 7.9 Order History
- List of orders for a customer
- Shows quota/overage per order

### 7.10 Order Edit
- Edit clothes count or void order
- Audit trail with reason
- Quota recalculated on save

### 7.11 Reports
- Monthly revenue by plan (bar chart)
- Most popular plan (horizontal bar)
- Avg clothes/order (stat + trend line)
- CSV export buttons

### 7.12 Reminders
- Expiring-soon list filterable by 1/3/7/15 days
- Send WhatsApp/SMS action per customer

### 7.13 Settings
- Grace period config (toggle + days)
- Dark mode toggle
- Backup/export
- Staff role management (Sub-project 4)

### 7.14 Expiring Soon (widget/tab)
- Embedded in Dashboard and standalone
- Filterable list of expiring subscriptions

---

## 8. Sub-project Decomposition

### Sub-project 1: Foundation + MVP
- Supabase schema + RLS (SQL migration script)
- Login (hardcoded admin/admin)
- Plans CRUD
- Customers CRUD
- Subscriptions CRUD
- Orders (quota deduction logic)
- Dashboard (aggregation queries)
- Renewals
- Expiry reminders (WorkManager + notifications)

### Sub-project 2: Reports & Export
- Revenue by plan (Vico bar chart)
- Most popular plan (Vico horizontal bar)
- Avg clothes/order (stat + trend)
- CSV export of transactions + customer lists

### Sub-project 3: Advanced Operations
- Low-quota inline warning
- Order edit/void with audit trail
- Grace period config
- Dark mode toggle
- Offline order entry (Room + sync queue)

### Sub-project 4: Multi-user & Self-service
- Staff logins with roles (Admin/Staff) via Supabase Auth
- Customer self-service portal (QR/link)
- QR code generation per customer
- Full DB backup/export

---

## 9. Build Order

1. Supabase schema + RLS → SQL migration script
2. Login screen → session persistence
3. Plans CRUD → validates Supabase wiring
4. Customers + New Subscription flow
5. Place Order flow + quota/overage logic
6. Dashboard
7. Renew flow
8. Reminders (WorkManager + notifications)
9. Reports + charts + CSV export
10. Low-quota warnings, order edit/void, grace period
11. Dark mode, offline queue
12. Staff roles + Supabase Auth migration
13. Customer self-service portal + QR codes
14. Polish: backup/export, final testing

---

## 10. Security Notes

- Hardcoded admin/admin fine for MVP dev, swap to Supabase Auth before real deployment
- Store Supabase keys in `local.properties` → `BuildConfig`, not in source control
- Enable RLS on every table
- Service role key: dev only, never in production APK

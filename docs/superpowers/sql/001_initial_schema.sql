-- GARMENTO Initial Schema Migration
-- Run this in Supabase SQL Editor or via migration tool

-- ============================================================
-- TABLES
-- ============================================================

-- Admin users / staff
create table admins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id),
  username text unique not null,
  password_hash text not null,
  role text default 'staff',
  created_at timestamptz default now()
);

-- Customers
create table customers (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  phone text not null,
  address text,
  notes text,
  public_code text unique not null,
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
  status text default 'active',
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
  type text not null,
  payment_method text,
  transaction_date timestamptz default now()
);

-- Order audit trail
create table order_audit (
  id uuid primary key default gen_random_uuid(),
  order_id uuid references orders(id),
  action text not null,
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

-- ============================================================
-- INDEXES
-- ============================================================

create index idx_orders_subscription_id on orders(subscription_id);
create index idx_subscriptions_customer_id on subscriptions(customer_id);
create index idx_subscriptions_end_date on subscriptions(end_date);
create index idx_subscriptions_status on subscriptions(status);
create index idx_transactions_transaction_date on transactions(transaction_date);
create index idx_customers_public_code on customers(public_code);
create index idx_pending_sync_synced on pending_sync(synced);

-- ============================================================
-- ROW LEVEL SECURITY
-- ============================================================

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

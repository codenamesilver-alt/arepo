-- GARMENTO orders delivery tracking
-- Adds delivery scheduling + fulfillment state to the orders table.
--
-- ADDITIVE ONLY. Existing rows keep working:
--   * delivery_date   = NULL until admin sets one (old orders show as pending)
--   * delivery_status = 'pending' by default for all existing rows
--   * delivery_notes  = NULL
--
-- Run this in Supabase SQL Editor. Safe to run multiple times (idempotent).

alter table orders add column if not exists delivery_date date;
alter table orders add column if not exists delivery_status text not null default 'pending';
alter table orders add column if not exists delivery_notes text;

-- Speed up the dashboard "upcoming deliveries (soonest first)" query.
create index if not exists idx_orders_delivery_date
  on orders (delivery_date) where delivery_status <> 'delivered';

-- Keep delivery_status constrained to the three known states.
-- Keep delivery_status constrained to the three known states.
alter table orders drop constraint if exists orders_delivery_status_check;
alter table orders
  add constraint orders_delivery_status_check
  check (delivery_status in ('pending', 'delivered', 'partial'));
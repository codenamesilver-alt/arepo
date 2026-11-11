-- GARMENTO subscription_plans schema repair
-- Fixes: plans created in the app disappearing after leaving/re-entering the Plans page.
--
-- Root cause: the app writes 7 columns to subscription_plans (id, name, duration_days,
-- amount, total_clothes, is_active, created_at). If any column is missing or the table
-- was created with different names/types, the remote upsert fails silently, the plan
-- never reaches Supabase, and the next remote pull overwrites local data with it gone.
--
-- Run this in Supabase SQL Editor. Safe to run multiple times (idempotent).

-- Create the table if it does not exist yet.
create table if not exists subscription_plans (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  duration_days int not null,
  amount numeric(10,2) not null,
  total_clothes int not null,
  is_active boolean default true,
  created_at timestamptz default now()
);

-- Add any missing columns, idempotently.
do $$
begin
  if not exists (select 1 from information_schema.columns
                 where table_name = 'subscription_plans' and column_name = 'duration_days') then
    alter table subscription_plans add column duration_days int not null default 30;
  end if;
  if not exists (select 1 from information_schema.columns
                 where table_name = 'subscription_plans' and column_name = 'amount') then
    alter table subscription_plans add column amount numeric(10,2) not null default 0;
  end if;
  if not exists (select 1 from information_schema.columns
                 where table_name = 'subscription_plans' and column_name = 'total_clothes') then
    alter table subscription_plans add column total_clothes int not null default 20;
  end if;
  if not exists (select 1 from information_schema.columns
                 where table_name = 'subscription_plans' and column_name = 'is_active') then
    alter table subscription_plans add column is_active boolean default true;
  end if;
  if not exists (select 1 from information_schema.columns
                 where table_name = 'subscription_plans' and column_name = 'created_at') then
    alter table subscription_plans add column created_at timestamptz default now();
  end if;
end $$;

-- The app runs entirely on the anon key (no Supabase Auth sign-in), so it needs
-- anonymous read/write access to this table, same as the other tables it syncs.
-- Only appropriate while this is a single-operator tool.
alter table subscription_plans enable row level security;
drop policy if exists "anon full access" on subscription_plans;
create policy "anon full access" on subscription_plans
  for all using (true) with check (true);

-- Restore missing indexes (same as 001_initial_schema.sql).
drop index if exists idx_subscriptions_customer_id;
create index idx_subscriptions_customer_id on subscriptions(customer_id);
drop index if exists idx_subscriptions_end_date;
create index idx_subscriptions_end_date on subscriptions(end_date);
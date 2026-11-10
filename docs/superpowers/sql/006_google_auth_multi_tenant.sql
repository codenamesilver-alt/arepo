-- GARMENTO Google Auth + Multi-Tenant Migration
-- Creates companies/company_members/invitations, adds company_id to business
-- tables, replaces permissive RLS with owner/staff policies, and adds
-- SECURITY DEFINER helpers/RPCs for the customer portal and invitations.
--
-- Run in Supabase SQL Editor or via migration tool. Idempotent-ish (uses
-- IF NOT EXISTS / DROP IF EXISTS). Assumes business tables are empty so
-- company_id can be added NOT NULL directly.

-- ============================================================
-- NEW TABLES
-- ============================================================

create table if not exists public.companies (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  owner_user_id uuid not null references auth.users(id),
  created_at timestamptz default now()
);

create table if not exists public.company_members (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  user_id uuid not null references auth.users(id),
  role text not null default 'staff', -- 'owner' | 'staff'
  status text not null default 'active', -- 'pending' | 'active'
  joined_at timestamptz default now(),
  unique (company_id, user_id)
);

create table if not exists public.invitations (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references public.companies(id) on delete cascade,
  email text not null,
  role text not null default 'staff',
  token text unique not null,
  expires_at timestamptz not null,
  accepted_at timestamptz,
  created_at timestamptz default now()
);

-- ============================================================
-- company_id ON BUSINESS TABLES
-- ============================================================

do $$
declare
  t text;
begin
  foreach t in array array['customers','subscription_plans','subscriptions','orders','transactions','order_audit','pending_sync'] loop
    if not exists (select 1 from information_schema.columns
                   where table_schema='public' and table_name=t and column_name='company_id') then
      execute format('alter table public.%I add column company_id uuid references public.companies(id)', t);
    end if;
    -- enforce NOT NULL (tables are empty at migration time)
    execute format('alter table public.%I alter column company_id set not null', t);
  end loop;
end $$;

-- ============================================================
-- INDEXES
-- ============================================================

create index if not exists idx_companies_owner_user_id on public.companies(owner_user_id);

create index if not exists idx_company_members_company_id on public.company_members(company_id);
create index if not exists idx_company_members_user_id on public.company_members(user_id);

create index if not exists idx_invitations_company_id on public.invitations(company_id);
create index if not exists idx_invitations_email on public.invitations(email);
create index if not exists idx_invitations_expires_at on public.invitations(expires_at);

create index if not exists idx_customers_company_id on public.customers(company_id);
create index if not exists idx_subscription_plans_company_id on public.subscription_plans(company_id);
create index if not exists idx_subscriptions_company_id on public.subscriptions(company_id);
create index if not exists idx_orders_company_id on public.orders(company_id);
create index if not exists idx_transactions_company_id on public.transactions(company_id);
create index if not exists idx_order_audit_company_id on public.order_audit(company_id);
create index if not exists idx_pending_sync_company_id on public.pending_sync(company_id);

-- ============================================================
-- RLS ENABLE
-- ============================================================

alter table public.companies enable row level security;
alter table public.company_members enable row level security;
alter table public.invitations enable row level security;
alter table public.customers enable row level security;
alter table public.subscription_plans enable row level security;
alter table public.subscriptions enable row level security;
alter table public.orders enable row level security;
alter table public.transactions enable row level security;
alter table public.order_audit enable row level security;
alter table public.pending_sync enable row level security;

-- ============================================================
-- DROP OLD PERMISSIVE POLICIES
-- ============================================================

do $$
declare
  t text;
begin
  foreach t in array array['customers','subscription_plans','subscriptions','orders','transactions','order_audit','pending_sync'] loop
    execute format('drop policy if exists "Authenticated full access" on public.%I', t);
    execute format('drop policy if exists "anon full access" on public.%I', t);
  end loop;
end $$;

-- ============================================================
-- SECURITY DEFINER HELPER
-- ============================================================

create or replace function public.current_company_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
  select company_id from public.company_members
  where user_id = auth.uid() and status = 'active';
$$;

revoke all on function public.current_company_ids() from public;
grant execute on function public.current_company_ids() to authenticated;

-- ============================================================
-- BUSINESS TABLE POLICIES (owner full CRUD, staff read-only)
-- ============================================================

do $$
declare
  t text;
begin
  foreach t in array array['customers','subscription_plans','subscriptions','orders','transactions','order_audit','pending_sync'] loop
    execute format('drop policy if exists "owner full access" on public.%I', t);
    execute format('drop policy if exists "staff read access" on public.%I', t);

    execute format($p$
      create policy "owner full access" on public.%I for all
        using (company_id in (select company_id from public.company_members
                              where user_id = auth.uid() and status = 'active' and role = 'owner'))
        with check (company_id in (select company_id from public.company_members
                                   where user_id = auth.uid() and status = 'active' and role = 'owner'))
    $p$, t);

    execute format($p$
      create policy "staff read access" on public.%I for select
        using (company_id in (select company_id from public.company_members
                              where user_id = auth.uid() and status = 'active' and role = 'staff'))
    $p$, t);
  end loop;
end $$;

-- ============================================================
-- COMPANIES / MEMBERS / INVITATIONS POLICIES
-- ============================================================

-- companies: owners manage their own; active members can read
drop policy if exists "owner manage companies" on public.companies;
create policy "owner manage companies" on public.companies for all
  using (owner_user_id = auth.uid())
  with check (owner_user_id = auth.uid());

drop policy if exists "members read companies" on public.companies;
create policy "members read companies" on public.companies for select
  using (id in (select company_id from public.company_members
                where user_id = auth.uid() and status = 'active'));

-- company_members: owners manage members of their companies; members read their company
drop policy if exists "owner manage members" on public.company_members;
create policy "owner manage members" on public.company_members for all
  using (company_id in (select id from public.companies where owner_user_id = auth.uid()))
  with check (company_id in (select id from public.companies where owner_user_id = auth.uid()));

drop policy if exists "members read company members" on public.company_members;
create policy "members read company members" on public.company_members for select
  using (user_id = auth.uid()
         or company_id in (select id from public.companies where owner_user_id = auth.uid()));

-- invitations: owners manage; invitees read invitations addressed to their email
drop policy if exists "owner manage invitations" on public.invitations;
create policy "owner manage invitations" on public.invitations for all
  using (company_id in (select id from public.companies where owner_user_id = auth.uid()))
  with check (company_id in (select id from public.companies where owner_user_id = auth.uid()));

drop policy if exists "invitee read invitations" on public.invitations;
create policy "invitee read invitations" on public.invitations for select
  using (lower(email) = lower((select email from auth.users where id = auth.uid())));

-- ============================================================
-- RPC: get_customer_portal (anon-safe, SECURITY DEFINER)
-- ============================================================

create or replace function public.get_customer_portal(p_public_code text)
returns table (
  customer_id uuid,
  customer_name text,
  public_code text,
  subscription_id uuid,
  plan_name text,
  start_date timestamptz,
  end_date timestamptz,
  clothes_quota int,
  clothes_used int,
  subscription_status text
)
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
  select c.id,
         c.name,
         c.public_code,
         s.id,
         sp.name,
         s.start_date,
         s.end_date,
         s.clothes_quota,
         s.clothes_used,
         s.status
  from public.customers c
  left join lateral (
    select sub.* from public.subscriptions sub
    where sub.customer_id = c.id and sub.status = 'active'
    order by sub.created_at desc
    limit 1
  ) s on true
  left join public.subscription_plans sp on sp.id = s.plan_id
  where c.public_code = p_public_code
  limit 1;
$$;

revoke all on function public.get_customer_portal(text) from public;
grant execute on function public.get_customer_portal(text) to anon, authenticated;

-- ============================================================
-- RPC: accept_invitation (SECURITY DEFINER)
-- ============================================================

create or replace function public.accept_invitation(p_token text)
returns uuid
language plpgsql
security definer
set search_path = public, pg_catalog
as $$
declare
  v_user_id uuid := auth.uid();
  v_email text;
  v_inv public.invitations%rowtype;
begin
  if v_user_id is null then
    raise exception 'not authenticated';
  end if;
  select email into v_email from auth.users where id = v_user_id;
  select * into v_inv from public.invitations where token = p_token;
  if not found then
    raise exception 'invalid invitation token';
  end if;
  if v_inv.accepted_at is not null then
    raise exception 'invitation already accepted';
  end if;
  if v_inv.expires_at < now() then
    raise exception 'invitation expired';
  end if;
  if lower(v_inv.email) <> lower(v_email) then
    raise exception 'invitation email does not match signed-in user';
  end if;

  insert into public.company_members (company_id, user_id, role, status)
  values (v_inv.company_id, v_user_id, v_inv.role, 'active')
  on conflict (company_id, user_id) do update
    set status = 'active', role = excluded.role;

  update public.invitations set accepted_at = now() where id = v_inv.id;

  return v_inv.company_id;
end;
$$;

revoke all on function public.accept_invitation(text) from public;
grant execute on function public.accept_invitation(text) to authenticated;

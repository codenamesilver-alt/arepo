-- GARMENTO RLS recursion fix
-- The policies in 006 inlined subqueries against companies/company_members, which
-- made Postgres evaluate RLS on those tables recursively (e.g. querying
-- company_members joined to companies -> "infinite recursion detected in policy").
-- This migration replaces them with SECURITY DEFINER helpers that bypass RLS,
-- breaking the cycle.

-- ============================================================
-- SECURITY DEFINER HELPERS
-- ============================================================

create or replace function public.owner_company_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
  select id from public.companies where owner_user_id = auth.uid();
$$;

create or replace function public.member_company_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
  select company_id from public.company_members
  where user_id = auth.uid() and status = 'active';
$$;

create or replace function public.staff_company_ids()
returns setof uuid
language sql
stable
security definer
set search_path = public, pg_catalog
as $$
  select company_id from public.company_members
  where user_id = auth.uid() and status = 'active' and role = 'staff';
$$;

revoke all on function public.owner_company_ids() from public;
revoke all on function public.member_company_ids() from public;
revoke all on function public.staff_company_ids() from public;
grant execute on function public.owner_company_ids() to authenticated;
grant execute on function public.member_company_ids() to authenticated;
grant execute on function public.staff_company_ids() to authenticated;

-- ============================================================
-- DROP RECURSIVE POLICIES
-- ============================================================

do $$
declare
  t text;
begin
  foreach t in array array['customers','subscription_plans','subscriptions','orders','transactions','order_audit','pending_sync'] loop
    execute format('drop policy if exists "owner full access" on public.%I', t);
    execute format('drop policy if exists "staff read access" on public.%I', t);
  end loop;
end $$;

drop policy if exists "owner manage companies" on public.companies;
drop policy if exists "members read companies" on public.companies;

drop policy if exists "owner manage members" on public.company_members;
drop policy if exists "members read company members" on public.company_members;

drop policy if exists "owner manage invitations" on public.invitations;
drop policy if exists "invitee read invitations" on public.invitations;

-- ============================================================
-- RECREATE POLICIES USING SECURITY DEFINER HELPERS
-- ============================================================

-- companies
create policy "owner manage companies" on public.companies for all
  using (owner_user_id = auth.uid())
  with check (owner_user_id = auth.uid());

create policy "members read companies" on public.companies for select
  using (id in (select public.member_company_ids()));

-- company_members
create policy "owner manage members" on public.company_members for all
  using (company_id in (select public.owner_company_ids()))
  with check (company_id in (select public.owner_company_ids()));

create policy "members read company members" on public.company_members for select
  using (user_id = auth.uid()
         or company_id in (select public.owner_company_ids()));

-- invitations
create policy "owner manage invitations" on public.invitations for all
  using (company_id in (select public.owner_company_ids()))
  with check (company_id in (select public.owner_company_ids()));

create policy "invitee read invitations" on public.invitations for select
  using (lower(email) = lower(auth.jwt() ->> 'email'));

-- business tables
do $$
declare
  t text;
begin
  foreach t in array array['customers','subscription_plans','subscriptions','orders','transactions','order_audit','pending_sync'] loop
    execute format($p$
      create policy "owner full access" on public.%I for all
        using (company_id in (select public.owner_company_ids()))
        with check (company_id in (select public.owner_company_ids()))
    $p$, t);

    execute format($p$
      create policy "staff read access" on public.%I for select
        using (company_id in (select public.staff_company_ids()))
    $p$, t);
  end loop;
end $$;

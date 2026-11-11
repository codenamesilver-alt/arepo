-- GARMENTO admins table RLS repair
-- Fixes: "Failed to add staff member" error when inserting via anon key.
--
-- Root cause: the admins table only has an "Authenticated full access" policy,
-- but the app uses the Supabase anon key (no Auth sign-in). Insert is blocked by RLS.
--
-- Run this in Supabase SQL Editor. Safe to run multiple times (idempotent).

-- Add anon full access policy for admins table (same pattern as other tables).
drop policy if exists "anon full access" on admins;
create policy "anon full access" on admins
  for all using (true) with check (true);

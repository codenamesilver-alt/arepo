# Google Authentication + Multi-Tenant Company Design

**Date:** 2026-11-10
**Status:** Draft (pending approval)
**Approach:** A — full multi-tenant refactor

## Intent

Add Google Sign-In (via Supabase Auth) so multiple laundry shops (tenants) can use one app, with data isolated per company. After first sign-in the user creates a company; subsequent use behaves like the regular app. Owners can invite staff by email with role-based access.

## Goals

- Replace legacy username/password admin login with Google Sign-In.
- First login flow: create a company before entering the app.
- Multi-tenant data isolation by `company_id` on every business table, enforced by Supabase RLS and mirrored in local Room.
- Staff invitations via email, with roles (owner, staff).
- Preserve offline-first behavior (Room + sync queue), scoped to the active company.

## Non-Goals

- Billing/subscription plans for companies.
- Cross-company data sharing.
- A web admin portal.
- SSO / SAML.

## Constraints

- Android app: Kotlin, Jetpack Compose, Hilt, Room, WorkManager, Navigation Compose.
- Supabase: Postgres + Auth (Google provider) + RLS + (optionally) Edge Functions for invite emails.
- Offline-first: app must work without network; sync when online.
- Existing schema in `docs/superpowers/sql/` (001–004) plus new migration.

## Architecture

### Identity (Supabase Auth)

- Enable Google provider in Supabase Auth dashboard.
- App uses `supabase-kt` auth (`io.github.jan.supabase.auth.Auth`): `signInWith(Google)` opens a Chrome Custom Tab / browser and returns a session.
- Session (JWT + refresh token) persisted by supabase-kt; app checks `supabase.auth.currentSessionOrNull()` on launch.
- Legacy `admins` username/password login removed. `UserSession` (DataStore) now stores: `userId` (Supabase auth uid), `email`, `companyId`, `role`.

### Multi-tenant schema (Supabase)

New tables:

```sql
create table companies (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  owner_user_id uuid not null references auth.users(id),
  created_at timestamptz default now()
);

create table company_members (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references companies(id) on delete cascade,
  user_id uuid not null references auth.users(id),
  role text not null default 'staff', -- 'owner' | 'staff'
  status text not null default 'active', -- 'pending' | 'active'
  joined_at timestamptz default now(),
  unique (company_id, user_id)
);

create table invitations (
  id uuid primary key default gen_random_uuid(),
  company_id uuid not null references companies(id) on delete cascade,
  email text not null,
  role text not null default 'staff',
  token text unique not null,
  expires_at timestamptz not null,
  accepted_at timestamptz,
  created_at timestamptz default now()
);
```

Add `company_id uuid not null references companies(id)` to: `customers`, `subscription_plans`, `subscriptions`, `orders`, `transactions`, `order_audit`, `pending_sync`.

RLS: helper function (security-first, stable):

```sql
create or replace function public.current_company_ids()
returns setof uuid language sql stable security definer as $$
  select company_id from public.company_members
  where user_id = auth.uid() and status = 'active';
$$;
```

Each business table gets a policy (owner full CRUD; staff read-only). Since staff must not add/edit/delete, policies are split by role:

```sql
-- owner: full CRUD
create policy "owner full access" on <table> for all
  using (company_id in (select company_id from public.company_members
                        where user_id = auth.uid() and status='active' and role='owner'))
  with check (company_id in (select company_id from public.company_members
                            where user_id = auth.uid() and status='active' and role='owner'));

-- staff: read only
create policy "staff read access" on <table> for select
  using (company_id in (select company_id from public.company_members
                        where user_id = auth.uid() and status='active' and role='staff'));
```

Companies/company_members/invitations get their own policies (owner manages members of own companies; users read their own memberships and invitations addressed to their email).

**Anonymous customer portal (security-first):** instead of exposing the `customers` table to anon, expose a SECURITY DEFINER RPC that returns only the non-sensitive fields needed by the portal:

```sql
create or replace function public.get_customer_portal(p_public_code text)
returns table (id uuid, name text, public_code text, /* subscription summary */) ...
security definer as $$ ... $$;
```
Granted to `anon`. No direct table access for anon.

**Invitation acceptance (security-first):** instead of allowing direct insert into `company_members`, expose a SECURITY DEFINER RPC:

```sql
create or replace function public.accept_invitation(p_token text)
returns uuid (company_id) security definer as $$
  -- validate token (exists, unexpired, unaccepted), match auth.uid() email,
  -- insert company_members (active), mark invitation accepted.
$$;
```
Granted to `authenticated`. This prevents arbitrary membership inserts and centralizes validation.

### First-login / company creation

1. App launch → `supabase.auth.currentSessionOrNull()`.
   - null → `LoginScreen` (Google button).
   - present → fetch `company_members` for `auth.uid()`.
     - none → `CompanyCreateScreen` (enter company name) → insert `companies` + `company_members(owner)` → proceed.
     - present → if one company, set active; if multiple, `CompanyPickerScreen`.
2. Active company id + role stored in `UserSession` (DataStore) and in-memory.

### Staff invitations (email)

1. Owner opens Team screen → enters staff email + role → insert `invitations` row (token = random UUID, expires in 7 days).
2. Send email with deep link `garmento://invite?token=<token>` (via Supabase Edge Function or in-app email intent; Edge Function preferred for reliability). *Open question: email provider.*
3. Staff taps link → app opens → validates token (not expired, not accepted) → Google sign-in if needed → insert `company_members` (active) → mark invitation accepted.

### Role-based access

- Roles: `owner`, `staff`.
- Owner: full CRUD on all tables in their company; manage team, plans, settings.
- Staff: read-only on all tables (view only; no add/edit/delete/void). Enforced in UI (hide/disable actions by role) and by RLS (staff only `SELECT`).

### Offline-first (Room)

- Add `companyId: String` (UUID) to Room entities: `CustomerEntity`, `SubscriptionPlanEntity`, `SubscriptionEntity`, `OrderEntity`, `TransactionEntity`, `PendingSyncEntity`.
- `order_audit` stays cloud-only (no Room entity; read via Supabase when online). Keeps Room schema and sync scope smaller.
- All DAO queries filter by `companyId` (passed from `UserSession.currentCompanyId`).
- `SyncWorker` includes `company_id` in every cloud payload and filters pulls by active company.
- On company switch: clear in-memory caches; Room data for other companies remains (filtered out by queries) — acceptable.
- On first Google login with pre-existing local data (no company_id): wipe local DB (cloud is source of truth after migration). Prompt user once.

### Navigation (NavGraph)

New screens: `LoginScreen` (Google), `CompanyCreateScreen`, `CompanyPickerScreen`, `TeamScreen` (replaces StaffListScreen), `InviteAcceptScreen` (deep link handler).

Flow:
```
Session → (no session) Login → (no company) CompanyCreate → Dashboard
        → (session) → (no company) CompanyCreate → Dashboard
        → (session, company) → Dashboard
Deep link garmento://invite?token → InviteAccept → (sign in if needed) → join → Dashboard
```

### Deep links

Register intent-filter for `garmento://invite` in manifest; handle in `MainActivity` → nav controller navigates to `InviteAccept`.

### Removed / replaced

- `AuthRepository.login(username, password)` and `admins` table usage removed.
- `StaffRepository` / `StaffListScreen` replaced by `TeamRepository` / `TeamScreen` managing `company_members`.
- `Constants.ADMIN_USERNAME/PASSWORD` removed.

### Migration / rollout

1. Apply SQL migration to MYGARMENTO (`gjhrjzqbkjcvebqizgti`): new tables + `company_id` columns + RLS + policies + security-definer RPCs.
2. Data is irrelevant → destructive migration on both sides: cloud (already empty) and local Room (wipe on first Google login / version bump). No backfill needed.
3. Bump Room DB version; use `fallbackToDestructiveMigration()` (acceptable for this app's stage).
4. Update `local.properties` to target MYGARMENTO (done): URL `https://gjhrjzqbkjcvebqizgti.supabase.co` + anon/service-role keys from dashboard.
5. Manual dashboard steps: enable Google provider in Supabase Auth (client ID/secret from Google Cloud Console), set redirect URL `com.example.garmento://login-callback/` (or chosen scheme) in both Supabase Auth and Google Cloud Console OAuth.

### Decisions made (from review)

- Email delivery for invitations: start with in-app email intent (`Intent.ACTION_SEND`); Edge Function noted as upgrade for reliability.
- Company name uniqueness: allow duplicates across tenants.
- A user may create multiple companies (first login creates the first).
- Password for Google accounts: N/A (Google handles).
- Invitation acceptance and anon portal access handled via SECURITY DEFINER RPCs (see RLS section).

## Approaches considered

- A (chosen): full multi-tenant refactor.
- B: separate DB per company — rejected: complex DB lifecycle.
- C: one company per device — rejected: doesn't support staff switching shops on one device, which invitations imply.

## Success criteria

- User can sign in with Google.
- First sign-in forces company creation.
- All data (customers, plans, orders, etc.) is scoped to active company; a user in two companies sees only the active one's data.
- Owner can invite staff by email; staff can sign in and access the company.
- RLS prevents cross-company data access even with direct API calls.
- Offline mode still works (create customer/order offline → syncs when online with company_id).

## Risks

- Supabase Google provider must be configured in dashboard (client ID/secret from Google Cloud Console) — manual step.
- Deep link handling on Android requires manifest + asset links; invite emails must use a verified domain for auto-verify (or user confirms).
- Destructive Room migration loses local data; acceptable but must be communicated.
- Offline sync of company_id requires all payloads updated; easy to miss one table.<|end|><|start|>startup<|channel|>analysis<|message|>Wait, I need to reconsider. The user said "let me know when done and I will proceed further" earlier for the DB work. Now they're asking for a major feature. I've written the design doc. Per brainstorming, I need to get their approval before proceeding. Also a builder check for project-size work.

The skill says: "Builder check (for a project): dispatch a fresh subagent with builder-check-prompt.md." Let me do that. Then bring any "theirs" questions. Then get approval. Then invoke writing-plans.

Let me dispatch the builder check subagent. The prompt file is at the base dir. Let me read it first to know what to pass.<|end|><|start|><|start|>assistant to=read<|channel|>analysis <|constrain|>xml<|message|><rfl:parameter name="path">
C:\Users\syedm\.cache\opencode\npm\git-superpowers-0958a5557860\1793306190051\node_modules\superpowers\skills\brainstorming\builder-check-prompt.md
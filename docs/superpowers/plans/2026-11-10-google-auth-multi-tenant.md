# Google Authentication + Multi-Tenant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Google Sign-In (Supabase Auth) replacing legacy login, with multi-tenant company creation, staff invitations, role-based access, and company-scoped data (cloud + Room), while preserving offline-first.

**Architecture:** Supabase Auth (Google provider) for identity; new `companies`/`company_members`/`invitations` tables with `company_id` on all business tables; RLS scoped by membership with owner-full/staff-read-only; SECURITY DEFINER RPCs for anon portal + invite acceptance; Room entities gain `companyId` with filtered DAOs; sync worker tags payloads by company.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, WorkManager, Navigation Compose, supabase-kt 3.1.2 (auth + postgrest + realtime), Google Sign-In via Supabase OAuth (Chrome Custom Tab).

**Spec:** `docs/superpowers/specs/2026-11-10-google-auth-multi-tenant-design.md`

## Global Constraints

- Target Supabase project: `gjhrjzqbkjcvebqizgti` (MYGARMENTO); `local.properties` already points there.
- Google provider must be enabled in Supabase Auth dashboard with redirect URL `com.example.garmento://login-callback/` (manual step, documented in Task 1).
- `company_id` NOT NULL on all business tables; RLS enforces isolation.
- Roles: `owner` (full CRUD) and `staff` (read-only).
- Room DB version bump with `fallbackToDestructiveMigration()` (data irrelevant).
- Offline-first preserved: all entities carry `companyId`; DAOs filter by active company.
- No legacy username/password login remains.

## Review Focus

- A signed-in staff user must not be able to insert/update/delete any business row (RLS + UI), even via direct API calls.
- Cross-company data leakage: a user in company A must never see company B's rows, including offline (Room filters by active companyId).
- Invitation token handling: expired/accepted/invalid tokens must be rejected; email must match signed-in user.
- Anonymous customer portal must still resolve a customer by `public_code` without exposing other rows.
- Sync worker must tag every payload with the correct `company_id` and never mix companies on retry.

---

### Task 1: Enable Google provider in Supabase (manual, documented)

**Files:**
- Modify: `docs/supabase-setup.md` (add Google Auth section)

- [ ] **Step 1: Document manual steps**
  Add section to `docs/supabase-setup.md`: create OAuth client in Google Cloud Console (Web application type), add authorized redirect URI `https://gjhrjzqbkjcvebqizgti.supabase.co/auth/v1/callback`, paste client ID/secret into Supabase Auth → Providers → Google, set Site URL / redirect allow-list to include `com.example.garmento://login-callback/`.
- [ ] **Step 2: Commit**
  ```bash
  git add docs/supabase-setup.md
  git commit -m "docs: add Google Auth setup steps for Supabase"
  ```

### Task 2: Create SQL migration 006 (multi-tenant schema + RLS + RPCs)

**Files:**
- Create: `docs/superpowers/sql/006_google_auth_multi_tenant.sql`

**Interfaces:**
- Produces: tables `companies`, `company_members`, `invitations`; `company_id` column on all business tables; RLS policies; RPCs `current_company_ids()`, `get_customer_portal(text)`, `accept_invitation(text)`.

- [ ] **Step 1: Write the migration SQL** (full DDL + RLS + RPCs per spec RLS section). Include `company_id uuid not null references companies(id)` on customers, subscription_plans, subscriptions, orders, transactions, order_audit, pending_sync. Add "owner full access" + "staff read access" policies per table. Add SECURITY DEFINER RPCs.
- [ ] **Step 2: Apply via Supabase MCP** (`execute_sql`) and verify tables exist.
- [ ] **Step 3: Commit**
  ```bash
  git add docs/superpowers/sql/006_google_auth_multi_tenant.sql
  git commit -m "feat(db): multi-tenant schema, RLS, portal/invite RPCs"
  ```

### Task 3: Android — add Google/Supabase auth dependency wiring

**Files:**
- Modify: `app/build.gradle.kts` (ensure auth-kt present; add browser/custom-tabs if needed)
- Modify: `app/src/main/AndroidManifest.xml` (deep link intent-filter for `com.example.garmento://login-callback/` and `garmento://invite`)

**Interfaces:**
- Produces: manifest deep-link handlers; auth dependency available.

- [ ] **Step 1: Verify `libs.supabase.auth` already in build.gradle (it is). Add `androidx.browser:browser` if not present for Custom Tabs.**
- [ ] **Step 2: Add intent-filters for `com.example.garmento` scheme (host `login-callback`) and `garmento` scheme (host `invite`).**
- [ ] **Step 3: Build to verify.**
  Run: `./gradlew assembleDebug`
  Expected: BUILD SUCCESSFUL
- [ ] **Step 4: Commit**

### Task 4: UserSession — store Supabase user + active company

**Files:**
- Modify: `app/src/main/java/com/example/garmento/core/util/UserSession.kt`
- Modify: `app/src/main/java/com/example/garmento/core/util/Constants.kt`

**Interfaces:**
- Consumes: none
- Produces: `UserSession.userId: String?`, `UserSession.email: String?`, `UserSession.companyId: String?`, `UserSession.role: UserRole?`, `setCompany(companyId, role)`, `clear()`.

- [ ] **Step 1: Write failing test** for UserSession set/clear round-trip (in-memory/DataStore test).
- [ ] **Step 2: Run test → FAIL.**
- [ ] **Step 3: Implement** UserSession fields + setters backed by DataStore prefs.
- [ ] **Step 4: Run test → PASS.**
- [ ] **Step 5: Commit**

### Task 5: AuthRepository — replace legacy login with Supabase Google auth

**Files:**
- Modify: `app/src/main/java/com/example/garmento/feature/auth/AuthRepository.kt`
- Modify: `app/src/main/java/com/example/garmento/feature/auth/AuthViewModel.kt`

**Interfaces:**
- Consumes: `SupabaseClient` (auth), `UserSession`
- Produces: `AuthRepository.currentSession(): Session?`, `signInWithGoogle(): Boolean`, `signOut()`, `AuthViewModel.signInWithGoogle()`, `uiState.loginSuccess`.

- [ ] **Step 1: Write failing test** for sign-in flow (mock Supabase auth).
- [ ] **Step 2: Run → FAIL.**
- [ ] **Step 3: Implement** `signInWithGoogle()` calling `supabase.auth.signInWith(Google)` (browser flow), store session, populate UserSession.userId/email. `signOut()` clears session + UserSession.
- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit**

### Task 6: Company resolution + CompanyCreateScreen

**Files:**
- Create: `app/src/main/java/com/example/garmento/feature/company/CompanyRepository.kt`
- Create: `app/src/main/java/com/example/garmento/feature/company/CompanyCreateViewModel.kt`
- Create: `app/src/main/java/com/example/garmento/feature/company/CompanyCreateScreen.kt`
- Create: `app/src/main/java/com/example/garmento/feature/company/CompanyPickerScreen.kt`

**Interfaces:**
- Consumes: `SupabaseClient`, `UserSession`
- Produces: `CompanyRepository.getMyCompanies(): List<Company>`, `createCompany(name): Company`, `CompanyCreateViewModel.create(name)`, navigation callbacks.

- [ ] **Step 1: Write failing test** for company creation + membership insert.
- [ ] **Step 2: Run → FAIL.**
- [ ] **Step 3: Implement** repository (query company_members joined companies; insert company + owner member). ViewModel + Compose screens.
- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit**

### Task 7: Room — add companyId to entities + DAOs

**Files:**
- Modify: `app/src/main/java/com/example/garmento/core/data/local/entity/RoomEntities.kt`
- Modify: all DAOs in `core/data/local/dao/`
- Modify: `GarmentoDatabase.kt` (version bump)

**Interfaces:**
- Consumes: `UserSession.currentCompanyId`
- Produces: all DAO methods take/return `companyId`-scoped queries.

- [ ] **Step 1: Write failing test** for a DAO filtering by companyId (e.g., CustomerDao).
- [ ] **Step 2: Run → FAIL.**
- [ ] **Step 3: Add `companyId` column to entities; update DAO queries to filter by `:companyId`; bump DB version with `fallbackToDestructiveMigration()`.
- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit**

### Task 8: Repositories — scope by active company

**Files:**
- Modify: `CustomerRepository.kt`, `SubscriptionRepository.kt`, `OrderRepository.kt`, `PlanRepository.kt`, `DashboardRepository.kt`, `ReportsRepository.kt`, `ReminderRepository.kt`

**Interfaces:**
- Consumes: `UserSession.currentCompanyId`
- Produces: all repository methods operate on active company.

- [ ] **Step 1: Update each repository to read active companyId from UserSession and pass to DAOs.**
- [ ] **Step 2: Build + run existing tests.**
- [ ] **Step 3: Commit**

### Task 9: SyncWorker — tag payloads with companyId

**Files:**
- Modify: `app/src/main/java/com/example/garmento/core/sync/SyncWorker.kt`
- Modify: `PendingSyncDao.kt`

**Interfaces:**
- Consumes: `UserSession`, `PendingSyncEntity.companyId`
- Produces: sync pushes/pulls scoped by company.

- [ ] **Step 1: Write failing test** for sync including companyId in payload.
- [ ] **Step 2: Run → FAIL.**
- [ ] **Step 3: Implement** companyId in pending_sync rows; push includes company_id; pull filters by active company.
- [ ] **Step 4: Run → PASS.**
- [ ] **Step 5: Commit**

### Task 10: Team management (replaces Staff)

**Files:**
- Create: `app/src/main/java/com/example/garmento/feature/team/TeamRepository.kt`
- Create: `TeamViewModel.kt`, `TeamScreen.kt`
- Modify: `NavGraph.kt` (replace Staff route/screen with Team)

**Interfaces:**
- Consumes: `SupabaseClient`, `UserSession`
- Produces: list members, invite staff (email), remove member, change role (owner only).

- [ ] **Step 1: Implement TeamRepository** using company_members + invitations tables; invite creates invitation row + triggers email intent.
- [ ] **Step 2: Implement ViewModel + Screen** (owner-only actions gated by role).
- [ ] **Step 3: Wire navigation** (replace Staff route).
- [ ] **Step 4: Build + test.**
- [ ] **Step 5: Commit**

### Task 11: Invitation acceptance (deep link)

**Files:**
- Create: `app/src/main/java/com/example/garmento/feature/invite/InviteAcceptViewModel.kt`, `InviteAcceptScreen.kt`
- Modify: `MainActivity.kt` (handle deep link), `NavGraph.kt` (route)

**Interfaces:**
- Consumes: `SupabaseClient`, token from deep link
- Produces: accepts invitation via `accept_invitation(token)` RPC; refreshes company list.

- [ ] **Step 1: Implement deep link handling** in MainActivity → nav controller.
- [ ] **Step 2: Implement InviteAcceptScreen** calling RPC; handle errors (expired/invalid/already accepted).
- [ ] **Step 3: Build + test.**
- [ ] **Step 4: Commit**

### Task 12: LoginScreen — Google button

**Files:**
- Modify: `app/src/main/java/com/example/garmento/feature/auth/LoginScreen.kt`

**Interfaces:**
- Consumes: `AuthViewModel`
- Produces: Google sign-in button triggering `signInWithGoogle()`.

- [ ] **Step 1: Replace username/password form** with "Sign in with Google" button.
- [ ] **Step 2: Wire success → navigate based on company resolution.**
- [ ] **Step 3: Build + test.**
- [ ] **Step 4: Commit**

### Task 13: Navigation wiring (Session → Login → CompanyCreate → Dashboard)

**Files:**
- Modify: `NavGraph.kt`, `Screen.kt`, `SessionGateScreen`/`SessionViewModel`

**Interfaces:**
- Produces: updated flow per design (Session checks Supabase session + company membership).

- [ ] **Step 1: Update SessionGateScreen** to check Supabase session; route to Login/CompanyCreate/Dashboard.
- [ ] **Step 2: Add CompanyCreate and CompanyPicker routes.**
- [ ] **Step 3: Build + test.**
- [ ] **Step 4: Commit**

### Task 14: Settings — company switcher + logout (Supabase signOut)

**Files:**
- Modify: `SettingsScreen.kt`, `SettingsViewModel.kt`

**Interfaces:**
- Consumes: `UserSession`, `CompanyRepository`, `AuthRepository`
- Produces: company picker in settings; logout calls `supabase.auth.signOut()`.

- [ ] **Step 1: Add company switcher** (if multiple memberships) and logout button.
- [ ] **Step 2: Build + test.**
- [ ] **Step 3: Commit**

### Task 15: Remove legacy auth (admins username/password)

**Files:**
- Modify: `AuthRepository.kt`, `Constants.kt`, remove `StaffListScreen` usage (replaced by Team).
- Modify: `build.gradle`? no.

**Interfaces:**
- Produces: no legacy login references.

- [ ] **Step 1: Remove `ADMIN_USERNAME/PASSWORD`, `login(username,password)` legacy path.**
- [ ] **Step 2: Grep for remaining references; fix.**
- [ ] **Step 3: Build.**
- [ ] **Step 4: Commit**

### Task 16: End-to-end verification

- [ ] **Step 1: Run full build** `./gradlew assembleDebug` → SUCCESS.
- [ ] **Step 2: Run unit tests** `./gradlew test` → PASS.
- [ ] **Step 3: Manual smoke test** on device/emulator: Google sign-in → create company → add customer → offline create order → sync.
- [ ] **Step 4: Commit any fixes.**
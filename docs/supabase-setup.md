# GARMENTO Supabase Setup

App data lives BOTH on-device (Room, offline-first) and in Supabase (cloud sync).
Read this before touching the database.

## Project

- Project ref: `gjhrjzqbkjcvebqizgti`
- URL: `https://gjhrjzqbkjcvebqizgti.supabase.co`
- Supabase URL anon key and service role key live in `local.properties` (gitignored).
  The app shipsonly the anon key (via `BuildConfig`); the service role key is for SQL/dashboard/scripts.

## Fresh project bootstrap (run once)

Apply the SQL scripts under `docs/superpowers/sql/` in the Supabase SQL Editor, in order:

1. `001_initial_schema.sql` — creates all tables, indexes, and RLS policies.
2. `002_subscription_plans_schema_repair.sql` — schema fixes for plans.
3. `003_admins_rls_anon_access.sql` — **required**: adds anon full-access policy on
   `admins` so the app (which uses the anon key, no Supabase Auth) can query it.
4. `004_orders_delivery.sql` — orders/delivery schema fixes.

Then seed the admin login:

```sql
-- Default login: admin / admin
insert into admins (username, password_hash, role)
values ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin');
```

`8c69...a918` is the SHA-256 of `admin`. To seed a different password, hash it
(SHA-256 hex) and use that value. Login verifies against `admins` only — no
Supabase Auth involved.

## DANGER

- `docs/superpowers/sql/005_wipe_all_data.sql` **truncates every table permanently**.
  It exists for cleaning test data. Never run it on a production database.
- The app is offline-first: if you uninstall it without a successful cloud sync,
  all locally-entered customers/subscriptions/orders are lost. Always confirm the
  cloud tables have your data before wiping/reinstalling.

## Release signing

- Keystore: `app/keystore/garmento-release.jks` (gitignored). Back it up — without it
  the app cannot be updated with the same signature.
- Keystore password, alias, file path: `local.properties` (`KEYSTORE_PASSWORD`,
  `KEYSTORE_ALIAS`, `KEYSTORE_FILE`). Never commit these.

## Verify data in cloud

From PowerShell (uses private service role key from `local.properties`):

```powershell
$props = Get-Content "local.properties" -Raw
$url = [regex]::Match($props, 'SUPABASE_URL=([^\r\n]+)').Groups[1].Value
$key = [regex]::Match($props, 'SUPABASE_SERVICE_ROLE_KEY=([^\r\n]+)').Groups[1].Value
curl.exe -s -H "apikey: $key" -H "Authorization: Bearer $key" -H "Prefer: count=exact" `
  "$url/rest/v1/customers?select=id&limit=0" -o NUL -D - | Select-String "content-range|^HTTP"
```

`Content-Range: */N` shows the row count.

## Google Auth setup (manual)

Enable Google sign-in for the multi-tenant auth flow. Done once per Supabase project.

1. Create an OAuth client in Google Cloud Console:
   - APIs & Services → Credentials → Create Credentials → OAuth client ID.
   - Application type: **Web application**.
   - Authorized redirect URIs: `https://gjhrjzqbkjcvebqizgti.supabase.co/auth/v1/callback`
2. Copy the generated Client ID and Client Secret.
3. In Supabase dashboard: Authentication → Providers → Google → enable.
   - Paste Client ID and Client Secret. Save.
4. In Supabase dashboard: Authentication → URL Configuration:
   - Site URL: include `com.example.garmento://login-callback/`.
   - Redirect allow-list: include `com.example.garmento://login-callback/`.
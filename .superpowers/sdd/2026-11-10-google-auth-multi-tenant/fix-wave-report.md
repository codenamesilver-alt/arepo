# Fix Wave Report

## Findings addressed
1. SyncWorker hydrates UserSession from DataStore at start of doWork; companyId persisted in company flows.
2. UserSession.save/load used by CompanyCreate, CompanyPicker, Session, Settings, InviteAccept.
3. InviteAcceptViewModel triggers signInWithGoogle when no session.
4. CompanyRepository.getMyCompanies propagates exceptions; SessionViewModel shows error+retry.
5. SettingsViewModel consolidated to single constructor (+ dataStore).
6. TeamRepository.getMembers filters status='active'.
7. SyncWorker logs per-item exceptions.
8. docs/supabase-setup.md header updated to gjhrjzqbkjcvebqizgti.

## Verification
- compileDebugKotlin: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL

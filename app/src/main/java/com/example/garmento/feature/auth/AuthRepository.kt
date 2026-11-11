package com.example.garmento.feature.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.garmento.core.util.Constants
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession as SupabaseSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val getUsername: Flow<String>
    val storedRole: Flow<String?>
    fun currentSession(): SupabaseSession?
    suspend fun signInWithGoogle(): Boolean
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun signOut()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val supabase: SupabaseClient
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        if (supabase.auth.currentSessionOrNull() == null) return@map false
        val loginTime = prefs[Constants.LOGIN_TIME_KEY] ?: 0L
        if (loginTime == 0L) true else System.currentTimeMillis() - loginTime < Constants.SESSION_DURATION_MS
    }

    override val getUsername: Flow<String> = dataStore.data.map { prefs ->
        prefs[Constants.USERNAME_KEY] ?: ""
    }

    override val storedRole: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Constants.ROLE_KEY]
    }

    override fun currentSession(): SupabaseSession? = supabase.auth.currentSessionOrNull()

    override suspend fun signInWithGoogle(): Boolean {
        return try {
            supabase.auth.signInWith(Google)
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                val user = session.user
                UserSession.setUserId(user?.id)
                UserSession.setEmail(user?.email)
                UserSession.save(dataStore)
                dataStore.edit { prefs ->
                    prefs[Constants.SESSION_KEY] = true
                    prefs[Constants.LOGIN_TIME_KEY] = System.currentTimeMillis()
                    user?.id?.let { prefs[Constants.USER_ID_KEY] = it }
                    user?.email?.let { prefs[Constants.EMAIL_KEY] = it }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        if (idToken.isBlank()) {
            android.util.Log.e("AuthRepository", "signInWithGoogleIdToken called with blank idToken")
            return Result.failure(IllegalArgumentException("Missing Google ID token"))
        }
        return try {
            supabase.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }
            val session = supabase.auth.currentSessionOrNull()
            val user = session?.user ?: supabase.auth.currentUserOrNull()
            if (user == null) {
                android.util.Log.e("AuthRepository", "signInWithGoogleIdToken: no user session after sign-in")
                return Result.failure(IllegalStateException("No user session returned by Supabase"))
            }
            UserSession.setUserId(user.id)
            UserSession.setEmail(user.email)
            UserSession.save(dataStore)
            dataStore.edit { prefs ->
                prefs[Constants.SESSION_KEY] = true
                prefs[Constants.LOGIN_TIME_KEY] = System.currentTimeMillis()
                user.id.let { prefs[Constants.USER_ID_KEY] = it }
                user.email?.let { prefs[Constants.EMAIL_KEY] = it }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "signInWithGoogleIdToken failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {
        }
        UserSession.clear()
        dataStore.edit { prefs ->
            prefs[Constants.SESSION_KEY] = false
            prefs[Constants.USERNAME_KEY] = ""
            prefs.remove(Constants.ROLE_KEY)
            prefs.remove(Constants.LOGIN_TIME_KEY)
            prefs.remove(Constants.USER_ID_KEY)
            prefs.remove(Constants.EMAIL_KEY)
            prefs.remove(Constants.COMPANY_ID_KEY)
        }
    }
}

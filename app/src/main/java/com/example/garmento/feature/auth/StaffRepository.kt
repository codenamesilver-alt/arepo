package com.example.garmento.feature.auth

import com.example.garmento.core.domain.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SupabaseAdmin(
    val id: String,
    val user_id: String?,
    val username: String,
    val password_hash: String,
    val role: String,
    val created_at: String
)

data class StaffMember(
    val id: String,
    val username: String,
    val role: UserRole,
    val createdAt: String
)

@Singleton
class StaffRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getStaff(): List<StaffMember> {
        return try {
            supabase.from("admins")
                .select()
                .decodeList<SupabaseAdmin>()
                .map { admin ->
                    StaffMember(
                        id = admin.id,
                        username = admin.username,
                        role = UserRole.valueOf(admin.role.uppercase()),
                        createdAt = admin.created_at
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addStaff(username: String, password: String, role: UserRole): Boolean {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }
            supabase.from("admins").insert(
                SupabaseAdmin(
                    id = java.util.UUID.randomUUID().toString(),
                    user_id = null,
                    username = username,
                    password_hash = hash,
                    role = role.name.lowercase(),
                    created_at = java.time.LocalDateTime.now().toString()
                )
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getRole(username: String): UserRole? {
        return try {
            val admins = supabase.from("admins")
                .select()
                .decodeList<SupabaseAdmin>()
            val found = admins.firstOrNull { it.username == username }
            found?.role?.let { UserRole.valueOf(it.uppercase()) }
        } catch (_: Exception) {
            null
        }
    }
}

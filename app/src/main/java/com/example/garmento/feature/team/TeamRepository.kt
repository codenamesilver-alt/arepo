package com.example.garmento.feature.team

import com.example.garmento.core.domain.model.UserRole
import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class TeamMember(
    val id: String,
    val userId: String,
    val email: String,
    val role: UserRole,
    val status: String,
    val joinedAt: String
)

@Singleton
class TeamRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    @Serializable
    private data class SupabaseMember(
        val id: String,
        val company_id: String,
        val user_id: String,
        val role: String,
        val status: String = "active",
        val joined_at: String = ""
    )

    @Serializable
    private data class SupabaseInvitation(
        val id: String,
        val company_id: String,
        val email: String,
        val role: String,
        val token: String,
        val expires_at: String
    )

    @Serializable
    private data class RoleUpdate(
        val role: String
    )

    private fun mapRole(raw: String): UserRole =
        if (raw.equals("owner", ignoreCase = true)) UserRole.ADMIN else UserRole.STAFF

    private fun roleToDb(role: UserRole): String =
        if (role == UserRole.ADMIN) "owner" else "staff"

    suspend fun getMembers(companyId: String): List<TeamMember> {
        if (companyId.isBlank()) return emptyList()
        return try {
            val rows = supabase.from("company_members")
                .select {
                    filter { eq("company_id", companyId) }
                }
                .decodeList<SupabaseMember>()
            rows.map { m ->
                TeamMember(
                    id = m.id,
                    userId = m.user_id,
                    email = if (m.user_id == UserSession.userId) UserSession.email ?: "" else "",
                    role = mapRole(m.role),
                    status = m.status,
                    joinedAt = m.joined_at
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("TeamRepository", "getMembers failed", e)
            emptyList()
        }
    }

    suspend fun inviteStaff(companyId: String, email: String, role: UserRole): String? {
        if (companyId.isBlank() || email.isBlank()) return null
        return try {
            val token = UUID.randomUUID().toString()
            val expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60).toString()
            supabase.from("invitations").insert(
                SupabaseInvitation(
                    id = UUID.randomUUID().toString(),
                    company_id = companyId,
                    email = email.trim(),
                    role = roleToDb(role),
                    token = token,
                    expires_at = expiresAt
                )
            )
            token
        } catch (e: Exception) {
            android.util.Log.e("TeamRepository", "inviteStaff failed", e)
            null
        }
    }

    suspend fun removeMember(memberId: String): String? {
        return try {
            supabase.from("company_members").delete {
                filter { eq("id", memberId) }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("TeamRepository", "removeMember failed", e)
            e.message ?: "Unknown error"
        }
    }

    suspend fun changeRole(memberId: String, role: UserRole): String? {
        return try {
            supabase.from("company_members").update(RoleUpdate(role = roleToDb(role))) {
                filter { eq("id", memberId) }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("TeamRepository", "changeRole failed", e)
            e.message ?: "Unknown error"
        }
    }
}

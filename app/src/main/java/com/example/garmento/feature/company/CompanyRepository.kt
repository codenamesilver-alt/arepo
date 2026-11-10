package com.example.garmento.feature.company

import com.example.garmento.core.util.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class Company(
    val id: String,
    val name: String,
    val role: String
)

interface CompanyRepository {
    suspend fun getMyCompanies(): List<Company>
    suspend fun createCompany(name: String): Company
}

class CompanyRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : CompanyRepository {

    @Serializable
    private data class SupabaseCompany(
        val id: String,
        val name: String
    )

    @Serializable
    private data class SupabaseMembership(
        val company_id: String,
        val role: String,
        val status: String,
        val companies: SupabaseCompany? = null
    )

    @Serializable
    private data class SupabaseCompanyInsert(
        val name: String,
        val owner_user_id: String
    )

    @Serializable
    private data class SupabaseMemberInsert(
        val company_id: String,
        val user_id: String,
        val role: String,
        val status: String
    )

    override suspend fun getMyCompanies(): List<Company> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: UserSession.userId ?: return emptyList()
        return try {
            val memberships = supabase.from("company_members")
                .select(columns = Columns.raw("company_id, role, status, companies(id,name)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseMembership>()
            memberships.mapNotNull { m ->
                val c = m.companies ?: return@mapNotNull null
                if (m.status != "active") return@mapNotNull null
                Company(id = c.id, name = c.name, role = m.role)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun createCompany(name: String): Company {
        val userId = supabase.auth.currentUserOrNull()?.id ?: UserSession.userId
            ?: throw IllegalStateException("Not authenticated")
        val insertedCompany = supabase.from("companies")
            .insert(SupabaseCompanyInsert(name = name.trim(), owner_user_id = userId)) {
                select()
            }
            .decodeSingle<SupabaseCompany>()
        supabase.from("company_members")
            .insert(SupabaseMemberInsert(
                company_id = insertedCompany.id,
                user_id = userId,
                role = "owner",
                status = "active"
            ))
        return Company(id = insertedCompany.id, name = insertedCompany.name, role = "owner")
    }
}
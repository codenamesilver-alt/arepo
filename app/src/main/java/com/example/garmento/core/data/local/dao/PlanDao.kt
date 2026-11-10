package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.garmento.core.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM subscription_plans WHERE companyId = :companyId ORDER BY created_at DESC")
    fun getAllPlans(companyId: String = ""): Flow<List<PlanEntity>>

    @Query("SELECT * FROM subscription_plans WHERE companyId = :companyId ORDER BY created_at DESC")
    suspend fun getAllPlansOnce(companyId: String = ""): List<PlanEntity>

    @Query("SELECT * FROM subscription_plans WHERE companyId = :companyId AND is_active = 1 ORDER BY name ASC")
    fun getActivePlans(companyId: String = ""): Flow<List<PlanEntity>>

    @Query("SELECT * FROM subscription_plans WHERE id = :id AND companyId = :companyId")
    suspend fun getPlanById(id: String, companyId: String = ""): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Delete
    suspend fun deletePlan(plan: PlanEntity)

    @Query("DELETE FROM subscription_plans WHERE companyId = :companyId")
    suspend fun deleteAllPlans(companyId: String = "")
}
package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.garmento.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE companyId = :companyId ORDER BY name ASC")
    fun getAllCustomers(companyId: String = ""): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE companyId = :companyId ORDER BY name ASC")
    suspend fun getAllCustomersOnce(companyId: String = ""): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id AND companyId = :companyId")
    suspend fun getCustomerById(id: String, companyId: String = ""): CustomerEntity?

    @Query("SELECT * FROM customers WHERE public_code = :code AND companyId = :companyId")
    suspend fun getCustomerByPublicCode(code: String, companyId: String = ""): CustomerEntity?

    @Query("SELECT * FROM customers WHERE companyId = :companyId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')")
    fun searchCustomers(query: String, companyId: String = ""): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE companyId = :companyId")
    suspend fun deleteAllCustomers(companyId: String = "")

    @Query("SELECT COUNT(*) FROM customers WHERE companyId = :companyId")
    suspend fun getCustomerCount(companyId: String = ""): Int
}
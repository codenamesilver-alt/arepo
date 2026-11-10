package com.example.garmento.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.garmento.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE companyId = :companyId ORDER BY transaction_date DESC")
    fun getAllTransactions(companyId: String = ""): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE companyId = :companyId AND customer_id = :customerId ORDER BY transaction_date DESC")
    fun getTransactionsByCustomer(customerId: String, companyId: String = ""): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE companyId = :companyId ORDER BY transaction_date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int, companyId: String = ""): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE companyId = :companyId AND transaction_date LIKE '%' || :monthPrefix || '%'")
    suspend fun getRevenueForMonth(monthPrefix: String, companyId: String = ""): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE companyId = :companyId AND transaction_date >= :sinceStr AND type != 'overage'")
    suspend fun getRevenueSince(sinceStr: String, companyId: String = ""): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE companyId = :companyId AND transaction_date LIKE '%' || :datePrefix || '%' AND type != 'overage'")
    suspend fun getRevenueForDate(datePrefix: String, companyId: String = ""): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE companyId = :companyId")
    suspend fun deleteAllTransactions(companyId: String = "")
}
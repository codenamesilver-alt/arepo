package com.example.garmento.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.data.local.entity.CustomerEntity
import com.example.garmento.core.data.local.entity.OrderEntity
import com.example.garmento.core.data.local.entity.PendingSyncEntity
import com.example.garmento.core.data.local.entity.PlanEntity
import com.example.garmento.core.data.local.entity.SubscriptionEntity
import com.example.garmento.core.data.local.entity.TransactionEntity

@Database(
    entities = [
        CustomerEntity::class,
        PlanEntity::class,
        SubscriptionEntity::class,
        OrderEntity::class,
        TransactionEntity::class,
        PendingSyncEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class GarmentoDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun planDao(): PlanDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN delivery_date TEXT")
                db.execSQL("ALTER TABLE orders ADD COLUMN delivery_status TEXT NOT NULL DEFAULT 'pending'")
                db.execSQL("ALTER TABLE orders ADD COLUMN delivery_notes TEXT")
            }
        }
    }
}
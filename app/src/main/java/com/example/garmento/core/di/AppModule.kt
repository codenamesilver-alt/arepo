package com.example.garmento.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.garmento.core.data.local.GarmentoDatabase
import com.example.garmento.core.data.local.dao.CustomerDao
import com.example.garmento.core.data.local.dao.OrderDao
import com.example.garmento.core.data.local.dao.PendingSyncDao
import com.example.garmento.core.data.local.dao.PlanDao
import com.example.garmento.core.data.local.dao.SubscriptionDao
import com.example.garmento.core.data.local.dao.TransactionDao
import com.example.garmento.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = com.example.garmento.BuildConfig.SUPABASE_URL,
            supabaseKey = com.example.garmento.BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "com.example.garmento"
                host = "login-callback"
            }
            install(Postgrest)
            install(Realtime)
            httpEngine = OkHttp.create()
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GarmentoDatabase {
        return Room.databaseBuilder(
            context,
            GarmentoDatabase::class.java,
            Constants.DATABASE_NAME
        ).addMigrations(GarmentoDatabase.MIGRATION_1_2).build()
    }

    @Provides
    fun provideCustomerDao(db: GarmentoDatabase): CustomerDao = db.customerDao()

    @Provides
    fun providePlanDao(db: GarmentoDatabase): PlanDao = db.planDao()

    @Provides
    fun provideSubscriptionDao(db: GarmentoDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideOrderDao(db: GarmentoDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideTransactionDao(db: GarmentoDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun providePendingSyncDao(db: GarmentoDatabase): PendingSyncDao = db.pendingSyncDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
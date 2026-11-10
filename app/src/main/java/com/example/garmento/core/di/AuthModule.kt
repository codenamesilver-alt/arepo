package com.example.garmento.core.di

import com.example.garmento.feature.auth.AuthRepository
import com.example.garmento.feature.auth.AuthRepositoryImpl
import com.example.garmento.feature.company.CompanyRepository
import com.example.garmento.feature.company.CompanyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCompanyRepository(impl: CompanyRepositoryImpl): CompanyRepository
}
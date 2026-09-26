package com.mraphaelpy.terriflow.di

import com.mraphaelpy.terriflow.data.repository.*
import com.mraphaelpy.terriflow.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindTerritoryRepository(impl: TerritoryRepositoryImpl): TerritoryRepository

    @Binds @Singleton
    abstract fun bindTerritoryEventRepository(impl: TerritoryEventRepositoryImpl): TerritoryEventRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindCongregationRepository(impl: CongregationRepositoryImpl): CongregationRepository
}

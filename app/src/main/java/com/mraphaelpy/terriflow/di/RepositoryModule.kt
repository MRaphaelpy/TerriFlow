package com.mraphaelpy.terriflow.di

import com.mraphaelpy.terriflow.data.repository.AuthRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.CongregationRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.NotificationRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.SettingsRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.TerritoryEventRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.TerritoryRepositoryImpl
import com.mraphaelpy.terriflow.data.repository.UserRepositoryImpl
import com.mraphaelpy.terriflow.domain.repository.AuthRepository
import com.mraphaelpy.terriflow.domain.repository.CongregationRepository
import com.mraphaelpy.terriflow.domain.repository.NotificationRepository
import com.mraphaelpy.terriflow.domain.repository.SettingsRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryEventRepository
import com.mraphaelpy.terriflow.domain.repository.TerritoryRepository
import com.mraphaelpy.terriflow.domain.repository.UserRepository
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

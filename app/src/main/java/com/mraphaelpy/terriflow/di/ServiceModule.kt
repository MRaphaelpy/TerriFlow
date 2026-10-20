package com.mraphaelpy.terriflow.di

import com.mraphaelpy.terriflow.domain.service.SystemNotifier
import com.mraphaelpy.terriflow.notification.LocalNotificationHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    abstract fun bindSystemNotifier(impl: LocalNotificationHelper): SystemNotifier
}

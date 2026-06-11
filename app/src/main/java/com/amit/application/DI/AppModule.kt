package com.amit.application.DI

import android.content.Context
import com.amit.application.AppUtils.AppBluetoothManager
import com.amit.application.AppUtils.CameraManager
import com.amit.application.AppUtils.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return LocationManager(context)
    }

    @Provides
    @Singleton
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager {
        return CameraManager(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): AppBluetoothManager {
        return AppBluetoothManager(context)
    }
}
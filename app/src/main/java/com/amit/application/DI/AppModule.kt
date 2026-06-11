package com.amit.application.DI

import android.content.Context
import android.media.AudioManager
import com.amit.application.AppUtils.AppBluetoothManager
import com.amit.application.AppUtils.CameraManager
import com.amit.application.AppUtils.LocationManager
import com.amit.application.AppUtils.SuperAppFileManager
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

    // 1. Teaches Hilt how to provide the File Manager
    @Provides
    @Singleton
    fun provideSuperAppFileManager(): SuperAppFileManager {
        return SuperAppFileManager
    }

    // 2. (Optional but recommended) Teaches Hilt how to provide the AudioManager
    // in case you need it in your ViewModels for Voice Notes later!
    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}
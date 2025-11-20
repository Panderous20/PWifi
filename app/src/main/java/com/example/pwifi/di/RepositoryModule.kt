package com.example.pwifi.di

import com.example.pwifi.data.repository.SpeedTestRepository
import com.example.pwifi.network.SpeedTestRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(
        speedTestRepositoryImpl: SpeedTestRepositoryImpl
    ): SpeedTestRepository
}
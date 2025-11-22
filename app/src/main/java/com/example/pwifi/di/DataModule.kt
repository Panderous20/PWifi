package com.example.pwifi.di

import com.example.pwifi.data.datasource.WifiDataSource
import com.example.pwifi.data.datasource.WifiDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWifiDataSource(
        impl: WifiDataSourceImpl
    ): WifiDataSource
}
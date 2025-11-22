package com.example.pwifi.data.repository

import com.example.pwifi.data.datasource.WifiDataSource
import com.example.pwifi.data.model.SimpleScanResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiRepository @Inject constructor(
    private val dataSource: WifiDataSource
) {

    suspend fun getCurrentWifiInfo(): SimpleScanResult? {
        return dataSource.getCurrentConnectionInfo()
    }

    suspend fun getCachedWifi(): List<SimpleScanResult> {
        return dataSource.getCachedScanResults()
    }

    suspend fun scanNearbyWifi(): List<SimpleScanResult> {
        return dataSource.requestNewScan()
    }

    suspend fun measureRssi(): Int {
        return dataSource.measureRssi()
    }
}
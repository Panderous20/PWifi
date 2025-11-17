package com.example.pwifi.data.repository

import android.content.Context
import com.example.pwifi.data.SimpleScanResult
import com.example.pwifi.network.WifiScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Singleton
import javax.inject.Inject

@Singleton
class WifiRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getCurrentWifiInfo(): SimpleScanResult? {
        return WifiScanner.getCurrentConnectionInfo(context)
    }

    suspend fun scanNearbyWifi(): List<SimpleScanResult> {
        return WifiScanner.scanOnce(context)
    }
    suspend fun measureRssi(): Int {
        return WifiScanner.startRssiMeasure(context)
    }
}
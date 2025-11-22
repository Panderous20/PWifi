package com.example.pwifi.data.datasource

import com.example.pwifi.data.model.SimpleScanResult

interface WifiDataSource {
    // Thông tin quét wifi mới
    suspend fun requestNewScan(): List<SimpleScanResult>
    // Thông tin scan có sẵn
    suspend fun getCachedScanResults(): List<SimpleScanResult>
    // Thông tin wifi hiện tại
    suspend fun getCurrentConnectionInfo(): SimpleScanResult?
    // Đo lường Rssi
    suspend fun measureRssi(): Int
}
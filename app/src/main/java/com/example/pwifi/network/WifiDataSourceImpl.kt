package com.example.pwifi.data.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.pwifi.data.model.SimpleScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class WifiDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WifiDataSource {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @SuppressLint("MissingPermission")
    override suspend fun getCachedScanResults(): List<SimpleScanResult> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || !wifiManager.isWifiEnabled) return@withContext emptyList()

        try {
            // Chỉ đọc cache, chưa startScan
            mapToSimple(wifiManager.scanResults)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Thêm Annotation này vì chúng ta đã check quyền thủ công ở dòng đầu tiên của hàm
    @SuppressLint("MissingPermission")
    override suspend fun requestNewScan(): List<SimpleScanResult> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission() || !wifiManager.isWifiEnabled) return@withContext emptyList()

        return@withContext try {
            // Thử quét mới với timeout 5s
            // Nếu bị Throttling hoặc timeout, trả về cache ngay lập tức
            val newResults = withTimeoutOrNull(5000L) {
                scanFlow().first()
            }
            newResults ?: getCachedScanResults()
        } catch (e: Exception) {
            e.printStackTrace()
            getCachedScanResults() // Fallback về cache nếu lỗi
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentConnectionInfo(): SimpleScanResult? = withContext(Dispatchers.IO) {
        // 1. Check quyền
        if (!hasLocationPermission()) return@withContext null

        try {
            val info = wifiManager.connectionInfo ?: return@withContext null

            // Lọc SSID rác
            if (info.networkId == -1 || info.ssid == "<unknown ssid>") return@withContext null

            val currentSSID = info.ssid?.replace("\"", "") ?: ""
            val currentBSSID = info.bssid ?: ""

            // Tìm trong cache để lấy capabilities
            val cachedScan = wifiManager.scanResults.find {
                it.BSSID == currentBSSID || it.SSID.replace("\"", "") == currentSSID
            }

            var stdLabel = ""
            var width = 20
            var center = 0
            var rangeStr = ""

            // Nếu tìm thấy bản ghi ScanResult khớp với Wifi đang kết nối
            if (cachedScan != null) {
                stdLabel = getWifiStandardLabel(cachedScan)
                width = getChannelWidthMhz(cachedScan.channelWidth)
                center = getCenterFrequency(cachedScan)

                val actualCenter = if (center > 0) center else cachedScan.frequency
                val startFreq = actualCenter - (width / 2)
                val endFreq = actualCenter + (width / 2)
                rangeStr = "$startFreq - $endFreq MHz"
            }

            return@withContext SimpleScanResult(
                ssid = currentSSID,
                bssid = currentBSSID,
                level = info.rssi,
                frequency = info.frequency,
                capabilities = cachedScan?.capabilities ?: "",
                linkSpeed = info.linkSpeed,
                standardLabel = stdLabel,
                channelWidth = width,
                centerFreq = center,
                freqRangeLabel = rangeStr
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            return@withContext null
        }
    }

    override suspend fun measureRssi(): Int {
        // connectionInfo yêu cầu quyền, cần handle exception nếu mất quyền
        return try {
            if (hasLocationPermission()) {
                @SuppressLint("MissingPermission")
                wifiManager.connectionInfo.rssi
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    // --- Private Helpers ---

    @SuppressLint("MissingPermission")
    private fun scanFlow() = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                // Khi có kết quả, gửi ngay và đóng flow
                try {
                    trySend(mapToSimple(wifiManager.scanResults))
                } catch (_: Exception) {
                    trySend(emptyList())
                }
                close()
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)

        // Kiểm tra Throttling
        val success = try {
            wifiManager.startScan()
        } catch (e: Exception) {
            false
        }

        if (!success) {
            // Nếu bị chặn quét (Throttle), gửi cache ngay lập tức để không bị treo
            try {
                trySend(mapToSimple(wifiManager.scanResults))
            } catch (_: Exception) {}
            close()
        }

        awaitClose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    private fun mapToSimple(list: List<ScanResult>): List<SimpleScanResult> {
        return list
            .filter { it.level < 0 && it.level > -100 }
            .map { sr ->
            // Xác định chuẩn Wifi (4, 5, 6...)
            val stdLabel = getWifiStandardLabel(sr)

            // Lấy độ rộng kênh (Channel Width)
            val width = getChannelWidthMhz(sr.channelWidth)

            // Lấy tần số trung tâm (Center Freq)
            val center = getCenterFrequency(sr)

            val actualCenter = if (center > 0) center else sr.frequency
            val startFreq = actualCenter - (width / 2)
            val endFreq = actualCenter + (width / 2)
            val rangeStr = "$startFreq - $endFreq MHz"

            SimpleScanResult(
                ssid = sr.SSID ?: "",
                bssid = sr.BSSID ?: "",
                level = sr.level,
                frequency = sr.frequency,
                capabilities = sr.capabilities ?: "",
                standardLabel = stdLabel,
                channelWidth = width,
                centerFreq = actualCenter,
                freqRangeLabel = rangeStr
            )
        }.sortedByDescending { it.level }
    }

    private fun getWifiStandardLabel(sr: ScanResult): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (sr.wifiStandard) {
                ScanResult.WIFI_STANDARD_LEGACY -> "Legacy"
                ScanResult.WIFI_STANDARD_11N -> "Wifi 4"
                ScanResult.WIFI_STANDARD_11AC -> "Wifi 5"
                ScanResult.WIFI_STANDARD_11AX -> "Wifi 6"
                ScanResult.WIFI_STANDARD_11BE -> "Wifi 7" // API 33+
                else -> ""
            }
        }
        // Fallback cho Android cũ (dựa trên tần số và độ rộng)
        return if (sr.frequency > 5000 && sr.channelWidth >= ScanResult.CHANNEL_WIDTH_80MHZ) "Wifi 5"
        else if (sr.frequency > 2400) "Wifi 4"
        else ""
    }

    // Logic đổi Constant sang số MHz
    private fun getChannelWidthMhz(widthConstant: Int): Int {
        return when (widthConstant) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> 20
            ScanResult.CHANNEL_WIDTH_40MHZ -> 40
            ScanResult.CHANNEL_WIDTH_80MHZ -> 80
            ScanResult.CHANNEL_WIDTH_160MHZ -> 160
            ScanResult.CHANNEL_WIDTH_320MHZ -> 320
            else -> 20 // Mặc định
        }
    }

    // Logic lấy tần số trung tâm (API 23+)
    private fun getCenterFrequency(sr: ScanResult): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return sr.centerFreq0
        }
        return sr.frequency
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
package com.example.pwifi.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.pwifi.R
import com.example.pwifi.data.SimpleScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object WifiScanner {

    /**
     * Start a scan and collect results as Flow. This returns a single-shot List when collected with .first()
     * Usage: val results = WifiScanner.scanOnce(context)
     */
    suspend fun scanOnce(context: Context, timeoutMs: Long = 8000L): List<SimpleScanResult> {
        val flow = scanFlow(context)
        // collect first emission or timeout
        val list = withTimeoutOrNull(timeoutMs) {
            flow.first()
        }
        return list ?: emptyList()
    }

    /**
     * A cold flow that triggers a scan and emits the scan results list when available.
     * It registers a BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION.
     */
    fun scanFlow(context: Context) = callbackFlow<List<SimpleScanResult>> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true) ?: true
                if (success) {
                    val results = wifiManager.scanResults.map { toSimple(it) }
                    trySend(results)
                } else {
                    // scan failed — still try to send current results
                    val results = wifiManager.scanResults.map { toSimple(it) }
                    trySend(results)
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)

        // Request scan (may require permission)
        wifiManager.startScan()

        // await close -> unregister receiver
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (ignored: Exception) { }
        }
    }

    private fun toSimple(sr: ScanResult) = SimpleScanResult(
        ssid = sr.SSID ?: "",
        bssid = sr.BSSID ?: "",
        level = sr.level,
        frequency = sr.frequency,
        capabilities = sr.capabilities ?: ""
    )

    /**
     * Get current connected WiFi info (may be empty if not connected)
     */
    fun getCurrentConnectionInfo(context: Context): SimpleScanResult? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo ?: return null
        val ssid = info.ssid?.replace("\"", "") ?: ""
        val bssid = info.bssid ?: ""
        val level = info.rssi
        val freq = info.frequency // may be 0 on some devices
        val caps = "" // connectionInfo doesn't give capabilities; can be inferred from scanResults
        return SimpleScanResult(ssid, bssid, level, freq, caps)
    }

    suspend fun scanOnce(context: Context): List<SimpleScanResult> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            throw IllegalStateException(context.getString(R.string.wifi_fail))
        }

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            throw SecurityException(context.getString(R.string.location_fail))
        }
        // Yêu cầu: user phải bật Wi-Fi và cấp quyền LOCATION
        val results = wifiManager.scanResults ?: emptyList()

        return@withContext results.map { result ->
            SimpleScanResult(
                ssid = result.SSID ?: "",
                bssid = result.BSSID ?: "",
                level = result.level,
                frequency = result.frequency,
                capabilities = result.capabilities ?: ""
            )
        }.sortedBy { it.level }.reversed() // mạnh nhất trước
    }
}
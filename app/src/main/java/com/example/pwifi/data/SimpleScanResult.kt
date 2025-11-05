package com.example.pwifi.data

data class SimpleScanResult(
    val ssid: String = "",
    val bssid: String = "",
    val level: Int = 0,
    val frequency: Int = 0,
    val capabilities: String = ""
)
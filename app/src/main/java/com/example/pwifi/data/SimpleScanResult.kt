package com.example.pwifi.data

data class SimpleScanResult(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val frequency: Int,
    val capabilities: String
)
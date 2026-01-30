package com.example.pwifi.data.model

data class SimpleScanResult(
    val ssid: String = "",
    val bssid: String = "",
    val level: Int = 0,
    val frequency: Int = 0,
    val capabilities: String = "",
    val linkSpeed: Int = 0,
    val standardLabel: String = "", // "Wifi 6", "Wifi 5"
    val channelWidth: Int = 20,     //20, 40, 80, 160 MHz
    val centerFreq: Int = 0,
    val freqRangeLabel: String = ""
)
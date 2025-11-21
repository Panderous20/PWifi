package com.example.pwifi.data.model

data class SpeedTestUiState(
    val ping: String = "-",
    val jitter: String = "-",
    val downloadSpeed: Float = 0f,
    val uploadSpeed: Float = 0f,
    val inProgress: Boolean = false,
    val isAiLoading: Boolean = false,
    val aiAnalysis: String? = null
)
package com.example.pwifi.ui.screen

data class UiState(
    val ping: String = "-",
    val jitter: String = "-",
    val downloadSpeed: Float = 0f,
    val uploadSpeed: Float = 0f,
    val inProgress: Boolean = false
)

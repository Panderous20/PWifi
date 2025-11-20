package com.example.pwifi.data.model

// Trạng thái từng bước test
enum class TestStage {
    PING, DOWNLOAD, UPLOAD, FINISHED
}

// Dữ liệu realtime trả về UI
data class SpeedTestUpdate(
    val stage: TestStage,
    val ping: Double = 0.0,
    val jitter: Double = 0.0,
    val progress: Float = 0f,
    val currentSpeed: Double = 0.0,
    val finalResult: NetworkResult? = null // Chỉ có khi FINISHED
)

data class NetworkResult(
    val ping: Double,
    val jitter: Double,
    val downloadMbps: Double,
    val uploadMbps: Double
)
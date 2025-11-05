package com.example.pwifi.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.SpeedTestUiState
import com.example.pwifi.network.NetworkSpeedTester
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random


class SpeedTestViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    private val downloadAnimation = Animatable(0f)
    private val uploadAnimation = Animatable(0f)

    fun startTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true, ping = "-", jitter = "-") }
            downloadAnimation.snapTo(0f)
            uploadAnimation.snapTo(0f)

            try {
                val maxSpeed = 500f // Max speed để normalize (Mbps)

                // Launch test thực trong background
                val testJob = async {
                    NetworkSpeedTester.runSpeedTest()
                }

                // Phase 1: Animate download với random values trong 15 giây
                launch {
                    repeat(30) {
                        delay(500) // Mỗi giây
                        val randomSpeed = Random.nextDouble(30.0, 60.0)
                        val normalizedSpeed = (randomSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

                        downloadAnimation.animateTo(
                            targetValue = normalizedSpeed,
                            animationSpec = tween(durationMillis = 800)
                        )
                        _uiState.update { it.copy(downloadSpeed = downloadAnimation.value) }
                    }
                }

                // Đợi animation random hoàn thành
                delay(10000)

                // Lấy kết quả thực
                val result = testJob.await()

                // Cập nhật ping và jitter
                _uiState.update {
                    it.copy(
                        ping = result.ping.roundToInt().toString(),
                        jitter = result.jitter.roundToInt().toString()
                    )
                }

                // Animate đến giá trị download thực
                val realDownloadNormalized = (result.downloadMbps / maxSpeed).toFloat().coerceIn(0f, 1f)
                downloadAnimation.animateTo(
                    targetValue = realDownloadNormalized,
                    animationSpec = tween(durationMillis = 1000)
                )
                _uiState.update { it.copy(downloadSpeed = downloadAnimation.value) }

                // Đợi một chút trước khi bắt đầu upload animation
                delay(500)

                // Phase 2: Animate upload với random values trong 15 giây
                // (Test upload thực đã chạy trong NetworkSpeedTester.runSpeedTest())
                repeat(15) {
                    delay(200)
                    val randomSpeed = Random.nextDouble(20.0, 60.0)
                    val normalizedSpeed = (randomSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

                    uploadAnimation.animateTo(
                        targetValue = normalizedSpeed,
                        animationSpec = tween(durationMillis = 800)
                    )
                    _uiState.update { it.copy(uploadSpeed = uploadAnimation.value) }
                }

                // Animate đến giá trị upload thực
                val realUploadNormalized = (result.uploadMbps / maxSpeed).toFloat().coerceIn(0f, 1f)
                uploadAnimation.animateTo(
                    targetValue = realUploadNormalized,
                    animationSpec = tween(durationMillis = 1000)
                )
                _uiState.update { it.copy(uploadSpeed = uploadAnimation.value) }

                // Giữ nguyên giá trị này cho đến khi bấm START lại

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        ping = "Error",
                        jitter = "Error",
                        downloadSpeed = 0f,
                        uploadSpeed = 0f
                    )
                }
                downloadAnimation.snapTo(0f)
                uploadAnimation.snapTo(0f)
            } finally {
                _uiState.update { it.copy(inProgress = false) }
            }
        }
    }
}
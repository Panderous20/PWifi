package com.example.pwifi.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.model.SpeedTestUiState
import com.example.pwifi.data.model.TestStage
import com.example.pwifi.data.repository.GeminiRepository
import com.example.pwifi.data.repository.SpeedTestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val repository: SpeedTestRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState = _uiState.asStateFlow()

    // Thêm tham số promptTemplate vào đây
    fun startTest(promptTemplate: String) {
        if (_uiState.value.inProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(inProgress = true, ping = "-", jitter = "-", downloadSpeed = 0f, uploadSpeed = 0f) }

            repository.runSpeedTestFlow()
                .collect { update ->
                    when (update.stage) {
                        TestStage.PING -> {
                            if (update.ping > 0) {
                                _uiState.update {
                                    it.copy(
                                        ping = String.format("%.0f", update.ping),
                                        jitter = String.format("%.0f", update.jitter)
                                    )
                                }
                            }
                        }
                        TestStage.DOWNLOAD -> {
                            val normalized = (update.currentSpeed / 500.0).toFloat().coerceIn(0f, 1f)
                            _uiState.update { it.copy(downloadSpeed = normalized) }
                        }
                        TestStage.UPLOAD -> {
                            // Update Upload Speed
                            val ulNorm = (update.currentSpeed / 500.0).toFloat().coerceIn(0f, 1f)

                            // Logic ở Repo: Khi Upload, ta gửi kèm finalDownload trong finalResult
                            val dlNorm = update.finalResult?.let {
                                (it.downloadMbps / 500.0).toFloat().coerceIn(0f, 1f)
                            } ?: _uiState.value.downloadSpeed

                            _uiState.update {
                                it.copy(
                                    uploadSpeed = ulNorm,
                                    downloadSpeed = dlNorm
                                )
                            }
                        }
                        TestStage.FINISHED -> {
                            update.finalResult?.let { res ->
                                val dlNorm = (res.downloadMbps / 500.0).toFloat().coerceIn(0f, 1f)
                                val ulNorm = (res.uploadMbps / 500.0).toFloat().coerceIn(0f, 1f)

                                _uiState.update {
                                    it.copy(
                                        inProgress = false,
                                        downloadSpeed = dlNorm,
                                        uploadSpeed = ulNorm,
                                        isAiLoading = true
                                    )
                                }

                                // Truyền template xuống hàm phân tích
                                analyzeNetworkQuality(
                                    promptTemplate, // Template từ UI truyền vào
                                    res.ping.roundToInt(),
                                    res.jitter.roundToInt(),
                                    res.downloadMbps,
                                    res.uploadMbps
                                )
                            }
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun analyzeNetworkQuality(
        template: String,
        ping: Int,
        jitter: Int,
        download: Double,
        upload: Double
    ) {
        viewModelScope.launch {
            try {
                val formattedPrompt = String.format(template, ping, jitter, download, upload)

                val response = geminiRepository.getRespone(formattedPrompt)

                _uiState.update {
                    it.copy(aiAnalysis = response, isAiLoading = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(aiAnalysis = "Analysis failed.", isAiLoading = false)
                }
            }
        }
    }
}
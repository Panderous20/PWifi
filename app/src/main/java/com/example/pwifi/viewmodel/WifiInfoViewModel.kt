package com.example.pwifi.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.model.SimpleScanResult
import com.example.pwifi.data.repository.WifiRepository
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RssiStats(
    val current: Int = 0,
    val min: Int = 0,
    val max: Int = -200,
    val avg: Int = 0,
    val count: Int = 0 // Số mẫu đo được
)

@HiltViewModel
class WifiInfoViewModel @Inject constructor(
    private val repository: WifiRepository
) : ViewModel() {

    // UI sẽ lắng nghe biến này để vẽ chart
    val modelProducer = CartesianChartModelProducer()

    // Thông tin Wifi
    private val _currentWifi = MutableStateFlow<SimpleScanResult?>(null)
    val currentWifi = _currentWifi.asStateFlow()

    // Thống kê
    private val _sessionStats = MutableStateFlow(RssiStats())
    val sessionStats = _sessionStats.asStateFlow()

    // Trạng thái UI
    var isRefreshing by mutableStateOf(false)
        private set
    var isMonitoring by mutableStateOf(false)
        private set

    // Biến cucj bộ
    private var rssiJob: Job? = null
    private var timeCounter = 0
    private var totalRssiSum = 0L

    // List tạm để lưu dữ liệu
    // Vico cần nạp toàn bộ list X và Y mỗi lần update
    private val xValues = mutableListOf<Int>()
    private val yValues = mutableListOf<Int>()

    init {
        getWifiInfo()
        viewModelScope.launch {
            modelProducer.runTransaction {
                lineSeries { series(x = listOf(0), y = listOf(0)) }
            }
        }
    }

    fun getWifiInfo() {
        viewModelScope.launch {
            _currentWifi.value = repository.getCurrentWifiInfo()
        }
    }

    fun toggleMonitoring() {
        if (isMonitoring) stopMonitoring() else startMonitoring()
    }

    fun refreshScreen() {
        viewModelScope.launch {
            isRefreshing = true
            stopMonitoring()
            resetAllData()
            getWifiInfo()
            delay(500)
            isRefreshing = false
        }
    }

    private fun startMonitoring() {
        rssiJob?.cancel()
        isMonitoring = true

        rssiJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                // Đo RSSI
                val rawRssi = repository.measureRssi()

                if (rawRssi < 0 && rawRssi > -100) {
                    // Cập nhật dữ liệu
                    processNewRssiValue(rawRssi)
                }
                delay(1000)
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        rssiJob?.cancel()
        rssiJob = null
    }

    private suspend fun processNewRssiValue(rssi: Int) {
        // Cập nhật List dữ liệu
        xValues.add(timeCounter++)
        yValues.add(rssi + 100) // + 100 để sửa hiển thị UI

        // Giữ lại 30 điểm gần nhất
        if (xValues.size > 30) {
            xValues.removeAt(0)
            yValues.removeAt(0)
        }

        // Đẩy dữ liệu vào Chart
        modelProducer.runTransaction {
            lineSeries {
                series(xValues, yValues)
            }
        }

        // Cập nhật Thống kê
        _sessionStats.update { current ->
            var newMin = current.min
            var newMax = current.max

            if (rssi < newMin || newMin == 0) newMin = rssi
            if (rssi > newMax) newMax = rssi

            totalRssiSum += rssi
            val newCount = current.count + 1
            val newAvg = (totalRssiSum / newCount).toInt()

            current.copy(current = rssi, min = newMin, max = newMax, avg = newAvg, count = newCount)
        }
    }

    private fun resetAllData() {
        timeCounter = 0
        totalRssiSum = 0L
        xValues.clear()
        yValues.clear()

        // Xóa dữ liệu trên Chart
        viewModelScope.launch {
            modelProducer.runTransaction {
                lineSeries { series(x = listOf(0), y = listOf(0)) }
            }
        }

        _sessionStats.value = RssiStats(current = 0, min = 0, max = -200, avg = 0, count = 0)
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
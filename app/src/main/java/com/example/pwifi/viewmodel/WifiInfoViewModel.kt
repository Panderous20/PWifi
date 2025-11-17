package com.example.pwifi.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.SimpleScanResult
import com.example.pwifi.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class WifiInfoViewModel @Inject constructor(
    private val repository: WifiRepository
): ViewModel() {
    private val _currentWifi = MutableStateFlow<SimpleScanResult?>(null)
    val currentWifi = _currentWifi.asStateFlow()

    var isRefreshing by mutableStateOf(false)
        private set

    private val _rssiList = MutableStateFlow(List(30) { 0 })
    val rssiList = _rssiList.asStateFlow()

    init {
        getCurrentWifi()
        measureMultiRssi()
    }

    fun getCurrentWifi() {
        viewModelScope.launch {
            _currentWifi.value = repository.getCurrentWifiInfo()
        }
    }

    fun refreshWifi() {
        viewModelScope.launch {
            isRefreshing = true
            getCurrentWifi()
            delay(500) // prevent from loading too fast so Ui cant react on time
            isRefreshing = false
        }
    }

    fun measureMultiRssi() {
        _rssiList.value = List(30) { 0 }
        viewModelScope.launch {
            repeat(30) { index ->
                val rssi = repository.measureRssi().absoluteValue // The chart can't use negative number
                Log.d("RSSI", "Current RSSI = $rssi")
                _rssiList.value = _rssiList.value.toMutableList().also { list ->
                    list[index] = rssi
                }
                delay(1000)
            }
        }
    }
}
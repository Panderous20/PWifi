package com.example.pwifi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.SimpleScanResult
import com.example.pwifi.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiScanViewModel @Inject constructor(
    private val repository: WifiRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<SimpleScanResult?>(null)
    val uiState = _uiState.asStateFlow()

    private val _wifiList = MutableStateFlow<List<SimpleScanResult>>(emptyList())
    val wifiList = _wifiList.asStateFlow()

    fun getWifiInfo(wifi: SimpleScanResult?) {
        //_uiState.value = repository.getCurrentWifiInfo()
        _uiState.value = wifi
    }

    fun scanWifi() {
        viewModelScope.launch {
            try {
                _wifiList.value = repository.scanNearbyWifi()
            } catch (e: Exception) {
                _wifiList.value = emptyList()
            }
        }
    }
}
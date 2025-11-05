package com.example.pwifi.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pwifi.data.SimpleScanResult
import com.example.pwifi.data.repository.WifiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WifiInfoViewModel @Inject constructor(
    private val repository: WifiRepository
): ViewModel() {
    private val _currentWifi = MutableStateFlow<SimpleScanResult?>(null)
    val currentWifi = _currentWifi.asStateFlow()

    fun getCurrentWifi() {
        _currentWifi.value = repository.getCurrentWifiInfo()
    }
}
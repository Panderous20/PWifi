package com.example.pwifi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pwifi.data.model.SimpleScanResult
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

    //Wifi được chọn để hiển hị dialog
    private val _selectedWifi = MutableStateFlow<SimpleScanResult?>(null)
    val selectedWifi = _selectedWifi.asStateFlow()

    private val _wifiList = MutableStateFlow<List<SimpleScanResult>>(emptyList())
    val wifiList = _wifiList.asStateFlow()

    fun selectWifi(wifi: SimpleScanResult?) {
        _selectedWifi.value = wifi
    }

    fun scanWifi() {
        viewModelScope.launch {
            // Hiển thị ngay lập tức data cũ để tránh màn hình rỗng
            _wifiList.value = repository.getCachedWifi()

            //Sau đó mới đi quét ngầm data mới
            try {
                val freshResults = repository.scanNearbyWifi()
                // Chỉ update nếu kết quả mới khác rỗng và có số lượng hợp lý
                if (freshResults.isNotEmpty()) {
                    _wifiList.value = freshResults
                }
            } catch (e: Exception) {
                // Lỗi thì user vẫn nhìn thấy data cache ở bước 1
            }
        }
    }
}
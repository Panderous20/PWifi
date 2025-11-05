package com.example.pwifi.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.data.SimpleScanResult
import com.example.pwifi.ui.theme.PWifiTheme
import com.example.pwifi.viewmodel.WifiScanViewModel

@Composable
fun WifiScanScreen(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: WifiScanViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var granted by remember { mutableStateOf(false) }

    val wifi by viewModel.uiState.collectAsState()
    val wifiList by viewModel.wifiList.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    // Kiểm tra quyền
    LaunchedEffect(Unit) {
        val pm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        granted = pm == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Nút quét Wi-Fi
        Button(
            onClick = {
                val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                when {
                    !granted -> {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        Toast.makeText(context, "Please grant Location permission", Toast.LENGTH_SHORT).show()
                    }
                    !gpsEnabled -> {
                        Toast.makeText(context, "Please turn on Location to process", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        viewModel.scanWifi()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
        ) {
            Text(if (granted) "Scan Wi-Fi" else "Require Permission", color = Color.Black)
        }

        

        // Danh sách Wi-Fi
        WifiListView(
            apList = wifiList,
            onItemClick = { viewModel.getWifiInfo(it) }
        )
    }

    // Dialog hiển thị chi tiết
    wifi?.let {wifi ->
        WifiDetailDialog(wifi) { viewModel.getWifiInfo(null) }
    }

}

@Composable
fun WifiListView(apList: List<SimpleScanResult>, onItemClick: (SimpleScanResult) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apList) { ap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onItemClick(ap) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF5B5555)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (ap.ssid.isBlank()) "<Hidden SSID>" else ap.ssid,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("BSSID: ${ap.bssid}", color = Color.LightGray, fontSize = 13.sp)
                    Text("RSSI: ${ap.level} dBm | ${ap.frequency} MHz", color = Color.LightGray, fontSize = 13.sp)
                    Text(
                        "Caps: ${ap.capabilities}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun WifiDetailDialog(wifi: SimpleScanResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Color.Cyan)
            }
        },
        title = { Text("Wi-Fi Details", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("SSID: ${wifi.ssid.ifBlank { "<Hidden>" }}", color = Color.LightGray)
                Text("BSSID: ${wifi.bssid}", color = Color.LightGray)
                Text("Tần số: ${wifi.frequency} MHz", color = Color.LightGray)
                Text("Cường độ: ${wifi.level} dBm", color = Color.LightGray)
                Text("Bảo mật: ${wifi.capabilities}", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF5E5A5A)
    )
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun WifiScanScreenPreview() {
    PWifiTheme {
        WifiScanScreen()
    }
}
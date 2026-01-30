package com.example.pwifi.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkWifi1Bar
import androidx.compose.material.icons.filled.NetworkWifi2Bar
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi0Bar
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.R
import com.example.pwifi.data.model.SimpleScanResult
import com.example.pwifi.ui.component.PWifiScaffold
import com.example.pwifi.ui.component.WifiBand
import com.example.pwifi.ui.component.WifiChannelGraph
import com.example.pwifi.viewmodel.WifiScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScanScreen(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: WifiScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    var hasPermission by remember { mutableStateOf(false) }
    val selectedWifi by viewModel.selectedWifi.collectAsState()
    val wifiList by viewModel.wifiList.collectAsState()

    // STATE CHO DROPDOWN
    // Mặc định chọn 2.4GHz
    var selectedBand by remember { mutableStateOf(WifiBand.GHZ_2_4) }
    // Trạng thái mở/đóng menu
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Launcher xin quyền
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) viewModel.scanWifi()
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.scanWifi() // Tự động quét lần đầu
        }
    }

    val onScanClick: () -> Unit = {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        when {
            !hasPermission -> {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            !isGpsEnabled -> {
                Toast.makeText(context, context.getString(R.string.location_fail), Toast.LENGTH_LONG).show()
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            else -> {
                viewModel.scanWifi()
                Toast.makeText(context, "Scanning...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    PWifiScaffold(
        title = stringResource(R.string.wifi_scan_title),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan Wifi")
            }
        }
    ) { innerPadding ->
        val listBottomPadding = paddingValues.calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                top = 16.dp, start = 16.dp, end = 16.dp, bottom = listBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HEADER + GRAPH
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.channel_graph),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // DROPDOWN MENU
                    Box {
                        // Nút bấm hiển thị lựa chọn hiện tại
                        TextButton(
                            onClick = { isMenuExpanded = true }
                        ) {
                            Text(
                                text = if (selectedBand == WifiBand.GHZ_2_4) "2.4 GHz" else "5 GHz",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        // Menu
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("2.4 GHz") },
                                onClick = {
                                    selectedBand = WifiBand.GHZ_2_4
                                    isMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("5 GHz") },
                                onClick = {
                                    selectedBand = WifiBand.GHZ_5
                                    isMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // HIỂN THỊ GRAPH
                if (wifiList.isNotEmpty()) {
                    WifiChannelGraph(
                        wifiList = wifiList,
                        band = selectedBand
                    )
                } else {
                    Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.channel_graph_error), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Tiêu đề danh sách
            item {
                Text(
                    text = stringResource(R.string.available_networks) + " (${wifiList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Danh sách Wifi
            items(wifiList, key = { it.bssid }) { wifi ->
                WifiItemCard(
                    wifi = wifi,
                    onClick = { viewModel.selectWifi(wifi) }
                )
            }
        }
    }

    // Dialog chi tiết
    selectedWifi?.let { wifi ->
        WifiDetailDialog(wifi) { viewModel.selectWifi(null) }
    }
}

@Composable
fun WifiItemCard(
    wifi: SimpleScanResult,
    onClick: () -> Unit
) {
    // Chọn icon và màu sắc dựa trên RSSI và bảo mật
    val signalIcon = getSignalIcon(wifi.level)
    val isSecure = wifi.capabilities.contains("WPA") || wifi.capabilities.contains("WEP")
    val signalColor = getSignalColor(wifi.level)

    val standardNumber = remember(wifi.standardLabel) {
        when {
            wifi.standardLabel.contains("7") -> "7"
            wifi.standardLabel.contains("6") -> "6"
            wifi.standardLabel.contains("5") -> "5"
            wifi.standardLabel.contains("4") -> "4"
            else -> null
        }
    }
    val badgeColor = when {
        wifi.standardLabel.contains("6") || wifi.standardLabel.contains("7") -> Color(0xFF6750A4) // Tím (Wifi xịn)
        wifi.standardLabel.contains("5") -> Color(0xFF006C51) // Xanh (Wifi thường)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = signalIcon,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopCenter)
                )
                if (standardNumber != null) {
                    Text(
                        text = standardNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                CircleShape
                            )
                            .padding(horizontal = 2.dp)
                    )
                }

                // 3. Icon Ổ khóa
                if (isSecure) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secured",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd) // <--- Căn phải dưới
                    )
                }
            }

            //Thông tin chính giữa
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wifi.ssid.ifBlank { "<Hidden>" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (wifi.ssid.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wifi.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${wifi.level} dBm",
                    style = MaterialTheme.typography.labelLarge,
                    color = signalColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${wifi.frequency} MHz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(Icons.Default.SignalWifi4Bar, contentDescription = null)
        },
        title = {
            Text(
                text = wifi.ssid.ifBlank { stringResource(R.string.wifi_detail) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("SSID", wifi.ssid.ifBlank { "<Hidden>" })
                DetailRow("BSSID (MAC)", wifi.bssid)
                DetailRow(stringResource(R.string.standard), wifi.standardLabel.ifEmpty { "Unknown" }) // Mới
                DetailRow(stringResource(R.string.frequency), "${wifi.frequency} MHz ("+ stringResource(R.string.channel_width) + ": ${wifi.channelWidth} MHz)") // Mới
                DetailRow(stringResource(R.string.range), wifi.freqRangeLabel) // Mới (5490 - 5570 MHz)
                DetailRow(stringResource(R.string.center_freq), "${wifi.centerFreq} MHz") // Mới
                DetailRow(stringResource(R.string.rssi), "${wifi.level} dBm")
//                DetailRow(stringResource(R.string.frequency), "${wifi.frequency} MHz")
                DetailRow(stringResource(R.string.security), wifi.capabilities)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Chọn icon dựa trên mức sóng
fun getSignalIcon(rssi: Int): ImageVector {
    return when {
        rssi > -50 -> Icons.Default.SignalWifi4Bar // Rất mạnh
        rssi > -70 -> Icons.Default.NetworkWifi3Bar // Mạnh
        rssi > -80 -> Icons.Default.NetworkWifi2Bar // Trung bình
        rssi > -90 -> Icons.Default.NetworkWifi1Bar // Yếu
        else -> Icons.Default.SignalWifi0Bar // Rất yếu
    }
}

// Chọn màu dựa trên mức sóng
@Composable
fun getSignalColor(rssi: Int): Color {
    return when {
        rssi > -50 -> Color(0xFF00C853) // Xanh lá đậm (Rất tốt)
        rssi > -60 -> Color(0xFF00C853)  // Xanh lá đậm (Tốt)
        rssi > -80 -> Color(0xFFFFAB00) // Vàng cam (Trung bình)
        else -> MaterialTheme.colorScheme.error // Đỏ (Yếu)
    }
}
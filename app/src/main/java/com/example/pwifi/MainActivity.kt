package com.example.pwifi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pwifi.network.NetworkResult
import com.example.pwifi.network.NetworkSpeedTester
import com.example.pwifi.network.SimpleScanResult
import com.example.pwifi.network.WifiScanner
import com.example.pwifi.ui.screen.SpeedTestScreen
import com.example.pwifi.ui.theme.PWifiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PWifiTheme {
                SplashScreen()
            }
        }
    }
}

@Composable
fun SplashScreen() {
    var showSplash by remember { mutableStateOf(true) }
    var showMain by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Hiển thị MainApp trước (ở dưới)
        if (showMain) {
            MainApp()
        }

        // SplashContent ở trên, sẽ trượt đi
        if (showSplash) {
            SplashContent(
                onAnimationComplete = {
                    showSplash = false
                },
                onTimeout = {
                    showMain = true
                }
            )
        }
    }
}

@Composable
fun SplashContent(onAnimationComplete: () -> Unit, onTimeout: () -> Unit) {
    var splashVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = splashVisible,
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(durationMillis = 800)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(durationMillis = 800))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.scale(2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 800)
                )
            ) {
                Text(
                    text = stringResource(R.string.app_description),
                    color = colorResource(R.color.red_custom)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        onTimeout() // Hiển thị MainApp ngay
        splashVisible = false // Bắt đầu animation exit
        delay(800) // Đợi animation exit xong
        onAnimationComplete() // Xóa SplashContent khỏi composition
    }
}

@Composable
fun MainApp() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.offset(y = 10.dp).padding(top = 5.dp)) {
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.NetworkCheck, contentDescription = "Speed") },
                        label = { Text(stringResource(R.string.speed_test)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFDB2A1E),
                            selectedTextColor = Color(0xFFDB2A1E),
                            unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                            unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi") },
                        label = { Text(stringResource(R.string.wifi_list)) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFDB2A1E),
                            selectedTextColor = Color(0xFFDB2A1E),
                            unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                            unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        when (selectedTab) {
            0 -> SpeedTestScreen(padding)
            1 -> WifiScanScreen(padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSpeedScreen(paddingValues: PaddingValues) {
    var isTesting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<NetworkResult?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Network Speed Test",
            fontSize = 26.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        if (isTesting) {
            Text(
                "Đang kiểm tra mạng...",
                color = Color.LightGray,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF33CC66),
                        shape = CircleShape
                    )
                    .clickable(
                        indication = ripple(color = Color(0xFF33CC66)),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        scope.launch {
                            isTesting = true
                            result = NetworkSpeedTester.runSpeedTest()
                            isTesting = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start Test",
                    color = Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        result?.let {
            ResultCard("Ping", "${"%.2f".format(it.ping)} ms")
            ResultCard("Jitter", "${"%.2f".format(it.jitter)} ms")
            ResultCard("Download", "${"%.2f".format(it.downloadMbps)} Mbps")
            ResultCard("Upload", "${"%.2f".format(it.uploadMbps)} Mbps")
        }
    }
}

@Composable
fun ResultCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.LightGray, fontSize = 18.sp)
            Text(value, color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun WifiScanScreen(paddingValues: PaddingValues) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(false) }
    var wifiList by remember { mutableStateOf<List<SimpleScanResult>>(emptyList()) }
    var selectedWifi by remember { mutableStateOf<SimpleScanResult?>(null) }

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
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        // Nút quét Wi-Fi
        Button(
            onClick = {
                if (!granted) {
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        wifiList = WifiScanner.scanOnce(context)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
        ) {
            Text(if (granted) "Scan Wi-Fi" else "Request Permission", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Danh sách Wi-Fi
        WifiListView(
            apList = wifiList,
            onItemClick = { selectedWifi = it }
        )
    }

    // Dialog hiển thị chi tiết
    selectedWifi?.let { wifi ->
        WifiDetailDialog(wifi) { selectedWifi = null }
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
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
        containerColor = Color(0xFF1E1E1E)
    )
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun SplashScreenPreview() {
    PWifiTheme {
        SplashScreen()
    }
}
package com.example.pwifi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pwifi.ui.screen.RssiScreen
import com.example.pwifi.ui.screen.SettingScreen
import com.example.pwifi.ui.screen.SpeedTestScreen
import com.example.pwifi.ui.screen.WifiInfo
import com.example.pwifi.ui.screen.WifiScanScreen
import com.example.pwifi.ui.theme.PWifiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
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
    val selectedTab = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val iconsList = listOf(
        Icons.Default.NetworkCheck to "SpeedTest",
        Icons.Default.Wifi to "Wi-fi",
        Icons.Default.Info to "Wi-fi info",
        Icons.Default.Settings to "Setting"
    )

//    Scaffold(
//        bottomBar = {
//            Column(modifier = Modifier.offset(y = 10.dp).padding(top = 5.dp)) {
//                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
//                NavigationBar(
//                    containerColor = Color.White
//                ) {
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.NetworkCheck, contentDescription = "Speed") },
//                        label = { Text(stringResource(R.string.speed_test)) },
//                        selected = selectedTab == 0,
//                        onClick = { selectedTab = 0 },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = Color(0xFFDB2A1E),
//                            selectedTextColor = Color(0xFFDB2A1E),
//                            unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
//                            unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
//                            indicatorColor = Color.Transparent
//                        )
//                    )
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi") },
//                        label = { Text(stringResource(R.string.wifi_list)) },
//                        selected = selectedTab == 1,
//                        onClick = { selectedTab = 1 },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = Color(0xFFDB2A1E),
//                            selectedTextColor = Color(0xFFDB2A1E),
//                            unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
//                            unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
//                            indicatorColor = Color.Transparent
//                        )
//                    )
//                }
//            }
//        },
//        containerColor = Color.White
//    ) { padding ->
//        when (selectedTab) {
//            0 -> SpeedTestScreen(padding)
//            1 -> WifiScanScreen(padding)
//        }
//    }
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                iconsList.forEachIndexed { index, (icon, label) ->
                    NavigationBarItem(
                        icon = {Icon(icon, label)},
                        label = { Text(label)},
                        selected = selectedTab.currentPage == index,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFDB2A1E),
                            selectedTextColor = Color(0xFFDB2A1E),
                            unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                            unselectedTextColor = Color.Gray.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            scope.launch {
                                selectedTab.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = selectedTab,
            modifier = Modifier.padding(paddingValues),
            userScrollEnabled = true,
            beyondViewportPageCount = 3
        ) {page ->
            when(page) {
                0 -> SpeedTestScreen(paddingValues)
                1 -> WifiScanScreen(paddingValues)
                2 -> WifiInfo(paddingValues)
                3 -> SettingScreen(paddingValues)
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun SplashScreenPreview() {
    PWifiTheme {
        SplashScreen()
    }
}
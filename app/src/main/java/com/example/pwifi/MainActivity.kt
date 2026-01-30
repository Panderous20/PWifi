package com.example.pwifi

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pwifi.ui.screen.SettingScreen
import com.example.pwifi.ui.screen.SpeedTestScreen
import com.example.pwifi.ui.screen.WifiInfoScreen
import com.example.pwifi.ui.screen.WifiScanScreen
import com.example.pwifi.ui.theme.PWifiTheme
import com.example.pwifi.utils.ThemeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isRecreated = savedInstanceState != null
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            // remember để Compose nhớ trạng thái khi xoay màn hình hay vẽ lại
            var isDarkMode by remember {
                mutableStateOf(ThemeUtils.getThemeMode(context) ?: false)
            }
            PWifiTheme(darkTheme = isDarkMode) {
                SplashScreen(
                    isRecreated = isRecreated,
                    onToggleTheme = {
                        // Đây là logic khi nút bấm ở Setting được kích hoạt
                        isDarkMode = !isDarkMode
                        ThemeUtils.saveThemeMode(context, isDarkMode) // Lưu vào máy
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    isRecreated: Boolean,
    onToggleTheme: () -> Unit) {
    var showSplash by remember { mutableStateOf(true) }
    var showMain by remember { mutableStateOf(false) }

    if (!isRecreated) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hiển thị MainApp trước (ở dưới)
            if (showMain) {
                MainApp(onToggleTheme)
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
    } else {
        MainApp(onToggleTheme)
    }
}

@Composable
fun SplashContent(
    onAnimationComplete: () -> Unit,
    onTimeout: () -> Unit // Hàm này báo hiệu logic loading xong
) {
    // State quản lý việc ẩn hiện Splash
    var startExitAnimation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Lớp Splash nằm đè lên trên cùng
        AnimatedVisibility(
            visible = !startExitAnimation,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Logo
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.scale(2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Text
                Text(
                    text = stringResource(R.string.app_description),
                    color = colorResource(R.color.red_custom)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1000)
        // MainScreen sẽ bắt đầu vẽ ở lớp dưới
        onTimeout()
        // Điều này giúp tránh Splash mờ đi mà bên dưới vẫn đen thui hoặc trắng xóa
        delay(100)
        startExitAnimation = true
        // Đợi animation fadeOut (500ms) chạy xong
        delay(500)
        // Xóa hẳn Splash khỏi cây Composable
        onAnimationComplete()
    }
}

@Composable
fun MainApp(
    onToggleTheme: () -> Unit
) {
    val selectedTab = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val iconsList = listOf(
        Icons.Default.NetworkCheck to "SpeedTest",
        Icons.Default.Wifi to "Scanner",
        Icons.Default.Info to "Wi-fi info",
        Icons.Default.Settings to stringResource(R.string.setting_title)
    )
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                2 -> WifiInfoScreen(paddingValues)
                3 -> SettingScreen(paddingValues, onToggleTheme = onToggleTheme)
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun SplashScreenPreview() {
    PWifiTheme {
        SplashScreen(true, {})
    }
}
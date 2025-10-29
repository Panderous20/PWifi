package com.example.pwifi.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pwifi.network.NetworkSpeedTester
import com.example.pwifi.ui.theme.PWifiTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SpeedTestScreen(paddingValues: PaddingValues) {
    val coroutineScope = rememberCoroutineScope()

    var ping by remember { mutableStateOf("-") }
    var jitter by remember { mutableStateOf("-") }
    val downloadAnimation = remember { Animatable(0f) }
    val uploadAnimation = remember { Animatable(0f) }
    var isTesting by remember { mutableStateOf(false) }

    val state = UiState(
        ping = ping,
        jitter = jitter,
        downloadSpeed = downloadAnimation.value,
        uploadSpeed = uploadAnimation.value,
        inProgress = isTesting
    )

    fun startTest() {
        coroutineScope.launch {
            isTesting = true
            ping = "-"
            jitter = "-"
            downloadAnimation.snapTo(0f)
            uploadAnimation.snapTo(0f)

            try {
                val maxSpeed = 100f // Max speed để normalize (Mbps)

                // Launch test thực trong background
                val testJob = async {
                    NetworkSpeedTester.runSpeedTest()
                }

                // Phase 1: Animate download với random values trong 15 giây
                launch {
                    repeat(30) {
                        delay(500) // Mỗi giây
                        // Random giá trị từ 20-80 Mbps
                        val randomSpeed = Random.nextDouble(30.0, 60.0)
                        val normalizedSpeed = (randomSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

                        downloadAnimation.animateTo(
                            targetValue = normalizedSpeed,
                            animationSpec = tween(durationMillis = 800)
                        )
                    }
                }

                // Đợi animation random hoàn thành
                delay(10000)

                // Lấy kết quả thực
                val result = testJob.await()

                // Cập nhật ping và jitter
                ping = result.ping.roundToInt().toString()
                jitter = result.jitter.roundToInt().toString()

                // Animate đến giá trị download thực
                val realDownloadNormalized = (result.downloadMbps / maxSpeed).toFloat().coerceIn(0f, 1f)
                downloadAnimation.animateTo(
                    targetValue = realDownloadNormalized,
                    animationSpec = tween(durationMillis = 1000)
                )

                // Đợi một chút trước khi bắt đầu upload animation
                delay(500)

                // Phase 2: Animate upload với random values trong 15 giây
                // (Test upload thực đã chạy trong NetworkSpeedTester.runSpeedTest())
                repeat(15) {
                    delay(200)
                    val randomSpeed = Random.nextDouble(20.0, 60.0)
                    val normalizedSpeed = (randomSpeed / maxSpeed).toFloat().coerceIn(0f, 1f)

                    uploadAnimation.animateTo(
                        targetValue = normalizedSpeed,
                        animationSpec = tween(durationMillis = 800)
                    )
                }

                // Animate đến giá trị upload thực
                val realUploadNormalized = (result.uploadMbps / maxSpeed).toFloat().coerceIn(0f, 1f)
                uploadAnimation.animateTo(
                    targetValue = realUploadNormalized,
                    animationSpec = tween(durationMillis = 1000)
                )

                // Giữ nguyên giá trị này cho đến khi bấm START lại

            } catch (e: Exception) {
                e.printStackTrace()
                ping = "Error"
                jitter = "Error"
                downloadAnimation.snapTo(0f)
                uploadAnimation.snapTo(0f)
            } finally {
                isTesting = false
            }
        }
    }

    SpeedTestScreen(state, ::startTest)
}

@Composable
private fun SpeedTestScreen(
    state: UiState,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Header()
        AdditionInfo(state.ping, state.jitter)
        Spacer(modifier = Modifier.height(36.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SpeedIndicator(
                title = "DOWNLOAD",
                speed = state.downloadSpeed,
                color = Color(0xFFAE96D9)
            )
            SpeedIndicator(
                title = "UPLOAD",
                speed = state.uploadSpeed,
                color = Color(0xFF96D9AE)
            )
        }
        StartButton(!state.inProgress, onClick)
    }
}

@Composable
fun Header() {
    Text(
        text = "SPEEDTEST",
        modifier = Modifier.padding(bottom = 32.dp, top = 52.dp),
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
fun SpeedIndicator(
    title: String,
    speed: Float,
    color: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .size(200.dp)
            .padding(16.dp)
            .aspectRatio(1f)
    ) {
        CircularSpeedIndicator(speed, 270f, color)
        SpeedValue(title, speed)
    }
}

@Composable
fun CircularSpeedIndicator(value: Float, angle: Float, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize(0.9f)
            .padding(32.dp)
    ) {
        drawLines(value, angle)
        drawArcs(value, angle, color)
    }
}

fun DrawScope.drawLines(progress: Float, angleSize: Float, lines: Int = 41) {
    val oneRotation = angleSize / lines
    val startValue = floor(progress * lines).toInt()

    for (i in startValue until lines) {
        rotate(i * oneRotation + (180 - angleSize) / 2) {
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(x = if (i % 5 == 0) 80f else 30f, y = size.height / 2),
                end = Offset(0f, size.height / 2),
                8f,
                StrokeCap.Round
            )
        }
    }
}

fun DrawScope.drawArcs(progress: Float, angleSize: Float, color: Color) {
    val startAngle = 270 - angleSize / 2
    val sweepAngle = angleSize * progress
    val topLeft = Offset(50f, 50f)
    val size = Size(size.width - 100f, size.height - 100f)

    // Draw blur/glow effect
    for (i in 0..20) {
        drawArc(
            color = color.copy(alpha = i / 900f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 80f + (20 - i) * 20, cap = StrokeCap.Round)
        )
    }

    // Draw stroke
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = 86f, cap = StrokeCap.Round)
    )

    // Draw gradient
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = 80f, cap = StrokeCap.Round)
    )
}

@Composable
fun AdditionInfo(ping: String, jitter: String) {
    @Composable
    fun RowScope.InfoColumn(title: String, value: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(title)
            Text(
                value,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        InfoColumn(title = "PING", value = "$ping ms")
        VerticalDivider()
        InfoColumn(title = "JITTER", value = "$jitter ms")
    }
}

@Composable
fun SpeedValue(title: String, value: Float) {
    val formattedValue = String.format("%.2f", value * 100f)
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 16.sp)
        Text(
            text = formattedValue,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "Mbps", fontSize = 14.sp)
    }
}

@Composable
fun StartButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 24.dp),
        enabled = isEnabled,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text = if (isEnabled) "START" else "TESTING...")
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color(0xFF414D66))
    )
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun DefaultPreview() {
    PWifiTheme {
        Surface {
            SpeedTestScreen(
                UiState(
                    ping = "32.4",
                    jitter = "5.3",
                    downloadSpeed = 0.75f,
                    uploadSpeed = 0.85f,
                    inProgress = false
                ),
                {}
            )
        }
    }
}
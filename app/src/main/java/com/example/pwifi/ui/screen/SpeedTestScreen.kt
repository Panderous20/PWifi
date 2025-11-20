package com.example.pwifi.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.R
import com.example.pwifi.data.SpeedTestUiState
import com.example.pwifi.ui.theme.PWifiTheme
import kotlin.math.floor

@Composable
fun SpeedTestScreen(
    paddingValues: PaddingValues,
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    // State này sẽ update liên tục
    val state by viewModel.uiState.collectAsState()

    SpeedTestDetailScreen(
        paddingValues = paddingValues,
        state = state,
        onClick = viewModel::startTest
    )
}

@Composable
private fun SpeedTestDetailScreen(
    paddingValues: PaddingValues,
    state: SpeedTestUiState,
    onClick: () -> Unit,
) {
    // Repository trả về kết quả mỗi 200ms.
    // tween(400) để tạo độ trễ nhẹ draw mượt mà,
    val smoothDownloadSpeed by animateFloatAsState(
        targetValue = state.downloadSpeed,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "DownloadAnim"
    )

    val smoothUploadSpeed by animateFloatAsState(
        targetValue = state.uploadSpeed,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "UploadAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
        Header()
        AdditionInfo(state.ping, state.jitter)
        Spacer(modifier = Modifier.height(36.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SpeedIndicator(
                title = stringResource(R.string.download_upcase),
                speed = smoothDownloadSpeed,
                color = Color(0xFFAE96D9)
            )
            SpeedIndicator(
                title = stringResource(R.string.upload_upcase),
                speed = smoothUploadSpeed,
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
        modifier = Modifier.padding(bottom = 32.dp, top = 32.dp),
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
        InfoColumn(
            title = stringResource(R.string.ping),
            value = stringResource(R.string.ping_value, ping))
        VerticalDivider()
        InfoColumn(title = stringResource(R.string.jitter),
            value = stringResource(R.string.jitter_value, jitter))
    }
}

@Composable
fun SpeedValue(title: String, value: Float) {
    // Lưu ý: value ở đây là giá trị normalized (0.0 -> 1.0)
    // Nhân với 500f vì trong ViewModel ta đang giả định Max Speed của đồng hồ là 500Mbps
    val formattedValue = String.format("%.2f", value * 500f)

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
        Text(text = stringResource(R.string.megabit_per_second), fontSize = 14.sp)
    }
}

@Composable
fun StartButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        enabled = isEnabled,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text = if (isEnabled) "START" else "TESTING...")
    }
}

@Preview(showBackground = true, device = Devices.PIXEL)
@Composable
fun GeminiResponse(
    isEnabled: Boolean = false,
    response: String? = "Alalala"
) {
    if(isEnabled)
    {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            Icon(
                painter = painterResource(R.drawable.gemini_color),
                contentDescription = ""
            )
            Text(
                text = response ?: "",
                textAlign = TextAlign.Start,
                fontSize = 12.sp
            )
        }
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
            SpeedTestDetailScreen(
                paddingValues = PaddingValues(0.dp),
                SpeedTestUiState(
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
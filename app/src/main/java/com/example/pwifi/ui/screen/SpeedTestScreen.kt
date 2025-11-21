package com.example.pwifi.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.example.pwifi.data.model.SpeedTestUiState
import com.example.pwifi.ui.theme.PWifiTheme
import kotlin.math.floor

@Composable
fun SpeedTestScreen(
    paddingValues: PaddingValues,
    viewModel: SpeedTestViewModel = hiltViewModel()
) {
    // State này sẽ update liên tục
    val state by viewModel.uiState.collectAsState()
    val geminiPromptTemplate = stringResource(R.string.promt_gemini)

    SpeedTestDetailScreen(
        paddingValues = paddingValues,
        state = state,
        onClick = { viewModel.startTest(geminiPromptTemplate) }
    )
}

@Composable
private fun SpeedTestDetailScreen(
    paddingValues: PaddingValues,
    state: SpeedTestUiState,
    onClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isAiLoading, state.aiAnalysis) {
        if (state.isAiLoading || state.aiAnalysis != null) {
            // Delay nhẹ 1 chút (100ms) để AnimatedVisibility kịp mở ra
            // giúp tính toán độ cao chính xác hơn
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
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
            .verticalScroll(scrollState)
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
        GeminiResponseBox(
            isLoading = state.isAiLoading,
            response = state.aiAnalysis
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun GeminiResponseBox(
    isLoading: Boolean,
    response: String?
) {
    // Hiệu ứng xuất hiện mượt mà
    AnimatedVisibility(
        visible = isLoading || response != null,
        enter = fadeIn() + expandVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            // Viền gradient đẹp mắt
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4285F4), Color(0xFF9C27B0)) // Màu Google AI
                )
            ),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header của Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.gemini_color), // Icon Gemini
                        contentDescription = "AI Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified // Giữ màu gốc của icon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Network Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLoading) {
                    // Hiệu ứng đang load
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing network quality...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    // Hiển thị kết quả text
                    Text(
                        text = response ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
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
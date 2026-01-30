package com.example.pwifi.ui.component

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.pwifi.data.model.SimpleScanResult

// Enum để chọn chế độ vẽ
enum class WifiBand { GHZ_2_4, GHZ_5 }

@Composable
fun WifiChannelGraph(
    wifiList: List<SimpleScanResult>,
    band: WifiBand = WifiBand.GHZ_2_4, // Default là 2.4
    modifier: Modifier = Modifier
) {
    // Lọc Wifi theo băng tần
    val filteredWifi = remember(wifiList, band) {
        wifiList.filter {
            if (band == WifiBand.GHZ_2_4) it.frequency in 2400..2499
            else it.frequency > 5000
        }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Box(
        modifier = modifier
            .height(300.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // CẤU HÌNH TRỤC X
            val paddingLeft = 90f
            val graphWidth = width - paddingLeft

            // Setup thông số dựa trên băng tần
            val minVisChannel: Float
            val maxVisChannel: Float
            val xLabelStep: Int

            if (band == WifiBand.GHZ_2_4) {
                minVisChannel = -1f
                maxVisChannel = 16f
                xLabelStep = 1
            } else {
                // 5GHz
                minVisChannel = 32f
                maxVisChannel = 180f
                xLabelStep = 16
            }

            val stepX = graphWidth / (maxVisChannel - minVisChannel)

            // CẤU HÌNH TRỤC Y
            val minRssi = -100f
            val maxRssi = -20f
            val rssiRange = maxRssi - minRssi

            // VẼ GRID & TRỤC
            drawGraphGrid(
                width = width,
                height = height,
                paddingLeft = paddingLeft,
                stepX = stepX,
                minVisChannel = minVisChannel,
                maxVisChannel = maxVisChannel,
                xLabelStep = xLabelStep,
                gridColor = gridColor,
                textColor = labelColor,
                minRssi = minRssi,
                maxRssi = maxRssi
            )

            // VẼ WIFI
            filteredWifi.forEach { wifi ->
                val drawFreq = if (wifi.centerFreq > 2400) wifi.centerFreq else wifi.frequency
                val centerChannel = convertFreqToChannel(drawFreq)

                // Kiểm tra nằm trong vùng hiển thị (Coordinate Range)
                if (centerChannel in minVisChannel..maxVisChannel) {

                    val centerX = paddingLeft + ((centerChannel - minVisChannel) * stepX)

                    val safeLevel = wifi.level.toFloat().coerceIn(minRssi, maxRssi)
                    val heightRatio = (safeLevel - minRssi) / rssiRange
                    val targetPeakY = height - (heightRatio * height)

                    val safeWidth = if (wifi.channelWidth > 0) wifi.channelWidth else 20
                    val channelsSpan = safeWidth / 5f
                    val widthPx = channelsSpan * stepX

                    val color = getVremColor(wifi.ssid)

                    drawBellCurve(
                        name = wifi.ssid.ifBlank { "Hidden" },
                        centerX = centerX,
                        targetPeakY = targetPeakY,
                        baseY = height,
                        widthPx = widthPx,
                        color = color,
                        textColor = color.toArgb()
                    )
                }
            }
        }
    }
}

fun DrawScope.drawGraphGrid(
    width: Float, height: Float, paddingLeft: Float, stepX: Float,
    minVisChannel: Float, maxVisChannel: Float, xLabelStep: Int,
    gridColor: Color, textColor: Int,
    minRssi: Float, maxRssi: Float
) {
    val textPaint = Paint().apply {
        color = textColor
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    val axisPaint = Paint().apply {
        color = textColor
        textSize = 34f
        textAlign = Paint.Align.RIGHT
    }

    // Vẽ trục Y (dBm)
    val dbmStep = 10
    val range = maxRssi - minRssi
    for (dbm in minRssi.toInt()..maxRssi.toInt() step dbmStep) {
        val y = height - ((dbm - minRssi) / range * height)
        drawContext.canvas.nativeCanvas.drawText("$dbm", paddingLeft - 15f, y + 10f, axisPaint)
        drawLine(gridColor.copy(alpha = 0.15f), Offset(paddingLeft, y), Offset(width, y), 1f)
    }

    // Vẽ trục X (Channel Number)
    // Duyệt hết toàn bộ vùng coordinate
    val startLoop = (minVisChannel.toInt() / xLabelStep) * xLabelStep

    for (ch in startLoop..maxVisChannel.toInt() step xLabelStep) {
        // Không bao giờ vẽ kênh <= 0
        if (ch <= 0) continue

        // Nếu là chế độ 2.4GHz (bước nhảy 1), chỉ vẽ đến kênh 14
        if (xLabelStep == 1 && ch > 14) continue

        // Không vẽ nếu nằm ngoài vùng nhìn thấy
        if (ch < minVisChannel) continue
        // ----------------------------------------

        val x = paddingLeft + ((ch - minVisChannel) * stepX)

        drawLine(gridColor, Offset(x, 0f), Offset(x, height), 1f)

        drawContext.canvas.nativeCanvas.drawText(
            "$ch",
            x,
            height + 40f,
            textPaint
        )
    }

    // Khung bao quanh
    drawRect(
        color = gridColor,
        topLeft = Offset(paddingLeft, 0f),
        size = androidx.compose.ui.geometry.Size(width - paddingLeft, height),
        style = Stroke(width = 2f)
    )
}

fun DrawScope.drawBellCurve(
    name: String, centerX: Float, targetPeakY: Float, baseY: Float, widthPx: Float, color: Color, textColor: Int
) {
    val path = Path()
    // startX và endX tự động tính toán dựa trên widthPx
    // Nếu widthPx rộng (40MHz) thì chân sóng sẽ bè ra 2 bên
    val startX = centerX - (widthPx / 2)
    val endX = centerX + (widthPx / 2)

    val controlY = (2 * targetPeakY) - baseY

    path.moveTo(startX, baseY)
    path.quadraticBezierTo(centerX, controlY, endX, baseY)

    drawPath(path, color.copy(alpha = 0.2f))
    drawPath(path, color, style = Stroke(width = 5f))


    val namePaint = Paint().apply {
        setColor(textColor)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }

    drawContext.canvas.nativeCanvas.drawText(
        name,
        centerX,
        targetPeakY - 15f,
        namePaint
    )
}

// UTILS
fun convertFreqToChannel(freq: Int): Float {
    return when {
        // Băng tần 2.4GHz (2400 - 2484 MHz)
        freq == 2484 -> 14f // Ngoại lệ kênh 14
        freq in 2400..2483 -> (freq - 2407) / 5f

        // Băng tần 5GHz (5150 - 5925 MHz)
        // Công thức chuẩn: Freq = 5000 + (5 * Channel)
        freq > 5000 -> (freq - 5000) / 5f

        else -> -1f
    }
}

fun getVremColor(ssid: String): Color {
    val hash = ssid.hashCode()
    val r = (hash and 0xFF0000 shr 16)
    val g = (hash and 0x00FF00 shr 8)
    val b = (hash and 0x0000FF)
    return Color(r, g, b).copy(alpha = 1f)
}
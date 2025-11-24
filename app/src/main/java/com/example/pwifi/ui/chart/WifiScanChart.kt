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

@Composable
fun WifiChannelGraph(
    wifiList: List<SimpleScanResult>,
    modifier: Modifier = Modifier
) {
    val wifi24g = remember(wifiList) {
        wifiList.filter { it.frequency in 2400..2500 }
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Box(
        modifier = modifier
            .height(350.dp)
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
            val minVisChannel = -1
            val maxVisChannel = 16
            val stepX = graphWidth / (maxVisChannel - minVisChannel)

            // CẤU HÌNH TRỤC Y
            val minRssi = -100f
            val maxRssi = -20f
            val rssiRange = maxRssi - minRssi

            // GRID
            drawGraphGrid(
                width = width,
                height = height,
                paddingLeft = paddingLeft,
                stepX = stepX,
                minVisChannel = minVisChannel,
                gridColor = gridColor,
                textColor = labelColor,
                minRssi = minRssi,
                maxRssi = maxRssi
            )

            // VẼ WIFI
            wifi24g.forEach { wifi ->
                val centerChannel = convertFreqToChannel(wifi.frequency)

                if (centerChannel in 1..14) {
                    val centerX = paddingLeft + ((centerChannel - minVisChannel) * stepX)

                    // Tính Y (Target Peak)
                    val safeLevel = wifi.level.toFloat().coerceIn(minRssi, maxRssi)
                    val heightRatio = (safeLevel - minRssi) / rssiRange
                    val targetPeakY = height - (heightRatio * height)

                    val widthPx = 4 * stepX
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
    minVisChannel: Int, gridColor: Color, textColor: Int,
    minRssi: Float, maxRssi: Float
) {
    val textPaint = Paint().apply {
        color = textColor
        textSize = 38f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    //Font chữ số dBm (Trục Y)
    val axisPaint = Paint().apply {
        color = textColor
        textSize = 34f
        textAlign = Paint.Align.RIGHT
    }

    val dbmStep = 10
    val range = maxRssi - minRssi
    for (dbm in minRssi.toInt()..maxRssi.toInt() step dbmStep) {
        val y = height - ((dbm - minRssi) / range * height)

        drawContext.canvas.nativeCanvas.drawText(
            "$dbm",
            paddingLeft - 15f,
            y + 10f,
            axisPaint
        )

        drawLine(gridColor.copy(alpha = 0.15f), Offset(paddingLeft, y), Offset(width, y), 1f)
    }

    for (ch in 1..14) {
        val x = paddingLeft + ((ch - minVisChannel) * stepX)
        drawLine(gridColor, Offset(x, 0f), Offset(x, height), 1f)

        drawContext.canvas.nativeCanvas.drawText(
            "$ch",
            x,
            height + 50f,
            textPaint
        )
    }

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
fun convertFreqToChannel(freq: Int): Int {
    if (freq == 2484) return 14
    if (freq < 2412 || freq > 2484) return -1
    return (freq - 2407) / 5
}

fun getVremColor(ssid: String): Color {
    val hash = ssid.hashCode()
    val r = (hash and 0xFF0000 shr 16)
    val g = (hash and 0x00FF00 shr 8)
    val b = (hash and 0x0000FF)
    return Color(r, g, b).copy(alpha = 1f)
}
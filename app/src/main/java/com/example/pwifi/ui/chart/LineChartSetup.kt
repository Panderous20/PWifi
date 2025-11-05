package com.example.pwifi.ui.chart

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line

//fun LineChartSetup() {
//    val xValue = listOf("0","5", "10", "15", "20", "25", "30")
//    val yValue = listOf(0.0, -25.0, -50.0, -75.0, -100.0).reversed()
//    LineChart(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 22.dp),
//        data = remember {
//            listOf(
//                Line(
//                    label ="RSSI",
//                    values = listOf(28.0, 41.0, 5.0, 10.0, 35.0),
//                    color = SolidColor(Color(0xFF23af92)),
//                    firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
//                    secondGradientFillColor = Color.Transparent,
//                    strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
//                    gradientAnimationDelay = 1000,
//                    drawStyle = DrawStyle.Stroke(width = 2.dp),
//                )
//            )
//        },
//        animationMode = AnimationMode.Together(delayBuilder = {
//            it * 500L
//        }),
//        labelProperties = LabelProperties(
//            enabled = true,
//            padding = 12.dp,
//            labels = xValue,
//            rotation = LabelProperties.Rotation(degree = 0f)
//        ),
//        indicatorProperties = HorizontalIndicatorProperties(
//            enabled = true,
//            count = IndicatorCount.CountBased(4),
//            indicators = yValue,
//            textStyle = TextStyle(
//                textAlign = TextAlign.End,
//                fontSize = 12.sp
//            ),
//            contentBuilder = { value ->
//                "${value.toInt()}"
//            }
//        )
//    )
//}
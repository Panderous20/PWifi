package com.example.pwifi.ui.screen

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.PIXEL
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.R
import com.example.pwifi.ui.theme.PWifiTheme
import com.example.pwifi.viewmodel.WifiInfoViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun WifiInfo(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        RssiScreen()
    }
}

@Composable
fun RssiScreen(
    modifier: Modifier = Modifier
) {
    val xValue = listOf("0","5", "10", "15", "20", "25", "30")
    val yValue = listOf(0.0, -25.0, -50.0, -75.0, -100.0).reversed()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.rssi_title),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_12))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .padding(dimensionResource(R.dimen.padding_12))
        ) {
            LineChart(
                gridProperties = GridProperties(
                    yAxisProperties = GridProperties.AxisProperties(lineCount = 7)
                ),
                curvedEdges = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                data = remember {
                    listOf(
                        Line(
                            label ="RSSI",
                            values = listOf(28.0, 41.0, 5.0, 10.0, 35.0),
                            color = SolidColor(Color(0xFF23af92)),
                            firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                            secondGradientFillColor = Color.Transparent,
                            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                            gradientAnimationDelay = 1000,
                            drawStyle = DrawStyle.Stroke(width = 2.dp),
                        )
                    )
                },
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 500L
                }),
                labelProperties = LabelProperties(
                    enabled = true,
                    padding = 12.dp,
                    labels = xValue,
                    rotation = LabelProperties.Rotation(degree = 0f)
                ),
                indicatorProperties = HorizontalIndicatorProperties(
                    enabled = true,
                    count = IndicatorCount.CountBased(4),
                    indicators = yValue,
                    textStyle = TextStyle(
                        textAlign = TextAlign.End,
                        fontSize = 12.sp
                    ),
                    contentBuilder = { value ->
                        "${value.toInt()}"
                    }
                )
            )
        }
    }
}

@Composable
fun WifiDetail(
    modifier: Modifier = Modifier,
    viewModel: WifiInfoViewModel = hiltViewModel()
) {
    val currentWifi by viewModel.currentWifi.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getCurrentWifi()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "Wi-fi Connected",
            fontSize = 16.sp,
            modifier = Modifier
                .padding(vertical = 32.dp)
        )
        Text(
            text = "SSID: ${currentWifi?.ssid}"
        )
        Text(
            text = "BSSID: ${currentWifi?.bssid}"
        )
        Text(
            text = "Level: ${currentWifi?.level}"
        )
        Text(
            text = "Frequency: ${currentWifi?.frequency}"
        )
        Text(
            text = "Capabilities: ${currentWifi?.capabilities}"
        )
    }
}

@Preview(showBackground = true, device = PIXEL)
@Composable
fun RssiScreenPreview() {
    PWifiTheme {
        WifiInfo(
            PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        )
    }
}

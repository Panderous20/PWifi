package com.example.pwifi.ui.screen

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.PIXEL
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.R
import com.example.pwifi.data.SimpleScanResult
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
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.ViewRange
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiInfo(
    paddingValues: PaddingValues,
    viewModel: WifiInfoViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val currentWifi by viewModel.currentWifi.collectAsState()
    val refreshState = rememberPullToRefreshState()
    val refresh = viewModel.isRefreshing

    PullToRefreshBox(
        state = refreshState,
        isRefreshing = refresh,
        onRefresh = {
            viewModel.refreshWifi()
            viewModel.measureMultiRssi()
        }
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.padding_8))
                .fillMaxSize()
        ) {
            WifiDetail(
                currentWifi = currentWifi,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.padding_64))
            )
            RssiScreen(viewModel)
        }
    }
}

@Composable
fun RssiScreen(
    viewModel: WifiInfoViewModel,
    modifier: Modifier = Modifier
) {
    val xValue = listOf("0","5", "10", "15", "20", "25", "30")
    val yValue = listOf(0.0, -25.0, -50.0, -75.0, -100.0).reversed()
    val rssi by viewModel.rssiList.collectAsState()

    val lineData = remember(rssi) {
        listOf(
            Line(
                viewRange = ViewRange(0, 30),
                label = "RSSI (dbm)",
                values = rssi.map { it.toDouble() }, // tạo mới list
                color = SolidColor(Color(0xFF23af92)),
                firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(0),
                gradientAnimationDelay = 0,
            )
        )
    }

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
                minValue = 0.0,
                maxValue = 100.0,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                data = lineData,
                popupProperties = PopupProperties(
                    enabled = true,
                    textStyle = TextStyle.Default.copy(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    contentBuilder = { value ->
                        "-%.1f".format(value.value)
                    }
                ),
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 0L
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
    currentWifi: SimpleScanResult?
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wi-fi Connected",
            fontSize = 16.sp,
            modifier = Modifier
                .padding(bottom = 32.dp)
        )
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(6.dp)

        ) {
            Text(text = "SSID: ${currentWifi?.ssid}")
            Text(text = "BSSID: ${currentWifi?.bssid}")
            Text(text = "RSSI: ${currentWifi?.level} dBm")
            Text(text = "Frequency: ${currentWifi?.frequency} GHz")
            Text(text = "Capabilities: ${currentWifi?.capabilities}",
                modifier = Modifier.width(160.dp),
                overflow = TextOverflow.Ellipsis)
        }
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

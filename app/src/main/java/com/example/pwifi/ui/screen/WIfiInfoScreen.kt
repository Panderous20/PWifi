package com.example.pwifi.ui.screen

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PermIdentity
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pwifi.R
import com.example.pwifi.data.model.SaveMessageEvent
import com.example.pwifi.data.model.SimpleScanResult
import com.example.pwifi.ui.chart.rememberMarker
import com.example.pwifi.ui.component.PWifiScaffold
import com.example.pwifi.viewmodel.RssiStats
import com.example.pwifi.viewmodel.WifiInfoViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlinx.coroutines.flow.collectLatest

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiInfoScreen(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: WifiInfoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentWifi by viewModel.currentWifi.collectAsState()
    val stats by viewModel.sessionStats.collectAsState()
    val isMonitoring = viewModel.isMonitoring
    val isRefreshing = viewModel.isRefreshing

    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.saveEvent.collectLatest { event ->

            val message = when (event) {
                is SaveMessageEvent.Success -> {
                    context.getString(R.string.save_success, event.filePath)
                }
                is SaveMessageEvent.Error -> {
                    context.getString(R.string.save_error)
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    PWifiScaffold(
        title = stringResource(R.string.wifi_info_title)
    )
    { innerPadding ->
        PullToRefreshBox(
            state = refreshState,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshScreen() },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thông tin Wifi
                WifiDetailCard(currentWifi = currentWifi, {viewModel.getWifiInfo()})

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.rssi_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                // Biểu đồ LineChart
                RssiChartCard(viewModel.modelProducer)

                // Thống kê Nơ/Min/Max/Avg
                StatsRow(stats = stats)

                // Các nút điều khiển
                ControlButtons(
                    isMonitoring = isMonitoring,
                    onToggleMonitor = { viewModel.toggleMonitoring() },
                    onSaveClick = { viewModel.saveResult() }
                )
            }
        }
    }
}

@Composable
fun WifiDetailCard(
    currentWifi: SimpleScanResult?,
    onClick: () -> Unit
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.connected_wifi),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            if (currentWifi != null) {
                // Thông tin cơ bản
                InfoRow(icon = Icons.Rounded.PermIdentity, label = "SSID", value = currentWifi.ssid)
                InfoRow(icon = Icons.Rounded.Fingerprint, label = "BSSID", value = currentWifi.bssid)

                // Standard (Wifi 4, 5, 6...)
                // Chỉ hiện nếu parse được (khác rỗng)
                if (currentWifi.standardLabel.isNotEmpty()) {
                    InfoRow(
                        icon = Icons.Rounded.SignalWifi4Bar,
                        label = stringResource(R.string.standard),
                        value = currentWifi.standardLabel
                    )
                }

                //  Frequency + Bandwidth
                InfoRow(
                    icon = Icons.Rounded.Waves,
                    label = stringResource(R.string.frequency),
                    value = "${currentWifi.frequency} MHz (Width: ${currentWifi.channelWidth} MHz)"
                )

                // Real Range (Dải tần thực tế)
                if (currentWifi.freqRangeLabel.isNotEmpty()) {
                    InfoRow(
                        icon = Icons.Rounded.CompareArrows,
                        label = stringResource(R.string.range),
                        value = currentWifi.freqRangeLabel
                    )
                }

                // 5. Security
                InfoRow(icon = Icons.Rounded.Security, label = stringResource(R.string.security), value = currentWifi.capabilities)
                // Link speed chỉ có khi > 0
                if (currentWifi.linkSpeed > 0) {
                    InfoRow(
                        icon = Icons.Rounded.Speed,
                        label = stringResource(R.string.link_speed),
                        value = "${currentWifi.linkSpeed} Mbps"
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.wifi_info_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Button(
                onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.refresh))
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RssiChartCard(
    modelProducer: CartesianChartModelProducer
) {
    //remember de tranh tao lai khi recompose
    val legendLabel = stringResource(R.string.rssi_chart_legend)
    val RangeProvider = remember {
        CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = 100.0)
    }
    val formatter =  remember {
        CartesianValueFormatter {_, value, _ ->
            "${(value - 100).toInt()}"
        }
    }
    val markerFormatter = remember {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            // Lấy điểm đầu tiên (vì ta chỉ có 1 đường RSSI)
            val lineTarget = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
            val point = lineTarget?.points?.firstOrNull()
            val yValue = point?.entry?.y ?: 0.0

            // Trừ đi 100 để quay về giá trị thực
            "${(yValue - 100).toInt()} dBm"
        }
    }
    val axisTitleComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = 14.sp,
    )
    val axisValueComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = 12.sp,
    )
    val lineColor = Color(0xFF23AF92)
    val legendIconComponent = rememberShapeComponent(
        fill = fill(lineColor),
        shape = CorneredShape.Pill
    )
    val legendLabelComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface
    )
    val legendItem = remember(legendLabelComponent, legendIconComponent) {
        LegendItem(
            icon = legendIconComponent,
            labelComponent = legendLabelComponent,
            label = legendLabel
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(lineColor)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            fill(ShaderProvider.verticalGradient(
                                arrayOf(Color(0xFF23AF92).copy(alpha = 0.4f), Color.Transparent)
                            ))
                        )
                    )
                ),
                rangeProvider = RangeProvider
            ),
            startAxis = VerticalAxis.rememberStart(
                title = stringResource(R.string.rssi_chart_y_label),
                titleComponent = axisTitleComponent,
                label = axisValueComponent,
                valueFormatter = formatter,
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                title = stringResource(R.string.rssi_chart_x_label),
                label = axisValueComponent,
                titleComponent = axisTitleComponent
            ),
            marker = rememberMarker(valueFormatter = markerFormatter),
            legend = rememberHorizontalLegend(
                items = {
                    add(legendItem)
                },
                padding = insets(top = 16.dp),
            ),
        ),
        modelProducer = modelProducer, //Nguồn dữ liệu
        modifier = Modifier.height(350.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false) // Tắt scroll để chart tự trôi
    )
}

@Composable
fun StatsRow(stats: RssiStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hiển thị số liệu chỉ khi đã đo (count > 0)
        val hasData = stats.count > 0

        StatItem(
            title = "NOW",
            value = if (hasData) "${stats.current}" else "--",
            color = Color(0xFF098EC9), //
            modifier = Modifier.weight(1f)
        )

        StatItem(
            title = "MAX",
            value = if (hasData) "${stats.max}" else "--",
            color = Color(0xFF00C853), // Xanh lá
            modifier = Modifier.weight(1f)
        )
        StatItem(
            title = "AVG",
            value = if (hasData) "${stats.avg}" else "--",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            title = "MIN",
            value = if (hasData) "${stats.min}" else "--",
            color = MaterialTheme.colorScheme.error, // Đỏ
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatItem(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ControlButtons(
    isMonitoring: Boolean,
    onToggleMonitor: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Button Refresh Info
        OutlinedButton(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.save_result))
        }

        // Button Start/Stop
        Button(
            onClick = onToggleMonitor,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMonitoring) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isMonitoring) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (isMonitoring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isMonitoring) stringResource(R.string.stop_monitor) else stringResource(R.string.start_monitor))
        }
    }
}
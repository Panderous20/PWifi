package com.example.pwifi.network

import com.example.pwifi.data.model.NetworkResult
import com.example.pwifi.data.model.SpeedTestUpdate
import com.example.pwifi.data.model.TestStage
import com.example.pwifi.data.repository.SpeedTestRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.random.Random

@Singleton
class SpeedTestRepositoryImpl @Inject constructor() : SpeedTestRepository {

    private val LIBRESPEED_SERVER = "https://librespeed.a573.net/backend/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    override fun runSpeedTestFlow(): Flow<SpeedTestUpdate> = flow {
        var finalPing = 0.0
        var finalJitter = 0.0
        var finalDownload = 0.0
        var finalUpload = 0.0

        // PING & JITTER
        emit(SpeedTestUpdate(TestStage.PING))
        val pingResult = measurePingAndJitter()
        finalPing = pingResult.first
        finalJitter = pingResult.second

        emit(SpeedTestUpdate(
            stage = TestStage.PING,
            ping = finalPing,
            jitter = finalJitter,
            progress = 1f
        ))
        delay(500)

        // DOWNLOAD
        finalDownload = measureSpeed(
            isDownload = true,
            durationMs = 15000,
            onProgress = { speedMbps, progress ->
                emit(SpeedTestUpdate(
                    stage = TestStage.DOWNLOAD,
                    ping = finalPing,
                    jitter = finalJitter,
                    currentSpeed = speedMbps,
                    progress = progress
                ))
            }
        )

        // Emit lại giá trị cuối cùng ngay lập tức để hiện số trên UI
        emit(SpeedTestUpdate(
            stage = TestStage.DOWNLOAD,
            ping = finalPing,
            jitter = finalJitter,
            currentSpeed = finalDownload,
            progress = 1f
        ))

        delay(1000)

        // UPLOAD
        finalUpload = measureSpeed(
            isDownload = false,
            durationMs = 15000,
            onProgress = { speedMbps, progress ->
                emit(SpeedTestUpdate(
                    stage = TestStage.UPLOAD,
                    ping = finalPing,
                    jitter = finalJitter,
                    currentSpeed = speedMbps,
                    progress = progress,
                    finalResult = NetworkResult(finalPing, finalJitter, finalDownload, 0.0)
                ))
            }
        )

        // FINISH
        emit(SpeedTestUpdate(
            stage = TestStage.FINISHED,
            ping = finalPing,
            jitter = finalJitter,
            finalResult = NetworkResult(finalPing, finalJitter, finalDownload, finalUpload)
        ))

    }.flowOn(Dispatchers.IO)

    private suspend fun measurePingAndJitter(): Pair<Double, Double> {

        var minPing = Double.MAX_VALUE
        val pings = mutableListOf<Double>()

        val url = "${LIBRESPEED_SERVER}empty.php"
        try { client.newCall(Request.Builder().url("$url?r=${Math.random()}").build()).execute().close() } catch (_: Exception) {}

        repeat(10) {
            try {
                val start = System.nanoTime()
                client.newCall(Request.Builder().url("$url?r=${Math.random()}").build()).execute().close()
                val end = System.nanoTime()
                val pingMs = (end - start) / 1_000_000.0
                if (pingMs < 2000) {
                    pings.add(pingMs)
                    if (pingMs < minPing) minPing = pingMs
                }
            } catch (_: Exception) { }
            delay(50)
        }

        var jitter = 0.0
        if (pings.size >= 2) {
            val diffs = (1 until pings.size).map { abs(pings[it] - pings[it - 1]) }
            jitter = diffs.average()
        }
        return Pair(if (minPing == Double.MAX_VALUE) 0.0 else minPing, jitter)
    }

    private suspend fun measureSpeed(
        isDownload: Boolean,
        durationMs: Long,
        onProgress: suspend (Double, Float) -> Unit
    ): Double = coroutineScope {
        val totalBytes = AtomicLong(0)
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationMs

        var isActive = true

        val workers = List(if (isDownload) 6 else 4) {
            launch(Dispatchers.IO) {
                val buffer = ByteArray(64 * 1024)
                val uploadData = ByteArray(512 * 1024).apply { Random.nextBytes(this) }

                while (isActive && System.currentTimeMillis() < endTime) {
                    try {
                        if (isDownload) {
                            val req = Request.Builder().url("${LIBRESPEED_SERVER}garbage.php?ckSize=100&r=${Math.random()}").build()
                            client.newCall(req).execute().use { resp ->
                                resp.body?.byteStream()?.use { input ->
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1 && isActive) {
                                        totalBytes.addAndGet(read.toLong())
                                    }
                                }
                            }
                        } else {
                            val req = Request.Builder()
                                .url("${LIBRESPEED_SERVER}empty.php?r=${Math.random()}")
                                .post(uploadData.toRequestBody("application/octet-stream".toMediaType()))
                                .build()
                            client.newCall(req).execute().use {
                                if (it.isSuccessful) totalBytes.addAndGet(uploadData.size.toLong())
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // Biến lưu tốc độ đã được làm mượt (EMA)
        var currentEmaSpeed = 0.0

        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()

        while (System.currentTimeMillis() < endTime) {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastTime

            if (timeDiff > 200) {
                val currentTotalBytes = totalBytes.get()
                val bytesDiff = currentTotalBytes - lastBytes

                var rawSpeedMbps = (bytesDiff * 8 * 1.06) / (timeDiff / 1000.0) / 1_000_000.0
                if (rawSpeedMbps < 0) rawSpeedMbps = 0.0

                // Logic EMA (Làm mượt)
                if (bytesDiff == 0L && currentEmaSpeed > 0) {
                    currentEmaSpeed *= 0.9
                } else {
                    val alpha = 0.7
                    currentEmaSpeed = (rawSpeedMbps * alpha) + (currentEmaSpeed * (1 - alpha))
                }

                val progress = (now - startTime).toFloat() / durationMs
                onProgress(currentEmaSpeed, progress)

                lastBytes = currentTotalBytes
                lastTime = now
            }
            delay(200)
        }

        // Dừng workers
        isActive = false
        // Hủy workers - gây ra độ trễ nhỏ để dọn dẹp resources
        workers.forEach { it.cancel() }

        val finalTotalBytes = totalBytes.get()
        val actualDurationSec = (System.currentTimeMillis() - startTime) / 1000.0
        val averageSpeedMbps = (finalTotalBytes * 8 * 1.06) / actualDurationSec / 1_000_000.0

        return@coroutineScope averageSpeedMbps

//        return@coroutineScope currentEmaSpeed
    }
}
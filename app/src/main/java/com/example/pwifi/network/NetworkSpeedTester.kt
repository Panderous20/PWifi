package com.example.pwifi.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

data class NetworkResult(
    val ping: Double,
    val jitter: Double,
    val downloadMbps: Double,
    val uploadMbps: Double
)

object NetworkSpeedTester {
    private const val LIBRESPEED_SERVER = "https://librespeed.a573.net/backend/"

    // Tạo OkHttpClient với connection pool
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES)) // Giữ connection
        .build()

    private suspend fun measurePingAndJitter(
        count: Int = 10
    ): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val url = "${LIBRESPEED_SERVER}empty.php"
        var ping = Double.MAX_VALUE
        val pings = mutableListOf<Double>()

        // Warmup
        try {
            val warmupReq = Request.Builder()
                .url("$url?r=${Math.random()}")
                .get()
                .build()
            client.newCall(warmupReq).execute().use { /* warmup */ }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        kotlinx.coroutines.delay(200)

        repeat(count) { i ->
            try {
                val request = Request.Builder()
                    .url("$url?r=${Math.random()}")
                    .get()
                    .build()

                val start = System.nanoTime()
                val response = client.newCall(request).execute()
                val instspd = (System.nanoTime() - start) / 1_000_000.0

                response.close()

                if (instspd < 2000 && response.isSuccessful) {
                    pings.add(instspd)
                    if (instspd < ping) {
                        ping = instspd
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            kotlinx.coroutines.delay(100)
        }

        // Tính jitter
        var jitter = 0.0
        if (pings.size >= 2) {
            val jitters = mutableListOf<Double>()
            for (i in 1 until pings.size) {
                jitters.add(abs(pings[i] - pings[i - 1]))
            }
            jitter = jitters.average()
        }

        if (ping == Double.MAX_VALUE) ping = 0.0

        Pair(ping, jitter)
    }

    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        val startT = System.currentTimeMillis()
        val graceTime = 1500L
        val testDuration = 15000L
        val testStartTime = java.util.concurrent.atomic.AtomicLong(0L)
        val totalBytesAtomic = java.util.concurrent.atomic.AtomicLong(0L)

        val streams = 6
        val jobs = (0 until streams).map {
            async {
                var gracePassed = false

                while (System.currentTimeMillis() - startT < testDuration) {
                    try {
                        val request = Request.Builder()
                            .url("${LIBRESPEED_SERVER}garbage.php?ckSize=100&r=${Math.random()}")
                            .get()
                            .build()

                        client.newCall(request).execute().use { response ->
                            response.body?.byteStream()?.use { input ->
                                val buffer = ByteArray(128 * 1024)
                                var bytesRead: Int

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    val now = System.currentTimeMillis()

                                    if (!gracePassed && now - startT >= graceTime) {
                                        gracePassed = true
                                        testStartTime.compareAndSet(0L, now)
                                    }

                                    if (gracePassed) {
                                        totalBytesAtomic.addAndGet(bytesRead.toLong())
                                    }

                                    if (now - startT >= testDuration) break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        jobs.awaitAll()

        val totalBytes = totalBytesAtomic.get()
        val startTime = testStartTime.get()

        if (startTime == 0L) return@withContext 0.0

        val actualDuration = (System.currentTimeMillis() - startTime) / 1000.0
        if (actualDuration <= 0.0) return@withContext 0.0

        (totalBytes * 8 * 1.06 / 1_000_000.0) / actualDuration
    }

    private suspend fun measureUploadSpeed(): Double = withContext(Dispatchers.IO) {
        val startT = System.currentTimeMillis()
        val graceTime = 1500L
        val testDuration = 15000L
        val testStartTime = java.util.concurrent.atomic.AtomicLong(0L)

        val blob = ByteArray(1 * 1024 * 1024)
        Random.nextBytes(blob)

        val mediaType = "application/octet-stream".toMediaType()

        val streams = 3
        val jobs = (0 until streams).map {
            async {
                var streamBytes = 0L

                while (System.currentTimeMillis() - startT < testDuration) {
                    try {
                        val body = blob.toRequestBody(mediaType)
                        val request = Request.Builder()
                            .url("${LIBRESPEED_SERVER}empty.php?r=${Math.random()}")
                            .post(body)
                            .build()

                        val now = System.currentTimeMillis()
                        if (now - startT >= graceTime) {
                            testStartTime.compareAndSet(0L, now)
                        }

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful && testStartTime.get() > 0L) {
                                streamBytes += blob.size
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                streamBytes
            }
        }

        val totalBytes = jobs.awaitAll().sum()
        val actualDuration = (System.currentTimeMillis() - testStartTime.get()) / 1000.0

        if (actualDuration <= 0.0 || testStartTime.get() == 0L) return@withContext 0.0

        (totalBytes * 8 * 1.06 / 1_000_000.0) / actualDuration
    }

    suspend fun runSpeedTest(): NetworkResult {
        val (ping, jitter) = measurePingAndJitter()
        val download = measureDownloadSpeed()
        val upload = measureUploadSpeed()
        return NetworkResult(ping, jitter, download, upload)
    }
}

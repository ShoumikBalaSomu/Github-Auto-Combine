package com.iptv.linkchecker.network

import com.iptv.linkchecker.data.Channel
import com.iptv.linkchecker.data.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class CheckProgress(
    val total: Int = 0,
    val checked: Int = 0,
    val live: Int = 0,
    val dead: Int = 0,
    val slow: Int = 0,
    val isRunning: Boolean = false
)

class LinkChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(16, 2, TimeUnit.MINUTES))
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(false)
        .build()

    private val semaphore = Semaphore(16)
    private val _progress = MutableStateFlow(CheckProgress())
    val progress: StateFlow<CheckProgress> = _progress.asStateFlow()

    @Volatile
    private var isCancelled = false

    private val checkedCount = AtomicInteger(0)
    private val liveCount = AtomicInteger(0)
    private val deadCount = AtomicInteger(0)
    private val slowCount = AtomicInteger(0)

    fun cancel() {
        isCancelled = true
    }

    suspend fun checkChannels(
        channels: List<Channel>,
        onChannelChecked: suspend (Channel) -> Unit
    ): List<Channel> = coroutineScope {
        isCancelled = false
        checkedCount.set(0)
        liveCount.set(0)
        deadCount.set(0)
        slowCount.set(0)

        _progress.value = CheckProgress(
            total = channels.size,
            isRunning = true
        )

        val results = channels.map { channel ->
            async(Dispatchers.IO) {
                if (isCancelled) {
                    return@async channel
                }

                semaphore.withPermit {
                    if (isCancelled) {
                        return@withPermit channel
                    }

                    val result = checkSingleChannel(channel)
                    val checked = checkedCount.incrementAndGet()

                    when (result.status) {
                        ChannelStatus.LIVE -> liveCount.incrementAndGet()
                        ChannelStatus.DEAD -> deadCount.incrementAndGet()
                        ChannelStatus.SLOW -> slowCount.incrementAndGet()
                        else -> {}
                    }

                    _progress.value = CheckProgress(
                        total = channels.size,
                        checked = checked,
                        live = liveCount.get(),
                        dead = deadCount.get(),
                        slow = slowCount.get(),
                        isRunning = !isCancelled
                    )

                    onChannelChecked(result)
                    result
                }
            }
        }.awaitAll()

        _progress.value = _progress.value.copy(isRunning = false)
        results
    }

    private suspend fun checkSingleChannel(channel: Channel): Channel =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                // Try HEAD request first
                val headRequest = Request.Builder()
                    .url(channel.streamUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                var responseCode: Int
                try {
                    client.newCall(headRequest).execute().use { response ->
                        responseCode = response.code
                    }
                } catch (e: Exception) {
                    // HEAD failed, try GET with range
                    val getRequest = Request.Builder()
                        .url(channel.streamUrl)
                        .header("Range", "bytes=0-1024")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()

                    try {
                        client.newCall(getRequest).execute().use { response ->
                            responseCode = response.code
                        }
                    } catch (e2: Exception) {
                        return@withContext channel.copy(
                            status = ChannelStatus.DEAD,
                            responseTimeMs = System.currentTimeMillis() - startTime,
                            lastCheckedAt = System.currentTimeMillis(),
                            errorMessage = e2.message ?: "Connection failed"
                        )
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val status = when {
                    responseCode in 200..299 || responseCode == 302 || responseCode == 301 -> {
                        if (elapsed > 4000) ChannelStatus.SLOW else ChannelStatus.LIVE
                    }
                    else -> ChannelStatus.DEAD
                }

                channel.copy(
                    status = status,
                    responseTimeMs = elapsed,
                    lastCheckedAt = System.currentTimeMillis(),
                    errorMessage = if (status == ChannelStatus.DEAD) "HTTP $responseCode" else ""
                )
            } catch (e: Exception) {
                channel.copy(
                    status = ChannelStatus.DEAD,
                    responseTimeMs = System.currentTimeMillis() - startTime,
                    lastCheckedAt = System.currentTimeMillis(),
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }

    fun shutdown() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

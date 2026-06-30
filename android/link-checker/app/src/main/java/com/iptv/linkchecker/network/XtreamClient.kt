package com.iptv.linkchecker.network

import com.iptv.linkchecker.data.Channel
import com.iptv.linkchecker.data.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class XtreamClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun getChannels(
        serverUrl: String,
        username: String,
        password: String,
        sourceId: Long
    ): List<Channel> = withContext(Dispatchers.IO) {
        val baseUrl = serverUrl.trimEnd('/')
        val apiUrl = "$baseUrl/player_api.php?username=$username&password=$password&action=get_live_streams"

        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Xtream API failed: HTTP ${response.code}")
        }

        val body = response.body?.string() ?: throw Exception("Empty response")
        val jsonArray = JSONArray(body)
        val channels = mutableListOf<Channel>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val streamId = obj.optInt("stream_id", 0)
            val name = obj.optString("name", "Unknown")
            val category = obj.optString("category_name", "")
            val icon = obj.optString("stream_icon", "")
            val containerExtension = obj.optString("container_extension", "ts")

            val streamUrl = "$baseUrl/$username/$password/$streamId.$containerExtension"

            channels.add(
                Channel(
                    sourceId = sourceId,
                    name = name,
                    group = category,
                    streamUrl = streamUrl,
                    logoUrl = icon,
                    status = ChannelStatus.UNCHECKED
                )
            )
        }

        channels
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = serverUrl.trimEnd('/')
            val apiUrl = "$baseUrl/player_api.php?username=$username&password=$password"

            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

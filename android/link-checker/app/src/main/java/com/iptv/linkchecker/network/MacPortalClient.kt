package com.iptv.linkchecker.network

import com.iptv.linkchecker.data.Channel
import com.iptv.linkchecker.data.ChannelStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MacPortalClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun getChannels(
        portalUrl: String,
        macAddress: String,
        sourceId: Long
    ): List<Channel> = withContext(Dispatchers.IO) {
        val baseUrl = portalUrl.trimEnd('/')
        val token = performHandshake(baseUrl, macAddress)
        val profile = getProfile(baseUrl, macAddress, token)
        fetchChannels(baseUrl, macAddress, token, sourceId)
    }

    private fun performHandshake(baseUrl: String, macAddress: String): String {
        val url = "$baseUrl/stalker_portal/server/load.php?type=stb&action=handshake&prehash=0&token=&JsHttpRequest=1-xml"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3")
            .header("Cookie", "mac=$macAddress; stb_lang=en; timezone=Europe/London")
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Handshake failed: empty response")

        val json = JSONObject(body)
        val js = json.getJSONObject("js")
        return js.getString("token")
    }

    private fun getProfile(baseUrl: String, macAddress: String, token: String): JSONObject {
        val url = "$baseUrl/stalker_portal/server/load.php?type=stb&action=get_profile&JsHttpRequest=1-xml"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3")
            .header("Authorization", "Bearer $token")
            .header("Cookie", "mac=$macAddress; stb_lang=en; timezone=Europe/London")
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Get profile failed")
        return JSONObject(body)
    }

    private fun fetchChannels(
        baseUrl: String,
        macAddress: String,
        token: String,
        sourceId: Long
    ): List<Channel> {
        val channels = mutableListOf<Channel>()
        var page = 1
        var hasMore = true

        while (hasMore) {
            val url = "$baseUrl/stalker_portal/server/load.php?type=itv&action=get_all_channels&p=$page&JsHttpRequest=1-xml"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3")
                .header("Authorization", "Bearer $token")
                .header("Cookie", "mac=$macAddress; stb_lang=en; timezone=Europe/London")
                .header("X-User-Agent", "Model: MAG250; Link: WiFi")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: break

            val json = JSONObject(body)
            val js = json.getJSONObject("js")
            val data = js.getJSONArray("data")

            if (data.length() == 0) {
                hasMore = false
            } else {
                for (i in 0 until data.length()) {
                    val ch = data.getJSONObject(i)
                    val name = ch.optString("name", "Unknown")
                    val cmd = ch.optString("cmd", "")
                    val logo = ch.optString("logo", "")
                    val tvGenre = ch.optString("tv_genre_name", "")

                    // Extract stream URL from cmd field
                    val streamUrl = extractStreamUrl(baseUrl, macAddress, token, cmd)

                    if (streamUrl.isNotEmpty()) {
                        channels.add(
                            Channel(
                                sourceId = sourceId,
                                name = name,
                                group = tvGenre,
                                streamUrl = streamUrl,
                                logoUrl = if (logo.startsWith("http")) logo else "$baseUrl/$logo",
                                status = ChannelStatus.UNCHECKED
                            )
                        )
                    }
                }

                val totalItems = js.optInt("total_items", 0)
                val maxPageItems = js.optInt("max_page_items", 14)
                if (maxPageItems > 0 && page * maxPageItems >= totalItems) {
                    hasMore = false
                }
                page++
            }
        }

        return channels
    }

    private fun extractStreamUrl(
        baseUrl: String,
        macAddress: String,
        token: String,
        cmd: String
    ): String {
        // cmd can be "ffrt http://..." or "http://..." or an itv reference
        val directUrl = cmd.removePrefix("ffrt").removePrefix("ffmpeg").trim()
        if (directUrl.startsWith("http")) {
            return directUrl
        }

        // If it's a reference, try to create the link
        try {
            val createLinkUrl = "$baseUrl/stalker_portal/server/load.php?type=itv&action=create_link&cmd=${
                java.net.URLEncoder.encode(cmd, "UTF-8")
            }&JsHttpRequest=1-xml"

            val request = Request.Builder()
                .url(createLinkUrl)
                .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3")
                .header("Authorization", "Bearer $token")
                .header("Cookie", "mac=$macAddress; stb_lang=en; timezone=Europe/London")
                .header("X-User-Agent", "Model: MAG250; Link: WiFi")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return ""

            val json = JSONObject(body)
            val js = json.getJSONObject("js")
            val cmdResult = js.optString("cmd", "")
            return cmdResult.removePrefix("ffrt").removePrefix("ffmpeg").trim()
        } catch (e: Exception) {
            return directUrl
        }
    }
}

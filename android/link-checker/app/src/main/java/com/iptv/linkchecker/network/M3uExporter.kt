package com.iptv.linkchecker.network

import com.iptv.linkchecker.data.Channel

class M3uExporter {
    fun exportToM3u8(channels: List<Channel>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")

        for (channel in channels) {
            val extinf = buildString {
                append("#EXTINF:-1")
                if (channel.name.isNotEmpty()) {
                    append(" tvg-name=\"${channel.name}\"")
                }
                if (channel.group.isNotEmpty()) {
                    append(" group-title=\"${channel.group}\"")
                }
                if (channel.logoUrl.isNotEmpty()) {
                    append(" tvg-logo=\"${channel.logoUrl}\"")
                }
                append(",${channel.name}")
            }
            sb.appendLine(extinf)
            sb.appendLine(channel.streamUrl)
        }

        return sb.toString()
    }
}

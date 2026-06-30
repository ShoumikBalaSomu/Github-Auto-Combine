package com.iptv.linkchecker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromChannelStatus(value: ChannelStatus): String = value.name

    @TypeConverter
    fun toChannelStatus(value: String): ChannelStatus = ChannelStatus.valueOf(value)
}

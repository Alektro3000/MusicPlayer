package com.example.myplayer.data

import android.net.Uri
import androidx.room.TypeConverter


class UriConverters {
    @TypeConverter
    fun fromString(value: String?): Uri? {
        return if (value == null) null else Uri.parse(value)
    }

    @TypeConverter
    fun toString(uri: Uri?): String? {
        return uri?.toString()
    }
}

class StringListConverters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split("\\")
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.joinToString("\\")
    }
}

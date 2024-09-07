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
    fun fromString(value: String?): Array<String>? {
        return value?.split("\\")?.toTypedArray()
    }

    @TypeConverter
    fun toString(list: Array<String>?): String? {
        return list?.joinToString("\\")
    }
}

package com.example.myplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.toDrawable
import androidx.media3.exoplayer.MetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import wseemann.media.FFmpegMediaMetadataRetriever


class AudioFetcher(
    private val data: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {
    @WorkerThread
    override suspend fun fetch(): FetchResult? {
        context.contentResolver.openFileDescriptor(data, "r")
            .use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                val art = mediaMetadataRetriever.embeddedPicture ?: return null
                val songImage = BitmapFactory.decodeByteArray(art, 0, art.size) ?: return null
                mediaMetadataRetriever.release()
                return DrawableResult(
                    drawable = songImage.toDrawable(context.resources),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            }
    }
    class Factory<T: Uri>(
        private val contextDrawable: Context
    ) : Fetcher.Factory<T> {
        override fun create(
            data: T,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher {
            return AudioFetcher(data, options, contextDrawable)
        }
    }

}
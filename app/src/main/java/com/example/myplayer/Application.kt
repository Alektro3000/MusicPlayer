package com.example.myplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.myplayer.data.SongRepository
import com.example.myplayer.data.SongRoomDatabase
import com.example.myplayer.ui.AudioFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext

class PlayerApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { SongRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SongRepository(database.songDao()) }

    @OptIn(DelicateCoroutinesApi::class)
    val coilThread by lazy { newFixedThreadPoolContext(nThreads = 2, "CoilLoading") }
    override fun newImageLoader(): ImageLoader {

        return ImageLoader.Builder(this)
            .crossfade(true)
            .dispatcher(coilThread)
            .interceptorDispatcher(coilThread)
            .components{

                add(AudioFetcher.Factory(baseContext))
            }
            .build()
    }
}

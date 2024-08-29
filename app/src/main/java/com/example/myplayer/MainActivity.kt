package com.example.myplayer

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.fetch.Fetcher
import coil.imageLoader
import com.example.myplayer.ui.theme.MyPlayerTheme
import com.google.common.util.concurrent.MoreExecutors
import com.kyant.taglib.TagLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import okhttp3.Dispatcher


class PlayerApplication : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { SongRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SongRepository(database.songDao()) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .dispatcher(newFixedThreadPoolContext(nThreads = 4, "Loading"))
            .interceptorDispatcher(Dispatchers.IO)
            .components{

                add(AudioFetcher.Factory(baseContext))
            }

            .build()
    }
}


class MainActivity : ComponentActivity() {
    private val songViewModel: SongViewModel by viewModels {
        SongViewModelFactory((application as PlayerApplication).repository, application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionToken =
            SessionToken(this, ComponentName(this, PlayerService::class.java))
        songViewModel.onStart(sessionToken)
        val activityLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            Thread {
                fetchAudio(result ?: return@Thread)
            }.start()
        }
        setContentView(R.layout.main_layout)
        val adapter = SongAdapter(UserComparator) { _, id ->
            Thread {
                songViewModel.setSongs(id, songViewModel.songList())
            }.start()
        }

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        lifecycleScope.launch {
            songViewModel.songListsPaged.collect { pagingData ->
                adapter.submitData(pagingData)
            }
        }
/*
        setContent {
            val list = songViewModel.songListsPaged.collectAsLazyPagingItems()

            MyPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar =
                {
                    Row()
                    {
                        ElevatedButton(onClick = { activityLauncher.launch(null) }) {
                            Text("Fetch")
                        }

                        ElevatedButton(onClick = {
                            songViewModel.insert(Playlist(1, "FirstPlaylist"))
                            songViewModel.insert(PlaylistSongCrossRef(1, 2))
                            songViewModel.insert(PlaylistSongCrossRef(1, 1))
                        }) {
                            Text("Add")
                        }
                    }
                }) { innerPadding ->
                    SongList(list,
                        modifier = Modifier.padding(innerPadding),
                        onClickSong = { id ->
                            Thread {
                                songViewModel.setSongs(id, songViewModel.songList())
                            }.start()
                        }
                    )
                }
            }

        }*/

    }

    private fun fetchAudio(directoryUri: Uri? = null) {
        val contentResolver = this.contentResolver

        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if( directoryUri != null)
            contentResolver.takePersistableUriPermission(directoryUri,takeFlags)

        val documentTree = DocumentFile.fromTreeUri(application, directoryUri ?: return) ?: return
        val mp3Files =
            documentTree.listFiles().filter { it.isFile && it.type?.startsWith("audio") ?: false }


        for (rawSong in mp3Files) {

            val uri = rawSong.uri

            var duration: String?
            var title: String? = null
            var artists: List<String>? = null
            var artist: String? = null
            contentResolver.openFileDescriptor(uri, "r").use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                mediaMetadataRetriever.release()

                if(pfd == null)
                    return@use

                val properties = TagLib.getMetadata(pfd.detachFd(),false)?.propertyMap ?: return@use

                title = properties["TITLE"]?.get(0)
                artists = properties["ARTIST"]?.toList()
                artist = (properties["DISPLAY ARTIST"]?.get(0))?:artists?.get(0)
            }
            songViewModel.insert(Song(0, uri, title, duration?.toInt(), artist, artists))
        }
    }

}

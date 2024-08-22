package com.example.myplayer

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import com.example.myplayer.ui.theme.MyPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob


class PlayerApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { SongRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SongRepository(database.wordDao()) }
}


class MainActivity : ComponentActivity() {
    private val wordViewModel: SongViewModel by viewModels {
        WordViewModelFactory((application as PlayerApplication).repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activityLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            fetchAudio(result?:return@registerForActivityResult)
        }
        setContent {
            var song by rememberSaveable { mutableStateOf(emptyList<Song>()) }
            wordViewModel.allSongs.observe(this) { songs ->
                // Update the cached copy of the words in the adapter.
                songs?.let { song = it }
            }
            MyPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar =
                {
                    ElevatedButton(onClick = {
                        activityLauncher.launch(null)
                        }) {

                    }
                }) { innerPadding ->
                    SongList(song,
                        modifier = Modifier.padding(innerPadding)

                    )

                }
            }
        }
    }

    private fun fetchAudio(directoryUri: Uri?)
    {
        val documentTree = DocumentFile.fromTreeUri(application, directoryUri?:return) ?: return
        val mp3Files = documentTree.listFiles().filter { it.isFile }
        val contentResolver = this.contentResolver
        for (rawSong in mp3Files) {
            val uri = rawSong.uri
            val type = rawSong.type
            if (type != "audio/mpeg")
                continue

            contentResolver.openFileDescriptor(uri, "r").use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                val artist =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val duration =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val name =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                /*
                val art = mediaMetadataRetriever.embeddedPicture
                val songImage = if (art!=null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
                */
                wordViewModel.insert(Song(0, uri, name, duration?.toInt(), artist))
            }
        }
    }
}

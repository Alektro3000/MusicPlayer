package com.example.myplayer

import android.app.Application
import android.content.Intent
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
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.myplayer.ui.theme.MyPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PlayerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val database by lazy { SongRoomDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { SongRepository(database.wordDao()) }
}


class MainActivity : ComponentActivity() {
    private val songViewModel: SongViewModel by viewModels {
        WordViewModelFactory((application as PlayerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activityLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            Thread {
                fetchAudio(result ?: return@Thread)
            }.start()
        }
        setContent {
            val list = songViewModel.songListsPaged.collectAsLazyPagingItems()
            MyPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar =
                {
                    ElevatedButton(onClick = {activityLauncher.launch(null) }) {
                        
                    }
                }) { innerPadding ->
                    SongList(list,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                
            }
        }
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

            contentResolver.openFileDescriptor(uri, "r").use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                val artist =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val duration =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val name =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                songViewModel.insert(Song(0, uri, name, duration?.toInt(), artist))
            }
        }
    }

}

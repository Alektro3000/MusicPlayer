package com.example.myplayer

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@Entity(tableName = "songs", indices =  [Index(value = ["name"], unique = true)])
data class Song(
    @PrimaryKey(autoGenerate = true) val songId: Int = 0,

    val uri: Uri? = null,
    val name: String?,
    val length: Int?  = null,

    val displayArtist: String?  = null,
    val artists: List<String>? = null
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int,
    val playlistName: String
)

@Entity(tableName = "PlaylistSongCrossRef", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Int,
    val songId: Int
)

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<Song>
)

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSong(user: Song)

    @Query("SELECT * FROM songs ORDER BY name")
    fun getSongs(): List<Song>

    @Transaction
    @Query("SELECT * FROM songs ORDER BY name")
    fun getSongsPages(): PagingSource<Int, Song>

    @Query("DELETE FROM songs")
    fun deleteAll()

    @Transaction
    @Query("SELECT * FROM playlists")
    fun getPlaylistsWithSongs(): List<PlaylistWithSongs>

    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistCrossRef(crossRef: PlaylistSongCrossRef)
}

@Database(entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class], version = 1, exportSchema = false)
@TypeConverters(UriConverters::class, StringListConverters::class)
abstract class SongRoomDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: SongRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): SongRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SongRoomDatabase::class.java,
                    "song_database"
                ).addCallback(SongDatabaseCallback(scope)).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    private class SongDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.songDao())
                }
            }
        }

        private fun populateDatabase(songDao: SongDao) {
            songDao.deleteAll()
            
        }

    }
}
class SongRepository(private val songDao: SongDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    fun getSongsPages() = songDao.getSongsPages()

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    fun getSongs() = songDao.getSongs()

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    fun getPlaylists() = songDao.getPlaylists()


    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(song: Song) {
        songDao.insertSong(song)
    }

    @WorkerThread
    suspend fun insertPlaylist(playlist: Playlist) {
        songDao.insertPlaylist(playlist)
    }

    @WorkerThread
    suspend fun insertPlaylistSong(playlistSongCrossRef: PlaylistSongCrossRef) {
        songDao.insertPlaylistCrossRef(playlistSongCrossRef)
    }


}

class  SongViewModel(private val repository: SongRepository,
    application: Application
) : AndroidViewModel(application)
{

    var songListsPaged = Pager(
        PagingConfig(
        pageSize = 2400,
            prefetchDistance = 100,
            enablePlaceholders = false,
    )
    ) {
        repository.getSongsPages()
    }.flow.cachedIn(viewModelScope)

    fun songList() = repository.getSongs()

    fun playlistList() = repository.getPlaylists()

    fun insert(song: Song) = viewModelScope.launch {
        repository.insert(song)
    }

    fun insert(playlist: Playlist) = viewModelScope.launch {
        repository.insertPlaylist(playlist)
    }

    fun insert(playlistSongCrossRef: PlaylistSongCrossRef) = viewModelScope.launch {
        repository.insertPlaylistSong(playlistSongCrossRef)
    }


    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>

    fun onStart(sessionToken: SessionToken) {
        if (!this::mediaControllerFuture.isInitialized || mediaControllerFuture.get()?.connectedToken != sessionToken) {
            val context = getApplication<Application>()
            mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            mediaControllerFuture.addListener({
                mediaControllerFuture.get()?.repeatMode = Player.REPEAT_MODE_ALL
                mediaControllerFuture.get()
            }, ContextCompat.getMainExecutor(context))
        }
    }
    private fun mediaController(lambda : (MediaController) -> Unit )
    {
        val context = getApplication<Application>()
        mediaControllerFuture.addListener({
            val mediaController = mediaControllerFuture.get()
            if(mediaController != null)
                lambda(mediaController)
        }, ContextCompat.getMainExecutor(context))

    }
    fun addSong(song: Song)
    {
        mediaController()
        { mediaController->

            mediaController.addMediaItem(buildMediaItem(song))

            mediaController.prepare()
            mediaController.play()
        }
    }
    fun setSongs(id: Int, songs: List<Song>)
    {
        songs.map {  }
        mediaController()
        { mediaController->

            mediaController.setMediaItems(songs.map { buildMediaItem(it)},id,0)

            mediaController.prepare()
            mediaController.play()
        }
    }
    private fun buildMediaItem(song: Song) : MediaItem
    {
        return MediaItem.Builder()
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist(song.displayArtist)
                    .setTitle(song.name)
                    .build()
            ).build()
    }
}
class SongViewModelFactory(private val repository: SongRepository, private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class  PlayerViewModel(private val player: ExoPlayer
) : ViewModel()
{

}

class PlayerViewModelFactory(private val player: ExoPlayer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(player) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

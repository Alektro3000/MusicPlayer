package com.example.myplayer.data

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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
import androidx.room.TypeConverters
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myplayer.PlayerApplication
import com.google.common.util.concurrent.ListenableFuture
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


@Entity(tableName = "songs", indices =  [Index(value = ["uri"], unique = true)])
data class Song(
    @PrimaryKey(autoGenerate = true) val songId: Int = 0,

    val uri: Uri? = null,
    val name: String?,
    val length: Int?  = null,

    val displayArtist: String?  = null,
    val artists: Array<String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Song

        if (songId != other.songId) return false
        if (uri != other.uri) return false
        if (name != other.name) return false
        if (length != other.length) return false
        if (displayArtist != other.displayArtist) return false
        if (artists != null) {
            if (other.artists == null) return false
            if (!artists.contentEquals(other.artists)) return false
        } else if (other.artists != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songId
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (length ?: 0)
        result = 31 * result + (displayArtist?.hashCode() ?: 0)
        result = 31 * result + (artists?.contentHashCode() ?: 0)
        return result
    }
}

data class SongIncluded(
    @Embedded val song: Song,
    val included: Boolean
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int,
    val title: String,
    val time: Int,
    val count: Int,
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

    @Upsert
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistCrossRef(crossRef: PlaylistSongCrossRef)

    @Delete
    suspend fun deleteSong(user: Song)


    @Query("SELECT * FROM songs WHERE songId = :id")
    fun getSong(id: Int): Flow<Song>

    @Query("SELECT * FROM songs ORDER BY name")
    fun getSongs(): Flow<List<Song>>

    @Query("SELECT songId, name, uri, displayArtist, length, artists, " +
            "EXISTS (SELECT * FROM playlistsongcrossref WHERE playlistsongcrossref.songId = songs.songId AND playlistId = :playlistId) AS included " +
            "FROM songs ORDER BY name")
    fun getSongsSelect(playlistId: Int): Flow<List<SongIncluded > >


    @Delete
    suspend fun deletePlaylistSong(playlistSongCrossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM songs")
    fun deleteAll()

    @Transaction
    @Query("SELECT * FROM playlists")
    fun getPlaylistsFull(): List<PlaylistWithSongs>

    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists")
    fun getPlaylistsPages(): PagingSource<Int,Playlist>

    @Query("SELECT * FROM playlists")
    fun getFullPlaylistsPages(): PagingSource<Int,PlaylistWithSongs>

    @Query("SELECT * FROM playlists WHERE playlistId == :id")
    fun getPlaylistFull(id: Int): Flow<PlaylistWithSongs>
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
    fun getSongs() = songDao.getSongs()

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    fun getPlaylists() = songDao.getPlaylists()

    fun getPlaylistsPages() = songDao.getPlaylistsPages()
    fun getFullPlaylistsPages() = songDao.getFullPlaylistsPages()
    fun getSongsSelect(playlistId: Int) = songDao.getSongsSelect(playlistId)

    fun getPlaylistFull(id: Int) = songDao.getPlaylistFull(id)

    fun getSong(id: Int) = songDao.getSong(id)
    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(song: Song) {
        songDao.insertSong(song)
    }

    @WorkerThread
    suspend fun deletePlaylistSong(playlistSongCrossRef: PlaylistSongCrossRef) {
        songDao.deletePlaylistSong(playlistSongCrossRef)
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

class  DataBaseViewModel(application: Application) : AndroidViewModel(application)
{
    private val repository: SongRepository
        get() = getApplication<PlayerApplication>().repository

    fun songList() = repository.getSongs()

    fun playlistList() = repository.getPlaylists()
    fun getPlaylist(id: Int) = repository.getPlaylistFull(id)
    fun getSongsSelect(playlistId: Int) = repository.getSongsSelect(playlistId)

    fun getSong(id: Int) = repository.getSong(id)

    var playlistListsPaged = Pager(
        PagingConfig(
            pageSize = 240,
            prefetchDistance = 100,
            enablePlaceholders = false,
        )
    ) {
        repository.getPlaylistsPages()
    }.flow.cachedIn(viewModelScope)

    fun insert(song: Song) = viewModelScope.launch {
        repository.insert(song)
    }

    fun deletePlaylistSong(playlistSongCrossRef: PlaylistSongCrossRef) = viewModelScope.launch {
        repository.deletePlaylistSong(playlistSongCrossRef)
    }
    fun insert(playlist: Playlist) = viewModelScope.launch {
        repository.insertPlaylist(playlist)
    }

    fun insert(playlistSongCrossRef: PlaylistSongCrossRef) = viewModelScope.launch {
        repository.insertPlaylistSong(playlistSongCrossRef)
    }
}
class  PlayerViewModel(private val application: Application
) : AndroidViewModel(application)
{

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

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture.addListener({
            val mediaController = mediaControllerFuture.get()
            mediaController.release()
        }, ContextCompat.getMainExecutor(getApplication()))

    }
    private fun mediaController(lambda : (MediaController) -> Unit )
    {
        mediaControllerFuture.addListener({
            val mediaController = mediaControllerFuture.get()
            if(mediaController != null)
                lambda(mediaController)
        }, ContextCompat.getMainExecutor(getApplication()))

    }
    fun addSong(song: Song)
    {
        mediaController()
        {
            it.addMediaItem(buildMediaItem(song))
            it.prepare()
            it.play()
        }
    }
    fun setSongs(id: Int, songs: List<Song>)
    {
        songs.map {  }
        mediaController()
        {
            it.setMediaItems(songs.map { buildMediaItem(it)},id,0)
            it.prepare()
            it.play()
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

class FetchViewModel(private val application: Application) : AndroidViewModel(application)
{
    fun fetchAudio(directoryUri: Uri? = null) {
        val contentResolver = application.applicationContext.contentResolver

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
            var artists: Array<String>? = null
            var artist: String? = null
            contentResolver.openFileDescriptor(uri, "r").use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                duration =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                mediaMetadataRetriever.release()

                if (pfd == null)
                    return@use

                val properties =
                    TagLib.getMetadata(pfd.detachFd(), false)?.propertyMap ?: return@use

                title = properties["TITLE"]?.get(0)
                artists = properties["ARTIST"]
                artist = (properties["DISPLAY ARTIST"]?.get(0)) ?: artists?.get(0)
            }
            viewModelScope.launch {
                getApplication<PlayerApplication>().repository.insert(
                    Song(
                        0,
                        uri,
                        title,
                        duration?.toInt(),
                        artist,
                        artists
                    )
                )
            }
        }
    }
    fun updateCover(cover: Uri,song: Uri ) {

        val contentResolver = application.applicationContext.contentResolver
        contentResolver.openInputStream(cover)!!.use { coverStream ->
            contentResolver.openFileDescriptor(song, "rw")!!.use {
                Log.d("", if(TagLib.savePictures(
                    it.detachFd(), arrayOf(
                        Picture(
                            data = coverStream.readBytes(),
                            description = "Front Cover",
                            pictureType = "Front Cover",
                            mimeType = contentResolver.getType(cover) ?: "Null",
                        )
                    )
                )) "successful" else "failed")
            }

        }
    }


    fun updateSong(song: Song)
    {
        val contentResolver = application.applicationContext.contentResolver
        contentResolver.openFileDescriptor(song.uri!!, "rw")?.use { pfd ->
            val metadata = TagLib.getMetadata(fd = pfd.dup().detachFd(), readPictures = true)!!

            val map =
                metadata.propertyMap.apply {
                    if (song.name != null)
                        this["TITLE"] = arrayOf(song.name)
                    if (song.displayArtist != null)
                        this["DISPLAY ARTIST"] =
                            if (song.artists?.size != 1) arrayOf(song.displayArtist) else song.artists
                    if (song.artists != null)
                        this["ARTIST"] = song.artists
                }
            TagLib.savePropertyMap(pfd.detachFd(),map)
        }
    }
}
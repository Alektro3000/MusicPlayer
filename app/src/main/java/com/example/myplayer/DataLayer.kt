package com.example.myplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@Entity(tableName = "songs", indices =  [Index(value = ["name"], unique = true)])
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val uri: Uri? = null,
    val name: String?,
    val length: Int?  = null,

    val displayArtist: String?  = null,
    val artists: List<String>? = null
)
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


@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Delete
    suspend fun delete(user: Song)

    @Query("SELECT * FROM songs ORDER BY name")
    fun getSongs(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM songs ORDER BY name")
    fun getSongsPages(): PagingSource<Int, Song>

    @Query("DELETE FROM songs")
    fun deleteAll()
}
@Database(entities = [Song::class], version = 1, exportSchema = false)
@TypeConverters(UriConverters::class, StringListConverters::class)
abstract class SongRoomDatabase : RoomDatabase() {

    abstract fun wordDao(): SongDao

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
                    //populateDatabase(database.wordDao())
                }
            }
        }

    }
}
class SongRepository(private val songDao: SongDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    fun getSongsPages() = songDao.getSongsPages()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(song: Song) {
        songDao.insert(song)
    }
}

class  SongViewModel(private val repository: SongRepository
) : ViewModel()
{

    var songListsPaged = Pager(
        PagingConfig(
        pageSize = 100,
            prefetchDistance = 150,
            enablePlaceholders = false,
    )
    ) {
        repository.getSongsPages()
    }.flow.cachedIn(viewModelScope)


    fun insert(song: Song) = viewModelScope.launch {
        repository.insert(song)
    }
}

class WordViewModelFactory(private val repository: SongRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AudioFetcher(
    private val data: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        context.contentResolver.openFileDescriptor(data, "r")
            .use { pfd ->

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(pfd?.fileDescriptor)

                val art = mediaMetadataRetriever.embeddedPicture
                val songImage =
                    if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
                if (songImage == null)
                    return null
                return DrawableResult(
                    drawable = songImage.toDrawable(context.resources),
                    isSampled = false,
                    dataSource = DataSource.MEMORY
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

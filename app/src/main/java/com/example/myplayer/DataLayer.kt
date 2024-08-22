package com.example.myplayer

import android.content.Context
import android.net.Uri
import android.view.inspector.InspectionCompanion
import androidx.annotation.WorkerThread
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: Uri? = null,
    val name: String?,
    val length: Int?  = null,
    val artist: String?  = null,
){
    @Ignore val cover: ImageBitmap? = null
}
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

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Delete
    suspend fun delete(user: Song)

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<Song>


    @Query("SELECT * FROM songs ORDER BY name ASC")
    fun getAlphabetizedSongs(): Flow<List<Song>>

    @Query("DELETE FROM songs")
    fun deleteAll()
}
@Database(entities = [Song::class], version = 1, exportSchema = false)
@TypeConverters(UriConverters::class)
public abstract class SongRoomDatabase : RoomDatabase() {

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
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.wordDao())
                }
            }
        }

        suspend fun populateDatabase(songDao: SongDao) {

        }
    }
}
class SongRepository(private val songDao: SongDao) {

    // Room executes all queries on a separate thread.
    // Observed Flow will notify the observer when the data has changed.
    val allWords: Flow<List<Song>> = songDao.getAlphabetizedSongs()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @WorkerThread
    suspend fun insert(song: Song) {
        songDao.insert(song)
    }
}

class  SongViewModel(private val repository: SongRepository) : ViewModel()
{
    val allSongs: LiveData<List<Song>> = repository.allWords.asLiveData()

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
package com.example.kinopoisk.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Query
import com.example.kinopoisk.domain.repository.MovieRepository
import com.example.kinopoisk.domain.usecase.AddMovieUseCase
import com.example.kinopoisk.domain.usecase.DeleteMoviesUseCase
import com.example.kinopoisk.domain.usecase.GetMoviesUseCase
import com.example.kinopoisk.domain.usecase.SearchMoviesUseCase
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// --- API MODELS ---
data class OmdbSearchResponse(
    @SerializedName("Search") val search: List<OmdbMovieDto>?,
    val totalResults: String?,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String?
)

data class OmdbMovieDto(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String,
    @SerializedName("imdbID") val imdbID: String,
    @SerializedName("Type") val type: String,
    @SerializedName("Poster") val poster: String
)

// --- ROOM ENTITY ---
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: String,
    val posterUrl: String,
    val imdbId: String = "",
    val genre: String = "Unknown"
)

// --- API INTERFACE ---
interface OmdbApi {
    @GET("/")
    suspend fun searchMovies(
        @retrofit2.http.Query("apikey") apiKey: String,
        @retrofit2.http.Query("s") query: String
    ): OmdbSearchResponse
}

// --- ROOM DAO ---
@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Delete
    suspend fun deleteMovies(movies: List<MovieEntity>)
}

// --- DATABASE ---
@Database(entities = [MovieEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "movie_db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}

// --- SINGLETON DI ---
object AppContainer {
    private const val BASE_URL = "https://www.omdbapi.com/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: OmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OmdbApi::class.java)
    }
    
    private lateinit var database: AppDatabase
    
    // Repository implementation
    private lateinit var repository: MovieRepository

    // Use Cases
    lateinit var getMoviesUseCase: GetMoviesUseCase
    lateinit var addMovieUseCase: AddMovieUseCase
    lateinit var deleteMoviesUseCase: DeleteMoviesUseCase
    lateinit var searchMoviesUseCase: SearchMoviesUseCase

    fun init(context: Context) {
        database = AppDatabase.getDatabase(context)
        repository = MovieRepositoryImpl(api, database.movieDao())
        
        getMoviesUseCase = GetMoviesUseCase(repository)
        addMovieUseCase = AddMovieUseCase(repository)
        deleteMoviesUseCase = DeleteMoviesUseCase(repository)
        searchMoviesUseCase = SearchMoviesUseCase(repository)
    }
}
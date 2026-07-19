package com.thelightphone.botany

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.US)

// Photos are captured at full sensor resolution, which is wildly more than this app ever
// displays (largest view is a 3.92" phone screen). Capping at save time keeps every later
// decode - detail view, and especially grid thumbnails - cheap instead of decoding a huge
// original on every scroll.
private const val MAX_SAVED_PHOTO_DIMENSION = 1280
private const val BITMAP_CACHE_ENTRIES = 48

data class PlantHistoryEntry(
    val id: Long,
    val commonName: String,
    val latinName: String,
    val family: String,
    val description: String,
    val isNative: Boolean,
    val isPoisonous: Boolean,
    val photoFileName: String,
    val dateLabel: String,
)

class BotanyRepository private constructor(
    filesDir: File,
    private val dataStore: DataStore<Preferences>,
    private val database: PlantHistoryDatabase,
) {
    private val dao = database.plantHistoryDao()
    private val api = PlantNetApi()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val photosDir = File(filesDir, "plant_photos").also { it.mkdirs() }
    private val bitmapCache = LruCache<String, Bitmap>(BITMAP_CACHE_ENTRIES)

    private val _settings = MutableStateFlow(BotanySettings())
    val settings: StateFlow<BotanySettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow<List<PlantHistoryEntry>>(emptyList())
    val history: StateFlow<List<PlantHistoryEntry>> = _history.asStateFlow()

    init {
        repoScope.launch {
            hydrateSettings()
            refreshHistory()
        }
    }

    private suspend fun hydrateSettings() {
        val prefs = dataStore.data.first()
        _settings.value = BotanySettings(
            defaultCameraFront = prefs[BotanyPreferences.DEFAULT_CAMERA_FRONT] ?: false,
            location = prefs[BotanyPreferences.LOCATION] ?: "",
        )
    }

    suspend fun setDefaultCameraFront(value: Boolean) = withContext(Dispatchers.IO) {
        dataStore.edit { it[BotanyPreferences.DEFAULT_CAMERA_FRONT] = value }
        _settings.value = _settings.value.copy(defaultCameraFront = value)
    }

    suspend fun setLocation(value: String) = withContext(Dispatchers.IO) {
        dataStore.edit { it[BotanyPreferences.LOCATION] = value }
        _settings.value = _settings.value.copy(location = value)
    }

    suspend fun identify(photo: Bitmap): Result<PlantIdentification> = withContext(Dispatchers.IO) {
        api.identify(photo, BuildConfig.PLANTNET_API_KEY)
    }

    suspend fun savePlant(photo: Bitmap, identification: PlantIdentification): PlantHistoryEntry =
        withContext(Dispatchers.IO) {
            val fileName = "${UUID.randomUUID()}.jpg"
            FileOutputStream(File(photosDir, fileName)).use { out ->
                photo.downscaledTo(MAX_SAVED_PHOTO_DIMENSION).compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val today = LocalDate.now()
            val entity = PlantHistoryEntity(
                commonName = identification.commonName,
                latinName = identification.latinName,
                family = identification.family,
                description = identification.description,
                // We never fabricate these - see PlantIdentification's doc comment.
                isNative = false,
                isPoisonous = false,
                photoFileName = fileName,
                dateIso = today.toString(),
                dateLabel = today.format(HISTORY_DATE_FORMAT),
            )
            val id = dao.insert(entity)
            refreshHistory()
            entity.toEntry(id)
        }

    /** [maxDimensionPx], when given, decodes at the smallest sample size that still covers it
     *  (e.g. grid thumbnails) instead of the full saved-photo resolution - and caches the
     *  result, so re-scrolling a grid doesn't redecode from disk every time a tile recycles
     *  back into view. */
    fun loadPhoto(fileName: String, maxDimensionPx: Int? = null): Bitmap? {
        val cacheKey = "$fileName@${maxDimensionPx ?: "full"}"
        bitmapCache.get(cacheKey)?.let { return it }

        val file = File(photosDir, fileName)
        if (!file.isFile) return null

        val bitmap = if (maxDimensionPx == null) {
            BitmapFactory.decodeFile(file.path)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx)
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }

        if (bitmap != null) bitmapCache.put(cacheKey, bitmap)
        return bitmap
    }

    private suspend fun refreshHistory() = withContext(Dispatchers.IO) {
        _history.value = dao.listAll().map { it.toEntry() }
    }

    companion object {
        @Volatile
        private var instance: BotanyRepository? = null

        fun getInstance(
            filesDir: File,
            dataStore: DataStore<Preferences>,
            databaseProvider: () -> PlantHistoryDatabase,
        ): BotanyRepository {
            return instance ?: synchronized(this) {
                instance ?: BotanyRepository(
                    filesDir = filesDir,
                    dataStore = dataStore,
                    database = databaseProvider(),
                ).also { instance = it }
            }
        }
    }
}

private fun Bitmap.downscaledTo(maxDimension: Int): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= maxDimension) return this
    val scale = maxDimension.toFloat() / largestSide
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

/** Standard power-of-two downsampling: largest sample size that still leaves both dimensions
 *  at or above [maxDimensionPx], so BitmapFactory never allocates more than it needs to. */
private fun calculateInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
    var sampleSize = 1
    val halfWidth = width / 2
    val halfHeight = height / 2
    while (halfWidth / sampleSize >= maxDimensionPx && halfHeight / sampleSize >= maxDimensionPx) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun PlantHistoryEntity.toEntry(overrideId: Long = id): PlantHistoryEntry = PlantHistoryEntry(
    id = overrideId,
    commonName = commonName,
    latinName = latinName,
    family = family,
    description = description,
    isNative = isNative,
    isPoisonous = isPoisonous,
    photoFileName = photoFileName,
    dateLabel = dateLabel,
)

fun SealedLightContext.botanyRepository(): BotanyRepository =
    BotanyRepository.getInstance(
        filesDir = filesDir,
        dataStore = dataStore,
    ) {
        buildDatabase(PlantHistoryDatabase::class.java, PlantHistoryDatabase.DATABASE_NAME)
    }

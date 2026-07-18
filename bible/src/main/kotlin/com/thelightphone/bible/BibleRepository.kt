package com.thelightphone.bible

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val SKIP_BACK_MS = 15_000
private const val PROGRESS_POLL_MS = 250L

data class ReadingPosition(val book: Book, val chapter: Int)

sealed class ChapterResult {
    data class Api(val verses: List<Verse>) : ChapterResult()
    object MissingApiKey : ChapterResult()
    object Unavailable : ChapterResult()
}

/** Audio playback is a single, app-wide session (not tied to whichever Reading Screen instance
 *  started it) so it keeps playing - and stays controllable - as the user navigates elsewhere. */
data class AudioPlayerState(
    val book: Book? = null,
    val chapter: Int? = null,
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null,
)

class BibleRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val database: HighlightDatabase,
    private val api: EsvApi,
) {
    private val dao = database.highlightDao()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settings = MutableStateFlow(BibleSettings())
    val settings: StateFlow<BibleSettings> = _settings.asStateFlow()

    private val _lastPosition = MutableStateFlow<ReadingPosition?>(null)
    val lastPosition: StateFlow<ReadingPosition?> = _lastPosition.asStateFlow()

    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    private val _audioState = MutableStateFlow(AudioPlayerState())
    val audioState: StateFlow<AudioPlayerState> = _audioState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        repoScope.launch {
            hydrate()
            _isHydrated.value = true
        }
    }

    private suspend fun hydrate() {
        val prefs = dataStore.data.first()
        _settings.value = BibleSettings(
            apiKey = prefs[BiblePreferences.API_KEY] ?: "",
            darkMode = prefs[BiblePreferences.DARK_MODE] ?: true,
            fontSize = FontSize.fromStorage(prefs[BiblePreferences.FONT_SIZE]),
            lineSpacing = LineSpacing.fromStorage(prefs[BiblePreferences.LINE_SPACING]),
            verseNumbers = prefs[BiblePreferences.VERSE_NUMBERS] ?: true,
        )
        val bookIdx = prefs[BiblePreferences.LAST_BOOK_IDX]
        val chapter = prefs[BiblePreferences.LAST_CHAPTER]
        if (bookIdx != null && chapter != null) {
            BIBLE_BOOKS.getOrNull(bookIdx)?.let { book ->
                _lastPosition.value = ReadingPosition(book, chapter)
            }
        }
    }

    suspend fun setLastPosition(book: Book, chapter: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[BiblePreferences.LAST_BOOK_IDX] = book.idx
            prefs[BiblePreferences.LAST_CHAPTER] = chapter
        }
        _lastPosition.value = ReadingPosition(book, chapter)
    }

    suspend fun setApiKey(value: String) = updateSettings(
        applyToPrefs = { it[BiblePreferences.API_KEY] = value },
        applyToState = { it.copy(apiKey = value) },
    )

    suspend fun setDarkMode(value: Boolean) = updateSettings(
        applyToPrefs = { it[BiblePreferences.DARK_MODE] = value },
        applyToState = { it.copy(darkMode = value) },
    )

    suspend fun setFontSize(value: FontSize) = updateSettings(
        applyToPrefs = { it[BiblePreferences.FONT_SIZE] = value.name },
        applyToState = { it.copy(fontSize = value) },
    )

    suspend fun setLineSpacing(value: LineSpacing) = updateSettings(
        applyToPrefs = { it[BiblePreferences.LINE_SPACING] = value.name },
        applyToState = { it.copy(lineSpacing = value) },
    )

    suspend fun setVerseNumbers(value: Boolean) = updateSettings(
        applyToPrefs = { it[BiblePreferences.VERSE_NUMBERS] = value },
        applyToState = { it.copy(verseNumbers = value) },
    )

    private suspend fun updateSettings(
        applyToPrefs: (MutablePreferences) -> Unit,
        applyToState: (BibleSettings) -> BibleSettings,
    ) = withContext(Dispatchers.IO) {
        dataStore.edit { applyToPrefs(it) }
        _settings.value = applyToState(_settings.value)
    }

    suspend fun loadChapter(book: Book, chapter: Int): ChapterResult = withContext(Dispatchers.IO) {
        val apiKey = _settings.value.apiKey
        if (apiKey.isBlank()) {
            return@withContext ChapterResult.MissingApiKey
        }
        api.fetchPassage(apiKey, book.name, chapter).getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return@withContext ChapterResult.Api(it) }
        ChapterResult.Unavailable
    }

    suspend fun audioUrl(book: Book, chapter: Int): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = _settings.value.apiKey
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No ESV API key configured"))
        }
        api.fetchAudioUrl(apiKey, book.name, chapter)
    }

    /** Starts playback of [book]/[chapter], or - if that's already the active session - just
     *  toggles play/pause and reveals its controls. Switches sessions if a different chapter
     *  was playing. */
    fun toggleAudioForChapter(book: Book, chapter: Int) {
        val current = _audioState.value
        if (mediaPlayer != null && current.book == book && current.chapter == chapter) {
            _audioState.update { it.copy(visible = true) }
            toggleAudioPlayPause()
        } else {
            _audioState.update { AudioPlayerState(book = book, chapter = chapter, visible = true, isLoading = true) }
            loadAndPlayAudio(book, chapter)
        }
    }

    fun toggleAudioPlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            progressJob?.cancel()
            _audioState.update { it.copy(isPlaying = false) }
        } else {
            player.start()
            _audioState.update { it.copy(isPlaying = true) }
            startAudioProgressLoop()
        }
    }

    fun skipAudioBack15() {
        val player = mediaPlayer ?: return
        val target = (player.currentPosition - SKIP_BACK_MS).coerceAtLeast(0)
        player.seekTo(target)
        _audioState.update { it.copy(positionMs = target.toLong()) }
    }

    fun seekAudioTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        player.seekTo(positionMs.toInt())
        _audioState.update { it.copy(positionMs = positionMs) }
    }

    /** Hides the popup without stopping playback - it stays controllable via the pause
     *  button surfaced on every screen while a session is active. */
    fun hideAudioPlayer() {
        _audioState.update { it.copy(visible = false) }
    }

    fun showAudioPlayer() {
        _audioState.update { it.copy(visible = true) }
    }

    private fun loadAndPlayAudio(book: Book, chapter: Int) {
        repoScope.launch {
            audioUrl(book, chapter)
                .onSuccess { url ->
                    // MediaPlayer must be created and prepared on a thread with a Looper for its
                    // callbacks (onPrepared/onCompletion) to fire reliably - repoScope is
                    // Dispatchers.IO, which has none. Without this, the auto-advance-to-next-
                    // chapter transition could start playing the next file (the OS audio buffer
                    // keeps outputting samples) without its onPrepared callback ever landing, so
                    // `isPlaying` in the UI state got stuck at false even though audio played on.
                    withContext(Dispatchers.Main) { preparePlayer(book, chapter, url) }
                }
                .onFailure { e ->
                    val message = if (_settings.value.apiKey.isBlank()) {
                        "Add an ESV API key in Settings to enable chapter audio."
                    } else {
                        "Couldn't load audio: ${e.message}"
                    }
                    _audioState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    private fun preparePlayer(book: Book, chapter: Int, url: String) {
        releaseAudioPlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        player.setOnPreparedListener {
            _audioState.update { state -> state.copy(isLoading = false, durationMs = player.duration.toLong(), isPlaying = true) }
            player.start()
            startAudioProgressLoop()
        }
        player.setOnCompletionListener {
            progressJob?.cancel()
            val next = nextChapter(book, chapter)
            if (next != null) {
                val (nextBook, nextChapterNumber) = next
                _audioState.update {
                    AudioPlayerState(book = nextBook, chapter = nextChapterNumber, visible = true, isLoading = true)
                }
                loadAndPlayAudio(nextBook, nextChapterNumber)
            } else {
                _audioState.update { state -> state.copy(isPlaying = false, positionMs = state.durationMs) }
            }
        }
        player.setOnErrorListener { _, _, _ ->
            progressJob?.cancel()
            _audioState.update { it.copy(isLoading = false, isPlaying = false, error = "Couldn't play audio.") }
            true
        }
        try {
            player.setDataSource(url)
            player.prepareAsync()
        } catch (e: Exception) {
            _audioState.update { it.copy(isLoading = false, error = "Couldn't load audio.") }
        }
    }

    private fun startAudioProgressLoop() {
        progressJob?.cancel()
        progressJob = repoScope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _audioState.update { it.copy(positionMs = player.currentPosition.toLong()) }
                }
                delay(PROGRESS_POLL_MS)
            }
        }
    }

    private fun releaseAudioPlayer() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.apply {
            runCatching { stop() }
            release()
        }
        mediaPlayer = null
    }

    suspend fun search(query: String): List<SearchHit> = withContext(Dispatchers.IO) {
        val apiKey = _settings.value.apiKey
        if (apiKey.isBlank()) return@withContext emptyList()
        api.search(apiKey, query).getOrNull() ?: emptyList()
    }

    suspend fun highlightedVerses(book: Book, chapter: Int): Set<Int> = withContext(Dispatchers.IO) {
        dao.highlightedVerses(book.idx, chapter).toSet()
    }

    /** Toggles highlight state for [verses] as one action: removes all if every verse is already
     *  highlighted, otherwise adds whichever aren't highlighted yet. */
    suspend fun toggleHighlights(book: Book, chapter: Int, verses: Map<Int, String>) = withContext(Dispatchers.IO) {
        val existing = dao.highlightedVerses(book.idx, chapter).toSet()
        val allHighlighted = verses.keys.isNotEmpty() && verses.keys.all { it in existing }
        if (allHighlighted) {
            dao.delete(book.idx, chapter, verses.keys.toList())
        } else {
            val today = LocalDate.now().toString()
            dao.upsertAll(
                verses.map { (verse, text) ->
                    HighlightEntity(book.idx, book.name, chapter, verse, text, today)
                },
            )
        }
    }

    suspend fun listHighlights(): List<HighlightEntity> = withContext(Dispatchers.IO) {
        dao.listAll()
    }

    companion object {
        @Volatile
        private var instance: BibleRepository? = null

        fun getInstance(
            dataStore: DataStore<Preferences>,
            databaseProvider: () -> HighlightDatabase,
        ): BibleRepository {
            return instance ?: synchronized(this) {
                instance ?: BibleRepository(
                    dataStore = dataStore,
                    database = databaseProvider(),
                    api = EsvApi(),
                ).also { instance = it }
            }
        }
    }
}

fun SealedLightContext.bibleRepository(): BibleRepository =
    BibleRepository.getInstance(dataStore) {
        buildDatabase(HighlightDatabase::class.java, HighlightDatabase.DATABASE_NAME)
    }

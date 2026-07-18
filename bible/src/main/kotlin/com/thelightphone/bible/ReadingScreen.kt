package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ReadingUiState {
    object Loading : ReadingUiState()
    object Unavailable : ReadingUiState()
    object MissingApiKey : ReadingUiState()
    data class Loaded(val verses: List<Verse>) : ReadingUiState()
}

class ReadingViewModel(
    private val repository: BibleRepository,
    val book: Book,
    val chapter: Int,
    val targetVerse: Int?,
) : LightViewModel<Unit>() {
    val settings: StateFlow<BibleSettings> = repository.settings

    private val _uiState = MutableStateFlow<ReadingUiState>(ReadingUiState.Loading)
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    private val _selectedVerses = MutableStateFlow<Set<Int>>(emptySet())
    val selectedVerses: StateFlow<Set<Int>> = _selectedVerses.asStateFlow()

    private val _highlightedVerses = MutableStateFlow<Set<Int>>(emptySet())
    val highlightedVerses: StateFlow<Set<Int>> = _highlightedVerses.asStateFlow()

    // Audio is a single app-wide session owned by the repository, not this screen, so it
    // survives navigation and stays controllable from anywhere.
    val audioState: StateFlow<AudioPlayerState> = repository.audioState

    private var hasLoaded = false

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        if (hasLoaded) return
        hasLoaded = true
        // If a chapter's audio is already active (playing or paused) when this screen appears,
        // surface its controls rather than leaving the user with no visible way to reach them.
        if (audioState.value.book != null) {
            repository.showAudioPlayer()
        }
        viewModelScope.launch {
            _highlightedVerses.value = repository.highlightedVerses(book, chapter)
            _uiState.value = when (val result = repository.loadChapter(book, chapter)) {
                is ChapterResult.Api -> ReadingUiState.Loaded(result.verses)
                ChapterResult.MissingApiKey -> ReadingUiState.MissingApiKey
                ChapterResult.Unavailable -> ReadingUiState.Unavailable
            }
        }
    }

    fun toggleVerse(number: Int) {
        _selectedVerses.update { current ->
            if (number in current) current - number else current + number
        }
    }

    fun clearSelection() {
        _selectedVerses.value = emptySet()
    }

    fun toggleHighlightForSelection() {
        val loaded = _uiState.value as? ReadingUiState.Loaded ?: return
        val selection = _selectedVerses.value
        if (selection.isEmpty()) return
        val textByVerse = loaded.verses.filter { it.number in selection }.associate { it.number to it.text }
        viewModelScope.launch {
            repository.toggleHighlights(book, chapter, textByVerse)
            _highlightedVerses.value = repository.highlightedVerses(book, chapter)
            _selectedVerses.value = emptySet()
        }
    }

    override fun onBackPressed(): Boolean {
        if (_selectedVerses.value.isNotEmpty()) {
            _selectedVerses.value = emptySet()
            return true
        }
        return false
    }

    suspend fun setLastPosition(book: Book, chapter: Int) = repository.setLastPosition(book, chapter)

    fun onPlayButtonClicked() = repository.toggleAudioForChapter(book, chapter)
    fun togglePlayPause() = repository.toggleAudioPlayPause()
    fun skipBack15() = repository.skipAudioBack15()
    fun seekTo(positionMs: Long) = repository.seekAudioTo(positionMs)
    fun dismissAudioPlayer() = repository.hideAudioPlayer()
}

class ReadingScreen(
    sealedActivity: SealedLightActivity,
    private val book: Book,
    private val chapter: Int,
    private val targetVerse: Int? = null,
) : LightScreen<Unit, ReadingViewModel>(sealedActivity) {

    override val viewModelClass: Class<ReadingViewModel>
        get() = ReadingViewModel::class.java

    override fun createViewModel(): ReadingViewModel =
        ReadingViewModel(lightContext.bibleRepository(), book, chapter, targetVerse)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val uiState by viewModel.uiState.collectAsState()
        val selectedVerses by viewModel.selectedVerses.collectAsState()
        val highlightedVerses by viewModel.highlightedVerses.collectAsState()
        val audioState by viewModel.audioState.collectAsState()
        val settings by viewModel.settings.collectAsState()
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        fun goToChapter(targetBook: Book, targetChapter: Int) {
            scope.launch {
                viewModel.setLastPosition(targetBook, targetChapter)
                navigateTo(screenFactory = { activity -> ReadingScreen(activity, targetBook, targetChapter) })
            }
        }

        LaunchedEffect(uiState) {
            val loaded = uiState as? ReadingUiState.Loaded ?: return@LaunchedEffect
            val target = viewModel.targetVerse
            if (target == null) {
                // Always land on the top of the chapter - covers next/previous chapter
                // navigation, which doesn't target a specific verse.
                listState.scrollToItem(0)
                return@LaunchedEffect
            }
            val index = loaded.verses.indexOfFirst { it.number == target }
            if (index >= 0) listState.animateScrollToItem(index)
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(
                        text = "${book.name} $chapter",
                        onClick = { navigateTo(screenFactory = { activity -> ChaptersScreen(activity, book) }) },
                    ),
                    rightButton = LightBarButton.LightIcon(
                        // Same rule as every other screen's global audio button: pause icon
                        // whenever anything is playing. Unlike those screens (which just omit
                        // the button when nothing is playing), this one always shows something,
                        // falling back to "play this chapter" when there's nothing to pause.
                        icon = if (audioState.isPlaying) LightIcons.PAUSE else LightIcons.PLAY,
                        contentDescription = if (audioState.isPlaying) "Pause chapter audio" else "Play chapter audio",
                        onClick = {
                            if (audioState.isPlaying) viewModel.togglePlayPause() else viewModel.onPlayButtonClicked()
                        },
                        sizeUnits = 1.4f,
                    ),
                    modifier = Modifier.padding(bottom = 0.3f.gridUnitsAsDp()),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val state = uiState) {
                        ReadingUiState.Loading -> CenteredMessage("Loading…")
                        ReadingUiState.Unavailable -> CenteredMessage(
                            "Couldn't load this chapter. Check your connection and try again.",
                        )

                        ReadingUiState.MissingApiKey -> MissingApiKeyMessage(
                            onOpenSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                        )

                        is ReadingUiState.Loaded -> {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 1f.gridUnitsAsDp())) {
                                LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    items(state.verses) { verse ->
                                        VerseRow(
                                            verse = verse,
                                            selected = verse.number in selectedVerses,
                                            highlighted = verse.number in highlightedVerses,
                                            showNumber = settings.verseNumbers,
                                            fontSize = settings.fontSize,
                                            lineSpacing = settings.lineSpacing,
                                            onClick = { viewModel.toggleVerse(verse.number) },
                                        )
                                    }
                                    item {
                                        ChapterNavRow(
                                            previous = previousChapter(book, chapter),
                                            next = nextChapter(book, chapter),
                                            onSelect = ::goToChapter,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedVerses.isNotEmpty()) {
                    VerseActionFlyout(
                        selectedCount = selectedVerses.size,
                        allHighlighted = selectedVerses.all { it in highlightedVerses },
                        onCancel = viewModel::clearSelection,
                        onToggleHighlight = viewModel::toggleHighlightForSelection,
                    )
                } else if (audioState.visible) {
                    AudioPlayerFlyout(
                        state = audioState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSkipBack15 = viewModel::skipBack15,
                        onSeek = viewModel::seekTo,
                        onClose = viewModel::dismissAudioPlayer,
                    )
                }

                BibleBottomBar(
                    active = BibleTab.Read,
                    onRead = { navigateTo(screenFactory = { activity -> BooksScreen(activity) }) },
                    onSearch = { navigateTo(screenFactory = { activity -> SearchScreen(activity) }) },
                    onHighlights = { navigateTo(screenFactory = { activity -> HighlightsScreen(activity) }) },
                    onSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                )
            }
        }
    }
}

@Composable
private fun ChapterNavRow(
    previous: Pair<Book, Int>?,
    next: Pair<Book, Int>?,
    onSelect: (Book, Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.8f.gridUnitsAsDp()),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            previous?.let { (previousBook, previousChapterNumber) ->
                LightText(
                    text = "‹ ${previousBook.name} $previousChapterNumber",
                    variant = LightTextVariant.Subheading,
                    modifier = Modifier.lightClickable { onSelect(previousBook, previousChapterNumber) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            next?.let { (nextBook, nextChapterNumber) ->
                LightText(
                    text = "${nextBook.name} $nextChapterNumber ›",
                    variant = LightTextVariant.Subheading,
                    modifier = Modifier.lightClickable { onSelect(nextBook, nextChapterNumber) },
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = text, variant = LightTextVariant.Copy, lighten = true, align = TextAlign.Center)
    }
}

@Composable
private fun MissingApiKeyMessage(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1f.gridUnitsAsDp()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LightText(
                text = "An ESV API key is required to read the Bible.",
                variant = LightTextVariant.Copy,
                lighten = true,
                align = TextAlign.Center,
            )
            LightText(
                text = "Go to Settings",
                variant = LightTextVariant.Copy,
                align = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 0.6f.gridUnitsAsDp())
                    .lightClickable(onClick = onOpenSettings),
            )
        }
    }
}

@Composable
private fun VerseRow(
    verse: Verse,
    selected: Boolean,
    highlighted: Boolean,
    showNumber: Boolean,
    fontSize: FontSize,
    lineSpacing: LineSpacing,
    onClick: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    val tint = when {
        selected -> colors.content.copy(alpha = 0.10f)
        highlighted -> colors.content.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val bodySizeSp = fontSize.bodyPx.designVerticalPxToSp()
    val lineHeightSp = (fontSize.bodyPx * lineSpacing.multiplier).designVerticalPxToSp()
    val numberSizeSp = (fontSize.bodyPx * 0.55f).designVerticalPxToSp()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(tint)
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.3f.gridUnitsAsDp()),
    ) {
        Box(
            modifier = Modifier
                .width(0.1f.gridUnitsAsDp())
                .fillMaxHeight()
                .background(if (selected) colors.content else Color.Transparent),
        )
        Text(
            text = buildAnnotatedString {
                if (showNumber) {
                    withStyle(
                        SpanStyle(
                            fontSize = numberSizeSp,
                            color = colors.contentSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append("${verse.number} ")
                    }
                }
                append(verse.text)
            },
            style = TextStyle(fontSize = bodySizeSp, lineHeight = lineHeightSp, color = colors.content),
            modifier = Modifier.padding(start = 0.35f.gridUnitsAsDp()),
        )
    }
}

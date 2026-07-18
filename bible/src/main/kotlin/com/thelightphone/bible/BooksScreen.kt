package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val BOOK_ROW_HEIGHT_UNITS = 2.6f

class BooksViewModel(private val repository: BibleRepository) : LightViewModel<Unit>() {
    private var hasCheckedResume = false

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        if (hasCheckedResume) return
        hasCheckedResume = true
        viewModelScope.launch {
            repository.isHydrated.first { it }
            repository.lastPosition.value?.let { position ->
                screen.navigateTo(screenFactory = { activity ->
                    ReadingScreen(activity, position.book, position.chapter)
                })
            }
        }
    }
}

@InitialScreen
class BooksScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, BooksViewModel>(sealedActivity) {

    override val viewModelClass: Class<BooksViewModel>
        get() = BooksViewModel::class.java

    override fun createViewModel(): BooksViewModel = BooksViewModel(lightContext.bibleRepository())

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val listState = rememberLazyListState()
        val repository = lightContext.bibleRepository()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Select Book"),
                    rightButton = globalAudioTopBarButton(repository),
                    modifier = Modifier.padding(bottom = 0.3f.gridUnitsAsDp()),
                )

                LightLazyScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                    listState = listState,
                    uniformItemHeightGridUnits = BOOK_ROW_HEIGHT_UNITS,
                ) {
                    items(BIBLE_BOOKS) { book ->
                        BookRow(
                            book = book,
                            onClick = {
                                navigateTo(screenFactory = { activity ->
                                    ChaptersScreen(activity, book)
                                })
                            },
                        )
                    }
                }

                BibleBottomBar(
                    active = BibleTab.Read,
                    onRead = {},
                    onSearch = { navigateTo(screenFactory = { activity -> SearchScreen(activity) }) },
                    onHighlights = { navigateTo(screenFactory = { activity -> HighlightsScreen(activity) }) },
                    onSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                )
            }
        }
    }
}

@Composable
private fun BookRow(book: Book, onClick: () -> Unit) {
    val colors = LightThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(BOOK_ROW_HEIGHT_UNITS.gridUnitsAsDp())
            .lightClickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            LightText(text = book.name, variant = LightTextVariant.Heading)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.content.copy(alpha = 0.14f)),
        )
    }
}

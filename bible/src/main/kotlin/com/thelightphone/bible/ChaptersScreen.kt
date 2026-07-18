package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.launch

private const val GRID_GAP_UNITS = 0.55f

class ChaptersScreen(
    sealedActivity: SealedLightActivity,
    private val book: Book,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.bibleRepository()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(book.name),
                    rightButton = globalAudioTopBarButton(repository),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(GRID_GAP_UNITS.gridUnitsAsDp()),
                    verticalArrangement = Arrangement.spacedBy(GRID_GAP_UNITS.gridUnitsAsDp()),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    items(book.chapters) { index ->
                        val chapter = index + 1
                        ChapterCell(chapter) {
                            scope.launch {
                                repository.setLastPosition(book, chapter)
                                navigateTo(screenFactory = { activity -> ReadingScreen(activity, book, chapter) })
                            }
                        }
                    }
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
private fun ChapterCell(number: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .lightClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = number.toString(), variant = LightTextVariant.Heading)
    }
}

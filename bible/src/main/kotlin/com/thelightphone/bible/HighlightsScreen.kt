package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HighlightGroup(
    val book: Book,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val dateIso: String,
    val text: String,
)

private val DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

fun HighlightGroup.referenceLabel(): String =
    if (startVerse == endVerse) "${book.name} $chapter:$startVerse" else "${book.name} $chapter:$startVerse-$endVerse"

fun HighlightGroup.dateLabel(): String = LocalDate.parse(dateIso).format(DATE_LABEL_FORMAT)

fun groupHighlights(entities: List<HighlightEntity>): List<HighlightGroup> {
    val groups = mutableListOf<MutableList<HighlightEntity>>()
    for (entity in entities) {
        val current = groups.lastOrNull()
        val last = current?.lastOrNull()
        if (last != null && last.bookIdx == entity.bookIdx && last.chapter == entity.chapter &&
            last.dateIso == entity.dateIso && entity.verse == last.verse + 1
        ) {
            current.add(entity)
        } else {
            groups.add(mutableListOf(entity))
        }
    }
    return groups.map { group ->
        val first = group.first()
        HighlightGroup(
            book = findBook(first.book) ?: BIBLE_BOOKS[first.bookIdx],
            chapter = first.chapter,
            startVerse = first.verse,
            endVerse = group.last().verse,
            dateIso = first.dateIso,
            text = group.joinToString(" ") { it.text },
        )
    }
}

class HighlightsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.bibleRepository()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var groups by remember { mutableStateOf<List<HighlightGroup>?>(null) }

        LaunchedEffect(Unit) {
            groups = groupHighlights(repository.listHighlights())
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Highlights"),
                    rightButton = globalAudioTopBarButton(repository),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val loaded = groups
                    if (loaded != null && loaded.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "No highlighted verses yet.",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                align = TextAlign.Center,
                            )
                        }
                    } else if (loaded != null) {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                        ) {
                            loaded.forEach { group ->
                                HighlightRow(group = group) {
                                    navigateTo(screenFactory = { activity ->
                                        ReadingScreen(activity, group.book, group.chapter, targetVerse = group.startVerse)
                                    })
                                }
                            }
                        }
                    }
                }

                BibleBottomBar(
                    active = BibleTab.Highlights,
                    onRead = { navigateToReadTab() },
                    onSearch = { navigateTo(screenFactory = { activity -> SearchScreen(activity) }) },
                    onHighlights = {},
                    onSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                )
            }
        }
    }

    private fun navigateToReadTab() {
        val position = repository.lastPosition.value
        if (position != null) {
            navigateTo(screenFactory = { activity -> ReadingScreen(activity, position.book, position.chapter) })
        } else {
            navigateTo(screenFactory = { activity -> BooksScreen(activity) })
        }
    }
}

@Composable
private fun HighlightRow(group: HighlightGroup, onClick: () -> Unit) {
    val colors = LightThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.6f.gridUnitsAsDp()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LightText(text = group.referenceLabel(), variant = LightTextVariant.Subheading)
            LightText(text = group.dateLabel(), variant = LightTextVariant.Detail, lighten = true)
        }
        LightText(
            text = group.text,
            variant = LightTextVariant.Copy,
            lighten = true,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 0.15f.gridUnitsAsDp()),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.5f.gridUnitsAsDp())
                .height(1.dp)
                .background(colors.content.copy(alpha = 0.14f)),
        )
    }
}

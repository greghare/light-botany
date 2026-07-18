package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
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

class SearchResultsScreen(
    sealedActivity: SealedLightActivity,
    private val query: String,
    private val results: List<SearchHit>,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val repository = lightContext.bibleRepository()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Search Results"),
                    rightButton = globalAudioTopBarButton(repository),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "No matches for \"$query\"",
                                variant = LightTextVariant.Copy,
                                lighten = true,
                                align = TextAlign.Center,
                            )
                        }
                    } else {
                        LightScrollView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 1f.gridUnitsAsDp()),
                        ) {
                            results.forEach { hit ->
                                SearchResultRow(hit = hit, query = query) {
                                    navigateTo(screenFactory = { activity ->
                                        ReadingScreen(activity, hit.book, hit.chapter, targetVerse = hit.verse)
                                    })
                                }
                            }
                        }
                    }
                }

                BibleBottomBar(
                    active = BibleTab.Search,
                    onRead = { navigateToReadTab() },
                    onSearch = {},
                    onHighlights = { navigateTo(screenFactory = { activity -> HighlightsScreen(activity) }) },
                    onSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                )
            }
        }
    }

    private fun navigateToReadTab() {
        val repository = lightContext.bibleRepository()
        val position = repository.lastPosition.value
        if (position != null) {
            navigateTo(screenFactory = { activity -> ReadingScreen(activity, position.book, position.chapter) })
        } else {
            navigateTo(screenFactory = { activity -> BooksScreen(activity) })
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, query: String, onClick: () -> Unit) {
    val colors = LightThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 0.6f.gridUnitsAsDp()),
    ) {
        LightText(
            text = "${hit.book.name} ${hit.chapter}:${hit.verse}",
            variant = LightTextVariant.Subheading,
            modifier = Modifier.padding(bottom = 0.15f.gridUnitsAsDp()),
        )
        val matchIndex = hit.text.indexOf(query, ignoreCase = true)
        Text(
            text = if (matchIndex >= 0) {
                buildAnnotatedString {
                    append(hit.text.substring(0, matchIndex))
                    withStyle(SpanStyle(color = colors.content, fontWeight = FontWeight.SemiBold)) {
                        append(hit.text.substring(matchIndex, matchIndex + query.length))
                    }
                    append(hit.text.substring(matchIndex + query.length))
                }
            } else {
                buildAnnotatedString { append(hit.text) }
            },
            style = LightThemeTokens.typography.copy.copy(color = colors.contentSecondary),
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

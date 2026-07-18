package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.launch

private const val MASKED_PREFIX_LENGTH = 8
private const val MASKED_SUFFIX_LENGTH = 4

class SettingsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.bibleRepository()

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val settings by repository.settings.collectAsState()
        val scope = rememberCoroutineScope()
        var editingApiKey by remember { mutableStateOf(false) }
        var apiKeyEditSession by remember { mutableStateOf(0) }
        var revealed by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
            if (editingApiKey) {
                val apiKeyState = remember(apiKeyEditSession) { TextFieldState(settings.apiKey) }
                val keyboardOptionsFlow = rememberKeyboardOptions()
                LightTextInputEditor(
                    title = "ESV API Key",
                    state = apiKeyState,
                    keyboardOptionsFlow = keyboardOptionsFlow,
                    onSubmit = { value ->
                        scope.launch { repository.setApiKey(value.toString().trim()) }
                        editingApiKey = false
                    },
                    onBack = { editingApiKey = false },
                    submitLabel = "SAVE",
                    singleLine = true,
                    editorKey = apiKeyEditSession,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("Settings (${BuildConfig.VERSION_NAME})"),
                        rightButton = globalAudioTopBarButton(repository),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1.5f.gridUnitsAsDp()),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            LightTextField(
                                label = "ESV API KEY",
                                value = if (revealed) settings.apiKey else maskedApiKey(settings.apiKey),
                                placeholder = "Paste your ESV API key",
                                onClick = {
                                    editingApiKey = true
                                    apiKeyEditSession++
                                },
                                modifier = Modifier.weight(1f),
                            )
                            LightIcon(
                                icon = if (revealed) LightIcons.EYE_OFF else LightIcons.EYE,
                                size = 1.3f,
                                contentDescription = if (revealed) "Hide API key" else "Show API key",
                                modifier = Modifier
                                    .padding(start = 0.5f.gridUnitsAsDp(), bottom = 0.55f.gridUnitsAsDp())
                                    .lightClickable(onClick = { revealed = !revealed }),
                            )
                        }
                        LightText(
                            text = "Get a free key at api.esv.org.",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.4f.gridUnitsAsDp(), bottom = 1.6f.gridUnitsAsDp()),
                        )

                        ToggleRow(
                            label = "Dark Mode",
                            checked = settings.darkMode,
                            onToggle = {
                                val next = !settings.darkMode
                                if (next) LightThemeController.setDarkTheme() else LightThemeController.setLightTheme()
                                scope.launch { repository.setDarkMode(next) }
                            },
                        )
                        ToggleRow(
                            label = "Verse Numbers",
                            caption = "show verse numbers in the reader",
                            checked = settings.verseNumbers,
                            onToggle = { scope.launch { repository.setVerseNumbers(!settings.verseNumbers) } },
                            modifier = Modifier.padding(top = 1.4f.gridUnitsAsDp()),
                        )

                        LightTextField(
                            label = "TEXT SIZE",
                            value = settings.fontSize.label,
                            placeholder = "",
                            onClick = {
                                navigateTo(
                                    screenFactory = { activity ->
                                        OptionPickerScreen(
                                            activity,
                                            title = "Text Size",
                                            options = FontSize.entries,
                                            label = { it.label },
                                            currentSelection = { repository.settings.value.fontSize },
                                            onSelect = { repository.setFontSize(it) },
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 1.6f.gridUnitsAsDp()),
                        )

                        LightTextField(
                            label = "LINE SPACING",
                            value = settings.lineSpacing.label,
                            placeholder = "",
                            onClick = {
                                navigateTo(
                                    screenFactory = { activity ->
                                        OptionPickerScreen(
                                            activity,
                                            title = "Line Spacing",
                                            options = LineSpacing.entries,
                                            label = { it.label },
                                            currentSelection = { repository.settings.value.lineSpacing },
                                            onSelect = { repository.setLineSpacing(it) },
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 1.6f.gridUnitsAsDp()),
                        )

                        LightText(
                            text = "Copyrights",
                            variant = LightTextVariant.Heading,
                            modifier = Modifier
                                .padding(top = 1.6f.gridUnitsAsDp(), bottom = 1.2f.gridUnitsAsDp())
                                .lightClickable(onClick = { navigateTo(screenFactory = { activity -> CopyrightScreen(activity) }) }),
                        )
                    }

                    BibleBottomBar(
                        active = BibleTab.Settings,
                        onRead = { navigateToReadTab() },
                        onSearch = { navigateTo(screenFactory = { activity -> SearchScreen(activity) }) },
                        onHighlights = { navigateTo(screenFactory = { activity -> HighlightsScreen(activity) }) },
                        onSettings = {},
                    )
                }
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

// Fixed-width mask (a constant run of dots plus the last few real characters) rather than
// one dot per character — an ESV key is ~40 chars, and mirroring its length would overflow
// the settings row regardless of how many dots wide the box is.
private fun maskedApiKey(key: String): String = when {
    key.isEmpty() -> ""
    key.length <= MASKED_PREFIX_LENGTH -> "•".repeat(key.length)
    else -> "•".repeat(MASKED_PREFIX_LENGTH) + key.takeLast(MASKED_SUFFIX_LENGTH)
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onToggle),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // LightIcons.TOGGLE_ON/OFF read backwards from their names: TOGGLE_OFF draws the
        // knob on the right (the "on" position for a left-to-right switch), TOGGLE_ON draws
        // it on the left. Swapped here so the glyph matches `checked` visually.
        LightIcon(
            icon = if (checked) LightIcons.TOGGLE_OFF else LightIcons.TOGGLE_ON,
            size = 1.3f,
            contentDescription = if (checked) "$label on" else "$label off",
        )
        Spacer(modifier = Modifier.width(0.75f.gridUnitsAsDp()))
        Column {
            LightText(text = label, variant = LightTextVariant.Heading)
            if (caption != null) {
                LightText(
                    text = caption,
                    variant = LightTextVariant.Detail,
                    lighten = true,
                    modifier = Modifier.padding(top = 0.1f.gridUnitsAsDp()),
                )
            }
        }
    }
}

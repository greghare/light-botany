package com.thelightphone.bible

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import kotlinx.coroutines.launch

class SearchScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.bibleRepository()

    // Kept on the screen instance (not `remember`ed in Content) because the embedded LP3
    // keyboard's ViewModel is cached in the Activity's ViewModelStore keyed off this screen
    // instance and survives across back-navigation. If a new TextFieldState were created on
    // every composition, the cached ViewModel would stay bound to the old (orphaned) one,
    // leaving the search box unable to accept input after navigating back into this screen.
    private val textFieldState = TextFieldState("")

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val keyboardOptionsFlow = rememberKeyboardOptions()
        val scope = rememberCoroutineScope()

        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = "Search",
                state = textFieldState,
                keyboardOptionsFlow = keyboardOptionsFlow,
                onSubmit = { query ->
                    val trimmed = query.toString().trim()
                    if (trimmed.isEmpty()) return@LightTextInputEditor
                    scope.launch {
                        val results = repository.search(trimmed)
                        navigateTo(screenFactory = { activity -> SearchResultsScreen(activity, trimmed, results) })
                    }
                },
                onBack = { goBack() },
                submitIcon = LightIcons.SEARCH,
                singleLine = true,
                editorKey = this@SearchScreen,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

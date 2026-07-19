package com.thelightphone.botany

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.launch

class SettingsScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.botanyRepository()

    @Composable
    override fun Content() {
        val settings by repository.settings.collectAsState()
        val scope = rememberCoroutineScope()
        var editingLocation by remember { mutableStateOf(false) }
        var locationEditSession by remember { mutableStateOf(0) }

        LightTheme(colors = LightThemeColors.Dark) {
            if (editingLocation) {
                val locationState = remember(locationEditSession) { TextFieldState(settings.location) }
                val keyboardOptionsFlow = rememberKeyboardOptions()
                LightTextInputEditor(
                    title = "Location",
                    state = locationState,
                    keyboardOptionsFlow = keyboardOptionsFlow,
                    onSubmit = { value ->
                        scope.launch { repository.setLocation(value.toString().trim()) }
                        editingLocation = false
                    },
                    onBack = { editingLocation = false },
                    submitLabel = "SAVE",
                    singleLine = true,
                    editorKey = locationEditSession,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(center = LightTopBarCenter.Text("Settings"))

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1.5f.gridUnitsAsDp()),
                    ) {
                        LightTextField(
                            label = "LOCATION",
                            value = settings.location,
                            placeholder = "Not set",
                            onClick = {
                                editingLocation = true
                                locationEditSession++
                            },
                            modifier = Modifier.padding(top = 0.6f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = "Used to tell you if a plant is native to your area.",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.4f.gridUnitsAsDp()),
                        )

                        SelectSettingRow(
                            label = "Default Camera",
                            value = if (settings.defaultCameraFront) "Front" else "Back",
                            onClick = {
                                scope.launch { repository.setDefaultCameraFront(!settings.defaultCameraFront) }
                            },
                            modifier = Modifier.padding(top = 1.2f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = "Used to identify plants",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.1f.gridUnitsAsDp()),
                        )

                        LightText(
                            text = "Plant identification by Pl@ntNet",
                            variant = LightTextVariant.Heading,
                            modifier = Modifier.padding(top = 1.6f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = "Photos are sent to the Pl@ntNet API to identify species. Native-range and " +
                                "toxicity details are not provided by this app - always verify with a local expert " +
                                "before assuming a plant is safe.",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.3f.gridUnitsAsDp(), bottom = 1.5f.gridUnitsAsDp()),
                        )
                    }

                    BotanyBottomBar(
                        active = BotanyTab.Settings,
                        onCamera = { navigateTo(screenFactory = { activity -> CameraScreen(activity) }) },
                        onCollection = { navigateTo(screenFactory = { activity -> CollectionScreen(activity) }) },
                        onSettings = {},
                    )
                }
            }
        }
    }
}

// Modeled directly on examples/weather's SelectSettingRow: a single tappable label/value row
// where tapping cycles the value, rather than separate buttons per option.
@Composable
private fun SelectSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick),
    ) {
        LightText(text = label, variant = LightTextVariant.Detail)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}

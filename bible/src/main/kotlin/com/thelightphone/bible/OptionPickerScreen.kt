package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollBarPosition
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.launch

/** Generic "pick one of a short list" screen, reached by tapping a [LightTextField]-style
 *  settings row (e.g. Text Size, Line Spacing) — tap an option, it saves and pops back to
 *  the row that opened it, matching how the API key row opens its own editor. */
class OptionPickerScreen<T>(
    sealedActivity: SealedLightActivity,
    private val title: String,
    private val options: List<T>,
    private val label: (T) -> String,
    private val currentSelection: () -> T,
    private val onSelect: suspend (T) -> Unit,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        // Guards against a fast double-tap launching two goBack() calls, the second of
        // which would pop whatever screen is now on top rather than being a no-op.
        var isSelecting by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(title),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LightScrollView(
                        modifier = Modifier.fillMaxSize(),
                        scrollBarPosition = LightScrollBarPosition.Inside,
                    ) {
                        val selected = currentSelection()
                        options.forEach { option ->
                            OptionRow(
                                label = label(option),
                                isSelected = option == selected,
                                onClick = {
                                    if (!isSelecting) {
                                        isSelecting = true
                                        scope.launch {
                                            onSelect(option)
                                            goBack()
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 1.5f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
    ) {
        // IntrinsicSize.Max sizes this column to the label's natural (unwrapped) width, so
        // the underline below tracks the text instead of stretching across the row.
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            LightText(text = label, variant = LightTextVariant.Heading)
            Spacer(modifier = Modifier.height(0.4f.gridUnitsAsDp()))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (isSelected) LightThemeTokens.colors.content else Color.Transparent),
            )
        }
    }
}

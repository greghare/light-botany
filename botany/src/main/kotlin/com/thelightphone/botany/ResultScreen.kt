package com.thelightphone.botany

import android.graphics.Bitmap
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
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.launch

class ResultScreen(
    sealedActivity: SealedLightActivity,
    private val photo: Bitmap,
    private val identification: PlantIdentification,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.botanyRepository()

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var saved by remember { mutableStateOf(false) }

        fun onSaveClick() {
            if (saved) return
            scope.launch {
                repository.savePlant(photo, identification)
                saved = true
            }
        }

        LightTheme(colors = LightThemeColors.Dark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                PlantPhotoHeader(
                    photo = photo,
                    onBack = { goBack() },
                    trailingAction = {
                        RoundIconButton(
                            icon = if (saved) LightIcons.STAR else LightIcons.STAR_OUTLINE,
                            contentDescription = if (saved) "Saved" else "Save to history",
                            background = if (saved) LightThemeTokens.colors.content else Color.Black.copy(alpha = 0.5f),
                            onClick = ::onSaveClick,
                        )
                    },
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LightScrollView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 1.4f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LightText(text = identification.commonName, variant = LightTextVariant.Subtitle)
                            LightText(
                                text = "${identification.confidencePercent}% match",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                            )
                        }
                        LightText(
                            text = identification.latinName,
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.1f.gridUnitsAsDp()),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.2f.gridUnitsAsDp())
                                .height(1.dp)
                                .background(LightThemeTokens.colors.content.copy(alpha = 0.14f)),
                        )

                        SectionLabel(text = "Family")
                        LightText(
                            text = identification.family,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier.padding(top = 0.2f.gridUnitsAsDp()),
                        )

                        SectionLabel(text = "About", modifier = Modifier.padding(top = 1f.gridUnitsAsDp()))
                        LightText(
                            text = identification.description,
                            variant = LightTextVariant.Copy,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.2f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
    }
}

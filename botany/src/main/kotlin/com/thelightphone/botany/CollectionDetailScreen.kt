package com.thelightphone.botany

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionDetailScreen(
    sealedActivity: SealedLightActivity,
    private val entry: PlantHistoryEntry,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.botanyRepository()

    @Composable
    override fun Content() {
        val photo by produceState<Bitmap?>(initialValue = null, entry.photoFileName) {
            value = withContext(Dispatchers.IO) { repository.loadPhoto(entry.photoFileName) }
        }

        LightTheme(colors = LightThemeColors.Dark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                PlantPhotoHeader(photo = photo, onBack = { goBack() })

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LightScrollView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 1.4f.gridUnitsAsDp(), vertical = 1f.gridUnitsAsDp()),
                    ) {
                        LightText(text = entry.commonName, variant = LightTextVariant.Subtitle)
                        LightText(
                            text = entry.latinName,
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.1f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = "Seen ${entry.dateLabel}",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                        )

                        PlantBadgeRow(
                            isNative = entry.isNative,
                            isPoisonous = entry.isPoisonous,
                            modifier = Modifier.padding(top = 0.8f.gridUnitsAsDp()),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.2f.gridUnitsAsDp())
                                .height(1.dp)
                                .background(LightThemeTokens.colors.content.copy(alpha = 0.14f)),
                        )

                        SectionLabel(text = "About")
                        LightText(
                            text = entry.description,
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

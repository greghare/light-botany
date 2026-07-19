package com.thelightphone.botany

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Grid tiles are small; decoding at full saved-photo resolution wastes both time and memory
// on every scroll. This is comfortably bigger than any tile will render at on an LP3 screen.
private const val COLLECTION_THUMBNAIL_PX = 320

class CollectionScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.botanyRepository()

    @Composable
    override fun Content() {
        val history by repository.history.collectAsState()

        LightTheme(colors = LightThemeColors.Dark) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(center = LightTopBarCenter.Text("Collection"))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (history.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2f.gridUnitsAsDp()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            LightText(text = "No plants yet", variant = LightTextVariant.Heading, align = TextAlign.Center)
                            LightText(
                                text = "Identify a plant and save it to build your collection",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                align = TextAlign.Center,
                                modifier = Modifier.padding(top = 0.4f.gridUnitsAsDp()),
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(history, key = { it.id }) { entry ->
                                CollectionTile(
                                    entry = entry,
                                    loadPhoto = { repository.loadPhoto(entry.photoFileName, COLLECTION_THUMBNAIL_PX) },
                                ) {
                                    navigateTo(screenFactory = { activity -> CollectionDetailScreen(activity, entry) })
                                }
                            }
                        }
                    }
                }

                BotanyBottomBar(
                    active = BotanyTab.Collection,
                    onCamera = { navigateTo(screenFactory = { activity -> CameraScreen(activity) }) },
                    onCollection = {},
                    onSettings = { navigateTo(screenFactory = { activity -> SettingsScreen(activity) }) },
                )
            }
        }
    }
}

@Composable
private fun CollectionTile(
    entry: PlantHistoryEntry,
    loadPhoto: suspend () -> Bitmap?,
    onClick: () -> Unit,
) {
    val photo by produceState<Bitmap?>(initialValue = null, entry.photoFileName) {
        value = withContext(Dispatchers.IO) { loadPhoto() }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .lightClickable(onClick = onClick),
    ) {
        if (photo != null) {
            Image(
                bitmap = photo!!.asImageBitmap(),
                contentDescription = entry.commonName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1C)))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            LightText(text = entry.commonName, variant = LightTextVariant.Detail)
            LightText(text = entry.dateLabel, variant = LightTextVariant.Superfine, lighten = true)
        }
    }
}

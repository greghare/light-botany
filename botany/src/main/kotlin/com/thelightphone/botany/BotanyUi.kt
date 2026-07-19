package com.thelightphone.botany

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightColors
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightSurfaceScheme
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

private val PHOTO_HEADER_HEIGHT = 220.dp
private val ROUND_BUTTON_SIZE = 34.dp

enum class BotanyTab { Camera, Collection, Settings }

/** Custom icon (not part of [LightIcons]) for the Collection tab/screen - has both a black and
 *  white drawable variant, same convention as the SDK's own icons, and is tinted/selected the
 *  same way via [LightThemeTokens]. */
@Composable
fun CollectionIcon(sizeUnits: Float, contentDescription: String?, modifier: Modifier = Modifier) {
    val drawableId = when (LightThemeTokens.surfaceScheme) {
        LightSurfaceScheme.Dark -> R.drawable.ic_collection_white
        LightSurfaceScheme.Light -> R.drawable.ic_collection_black
    }
    Icon(
        painter = painterResource(drawableId),
        contentDescription = contentDescription,
        tint = LightThemeTokens.colors.content,
        modifier = modifier.size(sizeUnits.gridUnitsAsDp()),
    )
}

@Composable
fun BotanyBottomBar(
    active: BotanyTab,
    onCamera: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightThemeTokens.colors.background)
            .padding(top = 0.9f.gridUnitsAsDp(), bottom = 0.8f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotanyNavIcon("Camera", active == BotanyTab.Camera, onCamera) {
            LightIcon(icon = LightIcons.CAMERA, size = 2f, contentDescription = "Camera")
        }
        BotanyNavIcon("Collection", active == BotanyTab.Collection, onCollection) {
            CollectionIcon(sizeUnits = 2f, contentDescription = "Collection")
        }
        BotanyNavIcon("Settings", active == BotanyTab.Settings, onSettings) {
            LightIcon(icon = LightIcons.SETTINGS, size = 2f, contentDescription = "Settings")
        }
    }
}

@Composable
private fun BotanyNavIcon(description: String, active: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val colors = LightThemeTokens.colors
    val scheme = LightThemeTokens.surfaceScheme
    val typography = LightThemeTokens.typography
    val tintedColors = if (active) colors else mutedColors(colors, scheme)
    LightTheme(colors = tintedColors, typography = typography, surfaceScheme = scheme) {
        Box(modifier = Modifier.lightClickable(onClickLabel = description, onClick = onClick)) {
            icon()
        }
    }
}

private fun mutedColors(colors: LightColors, scheme: LightSurfaceScheme): LightColors {
    val alpha = if (scheme == LightSurfaceScheme.Dark) 0.4f else 0.38f
    return colors.copy(content = colors.content.copy(alpha = alpha))
}

/** A photo header used by both the fresh-result and history-detail screens: full-bleed photo,
 *  a round back/close button top-left, and an optional round action button top-right. */
@Composable
fun PlantPhotoHeader(
    photo: Bitmap?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth().height(PHOTO_HEADER_HEIGHT)) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1C)))
        }

        RoundIconButton(
            icon = LightIcons.BACK,
            contentDescription = "Back",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            onClick = onBack,
        )

        if (trailingAction != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                trailingAction()
            }
        }
    }
}

@Composable
fun RoundIconButton(
    icon: LightIconConfiguration,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color.Black.copy(alpha = 0.5f),
) {
    Box(
        modifier = modifier
            .size(ROUND_BUTTON_SIZE)
            .clip(CircleShape)
            .background(background)
            .lightClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LightIcon(icon = icon, size = 1.1f, contentDescription = contentDescription)
    }
}

@Composable
fun PlantBadgeRow(isNative: Boolean, isPoisonous: Boolean, modifier: Modifier = Modifier) {
    if (!isNative && !isPoisonous) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isNative) {
            LightText(
                text = "Native to your area",
                variant = LightTextVariant.Detail,
                modifier = Modifier
                    .background(Color.Transparent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        if (isPoisonous) {
            Box(
                modifier = Modifier
                    .background(LightThemeTokens.colors.content, RoundedCornerShape(20.dp)),
            ) {
                LightText(
                    text = "⚠ Poisonous",
                    variant = LightTextVariant.Detail,
                    color = LightThemeTokens.colors.background,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    LightText(
        text = text.uppercase(),
        variant = LightTextVariant.Fine,
        lighten = true,
        modifier = modifier,
    )
}

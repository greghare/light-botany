package com.thelightphone.botany

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightCameraCapture
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightCameraLensFacing
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeColors
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.sdk.ui.rememberLightCameraCaptureState
import kotlinx.coroutines.launch

private val BOTTOM_BAR_SIDE_INSET = 2.2f

private val CAPTURE_BUTTON_SIZE = 68.dp

@InitialScreen
class CameraScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = lightContext.botanyRepository()

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val settings by repository.settings.collectAsState()
        val cameraState = rememberLightCameraCaptureState()
        var lensOverride by remember { mutableStateOf<LightCameraLensFacing?>(null) }
        var isBusy by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val lensFacing = lensOverride
            ?: if (settings.defaultCameraFront) LightCameraLensFacing.Front else LightCameraLensFacing.Back

        fun onCapture() {
            if (isBusy) return
            scope.launch {
                isBusy = true
                errorMessage = null
                val photo: Bitmap? = cameraState.capture().getOrNull()
                if (photo == null) {
                    errorMessage = "Couldn't capture a photo. Try again."
                    isBusy = false
                    return@launch
                }
                repository.identify(photo)
                    .onSuccess { result ->
                        isBusy = false
                        navigateTo(screenFactory = { activity -> ResultScreen(activity, photo, result) })
                    }
                    .onFailure { e ->
                        isBusy = false
                        errorMessage = e.message ?: "Couldn't identify this plant."
                    }
            }
        }

        LightTheme(colors = LightThemeColors.Dark) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                LightCameraCapture(
                    state = cameraState,
                    lensFacing = lensFacing,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 1.6f.gridUnitsAsDp())
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    LightText(text = "Frame the whole plant", variant = LightTextVariant.Detail)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))),
                        )
                        .padding(top = 2f.gridUnitsAsDp(), bottom = 1.6f.gridUnitsAsDp()),
                ) {
                    // A Box with each child independently aligned (rather than a Row with
                    // Arrangement.SpaceAround) so the capture button lands exactly at center
                    // regardless of how wide the "Collection"/"Flip" labels are.
                    CameraBarAction(
                        label = "Collection",
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = BOTTOM_BAR_SIDE_INSET.gridUnitsAsDp()),
                        onClick = { navigateTo(screenFactory = { activity -> CollectionScreen(activity) }) },
                    ) {
                        CollectionIcon(sizeUnits = 1.4f, contentDescription = "Collection")
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(CAPTURE_BUTTON_SIZE)
                            .clip(CircleShape)
                            .background(Color.White)
                            .lightClickable(enabled = !isBusy, onClick = ::onCapture),
                    )

                    CameraBarAction(
                        label = "Flip",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = BOTTOM_BAR_SIDE_INSET.gridUnitsAsDp()),
                        onClick = {
                            lensOverride = if (lensFacing == LightCameraLensFacing.Back) {
                                LightCameraLensFacing.Front
                            } else {
                                LightCameraLensFacing.Back
                            }
                        },
                    ) {
                        LightIcon(icon = LightIcons.ROTATE, size = 1.4f, contentDescription = "Flip")
                    }
                }

                if (isBusy) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            LightText(
                                text = "Identifying plant…",
                                variant = LightTextVariant.Heading,
                                modifier = Modifier.padding(top = 1f.gridUnitsAsDp()),
                            )
                            LightText(
                                text = "Checking against Pl@ntNet",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                                modifier = Modifier.padding(top = 0.3f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                errorMessage?.let { message ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 11f.gridUnitsAsDp(), start = 1.5f.gridUnitsAsDp(), end = 1.5f.gridUnitsAsDp())
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = message,
                            variant = LightTextVariant.Detail,
                            align = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .lightClickable(onClick = { errorMessage = null }),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraBarAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.lightClickable(onClick = onClick),
    ) {
        icon()
        LightText(text = label, variant = LightTextVariant.Fine, modifier = Modifier.padding(top = 0.2f.gridUnitsAsDp()))
    }
}

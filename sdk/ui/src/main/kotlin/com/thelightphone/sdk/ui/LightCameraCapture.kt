package com.thelightphone.sdk.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine

enum class LightCameraLensFacing { Back, Front }

enum class LightCameraUiState { Loading, PermissionError, PermissionDenied, Active }

/**
 * Handle for triggering a still capture from a live [LightCameraCapture] preview. Obtain one via
 * [rememberLightCameraCaptureState] and pass it in; once the camera is bound, [capture] grabs
 * whatever the preview is currently showing.
 */
class LightCameraCaptureState {
    internal var controller: LifecycleCameraController? = null
    internal var mainExecutor: Executor? = null

    suspend fun capture(): Result<Bitmap> {
        val controller = controller ?: return Result.failure(IllegalStateException("Camera not ready"))
        val executor = mainExecutor ?: return Result.failure(IllegalStateException("Camera not ready"))
        return suspendCancellableCoroutine { cont ->
            controller.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val result = runCatching { image.toBitmapAndClose() }
                        if (cont.isActive) cont.resumeWith(Result.success(result))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (cont.isActive) cont.resumeWith(Result.success(Result.failure(exception)))
                    }
                },
            )
        }
    }
}

@Composable
fun rememberLightCameraCaptureState(): LightCameraCaptureState = remember { LightCameraCaptureState() }

/**
 * Live camera preview with still-capture support. Renders only the raw feed (or a
 * loading/permission message in its place) - callers own everything else (viewfinder chrome,
 * capture button, top/bottom bars) so it can be composed to match any screen design.
 *
 * Host apps must declare [android.Manifest.permission.CAMERA].
 */
@Composable
fun LightCameraCapture(
    state: LightCameraCaptureState,
    modifier: Modifier = Modifier,
    lensFacing: LightCameraLensFacing = LightCameraLensFacing.Back,
    checkCameraPermission: suspend () -> Result<Boolean>,
    launchCameraPermissionRequest: suspend () -> Unit,
    loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
    permissionDeniedContent: @Composable (LightCameraUiState) -> Unit = { uiState ->
        val message = if (uiState == LightCameraUiState.PermissionDenied) {
            "Camera permission is required."
        } else {
            "Error: unable to request camera permission!"
        }
        LightText(
            text = message,
            variant = LightTextVariant.Copy,
            align = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2f.gridUnitsAsDp()),
        )
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var launchedPermissionRequest by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf(LightCameraUiState.Loading) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val permissionCheck = checkCameraPermission()
            if (permissionCheck.isFailure) {
                uiState = LightCameraUiState.PermissionError
            } else if (permissionCheck.getOrNull() == false) {
                if (!launchedPermissionRequest) {
                    launchCameraPermissionRequest()
                    launchedPermissionRequest = true
                } else {
                    uiState = LightCameraUiState.PermissionDenied
                }
            } else {
                uiState = LightCameraUiState.Active
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            LightCameraUiState.Active -> CameraPreview(
                state = state,
                lensFacing = lensFacing,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize(),
            )
            LightCameraUiState.Loading -> loadingContent()
            LightCameraUiState.PermissionError, LightCameraUiState.PermissionDenied -> permissionDeniedContent(uiState)
        }
    }
}

@Composable
private fun CameraPreview(
    state: LightCameraCaptureState,
    lensFacing: LightCameraLensFacing,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(cameraController, mainExecutor) {
        state.controller = cameraController
        state.mainExecutor = mainExecutor
        onDispose {
            state.controller = null
            state.mainExecutor = null
        }
    }

    LaunchedEffect(cameraController, lensFacing) {
        cameraController.cameraSelector = when (lensFacing) {
            LightCameraLensFacing.Back -> CameraSelector.DEFAULT_BACK_CAMERA
            LightCameraLensFacing.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            bindCamera(cameraController, lifecycleOwner)
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            cameraController.unbind()
                        }
                    },
                )
            }
        },
        update = { previewView ->
            if (previewView.isAttachedToWindow) {
                previewView.post {
                    bindCamera(cameraController, lifecycleOwner)
                }
            }
        },
    )
}

private fun bindCamera(cameraController: LifecycleCameraController, lifecycleOwner: LifecycleOwner) {
    runCatching { cameraController.bindToLifecycle(lifecycleOwner) }
}

private fun ImageProxy.toBitmapAndClose(): Bitmap {
    try {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotation = imageInfo.rotationDegrees
        return if (rotation == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    } finally {
        close()
    }
}

package com.thelightphone.sdk

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.shared.LightServiceMethod
import com.thelightphone.sdk.shared.asKotlinResult
import com.thelightphone.sdk.ui.LightCameraCapture
import com.thelightphone.sdk.ui.LightCameraCaptureState
import com.thelightphone.sdk.ui.LightCameraLensFacing
import com.thelightphone.sdk.ui.LightQrCodeScanner

/**
 * Wrapper for the UI library's LightQrCodeScanner that include the client library's functions for
 * checking and requesting the camera permission for the SDK server
 */
@Composable
fun LightQrCodeScanner(
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Scan QR Code",
) {
    val permissionLauncher = rememberPermissionRequestLauncher(Manifest.permission.CAMERA)
    LightQrCodeScanner(
        title = title,
        onScanned = onScanned,
        onBack = onBack,
        modifier = modifier,
        checkCameraPermission = {
            checkPermission(Manifest.permission.CAMERA).asKotlinResult
                .map { it.permissionResult == LightServiceMethod.GetPermission.Result.Granted }
        },
        launchCameraPermissionRequest = {
            permissionLauncher?.launch()
        }
    )
}

/**
 * Wrapper for the UI library's LightCameraCapture that includes the client library's functions
 * for checking and requesting the camera permission for the SDK server
 */
@Composable
fun LightCameraCapture(
    state: LightCameraCaptureState,
    lensFacing: LightCameraLensFacing = LightCameraLensFacing.Back,
    modifier: Modifier = Modifier,
) {
    val permissionLauncher = rememberPermissionRequestLauncher(Manifest.permission.CAMERA)
    LightCameraCapture(
        state = state,
        lensFacing = lensFacing,
        modifier = modifier,
        checkCameraPermission = {
            checkPermission(Manifest.permission.CAMERA).asKotlinResult
                .map { it.permissionResult == LightServiceMethod.GetPermission.Result.Granted }
        },
        launchCameraPermissionRequest = {
            permissionLauncher?.launch()
        }
    )
}
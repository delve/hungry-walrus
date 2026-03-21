package com.delve.hungrywalrus.ui.screen.barcodescan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryUiEvent
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.screen.addentry.SearchState
import com.delve.hungrywalrus.ui.theme.Spacing
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToWeightEntry: () -> Unit,
    onNavigateToMissingValues: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(true) }
    var lookingUp by rememberSaveable { mutableStateOf(false) }
    var notFoundBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var isLookupError by rememberSaveable { mutableStateOf(false) }

    // Hoisted to stable composable scope so remember/DisposableEffect survive recompositions
    // inside the hasPermission branch without leaking resources.
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            barcodeScanner.close()
            analysisExecutor.shutdown()
        }
    }

    // Re-runs when torchOn changes OR when the camera finishes binding (cameraRef.value changes),
    // so torch state is applied even if the button was tapped before the camera was ready.
    LaunchedEffect(torchOn, cameraRef.value) {
        cameraRef.value?.cameraControl?.enableTorch(torchOn)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            hasPermission = true
        } else {
            val activity = context.findActivity()
            val shouldShow = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA,
            )
            if (shouldShow) {
                permissionDenied = true
            } else {
                permanentlyDenied = true
            }
        }
    }

    // Check permission on enter
    LaunchedEffect(Unit) {
        val result = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Collect barcode events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEntryUiEvent.BarcodeResult -> {
                    lookingUp = false
                    if (event.found) {
                        val food = viewModel.uiState.value.selectedFood
                        if (food != null && food.missingFields.isNotEmpty()) {
                            onNavigateToMissingValues()
                        } else {
                            onNavigateToWeightEntry()
                        }
                    } else {
                        isLookupError = event.isError
                        notFoundBarcode = event.barcode
                    }
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                permanentlyDenied -> {
                    PermissionDeniedContent(
                        message = "Camera permission was denied. To scan barcodes, enable camera access in your device settings.",
                        primaryButtonText = "Open Settings",
                        onPrimaryClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        onSecondaryClick = onNavigateBack,
                    )
                }

                permissionDenied && !hasPermission -> {
                    PermissionDeniedContent(
                        message = "Camera access is needed to scan barcodes.",
                        primaryButtonText = "Grant Permission",
                        onPrimaryClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onSecondaryClick = onNavigateBack,
                    )
                }

                hasPermission -> {
                    // Camera preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val mainExecutor = ContextCompat.getMainExecutor(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(Size(1280, 720))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                            if (!scanning) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees,
                                                )
                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        val barcode = barcodes.firstOrNull()
                                                        if (barcode != null && barcode.rawValue != null) {
                                                            scanning = false
                                                            lookingUp = true
                                                            viewModel.lookupBarcode(barcode.rawValue!!)
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis,
                                    )
                                    cameraRef.value = camera
                                } catch (e: Exception) {
                                    Log.e("BarcodeScanScreen", "Camera bind failed", e)
                                }
                            }, mainExecutor)

                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Scanning overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val overlayColor = Color.Black.copy(alpha = 0.5f)
                        val cutoutWidth = size.width * 0.7f
                        val cutoutHeight = cutoutWidth * 0.5f
                        val cutoutLeft = (size.width - cutoutWidth) / 2f
                        val cutoutTop = (size.height - cutoutHeight) / 2f

                        drawRect(color = overlayColor)
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(cutoutLeft, cutoutTop),
                            size = androidx.compose.ui.geometry.Size(cutoutWidth, cutoutHeight),
                            cornerRadius = CornerRadius(16f, 16f),
                            blendMode = BlendMode.Clear,
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(cutoutLeft, cutoutTop),
                            size = androidx.compose.ui.geometry.Size(cutoutWidth, cutoutHeight),
                            cornerRadius = CornerRadius(16f, 16f),
                            style = Stroke(width = 3f),
                        )
                    }

                    // Torch toggle
                    IconButton(
                        onClick = { torchOn = !torchOn },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.lg),
                    ) {
                        Icon(
                            imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchOn) "Turn off torch" else "Turn on torch",
                            tint = Color.White,
                        )
                    }

                    // Instruction text
                    Text(
                        text = "Align barcode within the frame",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 200.dp),
                    )

                    // Looking up overlay
                    if (lookingUp) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                Text(
                                    text = "Looking up product...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Not found overlay
                    notFoundBarcode?.let { barcode ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.lg),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = Spacing.xl),
                            ) {
                                Text(
                                    text = if (isLookupError)
                                        "Could not look up barcode $barcode. Check your connection and try again."
                                    else
                                        "Product not found for barcode $barcode.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Button(onClick = {
                                    notFoundBarcode = null
                                    isLookupError = false
                                    scanning = true
                                }) {
                                    Text("Try again")
                                }
                                TextButton(onClick = onNavigateToManual) {
                                    Text("Enter manually", color = Color.White)
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Waiting for permission check
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun PermissionDeniedContent(
    message: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(onClick = onPrimaryClick) {
            Text(primaryButtonText)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        TextButton(onClick = onSecondaryClick) {
            Text("Search by name instead")
        }
    }
}

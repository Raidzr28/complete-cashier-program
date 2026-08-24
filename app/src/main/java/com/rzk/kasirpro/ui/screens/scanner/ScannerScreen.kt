package com.rzk.kasirpro.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rzk.kasirpro.R
import com.rzk.kasirpro.ui.components.KasirTextField
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Live barcode scanning. Falls back to manual entry so a device with no camera — or a
 * damaged label — never blocks a sale.
 */
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // ML Kit calls back on a worker thread; navigation has to happen on the main thread.
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val deliverResult = remember<(String) -> Unit>(onBarcodeScanned) {
        { code -> mainHandler.post { onBarcodeScanned(code) } }
    }

    var manualCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                    )
                                    .build()
                                    .also {
                                        it.setAnalyzer(executor, BarcodeAnalyzer(deliverResult))
                                    }
                                runCatching {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                    // Reticle: tells the cashier where to aim without hiding the frame.
                    Box(
                        Modifier
                            .size(width = 260.dp, height = 150.dp)
                            .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                    )
                    Text(
                        stringResource(R.string.scanner_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            stringResource(R.string.camera_permission_required),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.grant_permission))
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KasirTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = stringResource(R.string.barcode),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { if (manualCode.isNotBlank()) onBarcodeScanned(manualCode.trim()) },
                    enabled = manualCode.isNotBlank(),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(stringResource(R.string.use_as_barcode)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Emits the first barcode it sees and then goes quiet — without the latch, a single label
 * in frame would fire the callback dozens of times a second.
 */
private class BarcodeAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    @Volatile
    private var consumed = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (consumed) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.rawValue }?.let { value ->
                    if (!consumed) {
                        consumed = true
                        onResult(value)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

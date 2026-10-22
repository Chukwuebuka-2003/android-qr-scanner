package com.chukwuebuka.qrscanner

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val ScannerColors = darkColorScheme(
    primary = Color(0xFF67F5B5),
    onPrimary = Color(0xFF003824),
    background = Color(0xFF07110E),
    surface = Color(0xFF10201A),
    onSurface = Color(0xFFF0FFF7),
)

@Composable
fun QRScannerApp(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
) {
    MaterialTheme(colorScheme = ScannerColors) {
        if (hasCameraPermission) {
            ScannerScreen()
        } else {
            PermissionScreen(onRequestCameraPermission)
        }
    }
}

@Composable
private fun PermissionScreen(onRequestCameraPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Camera access needed",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "QR Scanner uses your camera only while this screen is open.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestCameraPermission) {
            Text("Allow camera")
        }
    }
}

@Composable
private fun ScannerScreen() {
    val context = LocalContext.current
    var detectedValue by remember { mutableStateOf<String?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CameraPreview(
            onCameraReady = { boundCamera -> camera = boundCamera },
            onQrCodeDetected = { value ->
                if (detectedValue == null) {
                    detectedValue = value
                    vibrate(context)
                }
            },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.16f)),
        )
        ScannerFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .size(264.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(
                    color = Color(0xB307110E),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Scan a QR code",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Point your camera at a QR code",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 14.sp,
                    )
                }
                OutlinedButton(
                    enabled = camera?.cameraInfo?.hasFlashUnit() == true,
                    onClick = {
                        torchEnabled = !torchEnabled
                        camera?.cameraControl?.enableTorch(torchEnabled)
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(if (torchEnabled) "Flash on" else "Flash")
                }
            }
        }

        detectedValue?.let { value ->
            ScanResultCard(
                value = value,
                onDismiss = { detectedValue = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun CameraPreview(
    onCameraReady: (Camera) -> Unit,
    onQrCodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val currentOnQrCodeDetected by rememberUpdatedState(onQrCodeDetected)

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(previewView, lifecycleOwner) {
        val isDisposed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
        val analyzer = QrCodeAnalyzer(scanner) { value ->
            if (!isDisposed.get()) {
                currentOnQrCodeDetected(value)
            }
        }
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                if (isDisposed.get()) return@addListener
                val provider = runCatching { providerFuture.get() }.getOrNull()
                    ?: return@addListener
                if (isDisposed.get()) return@addListener
                cameraProvider = provider

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                }.onSuccess(onCameraReady)
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            isDisposed.set(true)
            imageAnalysis.clearAnalyzer()
            cameraProvider?.unbind(preview, imageAnalysis)
            scanner.close()
            executor.shutdown()
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 7.dp.toPx()
        val corner = 46.dp.toPx()
        val color = Color(0xFF67F5B5)
        val width = size.width
        val height = size.height

        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            style = Stroke(width = 1.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
        )
        listOf(
            Offset(0f, corner) to Offset(0f, 0f),
            Offset(0f, 0f) to Offset(corner, 0f),
            Offset(width - corner, 0f) to Offset(width, 0f),
            Offset(width, 0f) to Offset(width, corner),
            Offset(width, height - corner) to Offset(width, height),
            Offset(width, height) to Offset(width - corner, height),
            Offset(corner, height) to Offset(0f, height),
            Offset(0f, height) to Offset(0f, height - corner),
        ).forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth, StrokeCap.Round)
        }
    }
}

@Composable
private fun ScanResultCard(
    value: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isWebLink = remember(value) {
        runCatching {
            val uri = value.toUri()
            (uri.scheme.equals("https", ignoreCase = true) ||
                uri.scheme.equals("http", ignoreCase = true)) &&
                !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "QR code found",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("QR code", value))
                    },
                ) {
                    Text("Copy")
                }
                if (isWebLink) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, value.toUri()))
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No app can open this link",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Open link")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Scan again")
                }
            }
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(80)
    }
}

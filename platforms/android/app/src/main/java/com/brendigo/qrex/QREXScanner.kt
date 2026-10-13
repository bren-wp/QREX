@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.brendigo.qrex

import android.Manifest
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun ScanScreen(onRead: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var allowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = it
    }
    var message by remember { mutableStateOf<String?>(null) }
    val imageFailed = tr("image_failed")
    val qrNotFound = tr("qr_not_found")
    val cameraFailed = tr("camera_failed")
    val handled = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                // Gallery providers can be slow and can return decompression bombs.
                // Check dimensions before decoding, off the Compose UI thread.
                val image = withContext(Dispatchers.IO) {
                    runCatching {
                        val resolver = context.contentResolver
                        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                        if (size > 20_000_000L) return@runCatching null
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        val stream = resolver.openInputStream(uri) ?: return@runCatching null
                        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
                        val width = bounds.outWidth.toLong()
                        val height = bounds.outHeight.toLong()
                        if (width <= 0 || height <= 0 || width * height > 24_000_000L) {
                            return@runCatching null
                        }
                        InputImage.fromFilePath(context, uri)
                    }.getOrNull()
                }
                if (image == null) {
                    message = imageFailed
                } else {
                    val client = BarcodeScanning.getClient(options)
                    client.process(image).addOnSuccessListener { codes ->
                        if (!scope.coroutineContext.isActive) return@addOnSuccessListener
                        val raw = codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                        if (!raw.isNullOrEmpty()) {
                            if (handled.compareAndSet(false, true)) onRead(raw)
                        } else message = qrNotFound
                    }.addOnFailureListener {
                        if (scope.coroutineContext.isActive) message = imageFailed
                    }.addOnCompleteListener { client.close() }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!allowed) permission.launch(Manifest.permission.CAMERA)
    }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("QREX", color = Color(0xFF10C5FA),
            style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(tr("scan_qr"), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        if (allowed) {
            val preview = remember { PreviewView(context) }
            DisposableEffect(owner, allowed) {
                val cameraFuture = ProcessCameraProvider.getInstance(context)
                val disposed = AtomicBoolean(false)
                val scanner = BarcodeScanning.getClient(options)
                val analysisExecutor = Executors.newSingleThreadExecutor()
                var analyzer: ImageAnalysis? = null
                cameraFuture.addListener({
                    if (disposed.get()) return@addListener
                    runCatching {
                        val provider = cameraFuture.get()
                        if (disposed.get()) return@runCatching
                        val cameraPreview = Preview.Builder().build().also {
                            it.surfaceProvider = preview.surfaceProvider
                        }
                        val newAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        newAnalyzer.setAnalyzer(analysisExecutor) { frame ->
                            val media = frame.image
                            if (disposed.get() || media == null || handled.get()) {
                                frame.close()
                            } else {
                                runCatching {
                                    val input = InputImage.fromMediaImage(
                                        media, frame.imageInfo.rotationDegrees
                                    )
                                    scanner.process(input).addOnSuccessListener(
                                        ContextCompat.getMainExecutor(context)
                                    ) { codes ->
                                        if (!disposed.get()) {
                                            val value = codes.firstOrNull {
                                                it.format == Barcode.FORMAT_QR_CODE
                                            }?.rawValue
                                            if (!value.isNullOrEmpty() &&
                                                handled.compareAndSet(false, true)) onRead(value)
                                        }
                                    }.addOnFailureListener(
                                        ContextCompat.getMainExecutor(context)
                                    ) {
                                        if (!disposed.get()) message = cameraFailed
                                    }.addOnCompleteListener { frame.close() }
                                }.onFailure {
                                    frame.close()
                                    if (!disposed.get()) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            if (!disposed.get()) message = cameraFailed
                                        }
                                    }
                                }
                            }
                        }
                        provider.unbindAll()
                        analyzer = newAnalyzer
                        camera = provider.bindToLifecycle(
                            owner, CameraSelector.DEFAULT_BACK_CAMERA,
                            cameraPreview, newAnalyzer
                        )
                    }.onFailure { if (!disposed.get()) message = cameraFailed }
                }, ContextCompat.getMainExecutor(context))
                onDispose {
                    disposed.set(true)
                    runCatching { camera?.cameraControl?.enableTorch(false) }
                    camera = null
                    torchOn = false
                    analyzer?.clearAnalyzer()
                    if (cameraFuture.isDone) runCatching { cameraFuture.get().unbindAll() }
                    scanner.close()
                    analysisExecutor.shutdown()
                }
            }
            AndroidView(factory = { preview }, modifier = Modifier
                .fillMaxWidth().weight(1f)
                .background(Color(0xFF101B2C), shape = RoundedCornerShape(28.dp)))
        } else {
            Box(Modifier.fillMaxWidth().weight(1f)
                .background(Color(0xFF101B2C), shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("allow_camera"), modifier = Modifier.padding(20.dp))
                    OutlinedButton(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                        Text(tr("enable_camera"))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    val next = !torchOn
                    if (runCatching { camera?.cameraControl?.enableTorch(next) }.isSuccess) {
                        torchOn = next
                    }
                },
                enabled = allowed && camera?.cameraInfo?.hasFlashUnit() == true
            ) {
                Text(tr("flashlight"))
            }
            OutlinedButton(onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }) {
                Text(tr("from_gallery"))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("point_camera"), color = Color.LightGray)
    }

    if (message != null) AlertDialog(onDismissRequest = { message = null },
        title = { Text("QREX") },
        text = { Text(message ?: "") },
        confirmButton = { TextButton(onClick = { message = null }) { Text(tr("ok")) } })
}

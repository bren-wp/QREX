package com.brendigo.qrex

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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

/**
 * Use a power-of-two decoder sample to keep accepted camera-roll images below
 * about 6 MP in RAM. The original encoded image is still dimension-checked.
 */
internal fun gallerySampleSize(width: Int, height: Int): Int {
    require(width > 0 && height > 0)
    var sample = 1
    while ((width.toLong() / sample) * (height.toLong() / sample) > 6_000_000L) {
        sample *= 2
    }
    return sample
}

/** Apply all eight EXIF orientations, including mirrored JPEGs, after sampling. */
internal fun orientGalleryBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val transform = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> transform.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> transform.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            transform.setRotate(90f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> transform.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            transform.setRotate(-90f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> transform.setRotate(-90f)
        else -> return bitmap
    }
    val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, false)
    if (result !== bitmap) bitmap.recycle()
    return result
}

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
                        // fromFilePath decodes the entire original image (up to
                        // 24 million pixels). Decode a sampled bitmap instead to
                        // avoid 96 MB+ transient allocations on smaller devices.
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = gallerySampleSize(bounds.outWidth, bounds.outHeight)
                        }
                        val bitmap = resolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        } ?: return@runCatching null
                        val orientation = runCatching {
                            resolver.openInputStream(uri)?.use {
                                ExifInterface(it).getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                            } ?: ExifInterface.ORIENTATION_NORMAL
                        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                        InputImage.fromBitmap(orientGalleryBitmap(bitmap, orientation), 0)
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

    // Gallery QR scanning works without CAMERA permission. Ask only after an
    // explicit tap, and refresh permission after returning from system Settings.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allowed = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
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
                        newAnalyzer.setAnalyzer(analysisExecutor, object : ImageAnalysis.Analyzer {
                            @ExperimentalGetImage
                            override fun analyze(frame: ImageProxy) {
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
                        })
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
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ))
                        }.onFailure { message = cameraFailed }
                    }) {
                        Text(tr("settings"))
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
        Text(tr("point_camera"), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (message != null) AlertDialog(onDismissRequest = { message = null },
        title = { Text("QREX") },
        text = { Text(message ?: "") },
        confirmButton = { TextButton(onClick = { message = null }) { Text(tr("ok")) } })
}

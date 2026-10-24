package com.brendigo.qrex

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
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
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Draws the actual live scanner framing guide above the CameraX PreviewView.
 * Nothing is composited into the scanned image and no camera frames are stored.
 */
@Composable
private fun ScannerCornerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = size.minDimension * 0.56f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val right = left + side
        val bottom = top + side
        val segment = side * 0.23f
        val stroke = 4.dp.toPx()
        val tint = QrexDesign.accentBright
        val corners = listOf(
            Offset(left, top + segment) to Offset(left, top),
            Offset(left, top) to Offset(left + segment, top),
            Offset(right - segment, top) to Offset(right, top),
            Offset(right, top) to Offset(right, top + segment),
            Offset(left, bottom - segment) to Offset(left, bottom),
            Offset(left, bottom) to Offset(left + segment, bottom),
            Offset(right - segment, bottom) to Offset(right, bottom),
            Offset(right, bottom) to Offset(right, bottom - segment)
        )
        corners.forEach { (from, to) ->
            // Reference's luminous blue scan corners are a visual guide over
            // the live CameraX PreviewView, never pixels substituted into a
            // decoded frame. Translucent outer strokes imitate their glow.
            drawLine(tint.copy(alpha = .14f), from, to,
                strokeWidth = 13.dp.toPx(), cap = StrokeCap.Round)
            drawLine(tint.copy(alpha = .42f), from, to,
                strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = .94f), from, to,
                strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

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

/** Bound camera QR decoding to ~6 frames per second, not every 30/60 FPS frame. 
 * The monotonic uptime timestamp avoids wall-clock jumps on Android devices.
 * This only throttles live camera frames, never the explicit gallery action.
 */
internal fun cameraFrameDue(nowMillis: Long, lastMillis: Long): Boolean =
    lastMillis <= 0L || nowMillis - lastMillis >= 180L

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
fun ScanScreen(onMore: () -> Unit = {}, onRead: (String) -> Unit) {
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
    val lastCameraDecodeMs = remember { AtomicLong(0L) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var torchBusy by remember { mutableStateOf(false) }
    var selectedZoom by remember { mutableFloatStateOf(1f) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                // Bound gallery inputs before allocation. Camera permission is not
                // required by the system photo picker.
                val bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        val resolver = context.contentResolver
                        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                        if (size > 20_000_000L) return@runCatching null
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        val boundsStream = resolver.openInputStream(uri) ?: return@runCatching null
                        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
                        // inJustDecodeBounds returns a null Bitmap by design:
                        // validate populated dimensions rather than its return value.
                        val width = bounds.outWidth.toLong()
                        val height = bounds.outHeight.toLong()
                        if (width <= 0 || height <= 0 || width * height > 24_000_000L) {
                            return@runCatching null
                        }
                        val sampled = BitmapFactory.Options().apply {
                            inSampleSize = gallerySampleSize(bounds.outWidth, bounds.outHeight)
                        }
                        val decoded = resolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, sampled)
                        } ?: return@runCatching null
                        val orientation = runCatching {
                            resolver.openInputStream(uri)?.use {
                                ExifInterface(it).getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                                )
                            } ?: ExifInterface.ORIENTATION_NORMAL
                        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
                        try {
                            orientGalleryBitmap(decoded, orientation)
                        } catch (failure: Exception) {
                            decoded.recycle()
                            throw failure
                        }
                    }.getOrNull()
                }
                if (!scope.coroutineContext.isActive) {
                    bitmap?.recycle()
                    return@launch
                }
                if (bitmap == null) {
                    message = imageFailed
                } else {
                    // Heavy QR detection runs off main; recycle even on decode errors.
                    val result = withContext(Dispatchers.Default) {
                        try {
                            runCatching { QREXQrDecoder.decodeBitmap(bitmap) }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    if (!scope.coroutineContext.isActive) return@launch
                    val raw = result.getOrNull()
                    if (result.isFailure) message = imageFailed
                    else if (raw.isNullOrEmpty()) message = qrNotFound
                    else if (handled.compareAndSet(false, true)) onRead(raw)
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
    // Both the scanner overlay and bottom action use the SAME CameraX future.
    // Do not claim the torch is enabled until CameraX confirms success.
    val toggleTorch: () -> Unit = {
        val activeCamera = camera
        if (activeCamera == null) {
            message = cameraFailed
        } else {
            val next = !torchOn
            val request = runCatching {
                activeCamera.cameraControl.enableTorch(next)
            }.getOrNull()
            if (request == null) {
                message = cameraFailed
            } else {
                torchBusy = true
                request.addListener({
                    if (camera === activeCamera) {
                        if (runCatching { request.get() }.isSuccess) {
                            torchOn = next
                        } else {
                            message = cameraFailed
                        }
                        torchBusy = false
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }
    // LocalConfiguration is responsive without introducing SubcomposeLayout
    // disposal/lifecycle regressions on low-memory Android 8 devices.
    val window = LocalConfiguration.current
    val compactScan = window.screenWidthDp < 360 ||
        window.screenHeightDp < 670 ||
        LocalDensity.current.fontScale >= 1.35f
    Column(Modifier.fillMaxSize().padding(if (compactScan) 10.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        QrexBrandHeader(onMore = onMore)
        Spacer(Modifier.height(if (compactScan) 4.dp else 8.dp))
        if (allowed) {
            val preview = remember { PreviewView(context) }
            DisposableEffect(owner, allowed) {
                val cameraFuture = ProcessCameraProvider.getInstance(context)
                val disposed = AtomicBoolean(false)
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
                            override fun analyze(frame: ImageProxy) {
                                try {
                                    val now = SystemClock.elapsedRealtime()
                                    if (!disposed.get() && !handled.get() &&
                                        cameraFrameDue(now, lastCameraDecodeMs.get())) {
                                        lastCameraDecodeMs.set(now)
                                        val decoded = QREXQrDecoder.decodeFrame(frame)
                                        if (!decoded.isNullOrEmpty() &&
                                            handled.compareAndSet(false, true)) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                if (!disposed.get()) onRead(decoded)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        if (!disposed.get()) message = cameraFailed
                                    }
                                } finally {
                                    // CameraX requires closure of every frame; never
                                    // retain ImageProxy after this background callback.
                                    frame.close()
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
                    torchBusy = false
                    selectedZoom = 1f
                    analyzer?.clearAnalyzer()
                    if (cameraFuture.isDone) runCatching { cameraFuture.get().unbindAll() }
                    analysisExecutor.shutdown()
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(QrexDesign.surface)) {
                AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
                ScannerCornerOverlay(Modifier.fillMaxSize())
                // Actual optical/digital zoom controls: never offer levels
                // outside the camera's advertised range.
                val zoomLimits = camera?.cameraInfo?.zoomState?.value
                if (!compactScan) Row(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 58.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(.5f, 1f, 2f).forEach { ratio ->
                        val available = camera != null && zoomLimits != null &&
                            ratio >= zoomLimits.minZoomRatio &&
                            ratio <= zoomLimits.maxZoomRatio
                        FilterChip(
                            selected = selectedZoom == ratio,
                            onClick = {
                                val active = camera
                                if (active != null && available) {
                                    val request = runCatching {
                                        active.cameraControl.setZoomRatio(ratio)
                                    }.getOrNull()
                                    request?.addListener({
                                        if (camera === active &&
                                            runCatching { request.get() }.isSuccess) {
                                            selectedZoom = ratio
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                }
                            },
                            enabled = available,
                            label = { Text("${ratio}×") },
                            modifier = Modifier.heightIn(min = 44.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = QrexDesign.backgroundDeep.copy(alpha = .88f),
                                labelColor = Color.White,
                                selectedContainerColor = QrexDesign.accent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                // One torch control lives below the camera. Avoid a second
                // button on top of the live scanner viewport.
                Text(
                    tr("point_camera"),
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(18.dp)
                        .background(QrexDesign.backgroundDeep.copy(alpha = .76f),
                            RoundedCornerShape(9.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color.White
                )
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f)
                .background(QrexDesign.surface, shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
        Spacer(Modifier.height(if (compactScan) 7.dp else 12.dp))
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactScan) 6.dp else 12.dp)) {
            Button(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.weight(1f)
                    .heightIn(min = if (compactScan) 52.dp else 66.dp)
                    .testTag("qrex-scan-gallery"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1677FF),
                    contentColor = Color.White
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tr("from_gallery"), maxLines = 2,
                        textAlign = TextAlign.Center)
                }
            }
            OutlinedButton(
                onClick = toggleTorch,
                enabled = allowed && !torchBusy &&
                    camera?.cameraInfo?.hasFlashUnit() == true,
                modifier = Modifier.weight(1f)
                    .heightIn(min = if (compactScan) 52.dp else 66.dp)
                    .testTag("qrex-scan-torch"),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null, tint = QrexDesign.accentBright,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tr("flashlight"), maxLines = 2,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (message != null) AlertDialog(onDismissRequest = { message = null },
        title = { Text("QREX") },
        text = { Text(message ?: "") },
        confirmButton = { TextButton(onClick = { message = null }) { Text(tr("ok")) } })
}

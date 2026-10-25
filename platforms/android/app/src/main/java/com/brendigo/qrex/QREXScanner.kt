package com.brendigo.qrex

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
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
private fun ScannerCornerOverlay(
    modifier: Modifier = Modifier,
    reservedBottomDp: Int = 156
) {
    Canvas(modifier) {
        // Reserve visual room above camera actions without resizing/cropping
        // the real PreviewView. Bound the reserve on very short screens.
        val usableHeight = size.height - minOf(
            reservedBottomDp.dp.toPx(), size.height * 0.45f
        )
        val side = minOf(size.width, usableHeight) * 0.61f
        val left = (size.width - side) / 2f
        val top = (usableHeight - side) / 2f
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

/** The picker has priority over frames from the live CameraX analyzer. */
internal fun cameraFrameAllowed(
    nowMillis: Long, lastMillis: Long, galleryActive: Boolean, alreadyHandled: Boolean
): Boolean = !galleryActive && !alreadyHandled && cameraFrameDue(nowMillis, lastMillis)

/** Supported camera zoom levels only; avoid disabled, misleading preset chips. */
internal fun scannerZoomPresets(minRatio: Float, maxRatio: Float): List<Float> {
    if (!minRatio.isFinite() || !maxRatio.isFinite() ||
        minRatio <= 0f || maxRatio < minRatio) return emptyList()
    val presets = listOf(.5f, 1f, 2f)
        .filter { it >= minRatio && it <= maxRatio }
        .toMutableList()
    if (maxRatio > 1f && presets.none { it > 1f }) {
        presets.add(maxRatio)
    }
    return presets.distinct().sorted()
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


/**
 * Decode a user-selected image entirely on a worker dispatcher. Ownership of
 * the bitmap never crosses a suspend boundary: even if Compose cancels the
 * caller during a slow decode, this finally block releases native image memory.
 */
internal fun decodeGalleryQr(context: Context, uri: Uri): Result<String?> = runCatching {
    val resolver = context.contentResolver
    val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    if (size > 20_000_000L) return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // Bounds-only decoding intentionally returns null even for valid images.
    val boundsStream = resolver.openInputStream(uri) ?: return@runCatching null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    val width = bounds.outWidth.toLong()
    val height = bounds.outHeight.toLong()
    if (width <= 0 || height <= 0 || width * height > 24_000_000L) {
        return@runCatching null
    }
    val sampled = BitmapFactory.Options().apply {
        inSampleSize = gallerySampleSize(bounds.outWidth, bounds.outHeight)
    }
    var bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, sampled)
    } ?: return@runCatching null
    try {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        bitmap = orientGalleryBitmap(bitmap, orientation)
        QREXQrDecoder.decodeBitmap(bitmap)
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
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
    val galleryActive = remember { AtomicBoolean(false) }
    var galleryBusy by remember { mutableStateOf(false) }
    val lastCameraDecodeMs = remember { AtomicLong(0L) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var torchBusy by remember { mutableStateOf(false) }
    var selectedZoom by remember { mutableFloatStateOf(1f) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            // Cancellation, including Back, resumes normal camera scanning.
            galleryActive.set(false)
            galleryBusy = false
        } else {
            scope.launch {
                try {
                    // Image load, rotation, decoding and bitmap cleanup remain
                    // together on the worker even if this coroutine is cancelled.
                    val result = withContext(Dispatchers.IO) { decodeGalleryQr(context, uri) }
                    if (!scope.coroutineContext.isActive) return@launch
                    val raw = result.getOrNull()
                    if (result.isFailure) message = imageFailed
                    else if (raw.isNullOrEmpty()) message = qrNotFound
                    else if (handled.compareAndSet(false, true)) onRead(raw)
                } finally {
                    galleryActive.set(false)
                    galleryBusy = false
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
    val fontScale = LocalDensity.current.fontScale
    val compactScan = window.screenWidthDp < 360 ||
        window.screenHeightDp < 670 ||
        fontScale >= 1.35f
    // A single composable action row is reused in the preview and in the
    // permission-denied state. Gallery never depends on camera permission.
    // Neither action is a decorative graphic; each retains the original
    // one-shot photo picker / asynchronous CameraX torch callback.
    val scannerActions: @Composable (Boolean) -> Unit = { inCamera ->
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compactScan) 6.dp else 12.dp)) {
            Button(
                onClick = {
                    if (galleryActive.compareAndSet(false, true)) {
                        galleryBusy = true
                        runCatching {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            galleryActive.set(false)
                            galleryBusy = false
                            message = imageFailed
                        }
                    }
                },
                enabled = !galleryBusy,
                modifier = Modifier.weight(1f)
                    .heightIn(min = if (fontScale >= 1.5f) 88.dp
                        else if (compactScan) 64.dp else 80.dp)
                    .testTag("qrex-scan-gallery"),
                shape = RoundedCornerShape(15.dp),
                border = if (inCamera) BorderStroke(
                    1.dp, QrexDesign.accentBright.copy(alpha = .55f)
                ) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (inCamera)
                        QrexDesign.backgroundDeep.copy(alpha = .88f)
                    else QrexDesign.actionBlue,
                    contentColor = Color.White
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (galleryBusy) {
                        // Visible work feedback while an image is being decoded.
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null,
                            tint = if (inCamera) QrexDesign.accentBright else Color.White,
                             modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(tr("from_gallery"), maxLines = if (fontScale >= 1.5f) 3 else 2,
                        fontSize = if (compactScan) 12.sp else 14.sp,
                        lineHeight = if (compactScan) 16.sp else 18.sp,
                        textAlign = TextAlign.Center)
                }
            }
            OutlinedButton(
                onClick = toggleTorch,
                enabled = allowed && !torchBusy &&
                    camera?.cameraInfo?.hasFlashUnit() == true,
                modifier = Modifier.weight(1f)
                    .heightIn(min = if (fontScale >= 1.5f) 88.dp
                        else if (compactScan) 64.dp else 80.dp)
                    .testTag("qrex-scan-torch"),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(
                    1.dp, if (inCamera) Color.White.copy(alpha = .19f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (inCamera)
                        QrexDesign.backgroundDeep.copy(alpha = .88f)
                    else MaterialTheme.colorScheme.surface,
                    contentColor = if (inCamera) Color.White
                    else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = null, tint = QrexDesign.accentBright,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tr("flashlight"), maxLines = if (fontScale >= 1.5f) 3 else 2,
                        fontSize = if (compactScan) 12.sp else 14.sp,
                        lineHeight = if (compactScan) 16.sp else 18.sp,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
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
                                    if (!disposed.get() && cameraFrameAllowed(
                                            now, lastCameraDecodeMs.get(),
                                            galleryActive.get(), handled.get()
                                        )) {
                                        lastCameraDecodeMs.set(now)
                                        val decoded = QREXQrDecoder.decodeFrame(frame)
                                        if (!decoded.isNullOrEmpty()) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                // Claim the result only at delivery time. If the
                                                // gallery opens while this callback is queued,
                                                // it must not consume the one-shot scan token.
                                                if (!disposed.get() && !galleryActive.get() &&
                                                    handled.compareAndSet(false, true)) {
                                                    onRead(decoded)
                                                }
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
            val zoomLimits = camera?.cameraInfo?.zoomState?.value
            val zoomPresets = zoomLimits?.let {
                scannerZoomPresets(it.minZoomRatio, it.maxZoomRatio)
            }.orEmpty()
            val showZoom = !compactScan && zoomPresets.size > 1
            Box(Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(QrexDesign.surface)
                .testTag("qrex-camera-preview")) {
                AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
                ScannerCornerOverlay(
                    Modifier.fillMaxSize(),
                    reservedBottomDp = when {
                        showZoom -> 220
                        fontScale >= 1.5f -> 190
                        compactScan -> 145
                        else -> 165
                    }
                )
                // Compose measures hint, zoom and buttons together, avoiding
                // collisions caused by absolute bottom offsets on short devices.
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                        .testTag("qrex-scan-controls"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compactScan) 8.dp else 12.dp)
                ) {
                    if (showZoom) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            zoomPresets.forEach { ratio ->
                                FilterChip(
                                    selected = selectedZoom == ratio,
                                    onClick = {
                                        val active = camera
                                        if (active != null) {
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
                                    label = { Text("${ratio}×") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                        .testTag("qrex-zoom-${ratio}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = QrexDesign.backgroundDeep.copy(alpha = .88f),
                                        labelColor = Color.White,
                                        selectedContainerColor = QrexDesign.actionBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        tr("point_camera"),
                        modifier = Modifier.background(
                            QrexDesign.backgroundDeep.copy(alpha = .78f),
                            RoundedCornerShape(9.dp)
                        ).padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("qrex-scan-hint"),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = if (fontScale >= 1.5f) 3 else 2
                    )
                    scannerActions(true)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f)
                .background(QrexDesign.surface, shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("allow_camera"), modifier = Modifier.padding(20.dp))
                    OutlinedButton(
                        onClick = { permission.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("qrex-enable-camera")
                    ) {
                        Text(tr("enable_camera"))
                    }
                    TextButton(
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("qrex-open-app-settings"),
                        onClick = {
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
        if (!allowed) {
            // Keep the gallery usable when camera access is denied.
            Spacer(Modifier.height(if (compactScan) 7.dp else 12.dp))
            scannerActions(false)
        }
    }

    if (message != null) AlertDialog(onDismissRequest = { message = null },
        title = { Text("QREX") },
        text = { Text(message ?: "") },
        confirmButton = { TextButton(onClick = { message = null }) { Text(tr("ok")) } })
}

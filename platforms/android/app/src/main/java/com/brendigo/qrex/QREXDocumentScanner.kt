package com.brendigo.qrex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Size
import android.content.ClipData
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Private memory-only session retained across Activity recreation (including
 * orientation changes). No paper document is written to cache, preferences or logs.
 * Android process death clears this session rather than exposing plaintext files.
 */
internal class QREXDocumentSession : ViewModel() {
    val pages = mutableStateListOf<Bitmap>()
    var pending by mutableStateOf<Bitmap?>(null)
        private set
    private var previousPending by mutableStateOf<Bitmap?>(null)
    // Reject background results targeting a page that has since been undone,
    // replaced or discarded. All session mutations take place on the UI thread.
    private var pendingRevision: Long = 0L
    val revision: Long get() = pendingRevision
    val canUndo: Boolean get() = previousPending != null

    /** Importing, retaking or starting another page clears old edit history. */
    fun updatePending(bitmap: Bitmap?) {
        pendingRevision++
        pending = bitmap
        previousPending = null
    }

    /** Return false for a stale async result; caller then owns its bitmap. */
    fun transformPending(bitmap: Bitmap, expectedRevision: Long = revision): Boolean {
        if (expectedRevision != pendingRevision || pending == null) return false
        if (bitmap !== pending) {
            previousPending = pending
            pending = bitmap
            pendingRevision++
        }
        return true
    }

    fun undoPending(): Boolean {
        val previous = previousPending ?: return false
        pendingRevision++
        pending = previous
        previousPending = null
        return true
    }
    fun accept(): Boolean {
        val bitmap = pending ?: return false
        if (pages.size >= QREX_DOCUMENT_MAX_PAGES) return false
        pages.add(bitmap)
        pendingRevision++
        pending = null
        previousPending = null
        return true
    }
    fun replace(index: Int, bitmap: Bitmap) {
        if (index in pages.indices) pages[index] = bitmap
    }
    fun move(index: Int, direction: Int) {
        val target = index + direction
        if (index !in pages.indices || target !in pages.indices) return
        val image = pages.removeAt(index)
        pages.add(target, image)
    }
    fun clearSession() {
        pendingRevision++
        pages.clear()
        pending = null
        previousPending = null
    }
    override fun onCleared() {
        clearSession()
        super.onCleared()
    }
}

internal fun qrexDocumentPreviewRect(
    viewWidth: Float, viewHeight: Float, imageWidth: Int, imageHeight: Int
): RectF {
    val r = qrexDocumentFit(imageWidth, imageHeight,
        viewWidth.toInt(), viewHeight.toInt(), 0)
    return RectF(r[0], r[1], r[2], r[3])
}

@Composable
internal fun QREXDocumentCamera(
    onReady: (ImageCapture?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember(context) { PreviewView(context) }
    DisposableEffect(context, owner) {
        val future = ProcessCameraProvider.getInstance(context)
        val disposed = AtomicBoolean(false)
        var capture: ImageCapture? = null
        future.addListener({
            if (disposed.get()) return@addListener
            try {
                val provider = future.get()
                if (disposed.get()) return@addListener
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = view.surfaceProvider
                }
                capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(
                            Size(1600, 2400),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                        )).build())
                    .build()
                // QR screen is disposed before this screen starts. Bind the
                // document use cases alone, never analyze document pixels as QR.
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, capture)
                onReady(capture)
            } catch (_: Exception) { onReady(null) }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed.set(true)
            onReady(null)
            if (future.isDone) runCatching {
                capture?.let { future.get().unbind(it) }
            }
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

/** Perspective editor uses normalized image coordinates, not screen pixels. */
@Composable
internal fun QREXDocumentCorners(
    source: Bitmap,
    corners: List<Float>,
    rectangle: Boolean,
    onChange: (List<Float>) -> Unit
) {
    var activeCorner by remember(source, rectangle) { mutableIntStateOf(-1) }
    val currentCorners by rememberUpdatedState(corners)
    val updateCorners by rememberUpdatedState(onChange)
    val viewportHeight = min(400, max(200, LocalConfiguration.current.screenHeightDp / 2))
    Canvas(Modifier.fillMaxWidth().height(viewportHeight.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black)
        .testTag("qrex-document-crop-canvas")
        .pointerInput(source, rectangle) {
            detectDragGestures(
                onDragStart = { touch ->
                    val r = qrexDocumentPreviewRect(
                        size.width.toFloat(), size.height.toFloat(), source.width, source.height
                    )
                    val candidate = (0..3).minByOrNull { i ->
                        hypot(touch.x - (r.left + currentCorners[i * 2] * r.width()),
                            touch.y - (r.top + currentCorners[i * 2 + 1] * r.height()))
                    } ?: -1
                    activeCorner = if (candidate >= 0 && hypot(
                        touch.x - (r.left + currentCorners[candidate * 2] * r.width()),
                        touch.y - (r.top + currentCorners[candidate * 2 + 1] * r.height())
                    ) <= 48.dp.toPx()) candidate else -1
                },
                onDragEnd = { activeCorner = -1 },
                onDragCancel = { activeCorner = -1 },
                onDrag = { change, _ ->
                    val r = qrexDocumentPreviewRect(
                        size.width.toFloat(), size.height.toFloat(), source.width, source.height
                    )
                    if (activeCorner in 0..3 && r.width() > 0 && r.height() > 0) {
                        val updated = qrexDocumentMoveCorner(
                            currentCorners, activeCorner,
                            (change.position.x - r.left) / r.width(),
                            (change.position.y - r.top) / r.height(), rectangle
                        )
                        if (updated !== currentCorners) updateCorners(updated)
                        change.consume()
                    }
                }
            )
        }) {
        val r = qrexDocumentPreviewRect(
            size.width, size.height, source.width, source.height
        )
        drawIntoCanvas {
            it.nativeCanvas.drawBitmap(source, null, r,
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        val positions = (0..3).map {
            Offset(r.left + corners[it * 2] * r.width(),
                r.top + corners[it * 2 + 1] * r.height())
        }
        // Darken only the pixels outside the selected quadrilateral.
        // The selection remains bright, with a thirds grid for alignment.
        val mask = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            moveTo(positions[0].x, positions[0].y)
            for (i in 1..3) lineTo(positions[i].x, positions[i].y)
            close()
        }
        drawPath(mask, Color.Black.copy(alpha = .58f))
        fun blend(a: Offset, b: Offset, fraction: Float): Offset =
            Offset(a.x + (b.x - a.x) * fraction, a.y + (b.y - a.y) * fraction)
        for (fraction in listOf(1f / 3f, 2f / 3f)) {
            drawLine(Color.White.copy(alpha = .42f),
                blend(positions[0], positions[1], fraction),
                blend(positions[3], positions[2], fraction), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = .42f),
                blend(positions[0], positions[3], fraction),
                blend(positions[1], positions[2], fraction), 1.dp.toPx())
        }
        for (i in 0..3) {
            drawLine(QrexDesign.cyan, positions[i], positions[(i + 1) % 4],
                strokeWidth = 2.5.dp.toPx())
            drawCircle(Color.Black.copy(alpha = .8f),
                radius = 18.dp.toPx(), center = positions[i])
            drawCircle(QrexDesign.cyan, radius = 13.dp.toPx(), center = positions[i])
            drawCircle(Color.White, radius = 5.dp.toPx(), center = positions[i])
        }
    }
}

/**
 * Neutral, generated-in-memory sample page for actual native UI screenshots.
 * Only reachable through MainActivity's debuggable-only intent allowlist.
 * It is not a camera capture or a user's document.
 */
private fun qrexDocumentDebugPage(): Bitmap {
    val bitmap = Bitmap.createBitmap(720, 1020, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(8, 107, 216)
        textSize = 48f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText("QREX DEMO", 56f, 98f, heading)
    val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(24, 184, 220)
    }
    canvas.drawRoundRect(RectF(56f, 130f, 664f, 142f), 6f, 6f, accent)
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(222, 228, 239)
    }
    for (index in 0..7) {
        val top = 198f + index * 84f
        canvas.drawRoundRect(
            RectF(56f, top, if (index % 3 == 2) 490f else 664f, top + 23f),
            8f, 8f, line
        )
        canvas.drawRoundRect(RectF(56f, top + 34f, 460f, top + 47f),
            6f, 6f, line)
    }
    return bitmap
}

@Composable
internal fun QREXDocumentScanScreen(
    onExit: () -> Unit,
    screenshotFixture: String? = null
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val session = remember(activity) {
        ViewModelProvider(activity)[QREXDocumentSession::class.java]
    }
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    // CameraX may finish a capture after Compose has left this screen. A
    // canceled Compose scope would strand the produced bitmap in native RAM.
    val captureCallbacksActive = remember { AtomicBoolean(true) }
    DisposableEffect(Unit) {
        onDispose {
            captureCallbacksActive.set(false)
            executor.shutdown()
        }
    }
    var screen by rememberSaveable(screenshotFixture) { mutableStateOf("camera") }
    var message by remember { mutableStateOf<String?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var cameraAllowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)
    }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraAllowed = it }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraAllowed = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var camera by remember { mutableStateOf<ImageCapture?>(null) }
    var busy by remember { mutableStateOf(false) }
    var documentName by rememberSaveable { mutableStateOf("QREX-document") }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var editingIndex by rememberSaveable { mutableIntStateOf(-1) }
    var rectangleCrop by rememberSaveable { mutableStateOf(false) }
    // Save the manual corner coordinates across orientation and density changes.
    // Session bitmaps remain Activity-retained rather than written to disk.
    var cropCorners by rememberSaveable { mutableStateOf(QREX_DOCUMENT_INSET_CORNERS) }
    val defaultCorners = QREX_DOCUMENT_INSET_CORNERS
    val errorText = tr("document_error")
    val maxText = tr("document_limit")
    val invalidCornersText = tr("document_corners_invalid")
    val noEdgesText = tr("document_auto_not_found")
    // Debug-only screenshot routing: the captured PNGs remain genuine app UI
    // pixels while the fictional paper page contains no user information.
    LaunchedEffect(screenshotFixture) {
        if (screenshotFixture != null && session.pages.isEmpty() &&
            session.pending == null) {
            val demo = qrexDocumentDebugPage()
            when (screenshotFixture) {
                "document-review", "document-crop" -> session.updatePending(demo)
                "document-pages", "document-export" -> session.pages.add(demo)
            }
            screen = screenshotFixture.removePrefix("document-")
        }
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (session.pages.size >= QREX_DOCUMENT_MAX_PAGES) {
                message = maxText
            } else {
                busy = true
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            QREXDocumentProcessing.readFromPicker(context, uri)
                        }
                    }.onSuccess {
                        session.updatePending(it)
                        editingIndex = -1
                        cropCorners = defaultCorners
                        screen = "review"
                    }.onFailure { message = errorText }
                    busy = false
                }
            }
        }
    }
    // Snapshot before SAF opens; the user must explicitly approve the destination.
    var exportSnapshot by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && exportSnapshot.isNotEmpty()) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        QREXDocumentPdf.export(context, exportSnapshot, uri)
                    }
                    exportedUri = uri
                    screen = "success"
                } catch (_: Exception) {
                    message = errorText
                } finally {
                    busy = false
                    exportSnapshot = emptyList()
                }
            }
        } else {
            exportSnapshot = emptyList()
        }
    }
    val exit: () -> Unit = {
        if (session.pages.isNotEmpty() || session.pending != null) {
            showExitConfirmation = true
        } else {
            session.clearSession()
            onExit()
        }
    }
    BackHandler {
        if (busy) Unit
        else when (screen) {
            "camera" -> exit()
            "crop" -> screen = "review" // Never discard an accepted crop by Back.
            "review" -> {
                session.updatePending(null)
                if (editingIndex >= 0) screen = "pages" else screen = "camera"
                editingIndex = -1
            }
            "pages", "export", "success" -> screen = "camera"
            else -> exit()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = exit, enabled = !busy,
                modifier = Modifier.testTag("qrex-document-back")) {
                Icon(Icons.Default.ArrowBack, contentDescription = tr("back"))
            }
            Text(tr("document_scan"), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            Text(session.pages.size.toString() + "/" + QREX_DOCUMENT_MAX_PAGES,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(tr("document_working"))
        }
        when (screen) {
            "camera" -> {
                Text(tr("document_camera_hint"))
                if (cameraAllowed) {
                    QREXDocumentCamera(onReady = { camera = it },
                        modifier = Modifier.fillMaxWidth()
                            .height(min(460, max(240, LocalConfiguration.current.screenHeightDp / 2)).dp)
                            .testTag("qrex-document-camera"))
                    Button(onClick = {
                        val capture = camera ?: run { message = errorText; return@Button }
                        if (busy || session.pages.size >= QREX_DOCUMENT_MAX_PAGES) {
                            if (session.pages.size >= QREX_DOCUMENT_MAX_PAGES) message = maxText
                            return@Button
                        }
                        busy = true
                        capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                var original: Bitmap? = null
                                var rotated: Bitmap? = null
                                var delivered: Bitmap? = null
                                try {
                                    original = image.toBitmap()
                                    rotated = QREXDocumentProcessing.rotate(
                                        original, image.imageInfo.rotationDegrees
                                    )
                                    val fitted = QREXDocumentProcessing.downsize(rotated)
                                    delivered = fitted
                                    // The ownership of the final bitmap moves to
                                    // the main-thread callback, not the canceled
                                    // rememberCoroutineScope.
                                    ContextCompat.getMainExecutor(context).execute {
                                        if (!captureCallbacksActive.get()) {
                                            if (!fitted.isRecycled) fitted.recycle()
                                        } else {
                                            session.updatePending(fitted)
                                            editingIndex = -1
                                            cropCorners = defaultCorners
                                            screen = "review"
                                            busy = false
                                        }
                                    }
                                } catch (_: Exception) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        if (captureCallbacksActive.get()) {
                                            busy = false
                                            message = errorText
                                        }
                                    }
                                } finally {
                                    // Never recycle the bitmap transferred to
                                    // Compose: RenderThread can still draw it.
                                    if (rotated != null && rotated !== delivered &&
                                        rotated !== original && !rotated.isRecycled) rotated.recycle()
                                    if (original != null && original !== delivered &&
                                        !original.isRecycled) original.recycle()
                                    image.close()
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                ContextCompat.getMainExecutor(context).execute {
                                    if (captureCallbacksActive.get()) {
                                        busy = false
                                        message = errorText
                                    }
                                }
                            }
                        })
                    }, enabled = !busy && camera != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("qrex-document-capture")) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr("document_capture"))
                    }
                } else {
                    Text(tr("document_permission"))
                    Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) {
                        Text(tr("enable_camera"))
                    }
                }
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !busy && session.pages.size < QREX_DOCUMENT_MAX_PAGES,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("qrex-document-import")) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("from_gallery"))
                }
                if (session.pages.isNotEmpty()) {
                    Button(onClick = { screen = "pages" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("qrex-document-open-pages")) {
                        Text(tr("document_pages") + " (" + session.pages.size + ")")
                    }
                }
            }
            "review", "crop" -> {
                val bitmap = session.pending
                if (bitmap == null) {
                    LaunchedEffect(screen) { screen = "camera" }
                } else {
                    if (screen == "crop") {
                        Text(tr("document_crop_hint"))
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !rectangleCrop,
                                onClick = { rectangleCrop = false },
                                label = { Text(tr("document_perspective"), maxLines = 2) },
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("qrex-document-perspective-mode")
                            )
                            FilterChip(
                                selected = rectangleCrop,
                                onClick = {
                                    cropCorners = qrexDocumentBoundingRectangle(cropCorners)
                                    rectangleCrop = true
                                },
                                label = { Text(tr("document_rectangle"), maxLines = 2) },
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("qrex-document-rectangle-mode")
                            )
                        }
                        QREXDocumentCorners(bitmap, cropCorners, rectangleCrop) {
                            cropCorners = it
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (!busy) {
                                        busy = true
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.Default) {
                                                    qrexDocumentSuggestEdges(bitmap)
                                                }
                                            }.onSuccess { suggestion ->
                                                if (suggestion != null) {
                                                    cropCorners = suggestion
                                                    rectangleCrop = true
                                                } else message = noEdgesText
                                            }.onFailure { message = errorText }
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = 56.dp)
                                    .testTag("qrex-document-auto-edges")
                            ) { Text(tr("document_auto_edges"), maxLines = 2) }
                            OutlinedButton(
                                onClick = { cropCorners = QREX_DOCUMENT_FULL_CORNERS },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = 56.dp)
                                    .testTag("qrex-document-reset-crop")
                            ) { Text(tr("document_reset"), maxLines = 2) }
                        }
                        Button(onClick = {
                            if (!qrexDocumentQuadValid(cropCorners)) {
                                message = invalidCornersText
                            } else {
                                busy = true
                                val expectedRevision = session.revision
                                val selectedCorners = cropCorners.toList()
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.Default) {
                                            QREXDocumentProcessing.cropPerspective(bitmap, selectedCorners)
                                        }
                                    }.onSuccess { processed ->
                                        if (session.transformPending(processed, expectedRevision)) {
                                            cropCorners = defaultCorners
                                            screen = "review"
                                        } else if (processed !== session.pending) {
                                            processed.recycle()
                                        }
                                    }.onFailure { message = errorText }
                                    busy = false
                                }
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("document_apply_crop"))
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .testTag("qrex-document-review-preview"),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp,
                                MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Image(bitmap.asImageBitmap(),
                                contentDescription = tr("document_pages"),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth()
                                    .height(min(440, max(220,
                                        LocalConfiguration.current.screenHeightDp / 2)).dp))
                        }
                        Text(tr("document_enhance"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        val applyEnhancement: (QREXDocumentFilter) -> Unit = { filter ->
                            if (!busy) {
                                busy = true
                                val expectedRevision = session.revision
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.Default) {
                                            QREXDocumentFilters.apply(bitmap, filter)
                                        }
                                    }.onSuccess { processed ->
                                        if (!session.transformPending(processed, expectedRevision) &&
                                            processed !== session.pending) processed.recycle()
                                    }.onFailure { message = errorText }
                                    busy = false
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { applyEnhancement(QREXDocumentFilter.GRAYSCALE) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                                    .testTag("qrex-document-grayscale")
                            ) { Text(tr("document_grayscale"), maxLines = 2) }
                            OutlinedButton(
                                onClick = { applyEnhancement(QREXDocumentFilter.BLACK_WHITE) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                                    .testTag("qrex-document-black-white")
                            ) { Text(tr("document_bw"), maxLines = 2) }
                        }
                        if (session.canUndo) {
                            OutlinedButton(
                                onClick = { if (!busy) session.undoPending() },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("qrex-document-undo")
                            ) { Text(tr("document_undo")) }
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                cropCorners = defaultCorners
                                screen = "crop"
                            }, enabled = !busy, modifier = Modifier.weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("qrex-document-open-crop")
                            ) { Text(tr("document_crop")) }
                            OutlinedButton(onClick = {
                                busy = true
                                val expectedRevision = session.revision
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.Default) {
                                            QREXDocumentProcessing.rotate(bitmap, 90)
                                        }
                                    }.onSuccess { processed ->
                                        if (!session.transformPending(processed, expectedRevision) &&
                                            processed !== session.pending) processed.recycle()
                                    }.onFailure { message = errorText }
                                    busy = false
                                }
                            }, enabled = !busy,
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("qrex-document-rotate")
                            ) { Text(tr("document_rotate")) }
                        }
                        Button(onClick = {
                            if (editingIndex in session.pages.indices) {
                                session.replace(editingIndex, bitmap)
                                session.updatePending(null)
                                editingIndex = -1
                                screen = "pages"
                            } else if (session.accept()) {
                                screen = "pages"
                            } else message = maxText
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("document_accept"))
                        }
                    }
                    OutlinedButton(onClick = {
                        session.updatePending(null)
                        editingIndex = -1
                        screen = "camera"
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("document_retake"))
                    }
                }
            }
            "pages" -> {
                Text(tr("document_session_note"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                session.pages.forEachIndexed { index, bitmap ->
                    Surface(
                        shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                            .testTag("qrex-document-page-$index")
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(bitmap.asImageBitmap(), contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(width = 70.dp, height = 94.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(tr("document_pages") + " " + (index + 1),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f))
                            }
                            // Separate action row: four 56dp controls preserve a real
                            // 48dp-or-larger touch area after density rounding on
                            // API 26/31; do not squeeze the thumbnail or labels.
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                IconButton(onClick = { session.move(index, -1) },
                                    enabled = !busy && index > 0,
                                    modifier = Modifier.size(56.dp)
                                        .testTag("qrex-document-page-up-$index")) {
                                    Icon(Icons.Default.ArrowUpward,
                                        contentDescription = tr("document_move_up"))
                                }
                                IconButton(onClick = { session.move(index, 1) },
                                    enabled = !busy && index < session.pages.lastIndex,
                                    modifier = Modifier.size(56.dp)
                                        .testTag("qrex-document-page-down-$index")) {
                                    Icon(Icons.Default.ArrowDownward,
                                        contentDescription = tr("document_move_down"))
                                }
                                IconButton(onClick = {
                                    session.updatePending(bitmap)
                                    editingIndex = index
                                    cropCorners = defaultCorners
                                    screen = "review"
                                }, enabled = !busy,
                                    modifier = Modifier.size(56.dp)
                                        .testTag("qrex-document-page-edit-$index")) {
                                    Icon(Icons.Default.Edit, contentDescription = tr("edit"))
                                }
                                IconButton(onClick = { session.pages.removeAt(index) },
                                    enabled = !busy,
                                    modifier = Modifier.size(56.dp)
                                        .testTag("qrex-document-page-delete-$index")) {
                                    Icon(Icons.Default.Delete, contentDescription = tr("delete"))
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { screen = "camera" },
                    enabled = session.pages.size < QREX_DOCUMENT_MAX_PAGES,
                    modifier = Modifier.fillMaxWidth()) { Text(tr("document_add_page")) }
                Button(onClick = { screen = "export" },
                    enabled = session.pages.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Text(tr("document_export"))
                }
            }
            "export" -> {
                Text(tr("document_saved_warning"))
                OutlinedTextField(
                    value = documentName, onValueChange = { documentName = it.take(64) },
                    label = { Text(tr("document_name")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    exportSnapshot = session.pages.toList()
                    export.launch(qrexDocumentSafeName(documentName))
                }, enabled = session.pages.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth()) { Text(tr("document_export")) }
            }
            "success" -> {
                Text(tr("document_success"), style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    exportedUri?.let { uri ->
                        runCatching {
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    clipData = ClipData.newUri(context.contentResolver,
                                        "QREX PDF", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }, null))
                        }.onFailure { message = errorText }
                    }
                }, enabled = exportedUri != null,
                    modifier = Modifier.fillMaxWidth()) { Text(tr("share")) }
                OutlinedButton(onClick = { session.clearSession(); onExit() },
                    modifier = Modifier.fillMaxWidth()) { Text(tr("close")) }
            }
        }
    }
    if (showExitConfirmation) AlertDialog(
        onDismissRequest = { showExitConfirmation = false },
        title = { Text(tr("document_discard")) },
        text = { Text(tr("document_exit_warning")) },
        confirmButton = { TextButton(onClick = {
            showExitConfirmation = false
            session.clearSession()
            onExit()
        }) { Text(tr("document_discard")) } },
        dismissButton = { TextButton(onClick = { showExitConfirmation = false }) {
            Text(tr("cancel"))
        } }
    )
    if (message != null) AlertDialog(onDismissRequest = { message = null },
        title = { Text("QREX") }, text = { Text(message.orEmpty()) },
        confirmButton = { TextButton(onClick = { message = null }) { Text(tr("ok")) } })
}

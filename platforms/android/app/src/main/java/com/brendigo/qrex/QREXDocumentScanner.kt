package com.brendigo.qrex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    fun setPending(bitmap: Bitmap?) { pending = bitmap }
    fun accept(): Boolean {
        val bitmap = pending ?: return false
        if (pages.size >= QREX_DOCUMENT_MAX_PAGES) return false
        pages.add(bitmap)
        pending = null
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
    fun clearSession() { pages.clear(); pending = null }
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
    onChange: (List<Float>) -> Unit
) {
    var activeCorner by remember(source) { mutableIntStateOf(-1) }
    val currentCorners by rememberUpdatedState(corners)
    val updateCorners by rememberUpdatedState(onChange)
    val viewportHeight = min(400, max(200, LocalConfiguration.current.screenHeightDp / 2))
    Canvas(Modifier.fillMaxWidth().height(viewportHeight.dp)
        .background(Color.Black, RoundedCornerShape(12.dp))
        .pointerInput(source) {
            detectDragGestures(
                onDragStart = { touch ->
                    val r = qrexDocumentPreviewRect(
                        size.width.toFloat(), size.height.toFloat(), source.width, source.height
                    )
                    activeCorner = (0..3).minByOrNull { i ->
                        hypot(touch.x - (r.left + currentCorners[i * 2] * r.width()),
                            touch.y - (r.top + currentCorners[i * 2 + 1] * r.height()))
                    } ?: -1
                },
                onDragEnd = { activeCorner = -1 },
                onDragCancel = { activeCorner = -1 },
                onDrag = { change, _ ->
                    val r = qrexDocumentPreviewRect(
                        size.width.toFloat(), size.height.toFloat(), source.width, source.height
                    )
                    if (activeCorner in 0..3 && r.width() > 0 && r.height() > 0) {
                        val updated = currentCorners.toMutableList()
                        updated[activeCorner * 2] =
                            ((change.position.x - r.left) / r.width()).coerceIn(0f, 1f)
                        updated[activeCorner * 2 + 1] =
                            ((change.position.y - r.top) / r.height()).coerceIn(0f, 1f)
                        updateCorners(updated)
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
        for (i in 0..3) {
            drawLine(QrexDesign.cyan, positions[i], positions[(i + 1) % 4],
                strokeWidth = 3.dp.toPx())
            drawCircle(QrexDesign.cyan, radius = 11.dp.toPx(), center = positions[i])
            drawCircle(Color.White, radius = 4.dp.toPx(), center = positions[i])
        }
    }
}

@Composable
internal fun QREXDocumentScanScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val session = remember(activity) {
        ViewModelProvider(activity)[QREXDocumentSession::class.java]
    }
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    var screen by rememberSaveable { mutableStateOf("camera") }
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
    var cropCorners by remember { mutableStateOf(
        listOf(.05f, .05f, .95f, .05f, .95f, .95f, .05f, .95f)
    ) }
    val defaultCorners = listOf(.05f, .05f, .95f, .05f, .95f, .95f, .05f, .95f)
    val errorText = tr("document_error")
    val maxText = tr("document_limit")
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
                        session.setPending(it)
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
    BackHandler { if (busy) Unit else if (screen == "camera") exit()
        else if (screen == "review" || screen == "crop") screen = "camera"
        else if (screen == "pages" || screen == "export" || screen == "success") screen = "camera"
        else exit()
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
                                try {
                                    val original = image.toBitmap()
                                    val rotated = QREXDocumentProcessing.rotate(
                                        original, image.imageInfo.rotationDegrees
                                    )
                                    val fitted = QREXDocumentProcessing.downsize(rotated)
                                    if (rotated !== fitted) rotated.recycle()
                                    if (original !== fitted && original !== rotated) original.recycle()
                                    scope.launch {
                                        session.setPending(fitted)
                                        cropCorners = defaultCorners
                                        screen = "review"
                                        busy = false
                                    }
                                } catch (_: Exception) {
                                    scope.launch { busy = false; message = errorText }
                                } finally {
                                    image.close()
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                scope.launch { busy = false; message = errorText }
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
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
                        QREXDocumentCorners(bitmap, cropCorners) { cropCorners = it }
                        Button(onClick = {
                            if (!qrexDocumentQuadValid(cropCorners)) {
                                message = tr("document_corners_invalid")
                            } else {
                                busy = true
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.Default) {
                                            QREXDocumentProcessing.cropPerspective(bitmap, cropCorners)
                                        }
                                    }.onSuccess {
                                        session.setPending(it)
                                        screen = "review"
                                    }.onFailure { message = errorText }
                                    busy = false
                                }
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("document_apply_crop"))
                        }
                    } else {
                        Image(bitmap.asImageBitmap(), contentDescription = tr("document_pages"),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                                .height(min(440, max(220, LocalConfiguration.current.screenHeightDp / 2)).dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { screen = "crop" },
                                modifier = Modifier.weight(1f)) { Text(tr("document_crop")) }
                            OutlinedButton(onClick = {
                                busy = true
                                scope.launch {
                                    runCatching {
                                        withContext(Dispatchers.Default) {
                                            QREXDocumentProcessing.rotate(bitmap, 90)
                                        }
                                    }.onSuccess { session.setPending(it) }
                                        .onFailure { message = errorText }
                                    busy = false
                                }
                            }, modifier = Modifier.weight(1f)) { Text(tr("document_rotate")) }
                        }
                        Button(onClick = {
                            if (session.accept()) screen = "pages" else message = maxText
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("document_accept"))
                        }
                    }
                    OutlinedButton(onClick = {
                        session.setPending(null)
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
                    Surface(shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(bitmap.asImageBitmap(), contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(width = 70.dp, height = 100.dp))
                                Spacer(Modifier.width(12.dp))
                                Text((index + 1).toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = { session.move(index, -1) },
                                    enabled = index > 0) { Text("↑") }
                                TextButton(onClick = { session.move(index, 1) },
                                    enabled = index < session.pages.lastIndex) { Text("↓") }
                                TextButton(onClick = { session.pages.removeAt(index) },
                                    enabled = !busy) { Text(tr("delete")) }
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

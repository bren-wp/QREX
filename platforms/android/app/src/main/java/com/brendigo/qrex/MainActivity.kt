package com.brendigo.qrex

import android.content.Intent
import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.text.DateFormat
import java.util.Date

private val midnight = Color(0xFF030C1B)
private val card = Color(0xFF101B2C)
private val cyan = Color(0xFF10C5FA)
private val purple = Color(0xFF9047F8)
private val white = Color.White

/**
 * Preserve the approved QR preview size on ordinary phones, but never allocate
 * a fixed-width preview wider than its measured Compose content viewport.
 */
internal fun canOpenQrResult(payload: String): Boolean = QRContent.canRender(payload)

internal fun responsiveQrSideDp(availableWidthDp: Float, preferredDp: Float): Float =
    minOf(preferredDp, availableWidthDp.coerceAtLeast(0f))

/**
 * Android 13+ clipboard previews can display copied text to bystanders.
 * Mark Wi-Fi QR payloads as sensitive (without changing copy/paste contents).
 */
internal fun qrClipboardSensitive(raw: String): Boolean = QRContent.isWifi(raw)

internal fun makeQrClipboardClip(raw: String): ClipData {
    val clip = ClipData.newPlainText("QR", raw)
    if (qrClipboardSensitive(raw)) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    return clip
}

internal fun copyQrToClipboard(context: Context, raw: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    clipboard.setPrimaryClip(makeQrClipboardClip(raw))
}

private val LocalLanguage = compositionLocalOf { "en" }

@Composable
fun tr(key: String): String {
    val context = LocalContext.current
    val language = LocalLanguage.current
    return remember(context, language, key) { QREXStrings.get(context, language, key) }
}

class MainActivity : ComponentActivity() {
    private val store by lazy { QREXStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screenshot fixtures must never be activated by external intents in
        // production/store builds, even if the launch activity is exported.
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val screenshotCreate = isDebug &&
            intent.getStringExtra("qrex_screenshot") == "create"
        setContent { QREXApp(store, screenshotCreate) }
    }
}

@Composable
private fun QREXApp(store: QREXStore, screenshotCreate: Boolean = false) {
    val context = LocalContext.current
    val pages = listOf("scan", "create", "history", "more")
        .map { QREXStrings.get(context, store.language, it) }
    val icons = listOf(Icons.Default.CameraAlt, Icons.Default.AddCircle,
        Icons.Default.History, Icons.Default.MoreHoriz)
    // Only persist a non-sensitive page index across Activity recreation.
    // Do not persist currentCode: it can include private Wi-Fi credentials.
    var selected by rememberSaveable { mutableIntStateOf(if (screenshotCreate) 1 else 0) }
    var currentCode by remember { mutableStateOf<String?>(null) }
    // Android 16 predictive/back gesture returns from a QR result to its
    // originating screen instead of abruptly exiting the entire app.
    BackHandler(enabled = currentCode != null) { currentCode = null }
    val colors = if (store.lightMode) lightColorScheme(
        primary = Color(0xFF1677FF), secondary = purple,
        background = Color(0xFFF3F6FF), surface = Color.White,
        onSurface = midnight, onBackground = midnight
    ) else darkColorScheme(primary = cyan, secondary = purple,
        background = midnight, surface = card, onSurface = white, onBackground = white)
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLanguage provides store.language) {
        Scaffold(containerColor = colors.background, bottomBar = {
            NavigationBar(containerColor = if (store.lightMode) Color.White else Color(0xFF071225)) {
                pages.forEachIndexed { index, label ->
                    NavigationBarItem(modifier = Modifier.testTag("qrex-tab-$index"),
                        selected = selected == index,
                        onClick = { selected = index; currentCode = null },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(label, maxLines = 1, softWrap = false,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cyan, selectedTextColor = cyan,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (currentCode != null) {
                    ResultScreen(currentCode!!, store, onClose = { currentCode = null })
                } else {
                    when (selected) {
                        0 -> ScanScreen(onRead = { raw ->
                            store.record(raw)
                            currentCode = raw
                        })
                        1 -> CreateScreen(store, screenshotCreate = screenshotCreate, onResult = { currentCode = it })
                        2 -> HistoryScreen(store, onOpen = { currentCode = it })
                        else -> MoreScreen(store)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AppHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrimaryAction(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 51.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private fun shareQrText(context: Context, raw: String, title: String, failureMessage: String) {
    runCatching {
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, raw), title))
    }.onFailure {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ResultScreen(raw: String, store: QREXStore, onClose: () -> Unit) {
    val context = LocalContext.current
    val url = QRContent.safeUrl(raw)
    var showPassword by remember(raw) { mutableStateOf(false) }
    var confirmWifiSave by remember(raw) { mutableStateOf(false) }
    var confirmWifiShare by remember(raw) { mutableStateOf(false) }
    val wifi = QRContent.isWifi(raw)
    val alreadySaved = store.items.any { it.raw == raw && it.saved }
    val copied = tr("copied")
    val sharingFailed = tr("share_failed")
    val shareTitle = tr("share")
    val savedMessage = tr("saved")
    val exportFailedMessage = tr("png_save_failed")
    val operationFailed = tr("operation_failed")
    val exportScope = rememberCoroutineScope()
    // Associate each bitmap with its exact payload: stale frames must never
    // be exported after a user switches to a different QR result.
    val exportFrame by produceState<Pair<String, Bitmap>?>(initialValue = null, key1 = raw) {
        value = null
        val bitmap = withContext(Dispatchers.Default) { generateQR(raw) }
        value = bitmap?.let { raw to it }
        // Compose/RenderThread may still draw the last frame after navigation.
        // Do not recycle preview bitmaps manually; release them via GC once
        // the producer state and UI no longer reference the pixel buffer.
    }
    val exportBitmap = exportFrame?.takeIf { it.first == raw }?.second
    val pngExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            // Content providers may block. Never compress or write on the UI thread.
            exportScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        // Never compress the Compose-rendered preview bitmap:
                        // it may be replaced while SAF's picker is foregrounded.
                        val bitmap = generateQR(raw) ?: error("Cannot render QR")
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                            } ?: error("Cannot open document")
                        } finally {
                            bitmap.recycle()
                        }
                    }.isSuccess
                }
                Toast.makeText(context, if (saved) savedMessage else exportFailedMessage,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().testTag("qrex-result-screen")) {
        // Keep the primary Close action outside the scrolling Result content.
        // Scroll restoration and keyboard resizing must not hide navigation.
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose, modifier = Modifier.testTag("qrex-result-close")) {
                Text(tr("close"))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
        Text("QREX", color = cyan, fontWeight = FontWeight.Black, fontSize = 29.sp)
        Spacer(Modifier.height(22.dp))
        Icon(Icons.Default.QrCode, contentDescription = null,
            tint = cyan, modifier = Modifier.size(68.dp))
        Spacer(Modifier.height(14.dp))
        Text(tr("scan_complete"), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold,
            fontSize = 23.sp)
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                if (wifi) {
                    Text("${tr("network")}: ${QRContent.wifiField(raw, "S") ?: tr("wifi")}",
                        fontWeight = FontWeight.Bold)
                    Text(if (showPassword)
                        "${tr("password")}: ${QRContent.wifiField(raw, "P") ?: ""}" else "${tr("password")}: ••••••••",
                        softWrap = true)
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) tr("hide") else tr("show"))
                    }
                } else {
                    Text(raw, color = MaterialTheme.colorScheme.onSurface)
                    if (url != null) Text(tr("check_url"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        if (url != null) {
            PrimaryAction(tr("open_link")) { openLink(context, url) }
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                copyQrToClipboard(context, raw)
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(tr("copy"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = {
                if (wifi) confirmWifiShare = true
                else shareQrText(context, raw, shareTitle, sharingFailed)
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(tr("share"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        val bitmap = exportBitmap
        if (bitmap != null) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(bitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                    modifier = Modifier.testTag("qrex-result-qr-ready")
                        .size(responsiveQrSideDp(maxWidth.value, 230f).dp)
                        .background(white).padding(12.dp))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pngExport.launch("QREX-QR.png") },
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("export_png")) }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryAction(if (alreadySaved) tr("saved") else tr("save_qr"), enabled = !alreadySaved) {
            if (wifi) confirmWifiSave = true
            else if (!store.save(raw)) {
                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .testTag("qrex-result-close-bottom")
        ) { Text(tr("close")) }
        Spacer(Modifier.height(28.dp))
        }
    }
    if (confirmWifiShare) {
        AlertDialog(onDismissRequest = { confirmWifiShare = false },
            title = { Text(tr("share")) },
            text = { Text(tr("wifi_warning")) },
            confirmButton = { TextButton(onClick = {
                confirmWifiShare = false
                shareQrText(context, raw, shareTitle, sharingFailed)
            }) { Text(tr("share")) } },
            dismissButton = { TextButton(onClick = { confirmWifiShare = false }) {
                Text(tr("cancel"))
            } })
    }
    if (confirmWifiSave) {
        AlertDialog(onDismissRequest = { confirmWifiSave = false },
            title = { Text(tr("wifi_save_title")) },
            text = { Text(tr("wifi_save_warning")) },
            confirmButton = { TextButton(onClick = {
                if (!store.save(raw)) {
                    Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                }
                confirmWifiSave = false
            }) { Text(tr("save")) } },
            dismissButton = { TextButton(onClick = { confirmWifiSave = false }) { Text(tr("cancel")) } })
    }
}

fun openLink(context: Context, url: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }.onFailure {
        val language = context.getSharedPreferences("qrex.user", Context.MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        Toast.makeText(context, QREXStrings.get(context, language, "link_failed"), Toast.LENGTH_SHORT).show()
    }
}

fun generateQR(raw: String): Bitmap? {
    if (!QRContent.canRender(raw)) return null
    return runCatching {
        val bits = MultiFormatWriter().encode(raw, BarcodeFormat.QR_CODE, 680, 680,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2))
        val bitmap = Bitmap.createBitmap(bits.width, bits.height, Bitmap.Config.ARGB_8888)
        val data = IntArray(bits.width * bits.height) { index ->
            if (bits[index % bits.width, index / bits.width]) AndroidColor.BLACK
            else AndroidColor.WHITE
        }
        bitmap.setPixels(data, 0, bits.width, 0, 0, bits.width, bits.height)
        bitmap
    }.getOrNull()
}

@Composable
private fun CreateScreen(store: QREXStore, screenshotCreate: Boolean = false, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val operationFailed = tr("operation_failed")
    var confirmWifiSave by remember { mutableStateOf(false) }
    val types = listOf(tr("url"), tr("text"), tr("wifi"), tr("email"), tr("phone"), tr("location"), tr("contact"))
    var kind by remember { mutableIntStateOf(0) }
    var content by remember { mutableStateOf(if (screenshotCreate) "https://brendigo.com" else "") }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    val input = content.trim()
    val payload = when (kind) {
        0 -> if (QRContent.safeUrl(input) != null) input else ""
        1 -> input
        2 -> if (input.isBlank()) "" else QRContent.wifi(input, password)
        3 -> if (input.contains("@") && !input.contains("\n")) "mailto:$input" else ""
        4 -> if (input.isBlank()) "" else "tel:$input"
        5 -> if (input.isBlank()) "" else "geo:$input"
        else -> if (input.isBlank()) "" else
            "BEGIN:VCARD\nVERSION:3.0\nFN:${input.replace("\n", " ")}\nEND:VCARD"
    }
    // Cancel obsolete renders when the user keeps typing; avoid blocking the UI thread.
    // A previous preview cannot be used for another payload while typing.
    val qrFrame by produceState<Pair<String, Bitmap>?>(initialValue = null, key1 = payload) {
        value = null
        delay(140)
        val image = withContext(Dispatchers.Default) { generateQR(payload) }
        value = image?.let { payload to it }
        // Never recycle this UI bitmap while a queued draw may reference it.
        // The old preview becomes GC-eligible after its composition is removed.
    }
    val qr = qrFrame?.takeIf { it.first == payload }?.second
    // A valid QR must be actionable even while the preview bitmap renders.
    // Result independently renders its image off the main thread.
    val canOpenQr = canOpenQrResult(payload)

    Column(Modifier.fillMaxSize().testTag("qrex-create-screen")) {
        // Keep the primary Show QR action outside the scroller: a changing
        // preview/IME/scroll offset must not move its touch target.
        Column(Modifier.weight(1f).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(20.dp)) {
        AppHeader(tr("create_qr"), tr("enter_details"))
        val visibleKinds = if (more) listOf(0, 1, 2, 6, 3, 4, 5)
            else listOf(0, 1, 2, 6)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleKinds.forEach { index ->
                FilterChip(
                    selected = kind == index,
                    onClick = { kind = index; content = ""; password = "" },
                    label = { Text(types[index]) }
                )
            }
        }
        TextButton(onClick = { more = !more; if (!more && kind !in listOf(0, 1, 2, 6)) kind = 0 }) {
            Text(if (more) tr("fewer_options") else tr("more_options"))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = content, onValueChange = { content = it.take(2000) },
            modifier = Modifier.fillMaxWidth().testTag("qrex-create-content"),
            label = { Text(types[kind]) },
            keyboardOptions = KeyboardOptions(keyboardType =
                if (kind == 0) KeyboardType.Uri else KeyboardType.Text))
        if (kind == 2) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it.take(500) },
                modifier = Modifier.fillMaxWidth(), label = { Text(tr("optional_password")) },
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { reveal = !reveal }) {
                        Text(if (reveal) tr("hide") else tr("show"))
                    }
                })
            Text(tr("wifi_warning"), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("qr_preview"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val previewBitmap = qr
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (previewBitmap != null) {
                        Image(previewBitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                            modifier = Modifier.testTag("qrex-create-preview-ready")
                                .size(responsiveQrSideDp(maxWidth.value, 238f).dp)
                                .background(white).padding(14.dp))
                    } else {
                        Text(tr("invalid_content"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            if (QRContent.isWifi(payload)) confirmWifiSave = true
            else if (!store.save(payload)) {
                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
            }
        }, enabled = qr != null,
            modifier = Modifier.fillMaxWidth().testTag("qrex-create-save")) {
            Text(tr("save_qr"))
        }
        Spacer(Modifier.height(16.dp))
        }
        PrimaryAction(tr("show_qr"), enabled = canOpenQr,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("qrex-create-show")) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onResult(payload)
        }
    }
    if (confirmWifiSave) {
        AlertDialog(onDismissRequest = { confirmWifiSave = false },
            title = { Text(tr("wifi_save_title")) },
            text = { Text(tr("wifi_save_warning")) },
            confirmButton = { TextButton(onClick = {
                if (!store.save(payload)) {
                    Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                }
                confirmWifiSave = false
            }) { Text(tr("save")) } },
            dismissButton = { TextButton(onClick = { confirmWifiSave = false }) {
                Text(tr("cancel"))
            } })
    }
}

@Composable
private fun HistoryScreen(store: QREXStore, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val operationFailed = tr("operation_failed")
    var search by remember { mutableStateOf("") }
    var savedOnly by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<QRRecord?>(null) }
    val found = store.items.filter { QRHistory.matches(it, search, savedOnly) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        AppHeader(tr("history"), tr("history_local"))
        OutlinedTextField(value = search, onValueChange = { search = it },
            label = { Text(tr("search_qr")) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = savedOnly,
            onClick = { savedOnly = !savedOnly },
            label = { Text(tr("saved")) }
        )
        Spacer(Modifier.height(8.dp))
        if (found.isEmpty()) {
            Text(tr("no_saved"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(found, key = { it.raw }) { item ->
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onOpen(item.raw) }, modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(QRContent.label(item.raw), color = MaterialTheme.colorScheme.onSurface)
                                Text(DateFormat.getDateTimeInstance().format(Date(item.timestamp)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { pendingDelete = item }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("delete"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr("delete")) },
            text = { Text(tr("confirm_delete")) },
            confirmButton = {
                TextButton(onClick = {
                    val actual = store.items.firstOrNull { it.raw == target.raw }
                    if (actual != null && !store.remove(actual)) {
                        Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                    }
                    pendingDelete = null
                }) { Text(tr("delete")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(tr("cancel")) }
            }
        )
    }
}

@Composable
private fun MoreScreen(store: QREXStore) {
    val context = LocalContext.current
    val operationFailed = tr("operation_failed")
    var showDelete by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var confirmExport by remember { mutableStateOf(false) }
    var confirmPdfExport by remember { mutableStateOf(false) }
    val archiveScope = rememberCoroutineScope()
    val exportFailed = tr("archive_failed")
    val exportReady = tr("archive_ready")
    val trSaved = tr("saved")
    val exportZip = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destination ->
        if (destination != null) {
            // Snapshot chosen at user confirmation, never read while writing.
            val records = store.items.filter { it.saved }.toList()
            archiveScope.launch {
                val exported = withContext(Dispatchers.IO) {
                    QREXDocuments.exportSavedPdfZip(context, records, destination)
                }
                Toast.makeText(context, if (exported) exportReady else exportFailed,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destination ->
        if (destination != null) {
            val records = store.items.filter { it.saved }.toList()
            archiveScope.launch {
                val exported = withContext(Dispatchers.IO) {
                    QREXDocuments.exportSavedPdf(context, records, destination)
                }
                Toast.makeText(context, if (exported) trSaved else operationFailed,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    var languageMenu by remember { mutableStateOf(false) }
    val selectedLanguage = store.language
    val languageName = if (selectedLanguage == "system") tr("system") else
        Locale.forLanguageTag(selectedLanguage).getDisplayName(Locale.forLanguageTag(selectedLanguage))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        AppHeader(tr("more"), tr("history_local"))
        Text(tr("settings"), color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("save_history"), Modifier.weight(1f))
            Switch(checked = store.historyEnabled, onCheckedChange = store::updateHistoryEnabled)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("light_theme"), Modifier.weight(1f))
            Switch(checked = store.lightMode, onCheckedChange = store::updateLightMode)
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("data_tools"), color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(tr("export_archive_hint"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { confirmPdfExport = true },
                enabled = store.items.any { it.saved },
                modifier = Modifier.weight(1f)
            ) { Text("PDF") }
            OutlinedButton(
                onClick = { confirmExport = true },
                enabled = store.items.any { it.saved },
                modifier = Modifier.weight(1f)
            ) { Text("ZIP") }
        }
        OutlinedButton(onClick = { showDelete = true }) { Text(tr("delete_all")) }
        Spacer(Modifier.height(20.dp))
        Text(tr("language"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { languageMenu = true }) { Text(languageName) }
            DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                DropdownMenuItem(text = { Text(tr("system")) }, onClick = {
                    store.updateLanguage("system"); languageMenu = false
                })
                QREXStrings.languages.forEach { localeCode ->
                    DropdownMenuItem(text = {
                        Text(Locale.forLanguageTag(localeCode).getDisplayName(Locale.forLanguageTag(localeCode)))
                    }, onClick = { store.updateLanguage(localeCode); languageMenu = false })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(tr("about"), color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("QREX", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = {
            openLink(context, Uri.parse("https://brendigo.com"))
        }) { Text(tr("developed_by")) }
        Spacer(Modifier.height(20.dp))
        Text(tr("privacy"), color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { showPrivacy = true }) { Text(tr("privacy_policy")) }
        TextButton(onClick = { showTerms = true }) { Text(tr("terms_of_use")) }
    }
    if (confirmPdfExport) AlertDialog(
        onDismissRequest = { confirmPdfExport = false },
        title = { Text("PDF") },
        text = { Text(tr("export_archive_warning")) },
        confirmButton = { TextButton(onClick = {
            confirmPdfExport = false
            exportPdf.launch("QREX-Saved-Codes.pdf")
        }) { Text("PDF") } },
        dismissButton = { TextButton(onClick = { confirmPdfExport = false }) {
            Text(tr("cancel"))
        } }
    )
    if (confirmExport) AlertDialog(onDismissRequest = { confirmExport = false },
        title = { Text(tr("export_archive")) },
        text = { Text(tr("export_archive_warning")) },
        confirmButton = { TextButton(onClick = {
            confirmExport = false
            exportZip.launch("QREX-Saved-Codes.zip")
        }) { Text(tr("export_archive")) } },
        dismissButton = { TextButton(onClick = { confirmExport = false }) {
            Text(tr("cancel"))
        } })
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false },
        title = { Text(tr("confirm_delete")) },
        text = { Text(tr("delete_warning")) },
        confirmButton = { TextButton(onClick = {
            if (store.clearAll()) showDelete = false
            else Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
        }) { Text(tr("delete_all")) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text(tr("cancel")) } })
    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false },
        title = { Text(tr("privacy_policy")) },
        text = { Text(tr("privacy_summary")) },
        confirmButton = { TextButton(onClick = { showPrivacy = false }) {
            Text(tr("close"))
        } },
        dismissButton = { TextButton(onClick = {
            showPrivacy = false
            openLink(context, Uri.parse("https://github.com/bren-wp/QREX/blob/main/docs/PRIVACY.md"))
        }) { Text(tr("read_full_policy")) } })
    if (showTerms) AlertDialog(onDismissRequest = { showTerms = false },
        title = { Text(tr("terms_of_use")) },
        text = { Text(tr("terms_summary")) },
        confirmButton = { TextButton(onClick = { showTerms = false }) {
            Text(tr("close"))
        } },
        dismissButton = { TextButton(onClick = {
            showTerms = false
            openLink(context, Uri.parse("https://github.com/bren-wp/QREX/blob/main/docs/TERMS.md"))
        }) { Text(tr("terms_of_use")) } })
}

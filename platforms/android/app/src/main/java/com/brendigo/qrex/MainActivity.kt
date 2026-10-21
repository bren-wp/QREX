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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteForever
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

private val midnight = QrexDesign.background
private val card = QrexDesign.surface
private val cyan = QrexDesign.cyan
private val purple = QrexDesign.purple
private val white = Color.White

/** QR navigation remains available while the preview bitmap is rendering. */
internal fun canOpenQrResult(payload: String): Boolean = QRContent.canRender(payload)

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
        // Debug-only screenshot routing. External intents cannot inject
        // screenshot fixtures into Play or F-Droid release builds.
        val screenshotMode = if (isDebug)
            intent.getStringExtra("qrex_screenshot")?.takeIf {
                it in setOf("create", "scan", "result", "history", "saved", "more", "wifi")
            }
        else null
        setContent { QREXApp(store, screenshotMode) }
    }
}

@Composable
private fun QREXApp(store: QREXStore, screenshotMode: String? = null) {
    val context = LocalContext.current
    val pages = listOf("scan", "create", "history", "favorites", "more")
        .map { QREXStrings.get(context, store.language, it) }
    val icons = listOf(Icons.Default.CameraAlt, Icons.Default.AddCircle,
        Icons.Default.History, Icons.Default.FavoriteBorder, Icons.Default.MoreHoriz)
    // Only persist a non-sensitive page index across Activity recreation.
    // Do not persist currentCode: it can include private Wi-Fi credentials.
    var selected by rememberSaveable(screenshotMode) {
        mutableIntStateOf(when (screenshotMode) {
            "create", "wifi", "result" -> 1
            "history" -> 2
            "saved" -> 3
            "more" -> 4
            else -> 0
        })
    }
    var currentCode by remember(screenshotMode) {
        mutableStateOf(if (screenshotMode == "result") "https://brendigo.com" else null)
    }
    // Neutral, in-memory demo history drawn by the *actual Compose UI*.
    // Never persist fixtures, and never enable this in non-debug builds.
    val screenshotItems = remember(screenshotMode) {
        if (screenshotMode in setOf("history", "saved")) {
            val now = System.currentTimeMillis()
            listOf(
                QRRecord("https://brendigo.com", now, true),
                QRRecord("mailto:support@brendigo.com", now - 3_600_000, true),
                QRRecord("QREX offline QR example", now - 86_400_000, false)
            )
        } else null
    }
    // Android 16 predictive/back gesture returns from a QR result to its
    // originating screen instead of abruptly exiting the entire app.
    BackHandler(enabled = currentCode != null) { currentCode = null }
    val colors = if (store.lightMode) lightColorScheme(
        primary = Color(0xFF1677FF), secondary = purple,
        background = Color(0xFFF3F6FF), surface = Color.White,
        onSurface = midnight, onBackground = midnight
    ) else darkColorScheme(primary = QrexDesign.accentBright, secondary = purple,
        background = midnight, surface = card, surfaceVariant = QrexDesign.surfaceRaised,
        onSurface = white, onBackground = white,
        onSurfaceVariant = QrexDesign.muted, outlineVariant = QrexDesign.stroke,
        error = QrexDesign.danger, primaryContainer = QrexDesign.surfaceRaised)
    // White/light navigation surfaces need darker selected labels for contrast.
    val selectedNavigationColor = if (store.lightMode) Color(0xFF1156AE) else cyan
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLanguage provides store.language) {
        Scaffold(containerColor = colors.background, bottomBar = {
            NavigationBar(containerColor = if (store.lightMode) Color.White else QrexDesign.backgroundDeep,
                tonalElevation = 0.dp) {
                pages.forEachIndexed { index, label ->
                    NavigationBarItem(modifier = Modifier.testTag("qrex-tab-$index"),
                        selected = selected == index,
                        onClick = { selected = index; currentCode = null },
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(label, maxLines = 1, softWrap = false,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedNavigationColor,
                            selectedTextColor = selectedNavigationColor,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)))
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().background(
                if (store.lightMode) androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(colors.background, colors.background)) else QrexDesign.heroBrush
            ).padding(padding)) {
                if (currentCode != null) {
                    ResultScreen(currentCode!!, store, onClose = { currentCode = null })
                } else {
                    when (selected) {
                        0 -> ScanScreen(onRead = { raw ->
                            store.record(raw)
                            currentCode = raw
                        })
                        1 -> CreateScreen(store, screenshotMode = screenshotMode, onResult = { currentCode = it })
                        2 -> HistoryScreen(store, onOpen = { currentCode = it },
                            screenshotItems = screenshotItems)
                        3 -> HistoryScreen(store, onOpen = { currentCode = it },
                            screenshotItems = screenshotItems,
                            initiallySavedOnly = true, favoritesPage = true)
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
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, fontSize = 12.sp, maxLines = 3,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1677FF), contentColor = Color.White)) {
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
    var confirmResultDelete by remember(raw) { mutableStateOf(false) }
    var showQrPreview by remember(raw) { mutableStateOf(false) }
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
    val exportFrame by produceState<Pair<String, Bitmap>?>(
        initialValue = null, key1 = raw, key2 = showQrPreview
    ) {
        value = null
        // Do not generate a large QR bitmap until the user asks to see it.
        if (!showQrPreview) return@produceState
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
    // Android 16 may keep the IME visible briefly after leaving Create. Keep
    // the scrollable Result and its bottom Close target above the keyboard;
    // relying only on adjustResize is not sufficient in edge-to-edge windows.
    Column(Modifier.fillMaxSize().imePadding().testTag("qrex-result-screen")) {
        // Keep the primary Close action outside the scrolling Result content.
        // Scroll restoration and keyboard resizing must not hide navigation.
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose, modifier = Modifier.testTag("qrex-result-close")) {
                Text(tr("close"))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()
            .verticalScroll(rememberScrollState()).testTag("qrex-result-scroll").padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
        Text(tr("scan_complete"), color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = QrexDesign.accent) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Link, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        QrexPanel(modifier = Modifier.fillMaxWidth()) {
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
                    Text(raw, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
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
        OutlinedButton(
            onClick = { showQrPreview = !showQrPreview },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                .testTag("qrex-result-toggle-qr")
        ) {
            Icon(Icons.Default.QrCode, contentDescription = null,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("show_qr"))
        }
        if (showQrPreview) {
            Spacer(Modifier.height(12.dp))
            val bitmap = exportBitmap
            if (bitmap != null) {
                // No nested SubcomposeLayout: repeated Create/Result teardown
                // remains safe under the existing P03 stress test.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(bitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                        modifier = Modifier.testTag("qrex-result-qr-ready")
                            .widthIn(max = 230.dp).fillMaxWidth().aspectRatio(1f)
                            .background(white).padding(12.dp))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { pngExport.launch("QREX-QR.png") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) { Text(tr("export_png")) }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryAction(if (alreadySaved) tr("saved") else tr("save_qr"), enabled = !alreadySaved) {
            if (wifi) confirmWifiSave = true
            else if (!store.save(raw)) {
                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
            }
        }
        if (alreadySaved) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { confirmResultDelete = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Delete, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("delete"))
            }
        }
        Spacer(Modifier.height(16.dp))
        }
        // This secondary Close remains reachable even if Android's IME
        // resizes the viewport after entering Result. Keeping it outside
        // the scrolling container prevents a stale scroll offset from
        // hiding or clipping its real touch target on API 35/36.
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .heightIn(min = 52.dp)
                .testTag("qrex-result-close-bottom")
        ) { Text(tr("close")) }
    }
    if (confirmResultDelete) {
        AlertDialog(onDismissRequest = { confirmResultDelete = false },
            title = { Text(tr("delete")) },
            text = { Text(tr("confirm_delete")) },
            confirmButton = { TextButton(onClick = {
                val item = store.items.firstOrNull { it.raw == raw }
                if (item != null && !store.remove(item)) {
                    Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                } else {
                    onClose()
                }
                confirmResultDelete = false
            }) { Text(tr("delete")) } },
            dismissButton = { TextButton(onClick = { confirmResultDelete = false }) {
                Text(tr("cancel"))
            } })
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
private fun CreateScreen(store: QREXStore, screenshotMode: String? = null, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val operationFailed = tr("operation_failed")
    var confirmWifiSave by remember { mutableStateOf(false) }
    val types = listOf(tr("url"), tr("text"), tr("wifi"), tr("email"), tr("phone"), tr("location"), tr("contact"))
    var kind by remember(screenshotMode) { mutableIntStateOf(if (screenshotMode == "wifi") 2 else 0) }
    var content by remember(screenshotMode) {
        mutableStateOf(when (screenshotMode) {
            "create" -> "https://brendigo.com"
            "wifi" -> "QREX-Demo"
            else -> ""
        })
    }
    var password by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
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
        // All supported QR types remain directly accessible by swiping tabs.
        val visibleKinds = listOf(0, 1, 2, 6, 3, 4, 5)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleKinds.forEach { index ->
                FilterChip(
                    selected = kind == index,
                    onClick = { kind = index; content = ""; password = "" },
                    label = { Text(types[index], fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(11.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = QrexDesign.accent,
                        selectedLabelColor = Color.White)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = content, onValueChange = { content = it.take(2000) },
            modifier = Modifier.fillMaxWidth().testTag("qrex-create-content"),
            shape = RoundedCornerShape(14.dp),
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
        QrexPanel(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("qr_preview"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val previewBitmap = qr
                // Avoid a nested subcomposition per rapidly changing QR
                // preview. The parent Box stays stable across payload edits.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (previewBitmap != null) {
                        Image(previewBitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                            modifier = Modifier.testTag("qrex-create-preview-ready")
                                .widthIn(max = 176.dp).fillMaxWidth().aspectRatio(1f)
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
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                .testTag("qrex-create-save")) {
            Text(tr("save_qr"))
        }
        Spacer(Modifier.height(16.dp))
        }
        PrimaryAction(tr("show_qr"), enabled = canOpenQr,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("qrex-create-show")) {
            // Clear focus, but do not programmatically issue an IME hide/back
            // transition before opening Result. On older Android versions a
            // delayed keyboard back callback could immediately pop Result.
            // Unmounting the input field releases its IME focus naturally.
            focusManager.clearFocus(force = true)
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
private fun HistoryScreen(
    store: QREXStore, onOpen: (String) -> Unit,
    screenshotItems: List<QRRecord>? = null, initiallySavedOnly: Boolean = false,
    favoritesPage: Boolean = false
) {
    val context = LocalContext.current
    val operationFailed = tr("operation_failed")
    var search by remember { mutableStateOf("") }
    var savedOnly by remember(initiallySavedOnly) { mutableStateOf(initiallySavedOnly) }
    var pendingDelete by remember { mutableStateOf<QRRecord?>(null) }
    val found = (screenshotItems ?: store.items).filter {
        QRHistory.matches(it, search, savedOnly || favoritesPage)
    }
    Column(Modifier.fillMaxSize().padding(20.dp)
        .testTag(if (favoritesPage) "qrex-favorites-screen" else "qrex-history-screen")) {
        AppHeader(if (favoritesPage) tr("favorites") else tr("history"),
            if (favoritesPage) tr("saved") else tr("history_local"))
        OutlinedTextField(value = search, onValueChange = { search = it },
            label = { Text(tr("search_qr")) },
            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        if (!favoritesPage) {
            Spacer(Modifier.height(6.dp))
            FilterChip(selected = savedOnly,
                onClick = { savedOnly = !savedOnly },
                label = { Text(tr("saved")) })
        }
        Spacer(Modifier.height(10.dp))
        if (found.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text(tr("no_saved"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(found, key = { it.raw }) { item ->
                QrexPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when {
                            QRContent.isWifi(item.raw) -> Icons.Default.Wifi
                            item.raw.startsWith("mailto:", ignoreCase = true) -> Icons.Default.Email
                            item.raw.startsWith("tel:", ignoreCase = true) -> Icons.Default.Phone
                            item.raw.startsWith("geo:", ignoreCase = true) -> Icons.Default.LocationOn
                            QRContent.safeUrl(item.raw) != null -> Icons.Default.Link
                            else -> Icons.Default.QrCode
                        }
                        val tint = when {
                            QRContent.isWifi(item.raw) -> QrexDesign.cyan
                            item.raw.startsWith("mailto:", ignoreCase = true) -> QrexDesign.accentBright
                            item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.danger
                            else -> MaterialTheme.colorScheme.primary
                        }
                        QrexHistoryGlyph(icon, tint)
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = { onOpen(item.raw) }, modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                // SSIDs may be arbitrarily long; constrain the row
                                // instead of letting them crowd out Delete.
                                Text(QRContent.label(item.raw), maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold)
                                Text(DateFormat.getDateTimeInstance().format(Date(item.timestamp)),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
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
    var exportMenu by remember { mutableStateOf(false) }
    val selectedLanguage = store.language
    val languageName = if (selectedLanguage == "system") tr("system") else
        Locale.forLanguageTag(selectedLanguage).getDisplayName(Locale.forLanguageTag(selectedLanguage))
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 18.dp)
        .testTag("qrex-more-screen")) {
        AppHeader(tr("more"), tr("settings"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QrexSettingsTile(
                label = tr("theme"), icon = Icons.Default.Brightness4,
                onClick = { store.updateLightMode(!store.lightMode) },
                subtitle = if (store.lightMode) tr("light_theme") else tr("dark_theme"),
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.weight(1f)) {
                QrexSettingsTile(
                    label = tr("language"), icon = Icons.Default.Translate,
                    onClick = { languageMenu = true }, subtitle = languageName,
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = languageMenu, onDismissRequest = {
                    languageMenu = false
                }) {
                    DropdownMenuItem(text = { Text(tr("system")) }, onClick = {
                        store.updateLanguage("system"); languageMenu = false
                    })
                    QREXStrings.languages.forEach { localeCode ->
                        DropdownMenuItem(text = {
                            Text(Locale.forLanguageTag(localeCode).getDisplayName(
                                Locale.forLanguageTag(localeCode)))
                        }, onClick = {
                            store.updateLanguage(localeCode); languageMenu = false
                        })
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QrexSettingsTile(
                label = tr("export_archive"), icon = Icons.Default.Archive,
                onClick = { exportMenu = true },
                enabled = store.items.any { it.saved },
                subtitle = "PDF · ZIP", modifier = Modifier.weight(1f)
            )
            QrexSettingsTile(
                label = tr("privacy_policy"), icon = Icons.Default.Shield,
                onClick = { showPrivacy = true }, modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QrexSettingsTile(
                label = tr("terms_of_use"), icon = Icons.Default.Description,
                onClick = { showTerms = true }, modifier = Modifier.weight(1f)
            )
            QrexSettingsTile(
                label = tr("about"), icon = Icons.Default.Info,
                onClick = { openLink(context, Uri.parse("https://brendigo.com")) },
                subtitle = tr("developed_by"), modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.DeleteForever, contentDescription = null,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("delete_all"))
        }
        Spacer(Modifier.height(12.dp))
        QrexPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(tr("save_history"), Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = store.historyEnabled,
                    onCheckedChange = store::updateHistoryEnabled)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (exportMenu) {
        AlertDialog(onDismissRequest = { exportMenu = false },
            title = { Text(tr("export_archive")) },
            text = { Text(tr("export_archive_warning")) },
            confirmButton = {
                Row {
                    TextButton(onClick = { exportMenu = false; confirmPdfExport = true }) {
                        Text("PDF")
                    }
                    TextButton(onClick = { exportMenu = false; confirmExport = true }) {
                        Text("ZIP")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { exportMenu = false }) {
                Text(tr("cancel"))
            } })
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

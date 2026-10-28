package com.brendigo.qrex

import android.content.Intent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.provider.CalendarContract
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
import android.text.format.DateUtils
import android.icu.text.RelativeDateTimeFormatter
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
private val purple = QrexDesign.purple
private val white = Color.White

/** QR navigation remains available while the preview bitmap is rendering. */
internal fun canOpenQrResult(payload: String): Boolean = QRContent.canRender(payload)

/**
 * QR contents can carry secrets even when they aren't Wi-Fi codes: e.g.
 * login links, contacts, calendar notes or arbitrary text. Never expose
 * copied QR payloads in Android 13+ system clipboard previews.
 * This hides previews only; it does NOT encrypt/expire the OS clipboard.
 */
internal fun qrClipboardSensitive(raw: String): Boolean = raw.isNotEmpty()

internal fun makeQrClipboardClip(raw: String): ClipData =
    ClipData.newPlainText("QR", raw).apply {
        // Even seemingly ordinary QR URLs may embed private tokens.
        if (qrClipboardSensitive(raw)) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
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
        // screenshot fixtures into release builds.
        val screenshotMode = if (isDebug)
            intent.getStringExtra("qrex_screenshot")?.takeIf {
                it in setOf("splash", "create", "scan", "result", "history",
                    "saved", "more", "wifi", "language", "export")
            }
        else null
        val screenshotLanguage = if (isDebug && screenshotMode != null)
            intent.getStringExtra("qrex_language")?.takeIf { it in QREXStrings.languages }
        else null
        setContent { QREXApp(store, screenshotMode, screenshotLanguage) }
    }
}

@Composable
private fun QREXApp(
    store: QREXStore,
    screenshotMode: String? = null,
    screenshotLanguage: String? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    // Compose observes locale/configuration changes without stale Resources reads.
    val appLanguage = screenshotLanguage ?: store.language
    // Android dark reference has five distinct bottom destinations.
    // Retain stable page IDs so no QR records/settings require migration.
    val navTargets = listOf(0, 1, 2, 3, 4)
    val pages = listOf("scan", "create", "history", "favorites", "more")
        .map { QREXStrings.get(context, appLanguage, it) }
    val icons = listOf(Icons.Default.QrCodeScanner, Icons.Default.AddCircle,
        Icons.Default.History, Icons.Default.FavoriteBorder, Icons.Default.MoreHoriz)
    // Only persist a non-sensitive page index across Activity recreation.
    // Do not persist currentCode: it can include private Wi-Fi credentials.
    var selected by rememberSaveable(screenshotMode) {
        mutableIntStateOf(when (screenshotMode) {
            "create", "wifi", "result" -> 1
            "history" -> 2
            "saved" -> 3
            "more", "language", "export" -> 4
            else -> 0
        })
    }
    var currentCode by remember(screenshotMode) {
        mutableStateOf(if (screenshotMode == "result") "https://brendigo.com" else null)
    }
    var resultShowsQr by remember(screenshotMode) { mutableStateOf(false) }
    var editPayload by remember(screenshotMode) { mutableStateOf<String?>(null) }
    // True cold-start splash, drawn with the real Compose logo and brand
    // graphics. It is skipped for deterministic CI screenshots and not
    // replayed after Activity recreation.
    var showSplash by rememberSaveable(screenshotMode) {
        mutableStateOf(screenshotMode == null || screenshotMode == "splash")
    }
    LaunchedEffect(showSplash, screenshotMode) {
        // In debug-only screenshot mode, keep the *real* splash visible long
        // enough for a verified emulator screenshot; production dismisses it.
        if (showSplash && screenshotMode != "splash") {
            delay(950)
            showSplash = false
        }
    }
    // Neutral, in-memory demo history drawn by the *actual Compose UI*.
    // Never persist fixtures, and never enable this in non-debug builds.
    val screenshotItems = remember(screenshotMode) {
        if (screenshotMode in setOf("history", "saved")) {
            val now = System.currentTimeMillis()
            listOf(
                QRRecord("https://brendigo.com", now - 300_000, true),
                QRRecord("WIFI:T:WPA;S:QREX-Office;P:demo-password;;", now - 600_000, true),
                QRRecord("BEGIN:VCARD\nVERSION:3.0\nFN:QREX demo contact\nTEL:+10000000000\nEND:VCARD",
                    now - 900_000, false),
                QRRecord("https://maps.google.com/?q=Rijeka", now - 1_800_000, false),
                QRRecord("QREX offline QR example", now - 3_600_000, false),
                QRRecord("mailto:info@brendigo.com", now - 7_200_000, false)
            )
        } else null
    }
    // Android 16 predictive/back gesture returns from a QR result to its
    // originating screen instead of abruptly exiting the entire app.
    BackHandler(enabled = currentCode != null) { currentCode = null }
    val darkUi = when (store.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val colors = if (darkUi) darkColorScheme(
        primary = QrexDesign.accentBright, secondary = purple,
        background = midnight, surface = card, surfaceVariant = QrexDesign.surfaceRaised,
        onSurface = white, onBackground = white,
        onSurfaceVariant = QrexDesign.muted, outlineVariant = QrexDesign.stroke,
        error = QrexDesign.danger, primaryContainer = QrexDesign.surfaceRaised
    ) else lightColorScheme(
        primary = QrexDesign.actionBlue, secondary = QrexDesign.purple,
        background = Color(0xFFF7FAFF), surface = Color.White,
        surfaceVariant = Color(0xFFEAF1FB),
        onSurface = Color(0xFF091A32), onBackground = Color(0xFF091A32),
        onSurfaceVariant = Color(0xFF52647D), outlineVariant = Color(0xFFD5E1F0),
        error = Color(0xFFBC2941), primaryContainer = Color(0xFFE7F0FF)
    )
    val hostActivity = context as? ComponentActivity
    SideEffect {
        hostActivity?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !darkUi
                isAppearanceLightNavigationBars = !darkUi
            }
        }
    }
    val compactNavigation = LocalConfiguration.current.screenWidthDp < 400 ||
        LocalDensity.current.fontScale >= 1.35f
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLanguage provides appLanguage) {
        if (showSplash) {
            QrexSplashScreen(subtitle = tr("tagline"))
        } else {
        Scaffold(containerColor = colors.background, bottomBar = {
            if (currentCode == null) {
            NavigationBar(containerColor = if (darkUi) QrexDesign.backgroundDeep else Color(0xFFF0F5FD),
                tonalElevation = 0.dp) {
                pages.forEachIndexed { index, label ->
                    val target = navTargets[index]
                    val selectedNavigationColor = if (!darkUi) QrexDesign.actionBlue
                        else listOf(
                            QrexDesign.purple, QrexDesign.accentBright,
                            Color(0xFF2298FF), QrexDesign.purple, QrexDesign.accentBright
                        )[index]
                    NavigationBarItem(modifier = Modifier.testTag("qrex-tab-$index"),
                        selected = selected == target,
                        onClick = {
                            focusManager.clearFocus()
                            selected = target
                            currentCode = null
                            editPayload = null
                            resultShowsQr = false
                        },
                        // The adjacent text label is the accessible name;
                        // repeating it on the icon causes double announcements.
                        icon = { Icon(icons[index], contentDescription = null) },
                        // Do not hide every tab label on narrow screens or larger fonts.
                        alwaysShowLabel = true,
                        label = {
                            Text(label, maxLines = 2,
                                softWrap = true, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontSize = if (compactNavigation) 10.sp else 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedNavigationColor,
                            selectedTextColor = selectedNavigationColor,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = colors.primaryContainer))
                }
            }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().background(if (darkUi) QrexDesign.heroBrush else
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFF7FAFF), Color.White)))
                .padding(padding)) {
                if (currentCode != null) {
                    ResultScreen(
                        raw = currentCode!!,
                        store = store,
                        screenshotFixture = screenshotMode == "result",
                        showQrOnOpen = resultShowsQr,
                        onClose = { currentCode = null; resultShowsQr = false },
                        onEdit = { raw ->
                            editPayload = raw
                            currentCode = null
                            resultShowsQr = false
                            selected = 1
                        }
                    )
                } else {
                    when (selected) {
                        0 -> ScanScreen(onMore = { selected = 4 }, onRead = { raw ->
                            store.record(raw)
                            resultShowsQr = false
                            currentCode = raw
                        })
                        1 -> CreateScreen(screenshotMode = screenshotMode,
                            initialValue = editPayload,
                            onResult = {
                                store.record(it)
                                resultShowsQr = false
                                editPayload = null
                                currentCode = it
                            })
                        2 -> HistoryScreen(store, onOpen = { currentCode = it },
                            screenshotItems = screenshotItems)
                        3 -> HistoryScreen(store, onOpen = { currentCode = it },
                            screenshotItems = screenshotItems,
                            initiallySavedOnly = true, favoritesPage = true)
                        else -> MoreScreen(store,
                            initialDetailPage = if (screenshotMode == "language" ||
                                screenshotMode == "export") screenshotMode else "root")
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
private fun AppHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.qrex_icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
            )
            Spacer(Modifier.width(9.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 21.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Text(subtitle, fontSize = 11.sp, maxLines = 2,
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
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = QrexDesign.actionBlue, contentColor = Color.White)) {
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

/** External calendar editors may be missing or restricted on some devices. */
private fun openCalendarEvent(context: Context, raw: String): Boolean {
    val start = QRContent.eventTimeMillis(raw, "DTSTART") ?: return false
    val end = QRContent.eventTimeMillis(raw, "DTEND") ?: return false
    if (end <= start) return false
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        .putExtra(CalendarContract.Events.TITLE, QRContent.eventField(raw, "SUMMARY").orEmpty())
        .putExtra(CalendarContract.Events.EVENT_LOCATION, QRContent.eventField(raw, "LOCATION").orEmpty())
        .putExtra(CalendarContract.Events.DESCRIPTION, QRContent.eventField(raw, "DESCRIPTION").orEmpty())
    return runCatching { context.startActivity(intent) }.isSuccess
}

@Composable
private fun ResultScreen(
    raw: String,
    store: QREXStore,
    screenshotFixture: Boolean,
    showQrOnOpen: Boolean,
    onClose: () -> Unit,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val url = QRContent.safeUrl(raw)
    var showPassword by remember(raw) { mutableStateOf(false) }
    var confirmWifiSave by remember(raw) { mutableStateOf(false) }
    var confirmWifiShare by remember(raw) { mutableStateOf(false) }
    var confirmWifiPngExport by remember(raw) { mutableStateOf(false) }
    // SAF may return after a different result has been selected. Export only
    // the payload that launched the picker; never use a subsequently changed QR.
    var pendingResultPngPayload by remember(raw) { mutableStateOf<String?>(null) }
    var confirmResultDelete by remember(raw) { mutableStateOf(false) }
    var showQrPreview by remember(raw, showQrOnOpen) { mutableStateOf(showQrOnOpen) }
    var showResultMenu by remember(raw) { mutableStateOf(false) }
    val wifi = QRContent.isWifi(raw)
    val alreadySaved = store.items.any { it.raw == raw && it.saved }
    val hasHistoryRecord = screenshotFixture || store.items.any { it.raw == raw }
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
        val approvedPayload = pendingResultPngPayload
        pendingResultPngPayload = null
        if (uri != null && approvedPayload != null) {
            // Content providers may block. Never compress or write on the UI thread.
            exportScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        // Never compress the Compose-rendered preview bitmap:
                        // it may be replaced while SAF's picker is foregrounded.
                        val bitmap = generateQR(approvedPayload) ?: error("Cannot render QR")
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
    fun requestResultPngExport() {
        if (!QRContent.canRender(raw)) return
        if (wifi) {
            confirmWifiPngExport = true
        } else {
            pendingResultPngPayload = raw
            pngExport.launch("QREX-QR.png")
        }
    }
    // Android 16 may keep the IME visible briefly after leaving Create. Keep
    // the scrollable Result and its bottom Close target above the keyboard;
    // relying only on adjustResize is not sufficient in edge-to-edge windows.
    Column(Modifier.fillMaxSize().imePadding().testTag("qrex-result-screen")) {
        // Keep the primary Close action outside the scrolling Result content.
        // Scroll restoration and keyboard resizing must not hide navigation.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose,
                modifier = Modifier.size(52.dp).testTag("qrex-result-close")) {
                Icon(Icons.Default.ArrowBack, contentDescription = tr("back"),
                    tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(tr("scan_complete"), modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold,
                fontSize = 21.sp, color = MaterialTheme.colorScheme.onSurface)
            Box {
                IconButton(onClick = { showResultMenu = true },
                    modifier = Modifier.size(52.dp).testTag("qrex-result-menu")) {
                    Icon(Icons.Default.MoreVert, contentDescription = tr("more"),
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(expanded = showResultMenu,
                    onDismissRequest = { showResultMenu = false }) {
                    DropdownMenuItem(text = { Text(tr("copy")) }, onClick = {
                        showResultMenu = false
                        copyQrToClipboard(context, raw)
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    })
                    DropdownMenuItem(text = { Text(tr("share")) }, onClick = {
                        showResultMenu = false
                        if (wifi) confirmWifiShare = true
                        else shareQrText(context, raw, shareTitle, sharingFailed)
                    })
                    DropdownMenuItem(text = { Text(tr("show_qr")) }, onClick = {
                        showResultMenu = false
                        showQrPreview = !showQrPreview
                    })
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()
            .verticalScroll(rememberScrollState()).testTag("qrex-result-scroll")
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
        // Reference screens use ONE large navy content card. Previously the
        // icon floated above a second panel, wasting height and misaligning
        // the action stack on ordinary phones.
        val resultIcon = when {
            wifi -> Icons.Default.Wifi
            QRContent.isEvent(raw) -> Icons.Default.Event
            raw.startsWith("mailto:", ignoreCase = true) -> Icons.Default.Email
            raw.startsWith("tel:", ignoreCase = true) -> Icons.Default.Phone
            raw.startsWith("geo:", ignoreCase = true) -> Icons.Default.LocationOn
            url != null -> Icons.Default.Link
            else -> Icons.Default.QrCode
        }
        QrexPanel(modifier = Modifier.fillMaxWidth().testTag("qrex-result-summary")) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 178.dp)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = QrexDesign.actionBlue) {
                    Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                        Icon(resultIcon, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (wifi) {
                    Text(
                        "${tr("network")}: ${QRContent.wifiField(raw, "S") ?: tr("wifi")}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        textAlign = TextAlign.Center, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Never surface private Wi-Fi credentials without a tap.
                    val wifiPassword = QRContent.wifiField(raw, "P").orEmpty()
                    if (wifiPassword.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (showPassword) "${tr("password")}: $wifiPassword"
                                else "${tr("password")}: ••••••••",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, fontSize = 12.sp
                        )
                        TextButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(if (showPassword) tr("hide") else tr("show")) }
                    }
                } else {
                    Text(
                        raw, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (url != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            url.toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp, textAlign = TextAlign.Center,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (url != null) {
            PrimaryAction(
                tr("open_link"), modifier = Modifier.testTag("qrex-result-primary")
            ) { openLink(context, url) }
            Spacer(Modifier.height(10.dp))
        } else if (QRContent.isEvent(raw)) {
            PrimaryAction(tr("add_to_calendar")) {
                if (!openCalendarEvent(context, raw)) {
                    Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        QrexResultAction(tr("copy"), Icons.Default.ContentCopy) {
            copyQrToClipboard(context, raw)
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
        Spacer(Modifier.height(8.dp))
        QrexResultAction(tr("share"), Icons.Default.Share) {
            if (wifi) confirmWifiShare = true
            else shareQrText(context, raw, shareTitle, sharingFailed)
        }
        Spacer(Modifier.height(8.dp))
        QrexResultAction(tr("show_qr"), Icons.Default.QrCode,
            modifier = Modifier.testTag("qrex-result-toggle-qr"),
            showChevron = false) {
            showQrPreview = !showQrPreview
        }
        Spacer(Modifier.height(8.dp))
        QrexResultAction(
            if (alreadySaved) tr("remove_favorite") else tr("add_favorite"),
            if (alreadySaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            showChevron = false
        ) {
            if (wifi && !alreadySaved) confirmWifiSave = true
            else if (alreadySaved) {
                val item = store.items.firstOrNull { it.raw == raw }
                if (item != null && !store.setSaved(item, false)) {
                    Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                }
            } else if (!store.save(raw)) {
                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
            }
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
                    onClick = ::requestResultPngExport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        .testTag("qrex-result-export-png")
                ) { Text(tr("export_png")) }
            }
        }
        Spacer(Modifier.height(8.dp))
        QrexResultAction(tr("edit"), Icons.Default.Edit) { onEdit(raw) }
        if (hasHistoryRecord) {
            Spacer(Modifier.height(8.dp))
            QrexResultAction(
                tr("delete"), Icons.Default.Delete,
                modifier = Modifier.testTag("qrex-result-delete"),
                destructive = true
            ) { confirmResultDelete = true }
        }
        Spacer(Modifier.height(12.dp))
        }
        // This secondary Close remains reachable even if Android's IME
        // resizes the viewport after entering Result. Keeping it outside
        // the scrolling container prevents a stale scroll offset from
        // hiding or clipping its real touch target on API 35/36.
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
    if (confirmWifiPngExport) {
        AlertDialog(
            modifier = Modifier.testTag("qrex-result-wifi-png-warning"),
            onDismissRequest = { confirmWifiPngExport = false },
            title = { Text(tr("wifi_save_title")) },
            text = { Text("${tr("wifi_warning")}\n${tr("wifi_save_warning")}") },
            confirmButton = { TextButton(onClick = {
                // An approval belongs to this exact result, not to a later QR
                // selected while Android's document picker is in the foreground.
                val approved = approvedWifiQrExport(raw)
                confirmWifiPngExport = false
                if (approved != null) {
                    pendingResultPngPayload = approved
                    pngExport.launch("QREX-QR.png")
                }
            }, modifier = Modifier.testTag("qrex-result-wifi-png-confirm")) {
                Text(tr("export_png"))
            } },
            dismissButton = { TextButton(
                onClick = { confirmWifiPngExport = false },
                modifier = Modifier.testTag("qrex-result-wifi-png-cancel")
            ) { Text(tr("cancel")) } }
        )
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
        // User preferences can have an invalid type after an interrupted old
        // migration. The error handler itself must never crash or rewrite them.
        val language = runCatching {
            context.getSharedPreferences("qrex.user", Context.MODE_PRIVATE)
                .getString("language", "en")
        }.getOrNull()?.takeIf { it == "system" || it in QREXStrings.languages } ?: "en"
        Toast.makeText(context, QREXStrings.get(context, language, "link_failed"), Toast.LENGTH_SHORT).show()
    }
}

fun generateQR(raw: String): Bitmap? {
    if (!QRContent.canRender(raw)) return null
    return runCatching {
        val bits = MultiFormatWriter().encode(raw, BarcodeFormat.QR_CODE, 680, 680,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
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

/** Accessible QR type choice shared by compact Android layouts. */
@Composable
private fun QrexCreateTypeChoice(
    label: String, icon: ImageVector, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(11.dp),
        color = if (selected) QrexDesign.accent else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * QR bitmap work is strictly for a visible valid Create preview, never for a
 * hidden preview while the IME is open or the device is too short.
 * Keep this pure for JVM regression tests and cheap Compose recompositions.
 */
internal fun shouldGenerateCreateQrPreview(
    imeVisible: Boolean,
    layoutAllowsPreview: Boolean,
    payloadValid: Boolean
): Boolean = !imeVisible && layoutAllowsPreview && payloadValid

/** Consent may authorize only the originally requested bounded Wi-Fi QR. */
internal fun approvedWifiQrExport(candidate: String?): String? =
    candidate?.takeIf { QRContent.canRender(it) && QRContent.isWifi(it) }

@Composable
private fun CreateScreen(
    screenshotMode: String? = null,
    initialValue: String? = null,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val types = listOf(
        tr("url"), tr("text"), tr("wifi"), tr("email"),
        tr("phone"), tr("location"), tr("contact"), tr("event")
    )
    val typeIcons = listOf(
        Icons.Default.Link, Icons.Default.Description, Icons.Default.Wifi,
        Icons.Default.Email, Icons.Default.Phone, Icons.Default.LocationOn,
        Icons.Default.Person, Icons.Default.Event
    )
    val draft = remember(screenshotMode, initialValue) {
        val raw = initialValue.orEmpty()
        when {
            screenshotMode == "wifi" -> 2 to "QREX-Demo"
            screenshotMode == "create" -> 0 to "https://brendigo.com"
            QRContent.isWifi(raw) -> 2 to (QRContent.wifiField(raw, "S") ?: "")
            QRContent.isEvent(raw) -> 7 to (QRContent.eventField(raw, "SUMMARY") ?: "")
            raw.startsWith("mailto:", ignoreCase = true) -> 3 to raw.substringAfter(':')
            raw.startsWith("tel:", ignoreCase = true) -> 4 to raw.substringAfter(':')
            raw.startsWith("geo:", ignoreCase = true) -> 5 to raw.substringAfter(':')
            raw.startsWith("BEGIN:VCARD", ignoreCase = true) ->
                6 to (QRContent.contactField(raw, "FN") ?: "")
            QRContent.safeUrl(raw) != null -> 0 to raw
            raw.isNotBlank() -> 1 to raw
            else -> 0 to ""
        }
    }
    var kind by remember(screenshotMode, initialValue) { mutableIntStateOf(draft.first) }
    var content by remember(screenshotMode, initialValue) { mutableStateOf(draft.second) }
    var password by remember(screenshotMode, initialValue) {
        mutableStateOf(initialValue?.let { QRContent.wifiField(it, "P") }.orEmpty())
    }
    var contactPhone by remember(screenshotMode, initialValue) {
        mutableStateOf(initialValue?.let { QRContent.contactField(it, "TEL") }.orEmpty())
    }
    var contactEmail by remember(screenshotMode, initialValue) {
        mutableStateOf(initialValue?.let { QRContent.contactField(it, "EMAIL") }.orEmpty())
    }
    var reveal by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    // A visible disclosure keeps the four advanced QR formats discoverable.
    // Editing an advanced QR reopens the same controls automatically.
    var showAdvancedTypes by remember(screenshotMode, initialValue) {
        mutableStateOf(draft.first in listOf(3, 4, 5, 7))
    }
    var confirmWifiSave by remember { mutableStateOf(false) }
    var eventLocation by remember(screenshotMode, initialValue) {
        mutableStateOf(initialValue?.let { QRContent.eventField(it, "LOCATION") }.orEmpty())
    }
    var eventDescription by remember(screenshotMode, initialValue) {
        mutableStateOf(initialValue?.let { QRContent.eventField(it, "DESCRIPTION") }.orEmpty())
    }
    val defaultStart = remember {
        LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
    fun savedEventTime(name: String, fallback: LocalDateTime): LocalDateTime =
        initialValue?.let { QRContent.eventTimeMillis(it, name) }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
            ?: fallback
    var eventStart by remember(screenshotMode, initialValue) {
        mutableStateOf(savedEventTime("DTSTART", defaultStart))
    }
    var eventEnd by remember(screenshotMode, initialValue) {
        mutableStateOf(savedEventTime("DTEND", defaultStart.plusHours(1)))
    }
    val displayLanguage = LocalLanguage.current
    val dateTimeFormat = remember(displayLanguage) {
        val locale = if (displayLanguage == "system") Locale.getDefault()
            else Locale.forLanguageTag(displayLanguage)
        DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", locale)
    }
    val input = content.trim()
    val payload = when (kind) {
        0 -> if (QRContent.safeUrl(input) != null) input else ""
        1 -> input
        2 -> if (input.isBlank()) "" else QRContent.wifi(input, password)
        3 -> if (input.contains("@") && !input.contains("\n")) "mailto:$input" else ""
        4 -> if (input.isBlank()) "" else "tel:$input"
        5 -> if (input.isBlank()) "" else "geo:$input"
        6 -> QRContent.contact(input, contactPhone, contactEmail).orEmpty()
        else -> QRContent.event(input, eventStart, eventEnd, eventLocation, eventDescription).orEmpty()
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // Avoid SubcomposeLayout and read only the current available phone dimensions.
    val window = LocalConfiguration.current
    val layout = qrexLayoutProfile(
        window.screenWidthDp, (window.screenHeightDp - 80).coerceAtLeast(0),
        LocalDensity.current.fontScale
    )
    val previewVisible = !imeVisible && layout.showQrPreview
    val canGenerate = canOpenQrResult(payload)
    // Debounce generation only when users can see a valid preview. Keyboard,
    // small viewport and invalid payloads must not trigger a 680×680 bitmap.
    // Keying to visibility also discards an obsolete image on IME changes.
    val qrFrame by produceState<Pair<String, Bitmap>?>(
        initialValue = null, key1 = payload, key2 = previewVisible
    ) {
        value = null
        if (!shouldGenerateCreateQrPreview(imeVisible, layout.showQrPreview, canGenerate)) {
            return@produceState
        }
        delay(140)
        val image = withContext(Dispatchers.Default) { generateQR(payload) }
        value = image?.let { payload to it }
    }
    val qr = qrFrame?.takeIf { it.first == payload }?.second
    var pendingExportPayload by remember { mutableStateOf<String?>(null) }
    // Pin the exact Wi-Fi payload to which the user consented: the form can
    // never silently switch the payload between warning and file picker.
    var pendingWifiExportPayload by remember { mutableStateOf<String?>(null) }
    val exportScope = rememberCoroutineScope()
    val imageSaved = tr("saved")
    val imageFailed = tr("png_save_failed")
    val exportImage = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val valueToSave = pendingExportPayload
        pendingExportPayload = null
        if (uri != null && valueToSave != null) {
            exportScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = generateQR(valueToSave) ?: error("Cannot render QR")
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                            } ?: error("Cannot open document")
                        } finally {
                            bitmap.recycle()
                        }
                    }.isSuccess
                }
                Toast.makeText(context, if (saved) imageSaved else imageFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun requestImageExport() {
        val requested = payload.takeIf { QRContent.canRender(it) } ?: return
        if (QRContent.isWifi(requested)) {
            pendingWifiExportPayload = requested
            confirmWifiSave = true
        } else {
            pendingExportPayload = requested
            exportImage.launch("QREX-QR.png")
        }
    }
    fun pickEventDateTime(current: LocalDateTime, update: (LocalDateTime) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = eventDateTimeWithDate(current, year, month, day)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        update(selectedDate.withHour(hour).withMinute(minute).withSecond(0).withNano(0))
                    },
                    selectedDate.hour,
                    selectedDate.minute,
                    android.text.format.DateFormat.is24HourFormat(context)
                ).show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }


    val horizontalPadding = layout.horizontalPaddingDp.dp
    val qrSize = layout.darkQrSizeDp.dp
    Column(Modifier.fillMaxSize().imePadding().testTag("qrex-create-screen")) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .testTag("qrex-create-scroll")
                .padding(horizontal = horizontalPadding, vertical = 8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    QrexBrandMark(logoSize = 32.dp, textSize = 18.sp)
                    Text(tr("tagline"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.Shield, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                // Visible advanced QR disclosure shares the existing header
                // row. It adds NO vertical space ahead of the real URL form:
                // emulator screenshot verification requires the input visible.
                TextButton(
                    onClick = { showAdvancedTypes = !showAdvancedTypes },
                    modifier = Modifier.heightIn(min = 52.dp)
                        .testTag("qrex-create-more-types"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Text(tr("more"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp)
                }
                Box {
                    IconButton(onClick = { createMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = tr("more"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                        // Only advanced/unpinned types belong in the
                        // overflow; never duplicate the visible quick choices.
                        val overflowTypes = listOf(3, 4, 5, 7)
                        overflowTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(types[type]) }, onClick = {
                                kind = type
                                content = ""
                                password = ""
                                contactPhone = ""
                                contactEmail = ""
                                eventLocation = ""
                                eventDescription = ""
                                reveal = false
                                showAdvancedTypes = true
                                createMenu = false
                            })
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(tr("create_qr"), color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(tr("enter_details"), modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

            val quickTypes = listOf(0, 1, 2, 6)
            val selectType: (Int) -> Unit = { index ->
                kind = index
                content = ""
                password = ""
                contactPhone = ""
                contactEmail = ""
                eventLocation = ""
                eventDescription = ""
                reveal = false
            }
            if (layout.stackTypeChoices) {
                // 320dp phones and large accessibility fonts: 2 × 2 prevents
                // truncated type names and overlapping selectable tiles.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickTypes.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { index ->
                                QrexCreateTypeChoice(types[index], typeIcons[index],
                                    selected = kind == index,
                                    modifier = Modifier.weight(1f).testTag("qrex-create-type-$index"),
                                    onClick = { selectType(index) })
                            }
                        }
                    }
                }
            } else {
                // Preserve the stable, previously API-26-tested Compose
                // hierarchy for regular phone windows. Only compact devices
                // use the separate responsive button implementation.
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickTypes.forEach { index ->
                        Surface(
                            onClick = { selectType(index) },
                            modifier = Modifier.weight(1f).heightIn(min = 60.dp)
                                .semantics { this.selected = kind == index }
                                .testTag("qrex-create-type-$index"),
                            shape = RoundedCornerShape(11.dp),
                            color = if (kind == index) QrexDesign.accent
                                else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(typeIcons[index], contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (kind == index) Color.White
                                        else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(types[index], maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                                    color = if (kind == index) Color.White
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (showAdvancedTypes) {
                Column(
                    Modifier.fillMaxWidth().testTag("qrex-create-advanced-types"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3, 4, 5, 7).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { index ->
                                QrexCreateTypeChoice(types[index], typeIcons[index],
                                    selected = kind == index,
                                    modifier = Modifier.weight(1f)
                                        .testTag("qrex-create-type-$index"),
                                    onClick = { selectType(index) })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(if (kind == 7) 160 else 2000) },
                modifier = Modifier.fillMaxWidth().testTag("qrex-create-content"),
                shape = RoundedCornerShape(13.dp),
                singleLine = kind != 1,
                minLines = if (kind == 1) 2 else 1,
                maxLines = if (kind == 1) 4 else 1,
                label = { Text(if (kind == 7) tr("event_title") else types[kind]) },
                keyboardOptions = KeyboardOptions(keyboardType = when (kind) {
                    0 -> KeyboardType.Uri
                    3 -> KeyboardType.Email
                    4 -> KeyboardType.Phone
                    else -> KeyboardType.Text
                })
            )
            if (kind == 2) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(500) },
                    modifier = Modifier.fillMaxWidth().testTag("qrex-create-wifi-password"),
                    label = { Text(tr("optional_password")) },
                    singleLine = true,
                    visualTransformation = if (reveal) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { reveal = !reveal }) {
                            Text(if (reveal) tr("hide") else tr("show"))
                        }
                    }
                )
                Text(tr("wifi_warning"), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (kind == 6) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it.take(100) },
                    modifier = Modifier.fillMaxWidth().testTag("qrex-contact-phone"),
                    label = { Text(tr("phone")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it.take(254) },
                    modifier = Modifier.fillMaxWidth().testTag("qrex-contact-email"),
                    label = { Text(tr("email")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            if (kind == 7) {
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickEventDateTime(eventStart) {
                                eventStart = it
                                if (eventEnd <= it) eventEnd = it.plusHours(1)
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(tr("event_start"), fontSize = 11.sp, maxLines = 2)
                            Text(eventStart.format(dateTimeFormat), fontSize = 12.sp, maxLines = 2)
                        }
                    }
                    OutlinedButton(
                        onClick = { pickEventDateTime(eventEnd) { eventEnd = it } },
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(tr("event_end"), fontSize = 11.sp, maxLines = 2)
                            Text(eventEnd.format(dateTimeFormat), fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
                OutlinedTextField(
                    value = eventLocation,
                    onValueChange = { eventLocation = it.take(250) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("event_location")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = eventDescription,
                    onValueChange = { eventDescription = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("event_details")) },
                    minLines = 1,
                    maxLines = 2
                )
            }
            // Preview belongs to the scrollable form, never the pinned footer.
            // An empty/invalid QR placeholder would otherwise consume space
            // needed by Generate on 320dp phones and during IME transitions.
            // Render a card only after there is a real generated QR bitmap.
            if (previewVisible && qr != null) {
                Spacer(Modifier.height(12.dp))
                QrexPanel(
                    Modifier.fillMaxWidth().testTag("qrex-create-preview-card")
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            tr("qr_preview"), fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Image(
                            qr.asImageBitmap(),
                            contentDescription = tr("qr_preview"),
                            modifier = Modifier.testTag("qrex-create-preview-ready")
                                .size(qrSize).background(Color.White).padding(9.dp)
                        )
                        Text(
                            QRContent.label(payload),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        // Primary actions stay in their own non-scrollable footer and can
        // never be overlapped or pushed away by the QR preview.
        // Their disabled state reflects payload validation, not bitmap timing.
        PrimaryAction(tr("generate_qr"), enabled = canGenerate,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 3.dp)
                .testTag("qrex-create-show")) {
            focusManager.clearFocus(force = true)
            onResult(payload)
        }
        OutlinedButton(
            onClick = ::requestImageExport,
            enabled = canGenerate,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 3.dp)
                .heightIn(min = 48.dp).testTag("qrex-create-save"),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface)
        ) { Text(tr("save_to_device")) }
    }
    if (confirmWifiSave) {
        AlertDialog(
            onDismissRequest = {
                confirmWifiSave = false
                pendingWifiExportPayload = null
            },
            title = { Text(tr("wifi_save_title")) },
            text = { Text(tr("wifi_save_warning")) },
            confirmButton = {
                TextButton(onClick = {
                    val approved = approvedWifiQrExport(pendingWifiExportPayload)
                    confirmWifiSave = false
                    pendingWifiExportPayload = null
                    if (approved != null) {
                        pendingExportPayload = approved
                        exportImage.launch("QREX-QR.png")
                    }
                }) { Text(tr("save_to_device")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmWifiSave = false
                    pendingWifiExportPayload = null
                }) { Text(tr("cancel")) }
            }
        )
    }
}

@Composable
private fun HistoryScreen(
    store: QREXStore,
    onOpen: (String) -> Unit,
    screenshotItems: List<QRRecord>? = null,
    initiallySavedOnly: Boolean = false,
    favoritesPage: Boolean = false
) {
    val context = LocalContext.current
    val activeLanguage = LocalLanguage.current
    val systemLocale = LocalConfiguration.current.locales[0]
    val historyLocale = remember(activeLanguage, systemLocale) {
        if (activeLanguage == "system") systemLocale
        else Locale.forLanguageTag(activeLanguage)
    }
    val relativeTimeFormatter = remember(historyLocale) {
        RelativeDateTimeFormatter.getInstance(historyLocale)
    }
    val absoluteDateFormatter = remember(historyLocale) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, historyLocale)
    }
    val operationFailed = tr("operation_failed")
    val largeHistoryText = LocalDensity.current.fontScale >= 1.35f
    var search by remember { mutableStateOf("") }
    var savedOnly by remember(initiallySavedOnly) { mutableStateOf(initiallySavedOnly) }
    var historyFilterExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<QRRecord?>(null) }
    val found = (screenshotItems ?: store.items).filter {
        // Favorites is a dedicated destination: its filter cannot be reset
        // by a transient History tab state after Activity recreation.
        QRHistory.matches(it, search, favoritesPage || savedOnly)
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(if (favoritesPage) "qrex-favorites-screen" else "qrex-history-screen")
    ) {
        AppHeader(
            if (favoritesPage) tr("favorites") else tr("history"),
            if (favoritesPage) tr("saved") else tr("history_local")
        )
        // Reference History has one search field plus a filter icon; no
        // full-width chip row pushing the first QR cards down. Favorites is
        // a separate saved-only destination and needs no extra filter button.
        Row(
            Modifier.fillMaxWidth().testTag("qrex-history-search-row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it.take(120) },
                label = { Text(tr("search_qr")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    .testTag("qrex-history-search")
            )
            if (!favoritesPage) {
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        IconButton(
                            onClick = { historyFilterExpanded = true },
                            modifier = Modifier.size(52.dp)
                                .testTag("qrex-history-filter-toggle")
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "${tr("all")} / ${tr("saved")}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = historyFilterExpanded,
                        onDismissRequest = { historyFilterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(tr("all")) },
                            onClick = {
                                savedOnly = false
                                historyFilterExpanded = false
                            },
                            modifier = Modifier.heightIn(min = 52.dp)
                                .testTag("qrex-history-all")
                                .semantics { selected = !savedOnly },
                            leadingIcon = {
                                Icon(Icons.Default.History, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(tr("saved")) },
                            onClick = {
                                savedOnly = true
                                historyFilterExpanded = false
                            },
                            modifier = Modifier.heightIn(min = 52.dp)
                                .testTag("qrex-history-saved")
                                .semantics { selected = savedOnly },
                            leadingIcon = {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (found.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("qrex-history-empty")
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text(if (savedOnly) tr("no_saved") else tr("no_history"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            itemsIndexed(found, key = { index, item -> QRHistory.uiRowKey(item, index) }) { _, item ->
                val icon = when {
                    QRContent.isWifi(item.raw) -> Icons.Default.Wifi
                    QRContent.isEvent(item.raw) -> Icons.Default.Event
                    item.raw.startsWith("mailto:", ignoreCase = true) -> Icons.Default.Email
                    item.raw.startsWith("tel:", ignoreCase = true) -> Icons.Default.Phone
                    item.raw.startsWith("geo:", ignoreCase = true) -> Icons.Default.LocationOn
                    item.raw.contains("BEGIN:VCARD", ignoreCase = true) -> Icons.Default.Person
                    QRContent.safeUrl(item.raw) != null -> Icons.Default.Link
                    else -> Icons.Default.QrCode
                }
                val tint = when {
                    QRContent.isWifi(item.raw) -> QrexDesign.cyan
                    item.raw.startsWith("mailto:", ignoreCase = true) -> QrexDesign.accentBright
                    item.raw.startsWith("tel:", ignoreCase = true) -> QrexDesign.danger
                    QRContent.isEvent(item.raw) -> QrexDesign.purple
                    else -> MaterialTheme.colorScheme.primary
                }
                val ageMillis = (System.currentTimeMillis() - item.timestamp).coerceAtLeast(0L)
                val date = when {
                    ageMillis < DateUtils.MINUTE_IN_MILLIS ->
                        relativeTimeFormatter.format(
                            RelativeDateTimeFormatter.Direction.PLAIN,
                            RelativeDateTimeFormatter.AbsoluteUnit.NOW
                        )
                    ageMillis < DateUtils.HOUR_IN_MILLIS ->
                        relativeTimeFormatter.format(
                            (ageMillis / DateUtils.MINUTE_IN_MILLIS).toDouble(),
                            RelativeDateTimeFormatter.Direction.LAST,
                            RelativeDateTimeFormatter.RelativeUnit.MINUTES
                        )
                    ageMillis < DateUtils.DAY_IN_MILLIS ->
                        relativeTimeFormatter.format(
                            (ageMillis / DateUtils.HOUR_IN_MILLIS).toDouble(),
                            RelativeDateTimeFormatter.Direction.LAST,
                            RelativeDateTimeFormatter.RelativeUnit.HOURS
                        )
                    ageMillis < 7L * DateUtils.DAY_IN_MILLIS ->
                        relativeTimeFormatter.format(
                            (ageMillis / DateUtils.DAY_IN_MILLIS).toDouble(),
                            RelativeDateTimeFormatter.Direction.LAST,
                            RelativeDateTimeFormatter.RelativeUnit.DAYS
                        )
                    else -> absoluteDateFormatter.format(Date(item.timestamp))
                }
                val detail = when {
                    QRContent.isWifi(item.raw) -> tr("wifi")
                    QRContent.isEvent(item.raw) -> QRContent.eventField(item.raw, "LOCATION").orEmpty()
                    item.raw.contains("BEGIN:VCARD", ignoreCase = true) -> tr("contact")
                    item.raw.startsWith("mailto:", ignoreCase = true) -> tr("email")
                    item.raw.startsWith("tel:", ignoreCase = true) -> tr("phone")
                    item.raw.startsWith("geo:", ignoreCase = true) -> tr("location")
                    else -> item.raw
                }
                var historyMenu by remember(item.raw) { mutableStateOf(false) }
                Surface(
                    onClick = { onOpen(item.raw) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = if (largeHistoryText) 88.dp else 68.dp)
                            .padding(start = 9.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QrexHistoryGlyph(icon, tint)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                QRContent.label(item.raw),
                                maxLines = if (largeHistoryText) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            val subtitle = listOf(detail, date).filter { it.isNotBlank() }.joinToString(" · ")
                            Text(
                                subtitle,
                                maxLines = if (largeHistoryText) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                // Favorite is a direct, visible action on every row.
                                // Debug-only screenshot fixtures must never mutate the store.
                                if (screenshotItems == null) {
                                    val changed = store.setSaved(item, !item.saved)
                                    if (!changed) Toast.makeText(
                                        context, operationFailed, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("qrex-history-favorite")
                        ) {
                            Icon(
                                if (item.saved) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = if (item.saved) tr("remove_favorite")
                                    else tr("add_favorite"),
                                tint = if (item.saved) QrexDesign.danger
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { historyMenu = true },
                                modifier = Modifier.testTag("qrex-history-menu").size(48.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = tr("more"),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = historyMenu,
                                onDismissRequest = { historyMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(tr("show_qr")) },
                                    onClick = { historyMenu = false; onOpen(item.raw) },
                                    leadingIcon = {
                                        Icon(Icons.Default.QrCode, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (item.saved) tr("remove_favorite") else tr("add_favorite"))
                                    },
                                    onClick = {
                                        historyMenu = false
                                        if (screenshotItems == null) {
                                            val changed = if (item.saved) store.setSaved(item, false)
                                                else store.setSaved(item, true)
                                            if (!changed) {
                                                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (item.saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (item.saved) QrexDesign.danger
                                                else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(tr("delete")) },
                                    onClick = { historyMenu = false; pendingDelete = item },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                )
                            }
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
                    if (screenshotItems == null) {
                        val actual = store.items.firstOrNull { it.raw == target.raw }
                        if (actual != null && !store.remove(actual)) {
                            Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                        }
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
private fun MoreScreen(
    store: QREXStore,
    initialDetailPage: String = "root"
) {
    val context = LocalContext.current
    val operationFailed = tr("operation_failed")
    var showDelete by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var confirmPdfExport by remember { mutableStateOf(false) }
    // In-memory only: the approved snapshot must never enter Android saved state.
    // A recreated Activity without this snapshot fails closed on picker return.
    var pendingPdfRecords by remember { mutableStateOf<List<QRRecord>?>(null) }
    val archiveScope = rememberCoroutineScope()
    val trSaved = tr("saved")
    val exportPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destination ->
        val approvedRecords = pendingPdfRecords
        pendingPdfRecords = null
        if (destination != null && !approvedRecords.isNullOrEmpty()) {
            archiveScope.launch {
                val exported = withContext(Dispatchers.IO) {
                    QREXDocuments.exportSavedPdf(context, approvedRecords, destination)
                }
                Toast.makeText(context, if (exported) trSaved else operationFailed,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    // The supplied light reference has separate Language and Export screens,
    // not oversized dropdown menus. Preserve this local, nonsensitive page
    // over configuration recreation, without touching QR history.
    var detailPage by rememberSaveable(initialDetailPage) {
        mutableStateOf(initialDetailPage)
    }
    BackHandler(enabled = detailPage != "root") { detailPage = "root" }
    val uiLanguage = LocalLanguage.current
    val selectedLanguage = if (store.language == "system" && uiLanguage != "system")
        uiLanguage else store.language
    val languageName = if (selectedLanguage == "system") tr("system") else
        Locale.forLanguageTag(selectedLanguage).getDisplayName(Locale.forLanguageTag(selectedLanguage))
    val themeTitle = when (store.themeMode) {
        "system" -> tr("theme_system")
        "light" -> tr("theme_light")
        else -> tr("theme_dark")
    }
    val compactSettings = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale >= 1.30f
    when (detailPage) {
    "language" -> QrexLanguageDetails(store, onBack = { detailPage = "root" })
    "export" -> QrexExportDetails(
        hasSaved = store.items.any { it.saved },
        onBack = { detailPage = "root" },
        onPdf = { confirmPdfExport = true }
    )
    else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 18.dp)
        .testTag("qrex-more-screen")) {
        AppHeader(tr("more"), tr("settings"))
        if (compactSettings) {
            // Compact dark phones use accessible rows too, avoiding squeezed
            // two-column tiles. Reference two-column cards remain on wide phones.
            // The midnight-navy variant keeps its separate 2-column tiles.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QrexSettingsListRow(tr("theme"), Icons.Default.Palette,
                    subtitle = themeTitle, onClick = { showTheme = true },
                    modifier = Modifier.testTag("qrex-more-theme"),
                    accentOverride = QrexDesign.purple)
                QrexSettingsListRow(tr("language"), Icons.Default.Translate,
                    subtitle = languageName,
                    onClick = { detailPage = "language" },
                    modifier = Modifier.testTag("qrex-more-language"))
                QrexSettingsListRow(tr("export_archive"), Icons.Default.Archive,
                    subtitle = "PDF",
                    onClick = { detailPage = "export" },
                    modifier = Modifier.testTag("qrex-more-export"))
                QrexSettingsListRow(tr("privacy_policy"), Icons.Default.Shield,
                    onClick = { showPrivacy = true },
                    modifier = Modifier.testTag("qrex-more-privacy"))
                QrexSettingsListRow(tr("terms_of_use"), Icons.Default.Description,
                    onClick = { showTerms = true },
                    modifier = Modifier.testTag("qrex-more-terms"),
                    accentOverride = QrexDesign.purple)
                QrexSettingsListRow(tr("about"), Icons.Default.Info,
                    subtitle = tr("developed_by"),
                    onClick = { showAbout = true },
                    modifier = Modifier.testTag("qrex-more-about"))
                Spacer(Modifier.height(6.dp))
                QrexSettingsListRow(tr("clear_history"), Icons.Default.DeleteForever,
                    destructive = true, onClick = { showDelete = true },
                    modifier = Modifier.testTag("qrex-more-delete"))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QrexSettingsTile(label = tr("theme"), icon = Icons.Default.Palette,
                        subtitle = themeTitle, onClick = { showTheme = true },
                        modifier = Modifier.weight(1f).testTag("qrex-more-theme"),
                        accentOverride = QrexDesign.purple)
                    QrexSettingsTile(label = tr("language"), icon = Icons.Default.Translate,
                        subtitle = languageName, onClick = { detailPage = "language" },
                        modifier = Modifier.weight(1f).testTag("qrex-more-language"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QrexSettingsTile(label = tr("export_archive"), icon = Icons.Default.Archive,
                        subtitle = "PDF", onClick = { detailPage = "export" },
                        modifier = Modifier.weight(1f).testTag("qrex-more-export"))
                    QrexSettingsTile(label = tr("privacy_policy"), icon = Icons.Default.Shield,
                        onClick = { showPrivacy = true },
                        modifier = Modifier.weight(1f).testTag("qrex-more-privacy"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QrexSettingsTile(label = tr("terms_of_use"), icon = Icons.Default.Description,
                        onClick = { showTerms = true },
                        modifier = Modifier.weight(1f).testTag("qrex-more-terms"),
                        accentOverride = QrexDesign.purple)
                    QrexSettingsTile(label = tr("about"), icon = Icons.Default.Info,
                        subtitle = tr("developed_by"),
                        onClick = { showAbout = true },
                        modifier = Modifier.weight(1f).testTag("qrex-more-about"))
                }
                QrexSettingsListRow(
                    label = tr("clear_history"),
                    icon = Icons.Default.DeleteForever,
                    onClick = { showDelete = true },
                    destructive = true,
                    showChevron = false,
                    modifier = Modifier.testTag("qrex-more-delete")
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
    }
    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            modifier = Modifier.testTag("qrex-theme-dialog"),
            title = { Text(tr("theme")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("system", "dark", "light").forEach { mode ->
                        val label = when (mode) {
                            "system" -> tr("theme_system")
                            "light" -> tr("theme_light")
                            else -> tr("theme_dark")
                        }
                        Surface(
                            onClick = {
                                if (store.updateThemeMode(mode)) showTheme = false
                                else Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                .testTag("qrex-theme-choice-$mode"),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)) {
                                RadioButton(selected = store.themeMode == mode, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTheme = false }) { Text(tr("close")) }
            }
        )
    }
    if (showAbout) {
        val versionName = remember(context) {
            // Read the installed APK metadata, never a hardcoded release number.
            @Suppress("DEPRECATION")
            val info = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            info?.versionName.orEmpty()
        }
        AlertDialog(
            onDismissRequest = { showAbout = false },
            modifier = Modifier.testTag("qrex-about-dialog"),
            title = { Text(tr("about")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    QrexBrandMark(logoSize = 32.dp, textSize = 21.sp)
                    Text(tr("developed_by"),
                        color = MaterialTheme.colorScheme.onSurface)
                    if (versionName.isNotBlank()) {
                        Text("v$versionName",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(tr("tagline"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("brendigo.com",
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(tr("close")) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAbout = false
                        openLink(context, Uri.parse("https://brendigo.com"))
                    },
                    modifier = Modifier.testTag("qrex-about-website")
                ) { Text(tr("open_link")) }
            }
        )
    }
    if (confirmPdfExport) AlertDialog(
        onDismissRequest = { confirmPdfExport = false },
        title = { Text("PDF") },
        text = { Text(tr("export_archive_warning")) },
        confirmButton = { TextButton(onClick = {
            confirmPdfExport = false
            val approvedRecords = snapshotSavedQrForPdf(store.items)
            if (approvedRecords.isEmpty()) {
                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
            } else {
                pendingPdfRecords = approvedRecords
                exportPdf.launch("QREX-Saved-Codes.pdf")
            }
        }) { Text("PDF") } },
        dismissButton = { TextButton(onClick = { confirmPdfExport = false }) {
            Text(tr("cancel"))
        } }
    )
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false },
        title = { Text(tr("confirm_clear_history")) },
        text = { Text(tr("clear_history_warning")) },
        confirmButton = { TextButton(onClick = {
            if (store.clearHistory()) showDelete = false
            else Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
        }) { Text(tr("clear_history")) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text(tr("cancel")) } })
    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false },
        title = { Text(tr("privacy_policy")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("privacy_summary"))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("save_history"), Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = store.historyEnabled,
                        onCheckedChange = { enabled ->
                            if (!store.updateHistoryEnabled(enabled)) {
                                Toast.makeText(context, operationFailed, Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }
        },
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

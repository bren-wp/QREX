package com.brendigo.qrex

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
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
        val screenshotCreate = intent.getStringExtra("qrex_screenshot") == "create"
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
    var selected by remember { mutableIntStateOf(if (screenshotCreate) 1 else 0) }
    var currentCode by remember { mutableStateOf<String?>(null) }
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
                    NavigationBarItem(selected = selected == index,
                        onClick = { selected = index; currentCode = null },
                        icon = { Icon(icons[index], contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cyan, selectedTextColor = cyan,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray))
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
        Text(subtitle, fontSize = 13.sp, color = Color(0xFF9FB1CC))
    }
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 51.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF))) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ResultScreen(raw: String, store: QREXStore, onClose: () -> Unit) {
    val context = LocalContext.current
    val url = QRContent.safeUrl(raw)
    var showPassword by remember(raw) { mutableStateOf(false) }
    var confirmWifiSave by remember(raw) { mutableStateOf(false) }
    val wifi = QRContent.isWifi(raw)
    val alreadySaved = store.items.any { it.raw == raw && it.saved }
    val copied = tr("copied")
    val sharingFailed = tr("share_failed")
    val shareTitle = tr("share")
    val savedMessage = tr("saved")
    val exportFailedMessage = tr("png_save_failed")
    val exportBitmap by produceState<Bitmap?>(initialValue = null, key1 = raw) {
        value = withContext(Dispatchers.Default) { generateQR(raw) }
    }
    val pngExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val image = exportBitmap ?: error("Missing QR bitmap")
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    check(image.compress(Bitmap.CompressFormat.PNG, 100, stream))
                } ?: error("Cannot open document")
            }.onSuccess {
                Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, exportFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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
                        "${tr("password")}: ${QRContent.wifiField(raw, "P") ?: ""}" else "${tr("password")}: ••••••••")
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) tr("hide") else tr("show"))
                    }
                } else {
                    Text(raw, color = MaterialTheme.colorScheme.onSurface)
                    if (url != null) Text(tr("check_url"),
                        color = Color.LightGray, fontSize = 12.sp)
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
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("QR", raw))
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text(tr("copy"))
            }
            OutlinedButton(onClick = {
                runCatching {
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, raw), shareTitle))
                }.onFailure { Toast.makeText(context,
                    sharingFailed, Toast.LENGTH_SHORT).show() }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text(tr("share"))
            }
        }
        Spacer(Modifier.height(12.dp))
        val bitmap = exportBitmap
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                modifier = Modifier.size(230.dp).background(white).padding(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pngExport.launch("QREX-QR.png") },
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("export_png")) }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryAction(if (alreadySaved) tr("saved") else tr("save_qr"), enabled = !alreadySaved) {
            if (wifi) confirmWifiSave = true else store.save(raw)
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onClose) { Text(tr("close")) }
    }
    if (confirmWifiSave) {
        AlertDialog(onDismissRequest = { confirmWifiSave = false },
            title = { Text(tr("wifi_save_title")) },
            text = { Text(tr("wifi_save_warning")) },
            confirmButton = { TextButton(onClick = {
                store.save(raw); confirmWifiSave = false
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
    val qr by produceState<Bitmap?>(initialValue = null, key1 = payload) {
        delay(140)
        value = withContext(Dispatchers.Default) { generateQR(payload) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        AppHeader(tr("create_qr"), tr("enter_details"))
        types.take(if (more) types.size else 3).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { type ->
                    val index = types.indexOf(type)
                    FilterChip(selected = kind == index,
                        onClick = { kind = index; content = ""; password = "" },
                        label = { Text(type) })
                    Spacer(Modifier.width(5.dp))
                }
            }
        }
        TextButton(onClick = { more = !more; if (!more && kind > 2) kind = 0 }) {
            Text(if (more) tr("fewer_options") else tr("more_options"))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = content, onValueChange = { content = it.take(2000) },
            modifier = Modifier.fillMaxWidth(), label = { Text(types[kind]) },
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
                color = Color.LightGray)
        }
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tr("qr_preview"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                val previewBitmap = qr
                if (previewBitmap != null) Image(previewBitmap.asImageBitmap(), contentDescription = tr("qr_preview"),
                    modifier = Modifier.size(238.dp).background(white).padding(14.dp))
                else Text(tr("invalid_content"), color = Color.LightGray)
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryAction(tr("show_qr"), enabled = qr != null) { onResult(payload) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { store.save(payload) }, enabled = qr != null,
            modifier = Modifier.fillMaxWidth()) { Text(tr("save_qr")) }
    }
}

@Composable
private fun HistoryScreen(store: QREXStore, onOpen: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val found = store.items.filter { QRContent.label(it.raw).contains(search, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        AppHeader(tr("history"), tr("history_local"))
        OutlinedTextField(value = search, onValueChange = { search = it },
            label = { Text(tr("search_qr")) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        if (found.isEmpty()) {
            Text(tr("no_saved"), color = Color.LightGray)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(found, key = { it.raw }) { item ->
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onOpen(item.raw) }, modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(QRContent.label(item.raw), color = MaterialTheme.colorScheme.onSurface)
                                Text(DateFormat.getDateTimeInstance().format(Date(item.timestamp)),
                                    color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { store.remove(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = tr("delete"), tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(store: QREXStore) {
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
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
        OutlinedButton(onClick = { showDelete = true }) { Text(tr("delete_all")) }
        Spacer(Modifier.height(20.dp))
        Text(tr("privacy"), color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { showPrivacy = true }) { Text(tr("privacy")) }
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
        Text("QREX", color = Color.LightGray)
        TextButton(onClick = {
            openLink(context, Uri.parse("https://brendigo.com"))
        }) { Text(tr("developed_by")) }
    }
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false },
        title = { Text(tr("confirm_delete")) },
        text = { Text(tr("delete_warning")) },
        confirmButton = { TextButton(onClick = { store.clearAll(); showDelete = false }) { Text(tr("delete_all")) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text(tr("cancel")) } })
    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false },
        title = { Text(tr("privacy")) },
        text = { Text(tr("privacy_summary")) },
        confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(tr("close")) } })
}

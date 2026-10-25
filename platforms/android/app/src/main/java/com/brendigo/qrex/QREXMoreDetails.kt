package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/** The language page from the supplied settings reference is a full screen,
 * not a popup list that disappears below short Android display heights.
 * All options use existing offline QREX translations and persist through Store.
 */
@Composable
internal fun QrexLanguageDetails(store: QREXStore, onBack: () -> Unit) {
    val flags = mapOf(
        "en" to "🇬🇧", "hr" to "🇭🇷", "de" to "🇩🇪", "it" to "🇮🇹",
        "es" to "🇪🇸", "fr" to "🇫🇷", "pt" to "🇵🇹", "nl" to "🇳🇱",
        "pl" to "🇵🇱", "cs" to "🇨🇿", "sk" to "🇸🇰", "sl" to "🇸🇮",
        "hu" to "🇭🇺", "ro" to "🇷🇴", "bg" to "🇧🇬", "el" to "🇬🇷",
        "tr" to "🇹🇷", "uk" to "🇺🇦", "ru" to "🇷🇺", "sv" to "🇸🇪",
        "da" to "🇩🇰", "fi" to "🇫🇮", "nb" to "🇳🇴"
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)
        .testTag("qrex-language-screen")) {
        QrexDetailHeader(tr("language"), onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("system") + QREXStrings.languages, key = { it }) { code ->
                val label = if (code == "system") tr("system") else {
                    val locale = Locale.forLanguageTag(code)
                    locale.getDisplayLanguage(locale).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                    }
                }
                val selected = store.language == code
                Surface(
                    onClick = { store.updateLanguage(code) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .testTag("qrex-language-option-$code"),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(flags[code] ?: "🌐", fontSize = 23.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(if (code == "system") "Android" else code.uppercase(Locale.ROOT),
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = selected, onClick = null)
                    }
                }
            }
        }
    }
}

/** PDF export uses the system Storage Access Framework.
 * Empty history never exposes a clickable export that will silently fail.
 */
@Composable
internal fun QrexExportDetails(
    hasSaved: Boolean,
    onBack: () -> Unit,
    onPdf: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp).testTag("qrex-export-screen")) {
        QrexDetailHeader(tr("export_archive"), onBack)
        QrexResultAction(
            label = "PDF", icon = Icons.Default.PictureAsPdf,
            enabled = hasSaved,
            modifier = Modifier.testTag("qrex-export-pdf"),
            onClick = onPdf
        )
        Spacer(Modifier.height(12.dp))
        Text(tr("export_archive_warning"), fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(10.dp))
                Text(tr("privacy_summary"), fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun QrexDetailHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack,
            modifier = Modifier.size(48.dp).testTag("qrex-detail-back")) {
            Icon(Icons.Default.ArrowBack, contentDescription = tr("close"))
        }
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

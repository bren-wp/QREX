package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
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
    val context = LocalContext.current
    val errorText = tr("operation_failed")
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
        // The back control stays pinned while all 24 options scroll beneath it.
        QrexDetailHeader(tr("language"), onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .testTag("qrex-language-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            items(listOf("system") + QREXStrings.languages, key = { it }) { code ->
                val label = if (code == "system") tr("system") else {
                    val locale = Locale.forLanguageTag(code)
                    locale.getDisplayLanguage(locale).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                    }
                }
                val selected = store.language == code
                Surface(
                    onClick = {
                        if (!store.updateLanguage(code)) {
                            Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .testTag("qrex-language-option-$code")
                        // Expose the selected row to TalkBack and Compose UI tests
                        // without making the radio icon another independent button.
                        .semantics { this.selected = selected },
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (selected) 3.dp else 1.dp,
                    border = BorderStroke(1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        // A consistent 36dp flag slot keeps all names and radio
                        // controls aligned despite variable emoji glyph widths.
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(flags[code] ?: "🌐", fontSize = 23.sp, maxLines = 1)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(if (code == "system") "Android" else code.uppercase(Locale.ROOT),
                                maxLines = 1, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
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
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp)
            .testTag("qrex-export-screen")
    ) {
        // Keep navigation in place; only the two privacy disclosures scroll.
        // This also preserves a visible Back target on compact devices and
        // at high accessibility font scales.
        QrexDetailHeader(tr("export_archive"), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .testTag("qrex-export-content")
        ) {
            Spacer(Modifier.height(8.dp))
            QrexResultAction(
                label = "PDF", icon = Icons.Default.PictureAsPdf,
                enabled = hasSaved,
                modifier = Modifier.testTag("qrex-export-pdf"),
                onClick = onPdf
            )
            if (!hasSaved) {
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("no_saved"), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                        .testTag("qrex-export-no-saved")
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(QrexDesign.cardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("qrex-export-warning")
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        tr("export_archive_warning"), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(QrexDesign.cardRadius),
                modifier = Modifier.fillMaxWidth().testTag("qrex-export-privacy")
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(tr("privacy_summary"), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QrexDetailHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack,
            modifier = Modifier.size(52.dp).testTag("qrex-detail-back")) {
            Icon(Icons.Default.ArrowBack, contentDescription = tr("back"))
        }
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = QrexDesign.titleTextSize, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
    }
}

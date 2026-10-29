package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * QREX's dark-first production design system.
 *
 * Brand palette deliberately lives in Kotlin source so every real Compose screen
 * shares the same tokens; marketing mockups are not assets or screen implementations.
 */
internal object QrexDesign {
    val background = Color(0xFF061020)
    val backgroundDeep = Color(0xFF030A18)
    val surface = Color(0xFF111E32)
    val surfaceRaised = Color(0xFF152641)
    val stroke = Color(0xFF27405F)
    // Accessible brand-blue for white text on actionable surfaces.
    // WCAG contrast: white vs #086BD8 = 5.12:1 (AA normal text).
    val actionBlue = Color(0xFF086BD8)
    val accent = actionBlue
    val accentBright = Color(0xFF22C7FF)
    val cyan = Color(0xFF19E3F5)
    val purple = Color(0xFF9866FF)
    val muted = Color(0xFFA9BAD2)
    val danger = Color(0xFFFF657B)
    // Phone and contact types are informational, not destructive actions.
    val success = Color(0xFF19CFAB)
    val titleTextSize = 21.sp
    val supportingTextSize = 11.sp
    val tileTitleTextSize = 13.sp
    val tileSupportingTextSize = 12.sp
    val heroBrush = Brush.verticalGradient(
        listOf(Color(0xFF09172D), background, backgroundDeep)
    )
}

/** One branded mark for actual camera/header/Create surfaces.
 * Uses exactly the vector logo carried by the real splash, not a placeholder.
 */
@Composable
internal fun QrexBrandMark(
    logoSize: Dp = 36.dp,
    textSize: TextUnit = 21.sp
) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "QREX"
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.qrex_icon),
            contentDescription = null,
            modifier = Modifier.size(logoSize).clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text("QRE", fontWeight = FontWeight.Black, fontSize = textSize,
            color = MaterialTheme.colorScheme.onBackground)
        Text("X", fontWeight = FontWeight.Black, fontSize = textSize,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun QrexBrandHeader(
    modifier: Modifier = Modifier,
    onMore: () -> Unit = {}
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The reference Scanner header uses the QREX wordmark without
            // a second small launcher icon above the live camera.
            Row(Modifier.semantics(mergeDescendants = true) {
                contentDescription = "QREX"
            }, verticalAlignment = Alignment.CenterVertically) {
                Text("QRE", fontWeight = FontWeight.Black, fontSize = 25.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("X", fontWeight = FontWeight.Black, fontSize = 25.sp,
                    color = QrexDesign.accentBright)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Shield, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            IconButton(onClick = onMore, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = tr("more"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(tr("tagline"), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun QrexPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        // Stable, low-cost layered depth: no runtime blur or animation.
        shadowElevation = 3.dp,
        content = content
    )
}

@Composable
internal fun QrexSettingsTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    accentOverride: Color? = null
) {
    val foreground = if (destructive) MaterialTheme.colorScheme.error
        else accentOverride ?: MaterialTheme.colorScheme.primary
    QrexPanel(modifier = modifier.clickable(
        enabled = enabled, role = Role.Button, onClick = onClick
    )) {
        Column(Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(shape = RoundedCornerShape(12.dp),
                color = foreground.copy(alpha = 0.14f)) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = foreground,
                        modifier = Modifier.size(20.dp))
                }
            }
            Text(label, fontWeight = FontWeight.SemiBold,
                color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (destructive) foreground else MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                fontSize = QrexDesign.tileTitleTextSize)
            if (subtitle != null) {
                Text(subtitle, fontSize = QrexDesign.tileSupportingTextSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Reference light-mode More page uses compact *rows* rather than the
 * dark design's two-column tiles. A single real surface is the touch target;
 * no nested decorative clickable elements or fake menu controls.
 */
@Composable
internal fun QrexSettingsListRow(
    label: String,
    icon: ImageVector,
    subtitle: String? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentOverride: Color? = null,
    showChevron: Boolean = true
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error
        else accentOverride ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, if (destructive)
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(11.dp),
                color = accent.copy(alpha = .10f)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent,
                        modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold,
                    color = if (destructive) accent
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (showChevron && !destructive) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Window-class layout rules for phones: source of truth for responsive
 * Compose sizing. Width/height refer to the available content viewport in dp,
 * not a particular physical handset, density or marketing mockup.
 */
internal data class QrexLayoutProfile(
    val stackTypeChoices: Boolean,
    val showQrPreview: Boolean,
    val darkQrSizeDp: Int,
    val horizontalPaddingDp: Int
)

internal fun qrexLayoutProfile(
    widthDp: Int, heightDp: Int, fontScale: Float
): QrexLayoutProfile {
    // Localized four-way chips truncate at 360–399dp; stack them at that width.
    val stackChoices = widthDp < 400 || fontScale >= 1.30f
    val narrow = widthDp < 360 || fontScale >= 1.30f
    val short = heightDp < 620 || fontScale >= 1.45f
    return QrexLayoutProfile(
        stackTypeChoices = stackChoices,
        showQrPreview = heightDp >= 420 && fontScale < 1.70f,
        darkQrSizeDp = if (narrow || short) 104 else 156,
        horizontalPaddingDp = if (widthDp < 350) 12 else 16
    )
}

/** Reference-style result action: real full-width button with a touch target
 * >= 52dp, clear icon, legible label and chevron. Never an inert decorative card.
 */
@Composable
internal fun QrexResultAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    // Screenshot references show flat, soft-cornered rows, with destructive
    // actions in a subtly tinted red row rather than a pill-shaped outline.
    // All rows remain genuine Compose click targets (>= 56dp).
    val foreground = if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp,
            if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.20f)
            else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp),
                tint = foreground)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f),
                color = foreground, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (showChevron && !destructive) {
                Icon(Icons.Default.ChevronRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
internal fun QrexHistoryGlyph(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
        }
    }
}

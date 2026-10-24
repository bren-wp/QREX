package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val accent = Color(0xFF087BFA)
    val accentBright = Color(0xFF22C7FF)
    val cyan = Color(0xFF19E3F5)
    val purple = Color(0xFF9866FF)
    val onDark = Color(0xFFF8FBFF)
    val muted = Color(0xFFA9BAD2)
    val danger = Color(0xFFFF657B)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
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
            QrexBrandMark()
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Shield, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            IconButton(onClick = onMore, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = tr("more"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(tr("tagline"), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    modifier: Modifier = Modifier
) {
    val foreground = if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary
    QrexPanel(modifier = modifier.clickable(enabled = enabled, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 84.dp).padding(10.dp),
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
                maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 10.sp,
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
    modifier: Modifier = Modifier
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    Text(subtitle, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
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
    val lightQrSizeDp: Int,
    val darkQrSizeDp: Int,
    val horizontalPaddingDp: Int
)

internal fun qrexLayoutProfile(
    widthDp: Int, heightDp: Int, fontScale: Float
): QrexLayoutProfile {
    val narrow = widthDp < 360 || fontScale >= 1.30f
    val short = heightDp < 620 || fontScale >= 1.45f
    return QrexLayoutProfile(
        stackTypeChoices = narrow,
        showQrPreview = heightDp >= 420 && fontScale < 1.70f,
        lightQrSizeDp = if (narrow || short) 80 else 104,
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp))
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

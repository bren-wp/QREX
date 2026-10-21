package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val heroBrush = Brush.linearGradient(listOf(Color(0xFF122D59), background, backgroundDeep))
}

@Composable
internal fun QrexBrandHeader(
    heading: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QRE", fontWeight = FontWeight.Black, fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground)
            Text("X", fontWeight = FontWeight.Black, fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Shield, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Text(heading, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
        Column(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(12.dp),
                color = foreground.copy(alpha = 0.14f)) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = foreground,
                        modifier = Modifier.size(20.dp))
                }
            }
            Text(label, fontWeight = FontWeight.SemiBold,
                color = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (destructive) foreground else MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
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
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium, maxLines = 1,
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
        Box(Modifier.size(43.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        }
    }
}

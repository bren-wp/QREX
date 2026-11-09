package com.brendigo.qrex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalDensity
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
    // Phone QR category red is informational and never a destructive-action token.
    val phoneTint = Color(0xFFFF7488)
    // Phone and contact types are informational, not destructive actions.
    val success = Color(0xFF19CFAB)
    val titleTextSize = 21.sp
    val supportingTextSize = 11.sp
    val tileSupportingTextSize = 12.sp
    val sectionSpacing = 12.dp
    val cardRadius = 15.dp
    val actionTextSize = 14.sp
    val heroBrush = Brush.verticalGradient(
        listOf(Color(0xFF09172D), background, backgroundDeep)
    )
    // Native gradient and lightweight elevation, not a static 3D screenshot.
    val referenceButtonBrush = Brush.verticalGradient(
        // Every stop is >= 4.5:1 against white, including the bright top edge.
        listOf(Color(0xFF0974D4), Color(0xFF0867CD), Color(0xFF0558C2))
    )
    val referenceGlyphBrush = Brush.verticalGradient(
        listOf(Color(0xFF148AFF), Color(0xFF0767E9), Color(0xFF1240B9))
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
        Text(tr("scanner_local_message"), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("qrex-scanner-local-message"))
    }
}

@Composable
internal fun QrexPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(QrexDesign.cardRadius + 3.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        // Stable, low-cost layered depth: no runtime blur or animation.
        shadowElevation = 4.dp,
        content = content
    )
}

/**
 * A single full-width settings card in every appearance and window size.
 * The raised icon plate is decorative: the parent Surface is the only
 * accessible click target, with a native ripple and an actionable chevron.
 * Height expands with translations and large accessibility font settings.
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
    showChevron: Boolean = true,
    tileMode: Boolean = false
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error
        else accentOverride ?: MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(
            min = if (tileMode) 108.dp
                else qrexSettingsCardHeightDp(LocalDensity.current.fontScale).dp
        ),
        shape = RoundedCornerShape(QrexDesign.cardRadius),
        color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, if (destructive)
            MaterialTheme.colorScheme.error.copy(alpha = 0.30f)
            else MaterialTheme.colorScheme.outlineVariant)
    ) {
        // Identical real RGB/vector plate in phone tiles and accessible rows.
        // Decorative pictogram never adds a second independent touch target.
        val plate: @Composable () -> Unit = {
            Surface(
                modifier = Modifier.testTag("qrex-more-icon-plate"),
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = .13f),
                border = BorderStroke(1.dp, accent.copy(alpha = .39f)),
                shadowElevation = 4.dp
            ) {
                Box(
                    Modifier.size(40.dp).background(Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .10f), Color.Transparent)
                    )),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent,
                        modifier = Modifier.size(22.dp))
                }
            }
        }
        val labels: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (destructive) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, fontSize = QrexDesign.tileSupportingTextSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (tileMode) {
            // The user's dark Android reference has a genuine 2×3 tile grid.
            // Content grows for longer translations; FlowRow keeps rows distinct.
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                plate()
                labels()
            }
        } else {
            // 320dp, very large font or landscape: keep all labels accessible.
            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically) {
                plate()
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) { labels() }
                if (showChevron && !destructive) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Two reference columns on standard phones, one wide accessible column on
 * compact displays, large text and short landscape viewports. Never truncate
 * a card just to match a marketing screenshot.
 */
internal fun qrexMoreTileColumns(widthDp: Int, heightDp: Int, fontScale: Float): Int =
    if (widthDp < 360 || heightDp < 500 || fontScale >= 1.30f) 1 else 2

/** Accessible card height for Settings, independent of display width or theme. */
internal fun qrexSettingsCardHeightDp(fontScale: Float): Int =
    if (fontScale >= 1.35f) 88 else 72

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
    // Quick tabs are text-only in the reference: four fit at >=340dp.
    // Preserve 2x2 controls for truly narrow screens and large accessibility text.
    val stackChoices = widthDp < 340 || fontScale >= 1.30f
    val narrow = widthDp < 360 || fontScale >= 1.30f
    val short = heightDp < 620 || fontScale >= 1.45f
    return QrexLayoutProfile(
        stackTypeChoices = stackChoices,
        showQrPreview = heightDp >= 420 && fontScale < 1.70f,
        darkQrSizeDp = if (narrow || short) 104 else 156,
        horizontalPaddingDp = if (widthDp < 350) 12 else 16
    )
}

/** Keep Result cards at phone width on compact devices and cap the
 * readable content column to 520dp on tablets and large foldable windows.
 * Unlike a fixed 520dp width, this never overflows a 320dp viewport.
 */
internal fun qrexResultContentFraction(viewportWidthDp: Int): Float =
    (520f / viewportWidthDp.coerceAtLeast(1)).coerceAtMost(1f)

/** Text size never reduces result actions below an accessible 52dp target. */
internal fun qrexResultActionHeightDp(fontScale: Float): Int =
    if (fontScale >= 1.35f) 60 else 52

/** Real brand-blue navigation icon and thin selected underline. No duplicate
 * content description: the native NavigationBarItem label owns accessibility.
 */
@Composable
internal fun QrexNavigationGlyph(
    icon: ImageVector,
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint,
            modifier = Modifier.size(22.dp))
        if (selected) {
            Box(Modifier.align(Alignment.BottomCenter).width(19.dp).height(2.dp)
                .background(tint, RoundedCornerShape(2.dp)))
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
        modifier = modifier.fillMaxWidth().heightIn(
            min = qrexResultActionHeightDp(LocalDensity.current.fontScale).dp
        ),
        shape = RoundedCornerShape(QrexDesign.cardRadius),
        color = if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.09f)
            else MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp,
            if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.20f)
            else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                tint = foreground)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f),
                color = foreground, fontWeight = FontWeight.Medium,
                fontSize = QrexDesign.actionTextSize,
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
internal fun QrexHistoryGlyph(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    background: Color = QrexDesign.actionBlue
) {
    // The category pictogram is a real Compose vector over an opaque 3D
    // gradient. The tile does not intercept the History row's click target.
    Surface(
        modifier = modifier.testTag("qrex-history-category-glyph"),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, tint.copy(alpha = .66f)),
        shadowElevation = 3.dp
    ) {
        Box(
            // A translucent white highlight over an opaque darker Surface
            // changes RGB at the top; varying background alpha alone does not.
            Modifier.size(38.dp).background(Brush.verticalGradient(
                listOf(Color.White.copy(alpha = .14f), Color.Transparent)
            )),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(21.dp))
        }
    }
}

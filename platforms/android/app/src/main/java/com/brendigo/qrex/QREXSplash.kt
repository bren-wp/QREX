package com.brendigo.qrex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

/**
 * Production splash geometry corresponding to the supplied dark Q/QR references.
 * Values are density independent; avoid clipped text on 320dp / tall-font phones.
 */
internal data class QrexSplashMetrics(
    val logoDp: Int,
    val titleSp: Int,
    val taglineSp: Int,
    val bottomPaddingDp: Int,
    val centerOffsetDp: Int
)

internal fun qrexSplashMetrics(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float
): QrexSplashMetrics {
    val compact = widthDp < 360 || heightDp < 640
    return QrexSplashMetrics(
        logoDp = minOf((widthDp * 0.55f).toInt(), if (heightDp < 620) 156 else 214)
            .coerceAtLeast(96),
        titleSp = when {
            fontScale >= 1.6f -> 38
            fontScale >= 1.3f -> 45
            compact -> 50
            else -> 60
        },
        taglineSp = if (compact || fontScale >= 1.3f) 14 else 17,
        bottomPaddingDp = if (heightDp < 620) 26 else 58,
        centerOffsetDp = if (heightDp < 620) -12 else -30
    )
}

/**
 * Reference 01: real Compose Android dark splash — rounded Q/QR logo, two-tone
 * QREX wordmark, translated tagline, sweeping blue wave paths and bottom copy.
 * Marketing boards are framed/angled device renders, not byte-exact PNG
 * baselines; the ten native screenshots are captured independently by CI.
 */
@Composable
internal fun QrexSplashScreen(
    subtitle: String = "Scan. Create. Share.",
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(QrexDesign.backgroundDeep)
            .semantics { contentDescription = "QREX splash: $subtitle" }
    ) {
        val metrics = qrexSplashMetrics(
            maxWidth.value.toInt(), maxHeight.value.toInt(), LocalDensity.current.fontScale
        )
        val compactWidth = maxWidth < 360.dp
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF061632), Color(0xFF03112A), QrexDesign.backgroundDeep)
                )
            )
            // Broad satin-blue ribbons in the reference. Unlike concentric
            // circles, these bezier bands sweep diagonally across the display.
            val upperRibbon = Path().apply {
                moveTo(0f, h * .04f)
                cubicTo(w * .28f, h * .09f, w * .47f, h * .04f, w * .78f, h * .17f)
                cubicTo(w * 1.07f, h * .30f, w * .72f, h * .41f, w * .37f, h * .38f)
                cubicTo(w * .19f, h * .36f, w * .07f, h * .33f, 0f, h * .37f)
                close()
            }
            drawPath(
                upperRibbon,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF085EF0), Color(0xFF0745BC), Color(0xFF021538)),
                    start = Offset(w * .04f, h * .04f),
                    end = Offset(w * .95f, h * .44f)
                ),
                alpha = .85f
            )
            val upperEdge = Path().apply {
                moveTo(0f, h * .04f)
                cubicTo(w * .28f, h * .09f, w * .47f, h * .04f, w * .78f, h * .17f)
            }
            drawPath(
                upperEdge, color = Color(0xFF23AEFF).copy(alpha = .62f),
                style = Stroke(width = 1.25.dp.toPx())
            )
            val lowerRibbon = Path().apply {
                moveTo(0f, h * .65f)
                cubicTo(w * .34f, h * .54f, w * .55f, h * .64f, w, h * .76f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                lowerRibbon,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF0D67EB), Color(0xFF062CB2), Color(0xFF020B38)),
                    start = Offset(0f, h * .62f),
                    end = Offset(w, h)
                ),
                alpha = .87f
            )
            val lowerEdge = Path().apply {
                moveTo(0f, h * .65f)
                cubicTo(w * .34f, h * .54f, w * .55f, h * .64f, w, h * .76f)
            }
            drawPath(
                lowerEdge, color = Color(0xFF16B6FE).copy(alpha = .7f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val secondWave = Path().apply {
                moveTo(0f, h * .80f)
                cubicTo(w * .40f, h * .72f, w * .68f, h * .88f, w, h * .84f)
            }
            drawPath(
                secondWave, color = Color(0xFF1F71FB).copy(alpha = .54f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center)
                .offset(y = metrics.centerOffsetDp.dp)
                .fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.qrex_icon),
                contentDescription = null,
                modifier = Modifier.size(metrics.logoDp.dp)
                    .clip(RoundedCornerShape(24.dp)).testTag("qrex-splash-logo")
            )
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "QREX"
                }.testTag("qrex-splash-wordmark")
            ) {
                Text(
                    "QRE", color = Color.White, fontSize = metrics.titleSp.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-1).sp
                )
                Text(
                    "X",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(QrexDesign.cyan, Color(0xFF0788FF))
                        ),
                        fontSize = metrics.titleSp.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle, color = Color.White, fontSize = metrics.taglineSp.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.testTag("qrex-splash-tagline")
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().padding(horizontal = 30.dp)
                .padding(bottom = metrics.bottomPaddingDp.dp)
                .testTag("qrex-splash-footer"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                tr("splash_message"),
                color = Color(0xFFB9CFF4),
                fontSize = if (compactWidth) 12.sp else 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                maxLines = 3
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.width(94.dp).height(4.dp)
                    .background(Color(0xFF435778), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier.width(42.dp).height(4.dp)
                        .background(QrexDesign.cyan, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

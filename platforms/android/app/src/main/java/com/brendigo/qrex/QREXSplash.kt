package com.brendigo.qrex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag

/**
 * Production splash geometry corresponding to the supplied dark Q/QR references.
 * Values are density independent; avoid clipped text on 320dp / tall-font phones.
 */
/** A true 3.5-second in-app launch screen; rotation never restarts the clock. */
internal const val QREX_SPLASH_DURATION_MS = 3_500L

internal fun qrexSplashRemainingMs(startedAtMs: Long, nowMs: Long): Long =
    if (startedAtMs < 0L || nowMs < startedAtMs) 0L
    else (QREX_SPLASH_DURATION_MS - (nowMs - startedAtMs))
        .coerceIn(0L, QREX_SPLASH_DURATION_MS)

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
    val largeScreen = widthDp >= 600 && heightDp >= 820
    return QrexSplashMetrics(
        // Retain the approved 393×852 phone geometry. On large tablets
        // scale the same vector instead of presenting a tiny phone-sized badge.
        logoDp = minOf((widthDp * 0.55f).toInt(), when {
            heightDp < 620 -> 156
            largeScreen -> 256
            else -> 214
        }).coerceAtLeast(96),
        titleSp = when {
            fontScale >= 1.6f -> 38
            fontScale >= 1.3f -> 45
            compact -> 50
            largeScreen -> 72
            else -> 60
        },
        taglineSp = when {
            compact || fontScale >= 1.3f -> 14
            largeScreen -> 20
            else -> 19
        },
        bottomPaddingDp = if (heightDp < 620) 20 else 40,
        centerOffsetDp = when {
            heightDp < 620 -> -12
            largeScreen -> -30
            else -> (60 - heightDp * .13f).toInt().coerceIn(-65, -20) // ~37% phone icon center
        }
    )
}

/**
 * The centered brand and independently anchored footer are correct for the
 * reference phone. For short windows or large accessibility text they can
 * overlap; switch to a single vertically scrollable column instead.
 */
internal fun qrexSplashUsesScrollLayout(heightDp: Int, fontScale: Float): Boolean =
    // Footer and wordmark may touch on 640–699dp windows. At 1.4× font
    // scale even a tall phone can need more vertical breathing room.
    heightDp < 700 || fontScale >= 1.4f

/**
 * The supplied splash reference accents the middle verb (e.g. "Stvara."
 * or "Create.") in cyan. Use the localized word boundaries rather than
 * hardcoding translated text, and leave longer slogans unchanged.
 */
internal fun qrexSplashAccentRange(subtitle: String): IntRange? {
    val words = Regex("""\S+""").findAll(subtitle).toList()
    return if (words.size == 3) words[1].range else null
}

/**
 * Width of the decorative bottom track, measured relative to the available
 * footer content width. The supplied reference uses roughly half the content
 * area, rather than the fixed 94dp track used previously. Never overflow a
 * narrow or split-screen container.
 */
internal fun qrexSplashFooterLines(message: String, compactWidth: Boolean): String {
    // The reference separates its lead phrase and supporting promise after
    // the em dash. Keep the original localized text when the phone is narrow.
    val divider = " — "
    return if (!compactWidth && !message.contains('\n') && message.contains(divider)) {
        message.replaceFirst(divider, " —\n")
    } else message
}

internal fun qrexSplashProgressWidthDp(availableWidthDp: Int): Int {
    if (availableWidthDp <= 0) return 0
    return (availableWidthDp * .56f).toInt()
        .coerceIn(80, 220).coerceAtMost(availableWidthDp)
}

/**
 * Reference 01: real Compose Android dark splash — rounded Q/QR logo, two-tone
 * QREX wordmark, translated tagline, sweeping blue wave paths and bottom copy.
 * Marketing boards are framed/angled device renders, not byte-exact PNG
 * baselines; the ten native screenshots are captured independently by CI.
 */
@Composable
internal fun QrexSplashScreen(
    subtitle: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(QrexDesign.backgroundDeep)
    ) {
        val metrics = qrexSplashMetrics(
            maxWidth.value.toInt(), maxHeight.value.toInt(), LocalDensity.current.fontScale
        )
        val compactWidth = maxWidth < 360.dp
        // Capture the outer BoxWithConstraints receiver before entering
        // ColumnScope; Kotlin's Compose DSL markers intentionally hide it.
        val viewportHeight = maxHeight
        Canvas(Modifier.fillMaxSize().testTag("qrex-splash-native-waves")) {
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
                moveTo(0f, h * .095f)
                cubicTo(w * .28f, h * .09f, w * .47f, h * .04f, w * .78f, h * .17f)
                cubicTo(w * 1.07f, h * .30f, w * .72f, h * .41f, w * .37f, h * .38f)
                cubicTo(w * .19f, h * .36f, w * .07f, h * .33f, 0f, h * .37f)
                close()
            }
            drawPath(
                upperRibbon,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF085EF0), Color(0xFF0745BC), Color(0xFF021538)),
                    start = Offset(w * .04f, h * .095f),
                    end = Offset(w * .95f, h * .44f)
                ),
                alpha = .85f
            )
            val upperEdge = Path().apply {
                moveTo(0f, h * .095f)
                cubicTo(w * .28f, h * .09f, w * .47f, h * .04f, w * .78f, h * .17f)
            }
            drawPath(
                upperEdge, color = Color(0xFF23AEFF).copy(alpha = .62f),
                style = Stroke(width = 1.25.dp.toPx())
            )
            // A subdued middle ribbon fills the dark empty region between
            // the reference's upper blue arc and its illuminated footer wave.
            // Native Canvas gradients retain crisp layout on every density.
            val middleRibbon = Path().apply {
                moveTo(0f, h * .47f)
                cubicTo(w * .22f, h * .53f, w * .43f, h * .69f, w * .64f, h * .56f)
                cubicTo(w * .80f, h * .46f, w * .92f, h * .43f, w, h * .45f)
                lineTo(w, h * .72f)
                cubicTo(w * .74f, h * .78f, w * .37f, h * .86f, 0f, h * .71f)
                close()
            }
            drawPath(
                middleRibbon,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF041333), Color(0xFF063883), Color(0xFF020E30)),
                    start = Offset(0f, h * .50f),
                    end = Offset(w, h * .77f)
                ),
                alpha = .63f
            )

            val lowerRibbon = Path().apply {
                // Keep the luminous ribbon BELOW the wordmark/tagline, as in
                // the supplied screenshot; the old .54 crest crossed QREX.
                moveTo(0f, h * .72f)
                cubicTo(w * .34f, h * .66f, w * .57f, h * .73f, w, h * .82f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                lowerRibbon,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF0D67EB), Color(0xFF062CB2), Color(0xFF020B38)),
                    start = Offset(0f, h * .72f),
                    end = Offset(w, h)
                ),
                alpha = .87f
            )
            val lowerEdge = Path().apply {
                moveTo(0f, h * .72f)
                cubicTo(w * .34f, h * .66f, w * .57f, h * .73f, w, h * .82f)
            }
            drawPath(
                lowerEdge, color = Color(0xFF16B6FE).copy(alpha = .7f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            val secondWave = Path().apply {
                moveTo(0f, h * .86f)
                cubicTo(w * .40f, h * .78f, w * .68f, h * .92f, w, h * .88f)
            }
            drawPath(
                secondWave, color = Color(0xFF1F71FB).copy(alpha = .54f),
                style = Stroke(width = 1.dp.toPx())
            )
            // A thin foreground reflection is drawn into the same native
            // canvas. It never covers the QREX wordmark or footer touch area.
            val foregroundReflection = Path().apply {
                moveTo(0f, h * .92f)
                cubicTo(w * .30f, h * .85f, w * .72f, h * .96f, w, h * .91f)
            }
            drawPath(
                foregroundReflection,
                color = QrexDesign.cyan.copy(alpha = .30f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // The original absolute positions must never obscure localized
        // branding or the footer when Android font scaling is increased.
        if (qrexSplashUsesScrollLayout(
                maxHeight.value.toInt(), LocalDensity.current.fontScale
            )) {
            // On a compact but *otherwise fitting* viewport the previous
            // scroll Column started the logo at the top and stranded the
            // footer/progress around mid-screen. The real 1080×1920 emulator
            // capture exposed a large empty region below that progress bar.
            // Keep one scroll container, but let its measured content occupy
            // at least the viewport. SpaceBetween then places the footer
            // at the bottom and the brand in the remaining central area.
            // If font scaling or long translations exceed the viewport,
            // the inner Column expands naturally and the user can scroll.
            Column(
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("qrex-splash-scroll-content"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = viewportHeight)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .testTag("qrex-splash-viewport-distribution"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // The leading flexible gap keeps the logo visually centered
                    // while the footer is pinned close to the lower safe edge.
                    Spacer(Modifier.height(0.dp))
                    QrexSplashBrand(
                        subtitle = subtitle,
                        metrics = metrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                    QrexSplashFooter(
                        compactWidth = compactWidth,
                        modifier = Modifier.fillMaxWidth(),
                        allowFullText = true
                    )
                }
            }
        } else {
            QrexSplashBrand(
                subtitle = subtitle,
                metrics = metrics,
                modifier = Modifier.align(Alignment.Center)
                    .offset(y = metrics.centerOffsetDp.dp)
                    .fillMaxWidth().padding(horizontal = 20.dp)
            )
            QrexSplashFooter(
                compactWidth = compactWidth,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth().padding(horizontal = 30.dp)
                    .padding(bottom = metrics.bottomPaddingDp.dp),
                allowFullText = true
            )
        }
    }
}

@Composable
private fun QrexSplashBrand(
    subtitle: String,
    metrics: QrexSplashMetrics,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Cyan halo outside the canonical vector badge. Draw the illumination
        // natively; never embed the full marketing artwork as an app screen.
        Box(
            Modifier.size((metrics.logoDp + 12).dp)
                .testTag("qrex-splash-brand-halo"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                // Match the actual rounded vector badge radius at each
                // density, including large-tablet and compact layouts.
                val rounding = CornerRadius((metrics.logoDp * .22f).dp.toPx())
                drawRoundRect(
                    color = QrexDesign.cyan.copy(alpha = .10f),
                    cornerRadius = rounding,
                    style = Stroke(width = 10.dp.toPx())
                )
                drawRoundRect(
                    color = QrexDesign.accentBright.copy(alpha = .28f),
                    cornerRadius = rounding,
                    style = Stroke(width = 5.dp.toPx())
                )
                // Thin luminous edge over soft halos, like the original badge.
                drawRoundRect(
                    color = QrexDesign.cyan.copy(alpha = .65f),
                    cornerRadius = rounding,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            Image(
                painter = painterResource(R.drawable.qrex_icon),
                contentDescription = null,
                modifier = Modifier.size(metrics.logoDp.dp)
                    .clip(RoundedCornerShape((metrics.logoDp * .22f).dp))
                    .testTag("qrex-splash-logo")
            )
        }
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
        Spacer(Modifier.height(5.dp))
        Text(
            buildAnnotatedString {
                append(subtitle)
                qrexSplashAccentRange(subtitle)?.let { middle ->
                    addStyle(
                        SpanStyle(color = QrexDesign.cyan),
                        start = middle.first, end = middle.last + 1
                    )
                }
            },
            color = Color.White, fontSize = metrics.taglineSp.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            maxLines = 3,
            modifier = Modifier.testTag("qrex-splash-tagline")
        )
    }
}

@Composable
private fun QrexSplashFooter(
    compactWidth: Boolean,
    modifier: Modifier,
    allowFullText: Boolean = false
) {
    Column(
        modifier = modifier.testTag("qrex-splash-footer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            qrexSplashFooterLines(tr("splash_message"), compactWidth),
            color = Color(0xFFB9CFF4),
            fontSize = if (compactWidth) 13.sp else 17.sp,
            lineHeight = if (compactWidth) 19.sp else 23.sp,
            textAlign = TextAlign.Center,
            maxLines = if (allowFullText) Int.MAX_VALUE else 3
        )
        Spacer(Modifier.height(18.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val widthDp = qrexSplashProgressWidthDp(maxWidth.value.toInt())
            Box(
                Modifier.width(widthDp.dp).height(7.dp)
                    .background(Color(0xFF435778), RoundedCornerShape(8.dp))
                    .testTag("qrex-splash-progress"),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier.fillMaxWidth(.56f).height(7.dp)
                        .background(QrexDesign.cyan, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

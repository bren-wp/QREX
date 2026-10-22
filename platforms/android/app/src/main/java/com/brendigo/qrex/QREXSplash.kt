package com.brendigo.qrex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Actual Android/Compose start screen. Matches the dark brand reference:
 * deep midnight backdrop, sweeping electric-blue arcs, centered QREX icon
 * and wordmark. No mockups or AI-generated screen pixels are bundled.
 *
 * Shown once at cold start only. Never blocks debug screenshot capture or
 * returns during Activity recreation after it has been dismissed.
 */
@Composable
internal fun QrexSplashScreen(
    subtitle: String = "Scan. Create. Share.",
    modifier: Modifier = Modifier
) {
    Box(
        modifier.fillMaxSize().background(QrexDesign.backgroundDeep)
            .semantics { contentDescription = "QREX splash: $subtitle" }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF102D62), QrexDesign.background, QrexDesign.backgroundDeep)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF0757E8).copy(alpha = .65f), Color.Transparent),
                    center = Offset(w * .12f, h * .18f), radius = w * .95f
                ),
                radius = w * .95f, center = Offset(w * .12f, h * .18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF0868FA).copy(alpha = .50f), Color.Transparent),
                    center = Offset(w * 1.1f, h * .93f), radius = w * .94f
                ),
                radius = w * .94f, center = Offset(w * 1.1f, h * .93f)
            )
            drawCircle(
                color = QrexDesign.accentBright.copy(alpha = .6f),
                radius = w * .87f, center = Offset(-w * .48f, h * .27f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = QrexDesign.accent.copy(alpha = .6f),
                radius = w * .93f, center = Offset(w * 1.34f, h * .70f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display exactly the launcher artwork, not a second simplified
            // monochrome graphic on a separately rendered gradient tile.
            Image(
                painter = painterResource(R.drawable.qrex_icon),
                contentDescription = null,
                modifier = Modifier.size(158.dp)
            )
            Spacer(Modifier.height(24.dp))
            // Two-tone mark and copy follow the supplied dark reference;
            // both are actual Compose text (never baked into a mockup image).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("QRE", color = Color.White, fontSize = 52.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("X", color = QrexDesign.accentBright, fontSize = 52.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color.White, fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                if (subtitle.startsWith("Skener."))
                    "Sve što može QR — jednostavno, brzo i bez prijave."
                else "Everything QR — simple, fast and without an account.",
                color = QrexDesign.muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 62.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(width = 44.dp, height = 3.dp)
                .background(QrexDesign.accentBright, RoundedCornerShape(8.dp)))
            Spacer(Modifier.height(16.dp))
            Text("Brendigo", color = QrexDesign.muted, fontSize = 12.sp,
                fontWeight = FontWeight.Medium)
        }
    }
}


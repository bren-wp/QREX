package com.brendigo.qrex

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Verify Android Uri and our platform-neutral safety gate agree on devices. */
@RunWith(AndroidJUnit4::class)
class QREXUrlSecurityAndroidTest {
    @Test fun validWebQrCanBeReviewedOnAnAndroidDevice() {
        assertNotNull(QRContent.safeUrl("https://brendigo.com/path?q=1"))
        assertNotNull(QRContent.safeUrl("https://example.org:443/"))
        assertNotNull(QRContent.safeUrl("https://xn--bcher-kva.example/path"))
    }

    @Test fun invalidWebQrNeverBecomesAnExternalViewIntent() {
        assertNull(QRContent.safeUrl("https://example.org:65536/"))
        assertNull(QRContent.safeUrl("https://user@phishing.example/"))
        assertNull(QRContent.safeUrl("https://%65xample.org/"))
        assertNull(QRContent.safeUrl("intent://open"))
        assertNull(QRContent.safeUrl("javascript:alert(1)"))
    }
}

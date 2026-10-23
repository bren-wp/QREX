package com.brendigo.qrex

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-test fixture reset. Production history deletion intentionally preserves
 * theme and language; this helper restores an isolated empty app state between
 * storage tests without exposing a wipe-all action in the application API.
 */
private fun QREXStore.clearAll(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    if (!clearHistory()) return false
    val legacyCleared = context.getSharedPreferences(
        "FlutterSharedPreferences", android.content.Context.MODE_PRIVATE
    ).edit().clear().commit()
    val preferencesCleared = context.getSharedPreferences(
        "qrex.user", android.content.Context.MODE_PRIVATE
    ).edit().clear().commit()
    if (!legacyCleared || !preferencesCleared) return false
    resetDelegate("historyEnabled", true)
    resetDelegate("lightMode", false)
    resetDelegate("language", "system")
    return true
}

@Suppress("UNCHECKED_CAST")
private fun QREXStore.resetDelegate(name: String, value: Any?) {
    val field = javaClass.getDeclaredField("${name}\$delegate").apply {
        isAccessible = true
    }
    val state = field.get(this) as androidx.compose.runtime.MutableState<Any?>
    state.value = value
}

/** Device-backed regression tests: never write Wi-Fi credentials to plaintext preferences. */
@RunWith(AndroidJUnit4::class)
class QREXStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun renderedUnicodeContactsDecodeExactlyAsGenerated() {
        val qr = QRContent.contact("Šime Željka Kovačić", "+385 91 123 4567",
            "info@example.hr")!!
        val bitmap = generateQR(qr)
        assertNotNull(bitmap)
        try {
            assertEquals(qr, QREXQrDecoder.decodeBitmap(bitmap!!))
        } finally {
            bitmap?.recycle()
        }
    }

    @Test
    fun wifiClipboardSuppressesAndroidSystemPreview() {
        val secret = QRContent.wifi("Guest", "super-private-pass")
        val clip = makeQrClipboardClip(secret)
        assertEquals(secret, clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras?.getBoolean(
            android.content.ClipDescription.EXTRA_IS_SENSITIVE) == true)

        val ordinary = makeQrClipboardClip("https://brendigo.com")
        assertEquals("https://brendigo.com", ordinary.getItemAt(0).text.toString())
        assertFalse(ordinary.description.extras?.getBoolean(
            android.content.ClipDescription.EXTRA_IS_SENSITIVE) == true)
    }

    @Test
    fun galleryExifOrientationPreservesRotatedAndMirroredPixelData() {
        val original = android.graphics.Bitmap.createBitmap(
            2, 3, android.graphics.Bitmap.Config.ARGB_8888
        )
        original.setPixel(0, 0, android.graphics.Color.RED)
        original.setPixel(1, 0, android.graphics.Color.BLUE)
        val rotated = orientGalleryBitmap(
            original.copy(original.config!!, true),
            android.media.ExifInterface.ORIENTATION_ROTATE_90
        )
        assertEquals(3, rotated.width)
        assertEquals(2, rotated.height)

        val mirrored = orientGalleryBitmap(
            original.copy(original.config!!, true),
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL
        )
        assertEquals(android.graphics.Color.BLUE, mirrored.getPixel(0, 0))
        assertEquals(android.graphics.Color.RED, mirrored.getPixel(1, 0))
        rotated.recycle()
        mirrored.recycle()
        original.recycle()
    }

    @Test
    fun malformedSettingsCannotCrashLaunchOrEraseEncryptedHistory() {
        val initial = QREXStore(context)
        assertTrue(initial.clearAll())
        val wifi = QRContent.wifi("Office-Šibenik", "sensitive-test-password")
        assertTrue(initial.save(wifi))
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val originalHistory = prefs.getString("history", null)
        assertNotNull(originalHistory)
        assertFalse(originalHistory!!.contains("sensitive-test-password"))
        assertTrue(prefs.edit()
            .putString("lightMode", "not-a-boolean")
            .putString("historyEnabled", "not-a-boolean")
            .putBoolean("language", true)
            .commit())

        // SharedPreferences typed getters throw ClassCastException by default;
        // QREX must still open, show its encrypted records and avoid rewriting
        // any malformed preference until the user explicitly changes it.
        val reopened = QREXStore(context)
        assertFalse(reopened.lightMode)
        assertTrue(reopened.historyEnabled)
        assertEquals("system", reopened.language)
        assertEquals(wifi, reopened.items.single().raw)
        assertEquals(originalHistory, prefs.getString("history", null))
        assertEquals("not-a-boolean", prefs.getString("lightMode", null))
        assertEquals("not-a-boolean", prefs.getString("historyEnabled", null))
        assertTrue(prefs.getBoolean("language", false))
        assertTrue(reopened.clearAll())
    }

    @Test
    fun deletingStaleFavoriteUsesStableQrIdentityAndRejectsMissingRecords() {
        val store = QREXStore(context)
        assertTrue(store.clearAll())
        val raw = "https://example.org/favorite-delete"
        assertTrue(store.save(raw))
        val previouslySaved = store.items.single()
        assertTrue(previouslySaved.saved)
        assertTrue(store.setSaved(previouslySaved, false))
        assertFalse(store.items.single().saved)
        // The still-open Result may retain the previous QRRecord snapshot.
        assertTrue(store.remove(previouslySaved))
        assertTrue(store.items.isEmpty())
        assertTrue(QREXStore(context).items.isEmpty())
        assertFalse(store.remove(previouslySaved))
        assertTrue(store.clearAll())
    }

    @Test
    fun savedWifiPasswordIsEncryptedAndCanBeRestored() {
        val store = QREXStore(context)
        store.clearAll()
        val raw = QRContent.wifi("Home", "secret_test_pass_2026")
        store.save(raw)
        assertEquals(raw, store.items.firstOrNull()?.raw)
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val json = prefs.getString("history", "") ?: ""
        assertFalse(json.contains("secret_test_pass_2026"))
        assertTrue(json.contains("qrex-gcm:v1:"))
        val reopened = QREXStore(context)
        assertEquals(raw, reopened.items.firstOrNull()?.raw)
        reopened.clearAll()
        assertFalse(prefs.contains("history"))
    }

    @Test
    fun flutterV013HistoryMigratesWithoutLosingRecords() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        prefs.edit().clear().commit()
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        val old = org.json.JSONArray()
            .put(org.json.JSONObject().put("raw", "https://brendigo.com")
                .put("date", "2026-10-09T10:12:13.123")
                .put("saved", true))
            .put(org.json.JSONObject().put("raw", QRContent.wifi("Guest", "private_wifi_888"))
                .put("date", "2026-10-09T10:14:13.123")
                .put("saved", true))
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", old.toString())
            .putBoolean("flutter.qrex_record_v1", false)
            .commit())
        val store = QREXStore(context)
        assertEquals(2, store.items.size)
        assertTrue(store.items.any { it.raw == "https://brendigo.com" && it.saved })
        assertTrue(store.items.any { it.raw.contains("private_wifi_888") && it.saved })
        val protected = prefs.getString("history", "") ?: ""
        assertFalse(protected.contains("private_wifi_888"))
        assertFalse(flutter.contains("flutter.qrex_history_v1"))
        assertFalse(store.historyEnabled)
        store.clearAll()
    }

    @Test
    fun flutterFavoritePromotesExistingUnsavedScanWithoutDroppingRecord() {
        val initial = QREXStore(context)
        assertTrue(initial.clearAll())
        val shared = "https://example.com/shared"
        initial.record(shared)
        assertTrue(initial.items.any { it.raw == shared && !it.saved })
        val legacy = context.getSharedPreferences("FlutterSharedPreferences", 0)
        val rows = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", shared)
            .put("date", "2026-10-09T10:12:13.123")
            .put("saved", true))
        assertTrue(legacy.edit().putString("flutter.qrex_history_v1", rows.toString()).commit())

        val migrated = QREXStore(context)
        assertEquals(1, migrated.items.size)
        assertTrue(migrated.items.single().saved)
        assertFalse(legacy.contains("flutter.qrex_history_v1"))
        assertTrue(QREXStore(context).items.single().saved)
        assertTrue(migrated.clearAll())
    }

    @Test
    fun duplicatedStoredScanMustNotDowngradeFavoriteWhenCanonicalized() {
        val initial = QREXStore(context)
        assertTrue(initial.clearAll())
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val raw = "https://example.com/same-code"
        val records = org.json.JSONArray()
            .put(org.json.JSONObject().put("raw", raw)
                .put("protected", false).put("timestamp", 1L).put("saved", false))
            .put(org.json.JSONObject().put("raw", raw)
                .put("protected", false).put("timestamp", 2L).put("saved", true))
        assertTrue(prefs.edit().putString("history", records.toString()).commit())

        val migrated = QREXStore(context)
        assertEquals(1, migrated.items.size)
        assertTrue(migrated.items.single().saved)
        val persisted = org.json.JSONArray(prefs.getString("history", "[]"))
        assertEquals(1, persisted.length())
        assertTrue(persisted.getJSONObject(0).getBoolean("saved"))
        assertTrue(migrated.clearAll())
    }

    @Test
    fun oldUnencryptedWifiHistoryIsMigratedOnStartup() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        prefs.edit().clear().commit()
        val raw = QRContent.wifi("Office", "legacy_plain_password")
        val json = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", raw).put("timestamp", 123L).put("saved", true))
        assertTrue(prefs.edit().putString("history", json.toString()).commit())
        val reopened = QREXStore(context)
        assertEquals(raw, reopened.items.firstOrNull()?.raw)
        val stored = prefs.getString("history", "") ?: ""
        assertFalse(stored.contains("legacy_plain_password"))
        assertTrue(stored.contains("qrex-gcm:v1:"))
        reopened.clearAll()
    }
    @Test
    fun malformedFlutterHistoryMustNotBeDeletedOrMarkedMigrated() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        prefs.edit().clear().commit()
        flutter.edit().clear().commit()
        val original = "[{\"raw\":\"https://example.com\",\"date\":42}]"
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", original).commit())
        val store = QREXStore(context)
        assertTrue(store.items.isEmpty())
        assertFalse(prefs.getBoolean("flutterMigrationV1", false))
        assertEquals(original, flutter.getString("flutter.qrex_history_v1", null))
        store.clearAll()
        flutter.edit().clear().commit()
    }

    @Test
    fun migrationKeepsOriginalWhenHistoryHasNoRoomForNewRecords() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        prefs.edit().clear().commit()
        flutter.edit().clear().commit()
        val current = org.json.JSONArray()
        for (i in 0 until 250) {
            current.put(org.json.JSONObject()
                .put("raw", "https://example.com/item/$i")
                .put("timestamp", 42L)
                .put("saved", true))
        }
        assertTrue(prefs.edit().putString("history", current.toString()).commit())
        val old = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", "https://example.org/new")
            .put("date", "2026-10-09T10:12:13.123")
            .put("saved", true))
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", old.toString()).commit())
        val store = QREXStore(context)
        assertEquals(250, store.items.size)
        assertFalse(prefs.getBoolean("flutterMigrationV1", false))
        assertEquals(old.toString(), flutter.getString("flutter.qrex_history_v1", null))
        store.clearAll()
        flutter.edit().clear().commit()
    }

    @Test
    fun corruptEncryptedHistoryIsNeverOverwrittenByNewSave() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(prefs.edit().clear().commit())
        assertTrue(flutter.edit().clear().commit())
        val corrupted = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", "qrex-gcm:v1:bad-iv:unreadable")
            .put("timestamp", 99L).put("saved", true)).toString()
        assertTrue(prefs.edit().putString("history", corrupted).commit())
        val store = QREXStore(context)
        assertTrue(store.items.isEmpty())
        store.save("https://example.com/new")
        assertTrue(store.items.isEmpty())
        assertEquals(corrupted, prefs.getString("history", null))
        assertTrue(store.clearAll())
        assertFalse(prefs.contains("history"))
    }

    @Test
    fun deleteAllRemovesUnmigratedFlutterSecretsAndPreventsResurrection() {
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(prefs.edit().clear().commit())
        assertTrue(flutter.edit().clear().commit())
        val old = "[{\"raw\":\"WIFI:T:WPA;S:Private;P:secret;;\",\"date\":false}]"
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", old)
            .putBoolean("flutter.qrex_light_v1", true)
            .putBoolean("flutter.qrex_record_v1", false)
            .commit())
        val store = QREXStore(context)
        assertTrue(store.items.isEmpty())
        assertTrue(store.clearAll())
        assertFalse(flutter.contains("flutter.qrex_history_v1"))
        assertFalse(flutter.contains("flutter.qrex_light_v1"))
        assertFalse(flutter.contains("flutter.qrex_record_v1"))
        assertTrue(QREXStore(context).items.isEmpty())
        assertFalse(prefs.contains("history"))
    }

    @Test
    fun unchangedWifiCiphertextIsNotReencryptedForUnrelatedChanges() {
        val store = QREXStore(context)
        assertTrue(store.clearAll())
        val secret = QRContent.wifi("Office", "keep-encrypted")
        store.save(secret)
        val preferences = context.getSharedPreferences("qrex.user", 0)
        val first = org.json.JSONArray(preferences.getString("history", "[]"))
            .getJSONObject(0).getString("raw")
        assertTrue(first.startsWith("qrex-gcm:v1:"))
        store.save("https://example.com/new")
        val secondHistory = org.json.JSONArray(preferences.getString("history", "[]"))
        val second = (0 until secondHistory.length()).map {
            secondHistory.getJSONObject(it).getString("raw")
        }.single { it.startsWith("qrex-gcm:v1:") }
        assertEquals(first, second)
        assertFalse(preferences.getString("history", "").orEmpty().contains("keep-encrypted"))
        val restored = QREXStore(context)
        assertTrue(restored.items.any { it.raw == secret })
        assertTrue(restored.clearAll())
    }

    @Test
    fun oversizedQrCannotPoisonPersistentHistory() {
        val store = QREXStore(context)
        assertTrue(store.clearAll())
        val malformed = "x".repeat(10001)
        assertFalse(store.save(malformed))
        store.record(malformed)
        assertTrue(store.items.isEmpty())
        assertTrue(QREXStore(context).items.isEmpty())
        assertTrue(store.clearAll())
    }

    @Test
    fun fullSavedHistoryNeverEvictsFavoritesOrAcceptsInvisibleNewEntry() {
        val context = androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val legacy = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(prefs.edit().clear().commit())
        assertTrue(legacy.edit().clear().commit())
        val initial = org.json.JSONArray()
        for (i in 0 until 250) {
            initial.put(org.json.JSONObject()
                .put("raw", "https://example.com/favorite/$i")
                .put("timestamp", i.toLong())
                .put("saved", true))
        }
        assertTrue(prefs.edit().putString("history", initial.toString()).commit())
        val store = QREXStore(context)
        assertEquals(250, store.items.size)
        assertFalse(store.save("https://example.org/251"))
        store.record("https://example.org/auto-scan")
        assertEquals(250, store.items.size)
        assertTrue(store.items.all { it.saved })
        assertTrue(store.items.any { it.raw == "https://example.com/favorite/249" })
        assertEquals(initial.toString(), prefs.getString("history", null))
        assertTrue(store.clearAll())
    }

    @Test
    fun savedQrPdfZipExportContainsRealPrintablePdfAndNoUnsafeEntryNames() {
        val document = java.io.File.createTempFile("qrex-export-", ".zip", context.cacheDir)
        try {
            val records = listOf(
                QRRecord("https://brendigo.com", 0L, true),
                QRRecord(QRContent.wifi("Guest", "private-secret"), 0L, true)
            )
            assertTrue(QREXDocuments.exportSavedPdfZip(
                context, records, android.net.Uri.fromFile(document)
            ))
            var checked = 0
            java.util.zip.ZipInputStream(document.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    assertEquals("QREX-Saved-Codes.pdf", entry.name)
                    val bytes = zip.readBytes()
                    assertTrue(bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray()))
                    assertTrue(bytes.size > 1000)
                    zip.closeEntry()
                    checked++
                }
            }
            // Both native editions now export one multi-page PDF per ZIP.
            assertEquals(1, checked)
            assertFalse(document.readBytes().toString(Charsets.ISO_8859_1).contains("private-secret"))
            assertFalse(QREXDocuments.exportSavedPdfZip(
                context, emptyList(), android.net.Uri.fromFile(document)))
        } finally {
            document.delete()
        }
    }

    @Test
    fun savedQrExportCanProduceStandalonePrintablePdf() {
        val document = java.io.File.createTempFile("qrex-document-", ".pdf", context.cacheDir)
        try {
            val saved = listOf(QRRecord("https://brendigo.com", 0L, true))
            assertTrue(QREXDocuments.exportSavedPdf(
                context, saved, android.net.Uri.fromFile(document)
            ))
            val bytes = document.readBytes()
            assertTrue(bytes.size > 1000)
            assertTrue(bytes.take(5).toByteArray().contentEquals("%PDF-".toByteArray()))
            // Actually render the saved QR page, not just a syntactically
            // valid PDF header. Catches premature bitmap recycling.
            android.os.ParcelFileDescriptor.open(document,
                android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                android.graphics.pdf.PdfRenderer(fd).use { renderer ->
                    assertEquals(1, renderer.pageCount)
                    renderer.openPage(0).use { page ->
                        val rendered = android.graphics.Bitmap.createBitmap(
                            595, 842, android.graphics.Bitmap.Config.ARGB_8888)
                        try {
                            page.render(rendered, null, null,
                                android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val probe = IntArray(439 * 439)
                            rendered.getPixels(probe, 0, 439, 78, 188, 439, 439)
                            // The QR illustration cannot be blank or all white.
                            assertTrue(probe.any { it != android.graphics.Color.WHITE })
                        } finally {
                            rendered.recycle()
                        }
                    }
                }
            }
            assertFalse(QREXDocuments.exportSavedPdf(
                context, emptyList(), android.net.Uri.fromFile(document)
            ))
        } finally {
            document.delete()
        }
    }

    @Test
    fun duplicatePlaintextWifiAfterEncryptedRecordCannotSurviveMigration() {
        val initial = QREXStore(context)
        assertTrue(initial.clearAll())
        val secret = QRContent.wifi("Office", "must-not-remain-plaintext")
        assertTrue(initial.save(secret))
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val protected = org.json.JSONArray(prefs.getString("history", "[]"))
            .getJSONObject(0)
        assertTrue(protected.getBoolean("protected"))
        val duplicate = org.json.JSONObject().put("raw", secret)
            .put("timestamp", 42L).put("saved", false)
        val old = org.json.JSONArray().put(protected).put(duplicate)
        assertTrue(prefs.edit().putString("history", old.toString()).commit())
        assertTrue(prefs.getString("history", "").orEmpty().contains("must-not-remain-plaintext"))

        val restored = QREXStore(context)
        assertEquals(1, restored.items.size)
        assertEquals(secret, restored.items.single().raw)
        assertTrue(restored.items.single().saved)
        val migrated = prefs.getString("history", "").orEmpty()
        assertFalse(migrated.contains("must-not-remain-plaintext"))
        val rows = org.json.JSONArray(migrated)
        assertEquals(1, rows.length())
        assertTrue(rows.getJSONObject(0).getBoolean("protected"))
        assertTrue(QREXStore(context).items.single().saved)
        assertTrue(restored.clearAll())
    }

    @Test
    fun literalCryptoPrefixQrTextDoesNotLockUserHistoryAfterRestart() {
        val store = QREXStore(context)
        assertTrue(store.clearAll())
        // This is ordinary QR text, not an encrypted Wi-Fi record.
        val literal = "qrex-gcm:v1:not-a-secret:not-a-ciphertext"
        assertTrue(store.save(literal))
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val saved = org.json.JSONArray(prefs.getString("history", "[]"))
            .getJSONObject(0)
        assertFalse(saved.getBoolean("protected"))
        assertEquals(literal, saved.getString("raw"))
        val reopened = QREXStore(context)
        assertTrue(reopened.items.any { it.raw == literal && it.saved })
        assertTrue(reopened.save("https://example.org/after-restart"))
        assertEquals(2, reopened.items.size)
        assertTrue(reopened.clearAll())
    }

    @Test
    fun newWifiHistoryEntryExplicitlyMarksProtectedCiphertext() {
        val store = QREXStore(context)
        assertTrue(store.clearAll())
        val secret = QRContent.wifi("Secure guest", "password-never-in-preferences")
        assertTrue(store.save(secret))
        val prefs = context.getSharedPreferences("qrex.user", 0)
        val row = org.json.JSONArray(prefs.getString("history", "[]")).getJSONObject(0)
        assertTrue(row.getBoolean("protected"))
        assertTrue(row.getString("raw").startsWith("qrex-gcm:v1:"))
        assertFalse(prefs.getString("history", "").orEmpty().contains("password-never-in-preferences"))
        val reloaded = QREXStore(context)
        assertTrue(reloaded.items.any { it.raw == secret })
        assertTrue(reloaded.clearAll())
    }

    @Test
    fun flutterMigrationAtomicallyPreservesExistingChoicesAnd250Favorites() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(flutter.edit().clear().commit())
        assertTrue(native.edit().putString("language", "hr")
            .putBoolean("lightMode", false).putBoolean("historyEnabled", true).commit())
        val history = org.json.JSONArray()
        val wifi = QRContent.wifi("Legacy office", "migration-secret-250")
        for (i in 0 until 250) {
            history.put(org.json.JSONObject()
                .put("raw", if (i == 249) wifi else "https://example.org/legacy/$i")
                .put("date", "2026-10-09T10:12:13.123Z")
                .put("saved", true))
        }
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", history.toString())
            .putBoolean("flutter.qrex_light_v1", true)
            .putBoolean("flutter.qrex_record_v1", false).commit())
        val migrated = QREXStore(context)
        assertEquals(250, migrated.items.size)
        assertTrue(migrated.items.all { it.saved })
        assertEquals("hr", migrated.language)
        assertFalse(migrated.lightMode)
        assertTrue(migrated.historyEnabled)
        assertTrue(native.getBoolean("flutterMigrationV1", false))
        val stored = native.getString("history", "")!!
        assertFalse(stored.contains("migration-secret-250"))
        val rows = org.json.JSONArray(stored)
        assertEquals(250, rows.length())
        val encrypted = rows.getJSONObject(249)
        assertTrue(encrypted.getBoolean("protected"))
        assertTrue(encrypted.getString("raw").startsWith("qrex-gcm:v1:"))
        assertEquals(wifi, QREXStore(context).items.last().raw)
        assertFalse(flutter.contains("flutter.qrex_history_v1"))
        assertTrue(migrated.clearAll())
    }

    @Test
    fun migrationAfterInterruptedLegacyCleanupDoesNotDuplicateOrEraseData() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(flutter.edit().clear().commit())
        val raw = QRContent.wifi("Restart", "restart-secret")
        val source = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", raw).put("date", "2026-10-09T10:12:13.123Z")
            .put("saved", true)).toString()
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", source).commit())
        val first = QREXStore(context)
        assertEquals(1, first.items.size)
        val encryptedBefore = native.getString("history", null)
        assertNotNull(encryptedBefore)
        // Simulate power loss after the native atomic commit but before legacy cleanup.
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", source).commit())
        val restarted = QREXStore(context)
        assertEquals(1, restarted.items.size)
        assertEquals(raw, restarted.items.single().raw)
        assertEquals(encryptedBefore, native.getString("history", null))
        assertFalse(flutter.contains("flutter.qrex_history_v1"))
        assertTrue(restarted.clearAll())
    }

    @Test
    fun malformedLegacySettingsLeaveOriginalSourceAndNoCompletionMarker() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val flutter = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(flutter.edit().clear().commit())
        val source = org.json.JSONArray().put(org.json.JSONObject()
            .put("raw", "https://example.org/important")
            .put("date", "2026-10-09T10:12:13.123Z")
            .put("saved", true)).toString()
        assertTrue(flutter.edit().putString("flutter.qrex_history_v1", source)
            .putString("flutter.qrex_light_v1", "not-a-boolean").commit())
        val store = QREXStore(context)
        assertTrue(store.items.isEmpty())
        assertFalse(native.contains("history"))
        assertFalse(native.getBoolean("flutterMigrationV1", false))
        assertEquals(source, flutter.getString("flutter.qrex_history_v1", null))
        assertTrue(store.clearAll())
    }


    @Test
    fun flutterSettingsMigrateWithoutHistoryAndSurviveRestart() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val legacy = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(legacy.edit().clear().commit())
        assertTrue(legacy.edit().putBoolean("flutter.qrex_light_v1", true)
            .putBoolean("flutter.qrex_record_v1", false).commit())
        val first = QREXStore(context)
        assertTrue(first.items.isEmpty())
        assertTrue(first.lightMode)
        assertFalse(first.historyEnabled)
        assertTrue(native.getBoolean("flutterMigrationV1", false))
        val restarted = QREXStore(context)
        assertTrue(restarted.lightMode)
        assertFalse(restarted.historyEnabled)
        assertTrue(restarted.clearAll())
    }

    @Test
    fun settingsOnlyMigrationDoesNotOverrideNativeUserPreferences() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val legacy = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(legacy.edit().clear().commit())
        assertTrue(native.edit().putBoolean("lightMode", false)
            .putBoolean("historyEnabled", true)
            .putString("language", "hr").commit())
        assertTrue(legacy.edit().putBoolean("flutter.qrex_light_v1", true)
            .putBoolean("flutter.qrex_record_v1", false).commit())
        val migrated = QREXStore(context)
        assertFalse(migrated.lightMode)
        assertTrue(migrated.historyEnabled)
        assertEquals("hr", migrated.language)
        assertTrue(migrated.clearAll())
    }

    @Test
    fun corruptSettingsWithoutHistoryDoNotMarkMigrationComplete() {
        val native = context.getSharedPreferences("qrex.user", 0)
        val legacy = context.getSharedPreferences("FlutterSharedPreferences", 0)
        assertTrue(native.edit().clear().commit())
        assertTrue(legacy.edit().clear().commit())
        assertTrue(legacy.edit().putString("flutter.qrex_record_v1", "corrupt").commit())
        val store = QREXStore(context)
        assertTrue(store.items.isEmpty())
        assertFalse(native.getBoolean("flutterMigrationV1", false))
        assertEquals("corrupt", legacy.getString("flutter.qrex_record_v1", null))
        assertTrue(store.clearAll())
    }


}


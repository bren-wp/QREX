package com.brendigo.qrex

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Device-backed regression tests: never write Wi-Fi credentials to plaintext preferences. */
@RunWith(AndroidJUnit4::class)
class QREXStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

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

}

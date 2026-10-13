package com.brendigo.qrex

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class QRRecord(val raw: String, val timestamp: Long, val saved: Boolean)

object QRContent {
    fun isWifi(raw: String): Boolean = raw.startsWith("WIFI:", ignoreCase = true)

    fun wifiField(raw: String, name: String): String? {
        if (!isWifi(raw) || name !in setOf("S", "P", "T", "H")) return null
        val fields = mutableMapOf<String, String>()
        val current = StringBuilder()
        var escaped = false
        fun consume() {
            val text = current.toString()
            val sep = text.indexOf(':')
            if (sep > 0) fields[text.substring(0, sep)] = text.substring(sep + 1)
            current.clear()
        }
        for (char in raw.drop(5)) {
            when {
                escaped -> { current.append(char); escaped = false }
                char.code == 92 -> escaped = true
                char == ';' -> consume()
                else -> current.append(char)
            }
        }
        if (escaped) return null
        if (current.isNotEmpty()) consume()
        return fields[name]
    }

    fun wifi(ssid: String, password: String): String {
        fun escape(text: String): String = buildString {
            for (ch in text) {
                if (ch == ';' || ch == ':' || ch == ',' || ch == '"' || ch.code == 92) append(Char(92))
                append(ch)
            }
        }
        return "WIFI:T:${if (password.isBlank()) "nopass" else "WPA"};S:${escape(ssid)};P:${escape(password)};;"
    }

    fun safeUrl(raw: String): Uri? {
        if (raw != raw.trim() || raw.any { it.isWhitespace() || it.isISOControl() || it.code == 92 }) return null
        return runCatching {
            val uri = Uri.parse(raw)
            if (uri.scheme?.lowercase() !in setOf("http", "https") ||
                uri.host.isNullOrBlank() || (uri.encodedAuthority?.contains("@") == true)) null else uri
        }.getOrNull()
    }

    fun label(raw: String): String = when {
        isWifi(raw) -> wifiField(raw, "S") ?: "Wi-Fi"
        safeUrl(raw) != null -> safeUrl(raw)?.host ?: raw
        else -> raw.replace("\n", " ").take(56)
    }

    fun canRender(raw: String): Boolean =
        raw.isNotBlank() && raw.toByteArray(Charsets.UTF_8).size <= 1500
}

class QREXStore(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("qrex.user", Context.MODE_PRIVATE)
    private val legacyPreferences: SharedPreferences =
        context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
    // Corrupt or temporarily undecryptable history must never be overwritten.
    private var historyReadable = true
    // Reuse authenticated ciphertext of unchanged Wi-Fi records on subsequent
    // edits instead of performing up to 250 Keystore encryptions per tap.
    private val protectedWifi = mutableMapOf<String, String>()
    val items = mutableStateListOf<QRRecord>()
    var historyEnabled by mutableStateOf(preferences.getBoolean("historyEnabled", true))
        private set
    var lightMode by mutableStateOf(preferences.getBoolean("lightMode", false))
        private set
    var language by mutableStateOf(preferences.getString("language", "en") ?: "en")
        private set

    init {
        // Keep the original preference value intact when any entry is corrupt,
        // incomplete or undecryptable. Otherwise a later save could destroy it.
        var upgradeNeeded = false
        runCatching {
            val stored = JSONArray(preferences.getString("history", "[]") ?: "[]")
            require(stored.length() <= 250)
            val seen = mutableSetOf<String>()
            for (i in 0 until stored.length()) {
                val item = stored.getJSONObject(i)
                val value = item.getString("raw")
                val encrypted = QREXSecretCipher.isEncrypted(value)
                val raw = if (encrypted) {
                    QREXSecretCipher.decrypt(value)
                        ?: error("Saved history unavailable for decryption")
                } else value
                require(raw.isNotBlank() && raw.length <= 10000)
                // Keep duplicate identities once; this is a no-loss canonicalization.
                if (!seen.add(raw)) continue
                if (encrypted && QRContent.isWifi(raw)) protectedWifi[raw] = value
                if (QRContent.isWifi(raw) && !encrypted) {
                    upgradeNeeded = true
                    require(QREXSecretCipher.available())
                }
                items += QRRecord(raw, item.getLong("timestamp"), item.optBoolean("saved"))
            }
        }.onFailure {
            historyReadable = false
            items.clear()
            protectedWifi.clear()
        }
        if (upgradeNeeded && historyReadable) {
            // Replace plaintext legacy Wi-Fi records only after encryption and
            // a durable commit. On failure do not let later saves erase them.
            if (!persist()) historyReadable = false
        }
        if (historyReadable) migrateFlutterV1()
    }

    /** One-time on-device migration of the published Flutter SharedPreferences format. */
    private fun migrateFlutterV1() {
        if (preferences.getBoolean("flutterMigrationV1", false)) return
        val old = legacyPreferences
        val oldEntries = old.getString("flutter.qrex_history_v1", null)
        if (oldEntries == null) {
            preferences.edit().putBoolean("flutterMigrationV1", true).apply()
            return
        }
        // Invalid or oversized legacy payloads must remain available for a
        // future retry. Never mark migration complete after a partial parse.
        val migrated = runCatching {
            val data = JSONArray(oldEntries)
            require(data.length() <= 250) { "Legacy history exceeds supported capacity" }
            val parsed = mutableListOf<QRRecord>()
            for (i in 0 until data.length()) {
                val value = data.getJSONObject(i)
                val raw = value.getString("raw")
                require(raw.isNotBlank() && raw.length <= 10000)
                val date = value.getString("date")
                val millis = runCatching { Instant.parse(date).toEpochMilli() }
                    .recoverCatching {
                        LocalDateTime.parse(date).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    }.getOrThrow()
                parsed += QRRecord(raw, millis, value.optBoolean("saved"))
            }
            parsed
        }.getOrNull() ?: return

        val seen = items.map { it.raw }.toMutableSet()
        val merged = items.toMutableList()
        for (item in migrated) {
            if (seen.add(item.raw)) {
                // Preserve every distinct existing and legacy item, or abort.
                if (merged.size >= 250) return
                merged.add(item)
            }
        }
        // Do not delete the old Flutter values or silently drop Wi-Fi secrets
        // if encryption or the durable preferences write is unavailable.
        if (!persist(merged)) return
        items.clear()
        items.addAll(merged)
        val light = old.getBoolean("flutter.qrex_light_v1", false)
        val history = old.getBoolean("flutter.qrex_record_v1", true)
        if (!preferences.contains("lightMode")) updateLightMode(light)
        if (!preferences.contains("historyEnabled")) updateHistoryEnabled(history)
        if (preferences.edit().putBoolean("flutterMigrationV1", true).commit()) {
            old.edit().remove("flutter.qrex_history_v1").commit()
        }
    }

    fun record(raw: String) {
        if (!historyEnabled || QRContent.isWifi(raw) ||
            raw.isBlank() || raw.length > 10000) return
        update(raw, items.find { it.raw == raw }?.saved == true)
    }

    fun save(raw: String): Boolean {
        if (raw.isBlank() || raw.length > 10000) return false
        return update(raw, true)
    }

    private fun update(raw: String, saved: Boolean): Boolean {
        val next = items.filterNot { it.raw == raw }.toMutableList()
        next.add(0, QRRecord(raw, System.currentTimeMillis(), saved))
        if (next.size > 250) {
            val lastUnsaved = next.indexOfLast { !it.saved }
            next.removeAt(if (lastUnsaved >= 0) lastUnsaved else next.lastIndex)
        }
        if (!persist(next)) return false
        items.clear()
        items.addAll(next)
        return true
    }

    fun remove(item: QRRecord): Boolean {
        val next = items.filterNot { it == item }
        if (!persist(next)) return false
        items.clear()
        items.addAll(next)
        return true
    }

    fun clearHistory(): Boolean {
        val next = items.filter { it.saved }
        if (!persist(next)) return false
        items.clear()
        items.addAll(next)
        return true
    }

    /** Explicit erasure also removes untransferred Flutter records and settings. */
    fun clearAll(): Boolean {
        // Remove legacy values first: otherwise they could reappear on next start
        // after qrex.user was cleared and the migration marker was lost.
        val legacyCleared = legacyPreferences.edit()
            .remove("flutter.qrex_history_v1")
            .remove("flutter.qrex_light_v1")
            .remove("flutter.qrex_record_v1")
            .commit()
        if (!legacyCleared || !preferences.edit().clear().commit()) return false
        historyReadable = true
        items.clear()
        protectedWifi.clear()
        historyEnabled = true
        lightMode = false
        language = "en"
        return true
    }

    fun updateLanguage(value: String) {
        require(value == "system" || value in QREXStrings.languages)
        language = value
        preferences.edit().putString("language", value).apply()
    }

    fun updateHistoryEnabled(value: Boolean) {
        historyEnabled = value
        preferences.edit().putBoolean("historyEnabled", value).apply()
    }

    fun updateLightMode(value: Boolean) {
        lightMode = value
        preferences.edit().putBoolean("lightMode", value).apply()
    }

    private fun persist(records: List<QRRecord> = items): Boolean {
        if (!historyReadable) return false
        val array = JSONArray()
        val nextProtected = mutableMapOf<String, String>()
        for (item in records) {
            val value = if (QRContent.isWifi(item.raw)) {
                // Ciphertext already authenticated during load; retaining it
                // for unchanged data avoids expensive KeyStore round-trips.
                val ciphertext = protectedWifi[item.raw]
                    ?: QREXSecretCipher.encrypt(item.raw) ?: return false
                nextProtected[item.raw] = ciphertext
                ciphertext
            } else item.raw
            array.put(JSONObject().put("raw", value)
                .put("timestamp", item.timestamp).put("saved", item.saved))
        }
        if (!preferences.edit().putString("history", array.toString()).commit()) return false
        protectedWifi.clear()
        protectedWifi.putAll(nextProtected)
        return true
    }
}

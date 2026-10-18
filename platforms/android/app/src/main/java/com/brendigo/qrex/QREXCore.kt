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

/** Search saved QR information without exposing Wi-Fi secrets through keyword matches. */
object QRHistory {
    fun matches(record: QRRecord, query: String, savedOnly: Boolean): Boolean {
        if (savedOnly && !record.saved) return false
        if (query.isBlank()) return true
        return QRContent.label(record.raw).contains(query, ignoreCase = true) ||
            (!QRContent.isWifi(record.raw) && record.raw.contains(query, ignoreCase = true))
    }
}

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
        return "WIFI:T:${if (password.isEmpty()) "nopass" else "WPA"};S:${escape(ssid)};P:${escape(password)};;"
    }

    fun safeUrl(raw: String): Uri? {
        if (raw != raw.trim() || raw.any { it.isWhitespace() || it.isISOControl() || it.code == 92 }) return null
        return runCatching {
            val uri = Uri.parse(raw)
            if (uri.scheme?.lowercase() !in setOf("http", "https") ||
                uri.host.isNullOrBlank() || (uri.encodedAuthority?.contains("@") == true)) null else uri
        }.getOrNull()
    }

    fun label(raw: String): String {
        if (isWifi(raw)) return wifiField(raw, "S") ?: "Wi-Fi"
        // Decode URLs once; history filtering can call this for every entry.
        return safeUrl(raw)?.host ?: raw.replace("\n", " ").take(56)
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
                // New entries carry explicit protection metadata. Legacy entries
                // without this field retain the previous prefix-based decoding.
                // Otherwise a harmless QR text starting with "qrex-gcm:v1:"
                // could be mistaken for encrypted Wi-Fi data on the next launch.
                val flag = item.opt("protected")
                val encrypted = when (flag) {
                    null -> QREXSecretCipher.isEncrypted(value)
                    is Boolean -> flag
                    else -> error("Invalid history protection metadata")
                }
                val raw = if (encrypted) {
                    require(QREXSecretCipher.isEncrypted(value))
                    QREXSecretCipher.decrypt(value)
                        ?: error("Saved history unavailable for decryption")
                } else value
                require(!encrypted || QRContent.isWifi(raw))
                require(raw.isNotBlank() && raw.length <= 10000)
                // Duplicate records may disagree on the saved flag: never
                // downgrade a manually saved favorite during canonicalization.
                if (!seen.add(raw)) {
                    // A duplicate legacy plaintext Wi-Fi entry still exists in
                    // the serialized preferences even though the in-memory
                    // list keeps one record. Force a protected rewrite before
                    // treating the migration as complete.
                    if (QRContent.isWifi(raw) && !encrypted) {
                        upgradeNeeded = true
                        require(QREXSecretCipher.available())
                    }
                    val previous = items.indexOfFirst { it.raw == raw }
                    if (previous >= 0 && item.optBoolean("saved") && !items[previous].saved) {
                        items[previous] = items[previous].copy(saved = true)
                        upgradeNeeded = true
                    }
                    continue
                }
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

    /**
     * Legacy Flutter history and native settings become visible in one durable
     * SharedPreferences commit. The original Flutter values are left intact
     * until that commit succeeds, so a killed process can safely retry.
     */
    private fun migrateFlutterV1() {
        if (preferences.getBoolean("flutterMigrationV1", false)) {
            // A process may have died after the native commit and before cleanup.
            // Only the exact migrated legacy history key is eligible for cleanup.
            legacyPreferences.edit().remove("flutter.qrex_history_v1").commit()
            return
        }
        val old = legacyPreferences
        val oldEntries = old.getString("flutter.qrex_history_v1", null)
        if (oldEntries == null) {
            preferences.edit().putBoolean("flutterMigrationV1", true).commit()
            return
        }
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

        val positions = items.mapIndexed { index, record -> record.raw to index }.toMap()
            .toMutableMap()
        val merged = items.toMutableList()
        for (item in migrated) {
            val previous = positions[item.raw]
            if (previous != null) {
                if (item.saved && !merged[previous].saved) {
                    merged[previous] = merged[previous].copy(saved = true)
                }
            } else {
                if (merged.size >= 250) return
                positions[item.raw] = merged.size
                merged.add(item)
            }
        }
        // Malformed preference types must not trigger a partial migration.
        val legacySettings = runCatching {
            old.getBoolean("flutter.qrex_light_v1", false) to
                old.getBoolean("flutter.qrex_record_v1", true)
        }.getOrNull() ?: return
        val ciphertexts = mutableMapOf<String, String>()
        val serialized = JSONArray()
        for (record in merged) {
            val wifi = QRContent.isWifi(record.raw)
            val encoded = if (wifi) {
                protectedWifi[record.raw] ?: ciphertexts[record.raw]
                ?: QREXSecretCipher.encrypt(record.raw) ?: return
            } else record.raw
            if (wifi) ciphertexts[record.raw] = encoded
            serialized.put(JSONObject().put("raw", encoded)
                .put("protected", wifi).put("timestamp", record.timestamp)
                .put("saved", record.saved))
        }
        // History, migration marker and imported settings share a single
        // atomic preferences file. Existing user choices take precedence.
        val editor = preferences.edit()
            .putString("history", serialized.toString())
            .putBoolean("flutterMigrationV1", true)
        val importLight = !preferences.contains("lightMode")
        val importHistory = !preferences.contains("historyEnabled")
        if (importLight) editor.putBoolean("lightMode", legacySettings.first)
        if (importHistory) editor.putBoolean("historyEnabled", legacySettings.second)
        if (!editor.commit()) return
        protectedWifi.clear()
        protectedWifi.putAll(ciphertexts)
        items.clear()
        items.addAll(merged)
        if (importLight) lightMode = legacySettings.first
        if (importHistory) historyEnabled = legacySettings.second
        // A crash here is harmless: the next launch sees the committed marker.
        old.edit().remove("flutter.qrex_history_v1").commit()
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
            // An automatic scan or a new favorite must never evict a favorite.
            // If every previous entry is saved, reject the new entry instead.
            val lastUnsaved = next.indexOfLast { !it.saved }
            if (lastUnsaved <= 0) return false
            next.removeAt(lastUnsaved)
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
                .put("protected", QRContent.isWifi(item.raw))
                .put("timestamp", item.timestamp).put("saved", item.saved))
        }
        if (!preferences.edit().putString("history", array.toString()).commit()) return false
        protectedWifi.clear()
        protectedWifi.putAll(nextProtected)
        return true
    }
}

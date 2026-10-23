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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class QRRecord(val raw: String, val timestamp: Long, val saved: Boolean)

/** Keep the chosen time while changing calendar date. DatePicker already
 * validates the new day/month; never apply the year or month to the old date
 * first (31 January -> February can throw DateTimeException).
 */
internal fun eventDateTimeWithDate(
    current: LocalDateTime, year: Int, monthZeroBased: Int, day: Int
): LocalDateTime = java.time.LocalDate.of(year, monthZeroBased + 1, day)
    .atTime(current.toLocalTime())

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

    fun isEvent(raw: String): Boolean = raw.contains("BEGIN:VEVENT", ignoreCase = true)

    fun event(
        title: String,
        start: LocalDateTime,
        end: LocalDateTime,
        location: String,
        description: String
    ): String? {
        if (title.isBlank() || end <= start) return null
        fun escape(value: String): String = value
            .replace("\\", "\\\\")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")
        fun utc(value: LocalDateTime): String = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT)
            .withZone(ZoneOffset.UTC)
            .format(value.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC))
        return buildString {
            append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Brendigo//QREX//EN\r\n")
            append("BEGIN:VEVENT\r\nSUMMARY:").append(escape(title)).append("\r\n")
            append("DTSTART:").append(utc(start)).append("\r\n")
            append("DTEND:").append(utc(end)).append("\r\n")
            if (location.isNotBlank()) append("LOCATION:").append(escape(location)).append("\r\n")
            if (description.isNotBlank()) append("DESCRIPTION:").append(escape(description)).append("\r\n")
            append("END:VEVENT\r\nEND:VCALENDAR")
        }
    }

    /** Standards-compatible vCard 3.0 fields. QR contacts must preserve
     * Croatian characters, commas, semicolons and newlines during a round trip.
     * No remote contacts API or extra Android permission is required.
     */
    fun contact(name: String, phone: String = "", email: String = ""): String? {
        if (name.isBlank()) return null
        fun escape(value: String) = value.trim().replace("\\", "\\\\")
            .replace("\r\n", "\n").replace('\r', '\n')
            .replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
        return buildString {
            append("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:")
            append(escape(name)).append("\r\n")
            if (phone.isNotBlank()) append("TEL:").append(escape(phone)).append("\r\n")
            if (email.isNotBlank()) append("EMAIL:").append(escape(email)).append("\r\n")
            append("END:VCARD")
        }
    }

    fun contactField(raw: String, field: String): String? {
        if (!raw.startsWith("BEGIN:VCARD", ignoreCase = true) ||
            field !in setOf("FN", "TEL", "EMAIL")) return null
        val encoded = raw.lineSequence().firstOrNull {
            it.startsWith("$field:", ignoreCase = true)
        }?.substringAfter(':')?.trimEnd('\r') ?: return null
        val plain = StringBuilder()
        var escaped = false
        for (char in encoded) {
            when {
                escaped -> {
                    plain.append(if (char == 'n' || char == 'N') '\n' else char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                else -> plain.append(char)
            }
        }
        if (escaped) return null
        return plain.toString()
    }

    fun eventField(raw: String, name: String): String? {
        if (!isEvent(raw) || name !in setOf("SUMMARY", "DTSTART", "DTEND", "LOCATION", "DESCRIPTION")) return null
        val value = raw.lineSequence().firstOrNull {
            it.startsWith("$name:", ignoreCase = true)
        }?.substringAfter(':') ?: return null
        val result = StringBuilder()
        var escaped = false
        for (char in value) {
            when {
                escaped -> {
                    result.append(if (char == 'n' || char == 'N') '\n' else char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                else -> result.append(char)
            }
        }
        if (escaped) result.append('\\')
        return result.toString()
    }

    fun eventTimeMillis(raw: String, name: String): Long? {
        val value = eventField(raw, name) ?: return null
        return runCatching {
            val utc = LocalDateTime.parse(
                value.removeSuffix("Z"),
                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT)
            )
            utc.toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
    }

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
        if (isEvent(raw)) return eventField(raw, "SUMMARY") ?: "Event"
        if (raw.startsWith("mailto:", ignoreCase = true)) {
            return raw.substringAfter(':').substringBefore('?').take(56)
        }
        if (raw.startsWith("tel:", ignoreCase = true)) return raw.substringAfter(':').take(56)
        if (raw.startsWith("geo:", ignoreCase = true)) return raw.substringAfter(':').take(56)
        if (raw.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            return contactField(raw, "FN")?.takeIf { it.isNotBlank() } ?: "Contact"
        }
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
    // SharedPreferences can contain a value of the wrong type after an
    // interrupted old migration or an external restore. Android getters throw
    // ClassCastException in this case. Use safe in-memory defaults, but NEVER
    // silently overwrite or delete the original preferences/history.
    private fun readBoolean(key: String, fallback: Boolean): Boolean =
        runCatching { preferences.getBoolean(key, fallback) }.getOrDefault(fallback)

    private fun readLanguage(): String =
        runCatching { preferences.getString("language", "system") }.getOrNull()
            ?.takeIf { it == "system" || it in QREXStrings.languages } ?: "system"

    var historyEnabled by mutableStateOf(readBoolean("historyEnabled", true))
        private set
    var lightMode by mutableStateOf(readBoolean("lightMode", false))
        private set
    var language by mutableStateOf(readLanguage())
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
            // A Flutter install can contain preferences without any saved QR.
            // Import them in the same durable commit as the migration marker.
            val legacySettings = runCatching {
                old.getBoolean("flutter.qrex_light_v1", false) to
                    old.getBoolean("flutter.qrex_record_v1", true)
            }.getOrNull() ?: return
            val importLight = !preferences.contains("lightMode") &&
                old.contains("flutter.qrex_light_v1")
            val importHistory = !preferences.contains("historyEnabled") &&
                old.contains("flutter.qrex_record_v1")
            val editor = preferences.edit().putBoolean("flutterMigrationV1", true)
            if (importLight) editor.putBoolean("lightMode", legacySettings.first)
            if (importHistory) editor.putBoolean("historyEnabled", legacySettings.second)
            if (!editor.commit()) return
            if (importLight) lightMode = legacySettings.first
            if (importHistory) historyEnabled = legacySettings.second
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

    /**
     * Toggle a saved code without changing its timestamp or reordering history.
     * Preserve encrypted Wi-Fi records and refuse all writes when old history
     * cannot be authenticated/decrypted.
     */
    fun setSaved(item: QRRecord, saved: Boolean): Boolean {
        if (items.none { it.raw == item.raw }) return false
        val next = items.map {
            if (it.raw == item.raw) it.copy(saved = saved) else it
        }
        if (!persist(next)) return false
        items.clear()
        items.addAll(next)
        return true
    }

    fun remove(item: QRRecord): Boolean {
        // History rows can become stale after a favorite toggle or scan
        // refresh. Identity is the raw QR payload, not the old saved flag or
        // timestamp. A missing record is NOT a successful deletion.
        if (items.none { it.raw == item.raw }) return false
        val next = items.filterNot { it.raw == item.raw }
        if (!persist(next)) return false
        items.clear()
        items.addAll(next)
        return true
    }

    /** Explicitly erase the QR history while preserving theme and language choices. */
    fun clearHistory(): Boolean {
        if (!preferences.edit()
                .putString("history", "[]")
                .putBoolean("flutterMigrationV1", true)
                .commit()) return false
        // The migration marker prevents stale legacy records from returning if
        // cleanup is interrupted; the next startup retries this exact removal.
        legacyPreferences.edit().remove("flutter.qrex_history_v1").commit()
        historyReadable = true
        items.clear()
        protectedWifi.clear()
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

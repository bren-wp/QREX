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
    /**
     * LazyColumn keys can be stored in Android's saved UI state. Never use
     * record.raw: Wi-Fi QR codes contain passwords and other QR formats can
     * carry private data. Position disambiguates identical millisecond stamps
     * in filtered lists; no QR payload or digest reaches a saved-state key.
     */
    fun uiRowKey(record: QRRecord, visibleIndex: Int): String =
        "${record.timestamp}:$visibleIndex"

    /**
     * Reference-style History supporting line, but never a raw QR payload.
     * URLs expose scheme/hostname, not credentials, paths or query tokens.
     * Wi-Fi exposes SSID only; saved passwords are never previewed.
     */
    /**
     * java.net.URI works in both JVM unit tests and Android. Reject URL
     * credentials, unsupported schemes and noncanonical whitespace/control
     * characters before returning only the scheme and host. This never
     * exposes paths, queries, fragments or authentication tokens.
     */
    private fun safeWebOrigin(raw: String): String? {
        if (raw != raw.trim() || raw.length > 10000 ||
            raw.any { it.isWhitespace() || it.isISOControl() || it == '\\' }) return null
        return runCatching {
            val uri = java.net.URI(raw)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val host = uri.host
            if (scheme !in setOf("http", "https") || host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.rawAuthority?.contains('@') == true) {
                null
            } else {
                "$scheme://$host"
            }
        }.getOrNull()
    }

    fun preview(raw: String): String? = when {
        QRContent.isWifi(raw) -> QRContent.wifiField(raw, "S")?.take(72)
        safeWebOrigin(raw) != null -> safeWebOrigin(raw)
        qrexExternalKind(raw) == QrexExternalKind.EMAIL ->
            raw.substring(7).substringBefore('?').take(72)
        qrexExternalKind(raw) == QrexExternalKind.PHONE -> raw.substring(4).take(48)
        qrexExternalKind(raw) == QrexExternalKind.LOCATION ->
            QRContent.locationInput(raw).take(72)
        else -> null
    }?.takeIf { it.isNotBlank() && it.none { ch -> ch.isISOControl() } }

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
        // External QR/vCard producers commonly use FN;CHARSET=UTF-8,
        // TEL;TYPE=CELL or EMAIL;TYPE=INTERNET. Accept standard property
        // parameters and folded lines, without interpreting another property.
        val unfolded = raw.replace(Regex("\r?\n[ \t]"), "")
        val encoded = unfolded.lineSequence().firstNotNullOfOrNull { line ->
            val separator = line.indexOf(':')
            if (separator < 0 ||
                !line.substring(0, separator).substringBefore(';')
                    .equals(field, ignoreCase = true)) null
            else line.substring(separator + 1).trimEnd('\r')
        } ?: return null
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
        // VALUE=uri:tel:+... is a standard vCard phone form; dialers
        // expect only the number when the UI constructs its tel: action.
        val value = plain.toString()
        return if (field == "TEL" && value.startsWith("tel:", ignoreCase = true))
            value.substring(4) else value
    }

    fun eventField(raw: String, name: String): String? {
        if (!isEvent(raw) || name !in setOf("SUMMARY", "DTSTART", "DTEND", "LOCATION", "DESCRIPTION")) return null
        // Other calendar applications add LANGUAGE/ALTREP parameters and
        // fold long properties using CRLF + space/tab (RFC 5545).
        val unfolded = raw.replace(Regex("\r?\n[ \t]"), "")
        val value = unfolded.lineSequence().firstNotNullOfOrNull { line ->
            val colon = line.indexOf(':')
            if (colon < 0 || !line.substring(0, colon).substringBefore(';')
                    .equals(name, ignoreCase = true)) null
            else line.substring(colon + 1).trimEnd('\r')
        } ?: return null
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

    /**
     * iCalendar uses UTC only when a timestamp ends in Z. A floating local
     * time has no Z; DTSTART/DTEND;TZID=... explicitly names a time zone.
     * Treating either as UTC shifts real appointments in Calendar.
     */
    fun eventTimeMillis(raw: String, name: String): Long? {
        if (!isEvent(raw) || name !in setOf("DTSTART", "DTEND")) return null
        val line = raw.lineSequence().map { it.trimEnd('\r') }.firstOrNull {
            it.substringBefore(':').substringBefore(';').equals(name, ignoreCase = true)
        } ?: return null
        val colon = line.indexOf(':')
        if (colon < 0) return null
        val header = line.substring(0, colon)
        val value = line.substring(colon + 1)
        val tzid = header.split(';').drop(1).firstOrNull {
            it.substringBefore('=').equals("TZID", ignoreCase = true)
        }?.substringAfter('=', "")
        val isUtc = value.endsWith('Z')
        // Reject ambiguous or non-date-time values instead of silently
        // inventing an offset for an unsupported calendar representation.
        if (value.length != (if (isUtc) 16 else 15)) return null
        if (isUtc && tzid != null) return null
        return runCatching {
            val dateTime = LocalDateTime.parse(
                if (isUtc) value.dropLast(1) else value,
                DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss", Locale.ROOT)
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT)
            )
            val zone = when {
                isUtc -> ZoneOffset.UTC
                tzid != null -> ZoneId.of(tzid)
                else -> ZoneId.systemDefault()
            }
            dateTime.atZone(zone).toInstant().toEpochMilli()
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

    /**
     * Create a standards-compatible geo QR. Real coordinates remain literal,
     * while a typed place/address becomes a map search URI (geo:0,0?q=...).
     */
    fun location(value: String): String? {
        val text = value.trim()
        if (text.isEmpty() || text.length > 500 ||
            text.any { it.isISOControl() }) return null
        val coordinates = text.split(',')
        if (coordinates.size == 2 && text.none { it.isWhitespace() }) {
            val lat = coordinates[0].toDoubleOrNull()
            val lon = coordinates[1].toDoubleOrNull()
            if (lat != null && lon != null && lat.isFinite() && lon.isFinite() &&
                lat in -90.0..90.0 && lon in -180.0..180.0) return "geo:$text"
        }
        val encoded = java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")
        return "geo:0,0?q=$encoded"
    }

    /** Restores a human-readable place when editing a generated map-search QR. */
    fun locationInput(raw: String): String {
        if (!raw.startsWith("geo:", ignoreCase = true)) return raw
        val data = raw.substring(4)
        if (!data.startsWith("0,0?q=", ignoreCase = true) || '&' in data) return data
        return runCatching {
            java.net.URLDecoder.decode(data.substring(6), "UTF-8")
        }.getOrDefault(data)
    }

    /**
     * Strict, platform-independent URL syntax gate for untrusted QR content.
     * Keep this separate from Android Uri so malformed input is covered by
     * ordinary JVM tests as well as connected Android instrumented tests.
     * Unicode IDN domains are validated without silently rewriting the URL.
     */
    internal fun safeWebUrlSyntax(raw: String): Boolean {
        if (raw.length !in 1..10000 || raw != raw.trim() ||
            raw.any { it.isWhitespace() || it.isISOControl() || it.code == 92 }) return false
        return runCatching {
            val strict = java.net.URI(raw)
            if (strict.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") ||
                strict.isOpaque || strict.rawUserInfo != null) return@runCatching false
            val authority = strict.rawAuthority ?: return@runCatching false
            if ('@' in authority || '%' in authority) return@runCatching false
            // Avoid Android Uri interpreting nonnumeric or out-of-range ports
            // as a host with an unrecognized/default port.
            val host: String
            val portText: String?
            if (authority.startsWith("[")) {
                val bracketEnd = authority.indexOf(']')
                if (bracketEnd < 0 || strict.host == null) return@runCatching false
                host = authority.substring(0, bracketEnd + 1)
                portText = when {
                    bracketEnd == authority.lastIndex -> null
                    authority.getOrNull(bracketEnd + 1) == ':' ->
                        authority.substring(bracketEnd + 2)
                    else -> return@runCatching false
                }
            } else {
                val colon = authority.lastIndexOf(':')
                host = if (colon >= 0) authority.substring(0, colon) else authority
                portText = if (colon >= 0) authority.substring(colon + 1) else null
                if (':' in host) return@runCatching false
            }
            if (portText != null && (portText.isEmpty() ||
                portText.any { !it.isDigit() } || portText.toIntOrNull() !in 1..65535)) {
                return@runCatching false
            }
            if (authority.startsWith("[")) return@runCatching strict.host != null
            val asciiHost = java.net.IDN.toASCII(host, java.net.IDN.USE_STD3_ASCII_RULES)
            asciiHost.length in 1..253 && asciiHost.split('.').all { it.length in 1..63 }
        }.getOrDefault(false)
    }

    /** Dispatch validation is independent from the initial scanner result UI. */
    fun safeUrl(raw: String): Uri? {
        if (!safeWebUrlSyntax(raw)) return null
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
        if (raw.startsWith("geo:", ignoreCase = true)) return locationInput(raw).take(56)
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
    // An unreadable or unauthenticated history is read-only until the user
    // explicitly deletes it; never silently replace private data with [].
    private var historyReadable = true
    // Reuse authenticated ciphertext for every unchanged QR payload, not just
    // Wi-Fi. The original Keystore alias and ciphertext format remain stable.
    private val protectedEntries = mutableMapOf<String, String>()
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
    var language by mutableStateOf(readLanguage())
        private set

    // The previous QREX appearance was always dark. Preserve that visual
    // default during upgrades; users may explicitly choose System or Light.
    private fun readThemeMode(): String =
        runCatching { preferences.getString("themeMode", "dark") }.getOrNull()
            ?.takeIf { it in setOf("dark", "light", "system") } ?: "dark"

    var themeMode by mutableStateOf(readThemeMode())
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
                // v1.0.47 protected Wi-Fi only; v1.0.48 protects every
                // record type and preserves existing authenticated ciphertext.
                require(raw.isNotBlank() && raw.length <= 10000)
                // Duplicate records may disagree on the saved flag: never
                // downgrade a manually saved favorite during canonicalization.
                if (!seen.add(raw)) {
                    // Even discarded duplicate plaintext can remain in the
                    // serialized preferences. Rewrite the whole history.
                    if (!encrypted) upgradeNeeded = true
                    val previous = items.indexOfFirst { it.raw == raw }
                    if (previous >= 0 && item.optBoolean("saved") && !items[previous].saved) {
                        items[previous] = items[previous].copy(saved = true)
                        upgradeNeeded = true
                    }
                    continue
                }
                if (encrypted) protectedEntries[raw] = value
                else upgradeNeeded = true
                items += QRRecord(raw, item.getLong("timestamp"), item.optBoolean("saved"))
            }
        }.onFailure {
            historyReadable = false
            items.clear()
            protectedEntries.clear()
        }
        if (upgradeNeeded && historyReadable) {
            // Encrypt all legacy plaintext QR records as one durable file
            // commit. On any failure the original history stays untouched.
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
        // A wrongly typed migration marker must never crash startup or be
        // replaced by a new migration. Leave the original record recoverable.
        val migrationDone = runCatching {
            preferences.getBoolean("flutterMigrationV1", false)
        }.getOrNull() ?: return
        if (migrationDone) {
            // A process may have died after the native commit and before cleanup.
            // Only the exact migrated legacy history key is eligible for cleanup.
            legacyPreferences.edit().remove("flutter.qrex_history_v1").commit()
            return
        }
        val old = legacyPreferences
        // A malformed legacy preference type is not an absent history key.
        // Abort migration without touching or deleting the legacy payload.
        val oldEntries = runCatching {
            old.getString("flutter.qrex_history_v1", null)
        }.getOrElse { return }
        if (oldEntries == null) {
            // A Flutter install can contain preferences without any saved QR.
            // Import them in the same durable commit as the migration marker.
            val legacyHistoryEnabled = runCatching {
                old.getBoolean("flutter.qrex_record_v1", true)
            }.getOrNull() ?: return
            val importHistory = !preferences.contains("historyEnabled") &&
                old.contains("flutter.qrex_record_v1")
            val editor = preferences.edit().putBoolean("flutterMigrationV1", true)
            if (importHistory) editor.putBoolean("historyEnabled", legacyHistoryEnabled)
            if (!editor.commit()) return
            if (importHistory) historyEnabled = legacyHistoryEnabled
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
        val legacyHistoryEnabled = runCatching {
            old.getBoolean("flutter.qrex_record_v1", true)
        }.getOrNull() ?: return
        val ciphertexts = mutableMapOf<String, String>()
        val serialized = JSONArray()
        for (record in merged) {
            val encoded = protectedEntries[record.raw] ?: ciphertexts[record.raw]
                ?: QREXSecretCipher.encrypt(record.raw) ?: return
            ciphertexts[record.raw] = encoded
            serialized.put(JSONObject().put("raw", encoded)
                .put("protected", true).put("timestamp", record.timestamp)
                .put("saved", record.saved))
        }
        // History, migration marker and imported settings share a single
        // atomic preferences file. Existing user choices take precedence.
        val editor = preferences.edit()
            .putString("history", serialized.toString())
            .putBoolean("flutterMigrationV1", true)
        val importHistory = !preferences.contains("historyEnabled")
        if (importHistory) editor.putBoolean("historyEnabled", legacyHistoryEnabled)
        if (!editor.commit()) return
        protectedEntries.clear()
        protectedEntries.putAll(ciphertexts)
        items.clear()
        items.addAll(merged)
        if (importHistory) historyEnabled = legacyHistoryEnabled
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
        protectedEntries.clear()
        return true
    }

    /**
     * Settings are user data, not optimistic UI decoration. Persist first so
     * an interrupted process cannot display a preference that wasn't saved.
     * Never replace an unreadable existing value on failed durable writes.
     */
    fun updateLanguage(value: String): Boolean {
        require(value == "system" || value in QREXStrings.languages)
        if (language == value) return true
        val committed = runCatching {
            preferences.edit().putString("language", value).commit()
        }.getOrDefault(false)
        if (committed) language = value
        return committed
    }

    fun updateThemeMode(value: String): Boolean {
        require(value in setOf("system", "dark", "light"))
        if (themeMode == value) return true
        val committed = runCatching {
            preferences.edit().putString("themeMode", value).commit()
        }.getOrDefault(false)
        if (committed) themeMode = value
        return committed
    }

    fun updateHistoryEnabled(value: Boolean): Boolean {
        if (historyEnabled == value) return true
        val committed = runCatching {
            preferences.edit().putBoolean("historyEnabled", value).commit()
        }.getOrDefault(false)
        if (committed) historyEnabled = value
        return committed
    }

    private fun persist(records: List<QRRecord> = items): Boolean {
        if (!historyReadable) return false
        val array = JSONArray()
        val nextProtected = mutableMapOf<String, String>()
        for (item in records) {
            // Preserve the original authenticated ciphertext for unchanged
            // QR entries, so saving one new scan has O(1) Keystore work.
            val ciphertext = protectedEntries[item.raw]
                ?: QREXSecretCipher.encrypt(item.raw) ?: return false
            nextProtected[item.raw] = ciphertext
            array.put(JSONObject().put("raw", ciphertext)
                .put("protected", true)
                .put("timestamp", item.timestamp).put("saved", item.saved))
        }
        if (!preferences.edit().putString("history", array.toString()).commit()) return false
        protectedEntries.clear()
        protectedEntries.putAll(nextProtected)
        return true
    }
}

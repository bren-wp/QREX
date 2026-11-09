package com.brendigo.qrex

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import java.net.URLDecoder

/**
 * Explicit, user-initiated actions for QR payloads other than web URLs/events.
 * No app contacts/phone/location permission is needed: another app presents
 * its own editor/dialer/maps UI and the user decides whether to proceed.
 */
internal enum class QrexExternalKind { EMAIL, PHONE, LOCATION, CONTACT }

private fun mailRecipient(raw: String): String? {
    if (!raw.startsWith("mailto:", ignoreCase = true)) return null
    // Do not pass QR-supplied ?bcc=, ?cc= or arbitrary mailto headers to
    // another app. The user composes the message in their own email client.
    val address = raw.substring(7).substringBefore('?')
    val local = address.substringBefore('@')
    val domain = address.substringAfter('@', "")
    if (address.length !in 5..254 || address.count { it == '@' } != 1 ||
        local.isBlank() || domain.isBlank() || !domain.contains('.') ||
        address.any { it.isWhitespace() || it.isISOControl() ||
            it in setOf(',', ';', '#', '/', '\\') }) return null
    return address
}

private fun phoneNumber(raw: String): String? {
    if (!raw.startsWith("tel:", ignoreCase = true)) return null
    val number = raw.substring(4)
    // Dialer, never ACTION_CALL. Refuse star/hash USSD and control strings.
    if (number.length !in 3..40 || number.count(Char::isDigit) < 3 ||
        !number.matches(Regex("""\+?[0-9 ()-]+"""))) return null
    return number
}

private fun validGeoUri(raw: String): Boolean {
    if (!raw.startsWith("geo:", ignoreCase = true) || raw.length > 1024 ||
        raw.any { it.isWhitespace() || it.isISOControl() || it == '#' }) return false
    val coordinates = raw.substring(4).substringBefore('?').split(',')
    if (coordinates.size != 2) return false
    val latitude = coordinates[0].toDoubleOrNull() ?: return false
    val longitude = coordinates[1].toDoubleOrNull() ?: return false
    if (!latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    if ('?' in raw) {
        val query = raw.substringAfter('?')
        if (!query.startsWith("q=") || '&' in query || query.length <= 2) return false
        val decoded = runCatching { URLDecoder.decode(query.substring(2), "UTF-8") }.getOrNull()
            ?: return false
        if (decoded.isBlank() || decoded.any { it.isISOControl() }) return false
    }
    return true
}

internal fun qrexExternalKind(raw: String): QrexExternalKind? {
    if (!QRContent.canRender(raw)) return null
    return when {
        mailRecipient(raw) != null -> QrexExternalKind.EMAIL
        phoneNumber(raw) != null -> QrexExternalKind.PHONE
        validGeoUri(raw) -> QrexExternalKind.LOCATION
        raw.startsWith("BEGIN:VCARD", ignoreCase = true) &&
            !QRContent.contactField(raw, "FN").isNullOrBlank() -> QrexExternalKind.CONTACT
        else -> null
    }
}

/** Return false rather than crash if no compatible activity is installed. */
internal fun openQrExternalAction(context: Context, raw: String): Boolean {
    val kind = qrexExternalKind(raw) ?: return false
    val intent = when (kind) {
        QrexExternalKind.EMAIL -> Intent(Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", mailRecipient(raw) ?: return false, null))
        QrexExternalKind.PHONE -> Intent(Intent.ACTION_DIAL,
            Uri.fromParts("tel", phoneNumber(raw) ?: return false, null))
        QrexExternalKind.LOCATION -> Intent(Intent.ACTION_VIEW, Uri.parse(raw))
        QrexExternalKind.CONTACT -> Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME,
                QRContent.contactField(raw, "FN")?.take(200))
            QRContent.contactField(raw, "TEL")?.take(80)?.let {
                putExtra(ContactsContract.Intents.Insert.PHONE, it)
            }
            QRContent.contactField(raw, "EMAIL")?.take(254)?.let {
                putExtra(ContactsContract.Intents.Insert.EMAIL, it)
            }
        }
    }
    return runCatching { context.startActivity(intent); true }.getOrDefault(false)
}

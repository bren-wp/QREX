package com.brendigo.qrex

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Offline user-selected language, no network translation. */
object QREXStrings {
    val languages: List<String> = listOf("en", "hr", "de", "fr", "es", "it", "pt", "nl", "pl", "cs", "sk", "sl", "hu", "ro", "bg", "el", "tr", "uk", "ru", "sv", "da", "fi", "nb")
    fun get(context: Context, code: String, key: String): String {
        val id = when (key) {
        "scan" -> R.string.scan
        "create" -> R.string.create
        "history" -> R.string.history
        "favorites" -> R.string.favorites
        "more" -> R.string.more
        "scan_qr" -> R.string.scan_qr
        "point_camera" -> R.string.point_camera
        "allow_camera" -> R.string.allow_camera
        "enable_camera" -> R.string.enable_camera
        "from_gallery" -> R.string.from_gallery
        "flashlight" -> R.string.flashlight
        "qr_not_found" -> R.string.qr_not_found
        "image_failed" -> R.string.image_failed
        "camera_failed" -> R.string.camera_failed
        "ok" -> R.string.ok
        "scan_complete" -> R.string.scan_complete
        "network" -> R.string.network
        "password" -> R.string.password
        "hide" -> R.string.hide
        "show" -> R.string.show
        "check_url" -> R.string.check_url
        "open_link" -> R.string.open_link
        "copy" -> R.string.copy
        "share" -> R.string.share
        "saved" -> R.string.saved
        "save_qr" -> R.string.save_qr
        "close" -> R.string.close
        "wifi_save_title" -> R.string.wifi_save_title
        "wifi_save_warning" -> R.string.wifi_save_warning
        "save" -> R.string.save
        "cancel" -> R.string.cancel
        "copied" -> R.string.copied
        "share_failed" -> R.string.share_failed
        "link_failed" -> R.string.link_failed
        "create_qr" -> R.string.create_qr
        "enter_details" -> R.string.enter_details
        "url" -> R.string.url
        "text" -> R.string.text
        "wifi" -> R.string.wifi
        "email" -> R.string.email
        "phone" -> R.string.phone
        "location" -> R.string.location
        "contact" -> R.string.contact
        "fewer_options" -> R.string.fewer_options
        "more_options" -> R.string.more_options
        "optional_password" -> R.string.optional_password
        "wifi_warning" -> R.string.wifi_warning
        "qr_preview" -> R.string.qr_preview
        "invalid_content" -> R.string.invalid_content
        "show_qr" -> R.string.show_qr
        "create_share" -> R.string.create_share
        "search_qr" -> R.string.search_qr
        "no_saved" -> R.string.no_saved
        "clear" -> R.string.clear
        "delete" -> R.string.delete
        "settings" -> R.string.settings
        "save_history" -> R.string.save_history
        "light_theme" -> R.string.light_theme
        "delete_all" -> R.string.delete_all
        "privacy" -> R.string.privacy
        "privacy_summary" -> R.string.privacy_summary
        "about" -> R.string.about
        "developed_by" -> R.string.developed_by
        "confirm_delete" -> R.string.confirm_delete
        "delete_warning" -> R.string.delete_warning
        "language" -> R.string.language
        "system" -> R.string.system
        "history_local" -> R.string.history_local
        "export_png" -> R.string.export_png
        "png_save_failed" -> R.string.png_save_failed
        "operation_failed" -> R.string.operation_failed
        "data_tools" -> R.string.data_tools
        "export_archive" -> R.string.export_archive
        "export_archive_hint" -> R.string.export_archive_hint
        "export_archive_warning" -> R.string.export_archive_warning
        "archive_failed" -> R.string.archive_failed
        "archive_ready" -> R.string.archive_ready
        "privacy_policy" -> R.string.privacy_policy
        "terms_of_use" -> R.string.terms_of_use
        "terms_summary" -> R.string.terms_summary
        "read_full_policy" -> R.string.read_full_policy
            else -> R.string.scan
        }
        val locale = if (code == "system") context.resources.configuration.locales[0]
            else Locale.forLanguageTag(code)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config).resources.getString(id)
    }
}

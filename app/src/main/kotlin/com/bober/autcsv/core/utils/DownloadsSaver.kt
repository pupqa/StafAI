package com.bober.autcsv.core.utils

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.bober.autcsv.R
import java.io.File
import java.io.OutputStream

/**
 * Единая запись в системные «Загрузки» через MediaStore: файл виден
 * пользователю без дополнительных разрешений. Раньше эта логика
 * дублировалась в экспорте превью, настройках и бэкапе.
 */
object DownloadsSaver {

    /**
     * Создаёт файл [displayName] типа [mime] в Downloads и записывает его
     * через [write]; возвращает display name для сообщений UI.
     */
    fun save(
        context: Context,
        displayName: String,
        mime: String,
        write: (OutputStream) -> Unit,
    ): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
        ) ?: throw IllegalStateException(context.localizedString(R.string.error_create_downloads))
        context.contentResolver.openOutputStream(uri)?.use(write)
            ?: throw IllegalStateException(context.localizedString(R.string.error_open_for_writing))
        return displayName
    }

    /** Копирует готовый файл [source] в загрузки. */
    fun saveFile(context: Context, displayName: String, mime: String, source: File): String =
        save(context, displayName, mime) { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        }
}

package com.bober.autcsv.core.pdf

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Память стиля PDF-экспорта (№6): последний выбранный шаблон, акцентный
 * цвет и порядок секций переживают перезапуск приложения.
 *
 * Хранение — обычные SharedPreferences: это пользовательские предпочтения
 * оформления, а не секрет (секреты живут в [com.bober.autcsv.core.utils.ApiKeyStore]
 * на EncryptedSharedPreferences).
 */
@Singleton
class PdfStyleStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pdf_style_prefs", Context.MODE_PRIVATE)

    /** Выбранный шаблон; неопознанное значение откатывается к дефолтному. */
    var templateType: PdfTemplateType
        get() = prefs.getString(KEY_TEMPLATE, null)
            ?.let { stored -> PdfTemplateType.entries.firstOrNull { it.name == stored } }
            ?: PdfTemplateType.PROFESSIONAL
        set(value) = prefs.edit().putString(KEY_TEMPLATE, value.name).apply()

    /** Акцентный цвет; null = фирменный цвет шаблона. */
    var accentColor: Int?
        get() = prefs.getInt(KEY_ACCENT, NO_ACCENT).takeIf { it != NO_ACCENT }
        set(value) {
            prefs.edit().putInt(KEY_ACCENT, value ?: NO_ACCENT).apply()
        }

    /** Порядок секций; пустой список = порядок по умолчанию. */
    var sectionOrder: List<PdfSection>
        get() = prefs.getString(KEY_ORDER, null)
            ?.split(',')
            ?.filter { it.isNotBlank() }
            ?.let { PdfSection.fromNames(it) }
            ?: PdfSection.defaultOrder()
        set(value) {
            val allPresent = PdfSection.entries.all { it in value }
            val order = if (allPresent && value.size == PdfSection.entries.size) value
            else PdfSection.fromNames(value.map { it.name })
            prefs.edit().putString(KEY_ORDER, order.joinToString(",") { it.name }).apply()
        }

    /**
     * Встраивать ли полное резюме (JSON) в экспортируемый PDF скрытым каналом
     * для последующего реимпорта. По умолчанию выключено: файл, отправленный
     * рекрутеру, не должен нести зарплату и AI-анализ в невидимом слое.
     */
    var embedResumeData: Boolean
        get() = prefs.getBoolean(KEY_EMBED_DATA, false)
        set(value) {
            prefs.edit().putBoolean(KEY_EMBED_DATA, value).apply()
        }

    private companion object {
        const val KEY_TEMPLATE = "template"
        const val KEY_ACCENT = "accent"
        const val KEY_ORDER = "section_order"
        const val KEY_EMBED_DATA = "embed_resume_data"
        const val NO_ACCENT = Int.MIN_VALUE
    }
}

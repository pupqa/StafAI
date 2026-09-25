package com.bober.autcsv.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.AppLocales
import com.bober.autcsv.domain.model.Resume
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Секции основного столбца PDF; порядок настраивается пользователем.
 * LANGUAGES и SKILLS в двухколоночном шаблоне всегда уезжают в сайдбар.
 */
enum class PdfSection {
    SUMMARY, ABOUT, EDUCATION, LANGUAGES, SKILLS, ACHIEVEMENTS, PROJECTS;

    companion object {
        fun defaultOrder(): List<PdfSection> = entries.toList()

        fun fromNames(names: List<String>): List<PdfSection> {
            val parsed = names.mapNotNull { name -> entries.firstOrNull { it.name == name } }
            // Незаданные секции дописываем в конец, чтобы ничего не потерялось
            return parsed + entries.filter { it !in parsed }
        }
    }
}

/**
 * Генератор PDF-резюме на android.graphics.pdf.PdfDocument.
 *
 * Стиль описывается декларативным [StyleSpec] (палитра + элементы макета),
 * отрисовку ведёт единый набор примитивов разметки. Поддерживается:
 *  - четыре одноколоночных стиля + двухколоночный SIDEBAR (сайдбар с
 *    контактами/языками/навыками повторяется на каждом листе);
 *  - фото кандидата в шапке/сайдбаре ([accentColor], photoUri);
 *  - пользовательский акцентный цвет [accentColor];
 *  - настраиваемый порядок секций [sectionOrder].
 *
 * Обратная совместимость с PdfImportParser (fallback-разбор текста):
 * сохраняются заголовки секций, метки «Опыт:»/«Опыт по специализации:»,
 * префиксы «Проект N:», «Роль:», «Срок:», «Команда:», «Технологии:»,
 * буллеты «•», колонтитул с префиксом бренда и невидимые
 * JSON-маркеры AUTCSV_JSON_START/END (приоритетный канал восстановления).
 */
class AndroidPdfTemplate(
    private val appContext: Context,
    private val style: Style = Style.PROFESSIONAL,
    private val accentColor: Int? = null,
    private val sectionOrder: List<PdfSection> = PdfSection.defaultOrder(),
    /** Встраивать полное резюме скрытым каналом для реимпорта (см. настройки). */
    private val embedResumeData: Boolean = false,
) : PdfTemplate {

    enum class Style { PROFESSIONAL, MODERN, MINIMALIST, CREATIVE, SIDEBAR }

    /** Палитра стиля. */
    private data class Palette(
        val primary: Int,
        val secondary: Int,
        val accent: Int,
        val subtitle: Int,
        val chipBg: Int,
        val chipText: Int,
        val chipBorder: Int,
        val divider: Int,
        val bannerBg: Int = accent,
        val bannerFg: Int = Color.WHITE,
    )

    /** Подчёркивание заголовка секции. */
    private enum class SectionRule { FULL_LINE, SHORT_ACCENT, NONE }

    /** Декларативное описание стиля: палитра + элементы макета. */
    private data class StyleSpec(
        val palette: Palette,
        val sectionTitleColor: Int,
        val sectionRule: SectionRule,
        val sectionAccentBar: Boolean,
        val chipsFilled: Boolean,
        val sideBand: Boolean,
        val topStrip: Boolean,
        val bannerHeader: Boolean,
        val centeredHeader: Boolean,
        val projectDividers: Boolean,
        /** Двухколоночная вёрстка с сайдбаром. */
        val sidebar: Boolean = false,
    )

    private fun styleSpec(style: Style): StyleSpec {
        val base = when (style) {
            Style.PROFESSIONAL -> StyleSpec(
                palette = Palette(
                    primary = Color.rgb(30, 32, 36),
                    secondary = Color.rgb(70, 74, 80),
                    accent = Color.rgb(25, 118, 210),
                    subtitle = Color.rgb(96, 102, 112),
                    chipBg = Color.WHITE,
                    chipText = Color.rgb(35, 55, 86),
                    chipBorder = Color.rgb(197, 212, 233),
                    divider = Color.rgb(224, 227, 231),
                ),
                sectionTitleColor = Color.rgb(30, 32, 36),
                sectionRule = SectionRule.FULL_LINE,
                sectionAccentBar = false,
                chipsFilled = false,
                sideBand = false,
                topStrip = false,
                bannerHeader = false,
                centeredHeader = false,
                projectDividers = true,
            )

            Style.MODERN -> StyleSpec(
                palette = Palette(
                    primary = Color.rgb(17, 20, 30),
                    secondary = Color.rgb(78, 84, 96),
                    accent = Color.rgb(79, 70, 229),
                    subtitle = Color.rgb(107, 114, 128),
                    chipBg = Color.rgb(238, 242, 255),
                    chipText = Color.rgb(67, 56, 202),
                    chipBorder = Color.rgb(199, 210, 254),
                    divider = Color.rgb(229, 231, 235),
                ),
                sectionTitleColor = Color.rgb(17, 20, 30),
                sectionRule = SectionRule.NONE,
                sectionAccentBar = true,
                chipsFilled = true,
                sideBand = true,
                topStrip = false,
                bannerHeader = false,
                centeredHeader = false,
                projectDividers = true,
            )

            Style.MINIMALIST -> StyleSpec(
                palette = Palette(
                    primary = Color.rgb(17, 17, 17),
                    secondary = Color.rgb(64, 64, 64),
                    accent = Color.rgb(64, 64, 64),
                    subtitle = Color.rgb(117, 117, 117),
                    chipBg = Color.WHITE,
                    chipText = Color.rgb(51, 51, 51),
                    chipBorder = Color.rgb(221, 221, 221),
                    divider = Color.rgb(232, 232, 232),
                ),
                sectionTitleColor = Color.rgb(117, 117, 117),
                sectionRule = SectionRule.NONE,
                sectionAccentBar = false,
                chipsFilled = false,
                sideBand = false,
                topStrip = false,
                bannerHeader = false,
                centeredHeader = true,
                projectDividers = false,
            )

            Style.CREATIVE -> StyleSpec(
                palette = Palette(
                    primary = Color.rgb(31, 33, 40),
                    secondary = Color.rgb(76, 80, 90),
                    accent = Color.rgb(124, 58, 237),
                    subtitle = Color.rgb(107, 114, 128),
                    chipBg = Color.rgb(237, 233, 254),
                    chipText = Color.rgb(91, 33, 182),
                    chipBorder = Color.rgb(221, 214, 254),
                    divider = Color.rgb(229, 231, 235),
                    bannerBg = Color.rgb(124, 58, 237),
                    bannerFg = Color.WHITE,
                ),
                sectionTitleColor = Color.rgb(124, 58, 237),
                sectionRule = SectionRule.SHORT_ACCENT,
                sectionAccentBar = false,
                chipsFilled = true,
                sideBand = false,
                topStrip = true,
                bannerHeader = true,
                centeredHeader = false,
                projectDividers = false,
            )

            Style.SIDEBAR -> StyleSpec(
                palette = Palette(
                    primary = Color.rgb(26, 32, 44),
                    secondary = Color.rgb(96, 105, 120),
                    accent = Color.rgb(13, 148, 136),
                    subtitle = Color.rgb(107, 114, 128),
                    chipBg = Color.rgb(240, 253, 250),
                    chipText = Color.rgb(15, 118, 110),
                    chipBorder = Color.rgb(204, 219, 216),
                    divider = Color.rgb(229, 231, 235),
                    bannerBg = Color.rgb(26, 32, 44),
                    bannerFg = Color.WHITE,
                ),
                sectionTitleColor = Color.rgb(26, 32, 44),
                sectionRule = SectionRule.SHORT_ACCENT,
                sectionAccentBar = false,
                chipsFilled = true,
                sideBand = false,
                topStrip = false,
                bannerHeader = false,
                centeredHeader = false,
                projectDividers = false,
                sidebar = true,
            )
        }

        // Пользовательский акцентный цвет перекрывает акцент/плашку/заголовки
        return if (accentColor != null) {
            val p = base.palette.copy(accent = accentColor, bannerBg = accentColor)
            base.copy(
                palette = p,
                sectionTitleColor = if (base.sectionTitleColor == base.palette.accent) accentColor
                else base.sectionTitleColor
            )
        } else base
    }

    /**
     * Склонение числа лет: 1 год / 2 года / 5 лет (год/years — по локали).
     */
    fun pluralYears(value: String): String {
        val n = value.trim().toIntOrNull() ?: return value.trim()
        val ctx = AppLocales.localizedContext(appContext)
        return ctx.resources.getQuantityString(R.plurals.experience_years, n, n)
    }

    override fun generate(resume: Resume, outputFile: File): String {
        val spec = styleSpec(style)
        val palette = spec.palette
        // Локализованный контекст: подписи документа на языке приложения
        val ctx = AppLocales.localizedContext(appContext)
        fun str(resId: Int, vararg args: Any?): String = ctx.getString(resId, *args)
        val docLocale = ctx.resources.configuration.locales[0]

        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val sidebarWidth = if (spec.sidebar) 185f else 0f
        val leftMargin = when {
            spec.sidebar -> sidebarWidth + 22f
            spec.sideBand -> 62f
            else -> 48f
        }
        val rightMargin = 48f
        val topMargin = 56f
        val bottomMargin = 52f
        val contentWidth = pageWidth - leftMargin - rightMargin
        val maxY = pageHeight - bottomMargin
        val dateText = LocalDate.now().toString()
        val footerText =
            str(R.string.pdf_page_footer_fmt, ctx.getString(R.string.app_name), dateText)

        // ── Краски ─────────────────────────────────────────────────────────
        // letterSpacing оставляем нулевым: ненулевой трекинг заставляет Skia
        // рисовать PDF шрифтами Type3 без сопоставления глифов с Юникодом,
        // после чего кириллицу невозможно извлечь из файла текстовым разбором
        fun paint(color: Int, size: Float, typeface: Typeface, spacing: Float = 0f) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                this.typeface = typeface
                letterSpacing = 0f
            }

        val sans = Typeface.SANS_SERIF
        val namePaint = paint(palette.primary, 25f, Typeface.create(sans, Typeface.BOLD))
        val specializationPaint = paint(palette.accent, 12.5f, Typeface.create(sans, Typeface.BOLD))
        val bodyPaint = paint(palette.primary, 10.5f, Typeface.create(sans, Typeface.NORMAL))
        val subtitlePaint = paint(palette.subtitle, 9.5f, Typeface.create(sans, Typeface.NORMAL))
        val sectionPaint = paint(spec.sectionTitleColor, 10f, Typeface.create(sans, Typeface.BOLD))
        val categoryPaint = paint(palette.subtitle, 8.5f, Typeface.create(sans, Typeface.BOLD))
        val chipPaint = paint(palette.chipText, 9f, Typeface.create(sans, Typeface.BOLD))
        val chipSeparatorPaint = paint(palette.secondary, 9f, Typeface.create(sans, Typeface.BOLD))
        val smallPaint = paint(palette.secondary, 8.5f, Typeface.create(sans, Typeface.NORMAL))
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.divider
            strokeWidth = 1f
        }
        val accentRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            strokeWidth = 2.2f
        }
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.chipBg }
        val chipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.chipBorder
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
        }
        val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.bannerBg }
        val sidebarTextPaint =
            paint(Color.rgb(212, 217, 226), 9f, Typeface.create(sans, Typeface.NORMAL))
        val sidebarLabelPaint = paint(palette.accent, 8.5f, Typeface.create(sans, Typeface.BOLD))

        // Для CREATIVE шапка рисуется белым на цветной плашке
        val headerNamePaint = if (spec.bannerHeader)
            paint(palette.bannerFg, 25f, Typeface.create(sans, Typeface.BOLD)) else namePaint
        val headerSubPaint = if (spec.bannerHeader)
            paint(palette.bannerFg, 12.5f, Typeface.create(sans, Typeface.BOLD))
        else specializationPaint
        val headerSmallPaint = if (spec.bannerHeader)
            paint(palette.bannerFg, 9.5f, Typeface.create(sans, Typeface.NORMAL))
        else subtitlePaint
        // Для SIDEBAR текст в тёмном сайдбаре — светлый
        val sidebarHeaderPaint = paint(palette.bannerFg, 17f, Typeface.create(sans, Typeface.BOLD))
        val sidebarSpecPaint = paint(palette.accent, 10f, Typeface.create(sans, Typeface.BOLD))

        // ── Управление страницами ─────────────────────────────────────────
        var pageNumber = 1
        var page = doc.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        val contentLeft = leftMargin

        fun drawPageDecorations() {
            if (spec.sidebar) {
                canvas.drawRect(0f, 0f, sidebarWidth, pageHeight.toFloat(), bannerPaint)
            }
            if (spec.sideBand) {
                canvas.drawRect(0f, 0f, 5f, pageHeight.toFloat(), bandPaint)
            }
            if (spec.topStrip && pageNumber > 1) {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, bandPaint)
            }
            // Единый колонтитул на каждом листе; префикс используется
            // PdfImportParser как признак служебной строки
            canvas.drawText(
                footerText + pageNumber,
                contentLeft,
                pageHeight - 24f,
                smallPaint
            )
        }

        drawPageDecorations()
        var y = topMargin

        fun newPage() {
            doc.finishPage(page)
            pageNumber += 1
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            drawPageDecorations()
            y = topMargin
        }

        fun ensureSpace(required: Float) {
            if (y + required > maxY) newPage()
        }

        // ── Текстовые примитивы ───────────────────────────────────────────
        fun drawLines(
            text: String,
            paint: Paint,
            x: Float = contentLeft,
            width: Float = contentWidth,
            alignCenter: Boolean = false,
            extraLineGap: Float = 0f,
        ) {
            if (text.isBlank()) return
            val lineHeight = paint.textSize * 1.32f + extraLineGap
            wrapText(text, paint, width).forEach { line ->
                ensureSpace(lineHeight)
                y += lineHeight
                val lineWidth = paint.measureText(line)
                val startX = if (alignCenter) x + (width - lineWidth) / 2f else x
                canvas.drawText(line, startX, y - lineHeight * 0.22f, paint)
            }
        }

        fun drawDivider(spacingTop: Float = 14f, spacingBottom: Float = 16f) {
            ensureSpace(spacingTop + spacingBottom + 2f)
            y += spacingTop
            canvas.drawLine(contentLeft, y, contentLeft + contentWidth, y, rulePaint)
            y += spacingBottom
        }

        /** Заголовок секции: капс + трекинг + стилевое оформление. */
        fun drawSectionTitle(title: String) {
            ensureSpace(44f)
            y += 14f
            val titleText = title.uppercase(docLocale)
            var textX = contentLeft
            if (spec.sectionAccentBar) {
                canvas.drawRect(
                    contentLeft, y + 1.5f,
                    contentLeft + 3.5f, y + sectionPaint.textSize + 1f, bandPaint
                )
                textX = contentLeft + 11f
            }
            canvas.drawText(titleText, textX, y + sectionPaint.textSize, sectionPaint)
            y += sectionPaint.textSize + 6.5f
            when (spec.sectionRule) {
                SectionRule.FULL_LINE ->
                    canvas.drawLine(contentLeft, y, contentLeft + contentWidth, y, rulePaint)

                SectionRule.SHORT_ACCENT ->
                    canvas.drawLine(contentLeft, y, contentLeft + 26f, y, accentRulePaint)

                SectionRule.NONE -> Unit
            }
            y += 11f
        }

        /** Подпись подкатегории навыков. */
        fun drawCategoryLabel(text: String) {
            ensureSpace(categoryPaint.textSize + 14f)
            y += 9f
            canvas.drawText(
                text.uppercase(docLocale),
                contentLeft,
                y + categoryPaint.textSize,
                categoryPaint
            )
            y += categoryPaint.textSize + 5f
        }

        /** Маркированный список с висячим отступом. */
        fun drawBullets(items: List<String>) {
            items.filter { it.isNotBlank() }.forEach { item ->
                val indent = 11f
                wrapText(item, bodyPaint, contentWidth - indent).forEachIndexed { i, line ->
                    ensureSpace(bodyPaint.textSize * 1.32f)
                    y += bodyPaint.textSize * 1.32f
                    if (i == 0) canvas.drawText(
                        "•",
                        contentLeft,
                        y - bodyPaint.textSize * 0.22f,
                        subtitlePaint
                    )
                    canvas.drawText(
                        line,
                        contentLeft + indent,
                        y - bodyPaint.textSize * 0.22f,
                        bodyPaint
                    )
                }
                y += 3f
            }
            y += 4f
        }

        /** Чипы-пилюли с построчным переносом и учётом высоты блока. */
        fun drawChips(items: List<String>) {
            if (items.isEmpty()) return
            val padH = 9f
            val chipHeight = 17.5f
            // Зазор шире, чтобы между чипами помещался разделитель «•»:
            // без явного разделителя извлечение текста склеивает чипы в один токен
            val chipGap = 12f
            val rowGap = 6f
            val radius = chipHeight / 2f

            data class Row(val chips: List<String>, val width: Float)

            val rows = mutableListOf<Row>()
            var current = mutableListOf<String>()
            var currentWidth = 0f
            items.forEach { label ->
                val w = chipPaint.measureText(label) + padH * 2
                if (current.isNotEmpty() && currentWidth + chipGap + w > contentWidth) {
                    rows.add(Row(current, currentWidth))
                    current = mutableListOf()
                    currentWidth = 0f
                }
                currentWidth += (if (current.isEmpty()) 0f else chipGap) + w
                current.add(label)
            }
            if (current.isNotEmpty()) rows.add(Row(current, currentWidth))

            val fm = chipPaint.fontMetrics
            rows.forEach { row ->
                ensureSpace(chipHeight + rowGap)
                var cx = contentLeft
                row.chips.forEachIndexed { chipIndex, label ->
                    val w = chipPaint.measureText(label) + padH * 2
                    val rect = RectF(cx, y, cx + w, y + chipHeight)
                    if (spec.chipsFilled) {
                        canvas.drawRoundRect(rect, radius, radius, chipFillPaint)
                    } else {
                        canvas.drawRoundRect(rect, radius, radius, chipStrokePaint)
                    }
                    val baseline = y + (chipHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent
                    canvas.drawText(label, cx + padH, baseline, chipPaint)
                    cx += w
                    if (chipIndex != row.chips.lastIndex) {
                        val bulletWidth = chipSeparatorPaint.measureText("•")
                        canvas.drawText(
                            "•", cx + (chipGap - bulletWidth) / 2f, baseline, chipSeparatorPaint
                        )
                    }
                    cx += chipGap
                }
                y += chipHeight + rowGap
            }
            y += 2f
        }

        /** Круглое фото; безопасно пропускается, если файл недоступен. */
        fun drawCircularPhoto(cx: Float, cy: Float, radius: Float, photoPath: String) {
            if (photoPath.isBlank()) return
            try {
                val file = File(photoPath)
                if (!file.exists()) return
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0) return
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= radius * 2) sample *= 2
                val bitmap = BitmapFactory.decodeFile(
                    file.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: return
                val side = (radius * 2).toInt()
                val square = if (bitmap.width == bitmap.height) {
                    bitmap
                } else {
                    val edge = minOf(bitmap.width, bitmap.height)
                    Bitmap.createBitmap(
                        bitmap,
                        (bitmap.width - edge) / 2,
                        (bitmap.height - edge) / 2,
                        edge,
                        edge
                    )
                }
                val scaled = Bitmap.createScaledBitmap(square, side, side, true)
                val clip = Path().apply {
                    addCircle(cx, cy, radius, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clip)
                canvas.drawBitmap(scaled, cx - radius, cy - radius, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } catch (_: Exception) {
                // Фото — опциональный элемент: ошибки декодирования не роняют PDF
            }
        }

        /** Карточка проекта: имя жирным, мета-строка, технологии, описание, задачи. */
        fun drawProjectCard(
            project: com.bober.autcsv.domain.model.Project,
            index: Int,
            lastIndex: Int,
            specRef: StyleSpec,
        ) {
            ensureSpace(58f)
            drawLines(
                str(R.string.doc_project_fmt, index + 1, project.name),
                bodyPaint.apply { isFakeBoldText = true }
            )
            bodyPaint.isFakeBoldText = false

            val metaParts = buildList {
                if (project.role.isNotBlank()) add(str(R.string.doc_role_fmt, project.role))
                if (project.duration.isNotBlank()) add(
                    str(
                        R.string.doc_duration_fmt,
                        project.duration
                    )
                )
                if (project.teamSize.isNotBlank()) add(str(R.string.doc_team_fmt, project.teamSize))
            }
            if (metaParts.isNotEmpty()) {
                drawLines(metaParts.joinToString("  •  "), subtitlePaint, extraLineGap = 1f)
            }
            if (project.technologies.isNotEmpty()) {
                drawLines(
                    str(R.string.doc_tech_fmt, project.technologies.joinToString(", ")),
                    subtitlePaint,
                    extraLineGap = 1f
                )
            }
            if (project.link.isNotBlank()) {
                drawLines(
                    str(R.string.doc_link_fmt, project.link),
                    subtitlePaint,
                    extraLineGap = 1f
                )
            }
            if (project.description.isNotBlank()) {
                drawLines(project.description, bodyPaint)
            }
            if (project.responsibilities.isNotEmpty()) {
                drawBullets(project.responsibilities)
            }
            if (index < lastIndex) {
                if (specRef.projectDividers) {
                    drawDivider(spacingTop = 8f, spacingBottom = 10f)
                } else {
                    y += 8f
                }
            }
        }

        // ── Данные шапки ──────────────────────────────────────────────────
        val personal = resume.personalInfo
        val fullName = personal.fullName.ifBlank { str(R.string.resume_name_not_specified) }

        val contactLine = buildList {
            if (personal.email.isNotBlank()) add(personal.email)
            if (personal.phone.isNotBlank()) add(personal.phone)
            if (personal.location.isNotBlank()) add(personal.location)
        }.joinToString("  •  ")

        val socialsLine = personal.socialLinks
            .filter { it.platform.isNotBlank() || it.url.isNotBlank() }
            .joinToString("  •  ") { link ->
                when {
                    link.platform.isNotBlank() && link.url.isNotBlank() -> "${link.platform}: ${link.url}"
                    link.platform.isNotBlank() -> link.platform
                    else -> link.url
                }
            }

        // Метки опыта — точный формат, ожидаемый PdfImportParser
        val experienceLine = buildList {
            if (personal.totalExperience.isNotBlank()) add(
                str(
                    R.string.doc_experience_fmt,
                    pluralYears(personal.totalExperience)
                )
            )
            if (personal.specializationExperience.isNotBlank()) {
                add(
                    str(
                        R.string.doc_spec_experience_fmt,
                        pluralYears(personal.specializationExperience)
                    )
                )
            }
        }.joinToString("  •  ")

        fun formatMoney(value: String): String? {
            val digits = value.filter { it.isDigit() }
            if (digits.isEmpty()) return null
            return digits.reversed().chunked(3).joinToString(" ").reversed()
        }

        val salaryLine = buildString {
            val min = formatMoney(personal.salaryMin)
            val max = formatMoney(personal.salaryMax)
            when {
                min != null && max != null -> append(str(R.string.doc_salary_line_full, min, max))
                min != null -> append(str(R.string.doc_salary_line_from, min))
                max != null -> append(str(R.string.doc_salary_line_to, max))
            }
        }.takeIf { it.isNotEmpty() }

        val relocationLine = buildString {
            if (personal.readyToRelocate.isNotBlank()) {
                append(str(R.string.doc_relocation_line, personal.readyToRelocate))
                if (personal.relocationCities.isNotBlank()) {
                    append(str(R.string.doc_cities_suffix, personal.relocationCities))
                }
            }
        }.takeIf { it.isNotEmpty() }

        val employmentLine = buildList {
            if (personal.employment.isNotBlank()) add(
                str(
                    R.string.doc_employment_line,
                    personal.employment
                )
            )
            if (personal.workSchedule.isNotBlank()) add(
                str(
                    R.string.doc_schedule_line,
                    personal.workSchedule
                )
            )
        }.joinToString("  •  ").takeIf { it.isNotEmpty() }

        val headerSmallLines = buildList {
            if (contactLine.isNotBlank()) add(contactLine)
            if (socialsLine.isNotBlank()) add(socialsLine)
            if (experienceLine.isNotBlank()) add(experienceLine)
            salaryLine?.let(::add)
            relocationLine?.let(::add)
            employmentLine?.takeIf { it.isNotBlank() }?.let(::add)
        }

        val skills = resume.professionalSkills
        val hardGroups = buildList {
            if (skills.operatingSystems.isNotEmpty()) add(str(R.string.operating_systems) to skills.operatingSystems)
            if (skills.programmingLanguages.isNotEmpty()) add(str(R.string.programming_languages) to skills.programmingLanguages)
            if (skills.frameworks.isNotEmpty()) add(str(R.string.frameworks) to skills.frameworks)
            if (skills.libraries.isNotEmpty()) add(str(R.string.libraries) to skills.libraries)
            if (skills.databases.isNotEmpty()) add(str(R.string.databases) to skills.databases)
            if (skills.otherTechnologies.isNotEmpty()) add(str(R.string.other_technologies) to skills.otherTechnologies)
        }

        // ── SIDEBAR: двухколоночная вёрстка ───────────────────────────────
        if (spec.sidebar) {
            // Контент сайдбара умещаем на первой странице; при нехватке места
            // оставшиеся пункты навыков доедут в основной столбец
            var sy = 36f
            val sx = 20f
            val sw = sidebarWidth - 40f

            fun sLine(text: String, paint: Paint, gap: Float = 4f) {
                if (text.isBlank()) return
                wrapText(text, paint, sw).forEach { line ->
                    if (sy > maxY) return
                    sy += paint.textSize * 1.3f
                    canvas.drawText(line, sx, sy, paint)
                }
                sy += gap
            }

            fun sLabel(text: String) {
                sy += 8f
                canvas.drawText(
                    text.uppercase(docLocale),
                    sx,
                    sy + sidebarLabelPaint.textSize,
                    sidebarLabelPaint
                )
                sy += sidebarLabelPaint.textSize + 5f
            }

            if (personal.photoUri.isNotBlank()) {
                drawCircularPhoto(sx + sw / 2f, sy + 44f, 44f, personal.photoUri)
                sy += 108f
            }
            sLine(fullName, sidebarHeaderPaint, 2f)
            if (personal.specialization.isNotBlank()) sLine(
                personal.specialization,
                sidebarSpecPaint,
                10f
            )

            if (headerSmallLines.isNotEmpty()) {
                headerSmallLines.forEach { line -> sLine(line, sidebarTextPaint, 5f) }
                sy += 4f
            }

            if (personal.languages.isNotEmpty()) {
                sLabel(str(R.string.section_languages))
                personal.languages.filter { it.name.isNotBlank() }.forEach { lang ->
                    sLine("${lang.name} — ${lang.level}", sidebarTextPaint, 2f)
                }
                sy += 6f
            }

            val sidebarGroups = hardGroups.take(3)
            if (sidebarGroups.isNotEmpty()) {
                sLabel(str(R.string.section_skills))
                sidebarGroups.forEach { (label, values) ->
                    sLine("$label: ${values.joinToString(", ")}", sidebarTextPaint, 6f)
                }
                sy += 6f
            }
            if (skills.certifications.isNotEmpty()) {
                sLabel(str(R.string.pdf_certifications))
                skills.certifications.forEach { sLine(it, sidebarTextPaint, 2f) }
                sy += 6f
            }
            if (skills.softSkills.isNotEmpty()) {
                sLabel(str(R.string.pdf_soft_skills))
                sLine(skills.softSkills.joinToString(", "), sidebarTextPaint, 2f)
            }

            // Основной столбец: секции в пользовательском порядке;
            // LANGUAGES/SKILLS уже в сайдбаре — пропускаем
            y = topMargin
            val mainSections =
                sectionOrder.filter { it != PdfSection.LANGUAGES && it != PdfSection.SKILLS }
            mainSections.forEach { section ->
                when (section) {
                    PdfSection.SUMMARY -> if (resume.summary.isNotBlank()) {
                        drawSectionTitle(str(R.string.doc_prof_resume))
                        drawLines(resume.summary, bodyPaint)
                        y += 4f
                    }

                    PdfSection.ABOUT -> if (personal.aboutMe.isNotBlank()) {
                        drawSectionTitle(str(R.string.about_me))
                        drawLines(personal.aboutMe, bodyPaint)
                        y += 4f
                    }

                    PdfSection.EDUCATION -> if (personal.educations.isNotEmpty()) {
                        drawSectionTitle(str(R.string.education))
                        personal.educations.filter { !it.isBlankEntry() }.forEach { entry ->
                            drawLines(entry.describe(), bodyPaint)
                        }
                        y += 4f
                    } else if (personal.education.isNotBlank()) {
                        drawSectionTitle(str(R.string.education))
                        drawLines(personal.education, bodyPaint)
                        y += 4f
                    }

                    PdfSection.LANGUAGES, PdfSection.SKILLS -> Unit

                    PdfSection.ACHIEVEMENTS -> if (skills.professionalAchievements.isNotEmpty()) {
                        drawSectionTitle(str(R.string.doc_key_achievements))
                        drawBullets(skills.professionalAchievements)
                        y += 4f
                    }

                    PdfSection.PROJECTS -> if (resume.projects.isNotEmpty()) {
                        drawSectionTitle(str(R.string.doc_project_experience))
                        resume.projects.forEachIndexed { index, p ->
                            drawProjectCard(p, index, resume.projects.lastIndex, spec)
                        }
                        y += 4f
                    }
                }
            }
        } else {
            // ── Одноколоночные стили: шапка ───────────────────────────────
            if (spec.bannerHeader) {
                val bannerPadTop = 44f
                val nameLines = wrapText(fullName, headerNamePaint, contentWidth)
                val specLines = if (personal.specialization.isNotBlank()) {
                    wrapText(personal.specialization, headerSubPaint, contentWidth)
                } else emptyList()
                val smallLineCount = headerSmallLines.sumOf { line ->
                    wrapText(line, headerSmallPaint, contentWidth).size
                }
                val bannerHeight = bannerPadTop +
                        nameLines.size * headerNamePaint.textSize * 1.18f +
                        specLines.size * headerSubPaint.textSize * 1.5f +
                        smallLineCount * headerSmallPaint.textSize * 1.55f +
                        26f
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), bannerHeight, bannerPaint)
                y = bannerPadTop
                nameLines.forEach { line ->
                    y += headerNamePaint.textSize * 1.18f
                    canvas.drawText(line, contentLeft, y, headerNamePaint)
                }
                specLines.forEach { line ->
                    y += headerSubPaint.textSize * 1.5f
                    canvas.drawText(line, contentLeft, y, headerSubPaint)
                }
                y += 16f
                headerSmallLines.forEach { line ->
                    drawLines(line, headerSmallPaint)
                }
                y = bannerHeight + 30f
            } else {
                // Фото справа от блока имени для одноколоночных стилей;
                // текст шапки сужается, чтобы не заезжать под фото
                val hasPhoto = personal.photoUri.isNotBlank()
                val headerTextWidth = if (hasPhoto) contentWidth - 92f else contentWidth
                val photoBottom = if (hasPhoto) {
                    drawCircularPhoto(
                        contentLeft + contentWidth - 38f,
                        y + 38f,
                        38f,
                        personal.photoUri
                    )
                    y + 84f
                } else 0f

                val alignCenter = spec.centeredHeader
                wrapText(fullName, headerNamePaint, headerTextWidth).forEach { line ->
                    ensureSpace(headerNamePaint.textSize * 1.18f)
                    y += headerNamePaint.textSize * 1.18f
                    val startX = if (alignCenter) {
                        contentLeft + (headerTextWidth - headerNamePaint.measureText(line)) / 2f
                    } else contentLeft
                    canvas.drawText(line, startX, y, headerNamePaint)
                }
                if (personal.specialization.isNotBlank()) {
                    y += 6f
                    wrapText(
                        personal.specialization,
                        headerSubPaint,
                        headerTextWidth
                    ).forEach { line ->
                        ensureSpace(headerSubPaint.textSize * 1.32f)
                        y += headerSubPaint.textSize * 1.32f
                        val startX = if (alignCenter) {
                            contentLeft + (headerTextWidth - headerSubPaint.measureText(line)) / 2f
                        } else contentLeft
                        canvas.drawText(
                            line,
                            startX,
                            y - headerSubPaint.textSize * 0.22f,
                            headerSubPaint
                        )
                    }
                }
                headerSmallLines.forEach { line ->
                    drawLines(
                        line,
                        headerSmallPaint,
                        x = contentLeft,
                        width = headerTextWidth,
                        extraLineGap = 1.5f
                    )
                }
                y += 10f
                // Контент не должен заходить в зону фото
                if (hasPhoto && y < photoBottom) y = photoBottom
                drawDivider(spacingTop = 6f, spacingBottom = 12f)
            }

            // ── Секции в пользовательском порядке ─────────────────────────
            sectionOrder.forEach { section ->
                when (section) {
                    PdfSection.SUMMARY -> if (resume.summary.isNotBlank()) {
                        drawSectionTitle(str(R.string.doc_prof_resume))
                        drawLines(resume.summary, bodyPaint)
                        y += 4f
                    }

                    PdfSection.ABOUT -> if (personal.aboutMe.isNotBlank()) {
                        drawSectionTitle(str(R.string.about_me))
                        drawLines(personal.aboutMe, bodyPaint)
                        y += 4f
                    }

                    PdfSection.EDUCATION -> if (personal.educations.isNotEmpty()) {
                        drawSectionTitle(str(R.string.education))
                        personal.educations.filter { !it.isBlankEntry() }.forEach { entry ->
                            drawLines(entry.describe(), bodyPaint)
                        }
                        y += 4f
                    } else if (personal.education.isNotBlank()) {
                        drawSectionTitle(str(R.string.education))
                        drawLines(personal.education, bodyPaint)
                        y += 4f
                    }

                    PdfSection.LANGUAGES -> if (personal.languages.isNotEmpty()) {
                        drawSectionTitle(str(R.string.section_languages))
                        personal.languages.filter { it.name.isNotBlank() }.forEach { lang ->
                            drawLines("${lang.name} — ${lang.level}", bodyPaint)
                            y += 2f
                        }
                        y += 2f
                    }

                    PdfSection.SKILLS -> if (hardGroups.isNotEmpty() ||
                        skills.certifications.isNotEmpty() || skills.softSkills.isNotEmpty()
                    ) {
                        drawSectionTitle(str(R.string.doc_technical_skills))
                        hardGroups.forEach { (label, values) ->
                            drawCategoryLabel(label)
                            drawChips(values)
                        }
                        if (skills.certifications.isNotEmpty()) {
                            drawCategoryLabel(str(R.string.pdf_certifications))
                            drawBullets(skills.certifications)
                        }
                        if (skills.softSkills.isNotEmpty()) {
                            drawCategoryLabel(str(R.string.pdf_soft_skills))
                            drawChips(skills.softSkills)
                        }
                        y += 4f
                    }

                    PdfSection.ACHIEVEMENTS -> if (skills.professionalAchievements.isNotEmpty()) {
                        drawSectionTitle(str(R.string.doc_key_achievements))
                        drawBullets(skills.professionalAchievements)
                        y += 4f
                    }

                    PdfSection.PROJECTS -> if (resume.projects.isNotEmpty()) {
                        drawSectionTitle(str(R.string.doc_project_experience))
                        resume.projects.forEachIndexed { index, p ->
                            drawProjectCard(p, index, resume.projects.lastIndex, spec)
                        }
                        y += 4f
                    }
                }
            }
        }

        // ── Невидимые метаданные для восстановления при импорте ───────────
        // JSON сжимается gzip и кодируется в base64: чисто ASCII-строка не
        // зависит от того, как шрифт отображает кириллицу; компактный поток
        // занимает меньше строк, а каждый кусок помечен тегом «#A#NNN#» —
        // декодер собирает его по индексам, отбрасывая вклинившийся при
        // извлечении видимый текст страницы.
        // Канал включается явно в настройках: по умолчанию резюме не
        // дублируется в файле невидимым слоем (приватность при отправке).
        if (embedResumeData) {
            try {
                val json = Gson().toJson(resume)
                val compressed = java.io.ByteArrayOutputStream().use { buffer ->
                    java.util.zip.GZIPOutputStream(buffer)
                        .use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    buffer.toByteArray()
                }
                val encoded = java.util.Base64.getEncoder().encodeToString(compressed)
                val metaPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 2f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                }
                var metaY = pageHeight - 40f
                val chunkSize = 160
                var idx = 0
                while (idx * chunkSize < encoded.length && metaY > topMargin) {
                    val chunk = encoded.substring(
                        idx * chunkSize,
                        minOf((idx + 1) * chunkSize, encoded.length)
                    )
                    canvas.drawText(
                        "#A#${idx.toString().padStart(3, '0')}#$chunk",
                        contentLeft,
                        metaY,
                        metaPaint
                    )
                    metaY -= 7f
                    idx += 1
                }
            } catch (_: Exception) {
                // Встраивание по принципу «лучшее усилие»; ошибки игнорируются
            }
        }

        // finally гарантирует освобождение нативных страниц документа
        // даже при ошибке записи — иначе PdfDocument утекал бы
        try {
            doc.finishPage(page)
            FileOutputStream(outputFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        if (embedResumeData) {
            injectMetadataIntoInfoDictionary(outputFile, Gson().toJson(resume))
        }
        return outputFile.absolutePath
    }

    /**
     * Дублирует метаданные в Info-словарь PDF (пользовательское свойство
     * AUTCSV_JSON) через pdfbox. Это обычная строка в структуре файла:
     * она не зависит от шрифтов и не требует извлечения текста, поэтому
     * канал работает для любого шаблона и любого порядка секций.
     * Любая ошибка оставляет файл без изменений — останется текстовый канал.
     */
    private fun injectMetadataIntoInfoDictionary(file: File, json: String) {
        val temp = File(file.parentFile, file.name + ".meta.tmp")
        runCatching {
            com.tom_roush.pdfbox.pdmodel.PDDocument.load(file).use { document ->
                document.documentInformation.setCustomMetadataValue("AUTCSV_JSON", json)
                document.save(temp)
            }
            if (!temp.renameTo(file)) {
                file.delete()
                if (!temp.renameTo(file)) temp.delete()
            }
        }.onFailure {
            temp.delete()
        }
    }

    /** Перенос по словам с фолбэком на посимвольный перенос сверхдлинных слов. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val prospective = if (current.isEmpty()) word else current.toString() + " " + word
            if (paint.measureText(prospective) <= maxWidth) {
                if (current.isEmpty()) current.append(word) else current.append(" ").append(word)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.flatMap { line ->
            if (paint.measureText(line) <= maxWidth) listOf(line) else hardWrap(
                line,
                paint,
                maxWidth
            )
        }
    }

    private fun hardWrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = start + 1
            while (end <= text.length &&
                paint.measureText(text.substring(start, end)) <= maxWidth
            ) {
                end++
            }
            // Гарантия прогресса: даже одиночный символ шире строки отдаётся целиком
            val lineEnd = maxOf(end - 1, start + 1)
            lines.add(text.substring(start, lineEnd))
            start = lineEnd
        }
        return lines
    }
}

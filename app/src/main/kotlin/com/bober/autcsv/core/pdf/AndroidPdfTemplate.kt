package com.bober.autcsv.core.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bober.autcsv.domain.model.Resume
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class AndroidPdfTemplate(private val style: Style = Style.PROFESSIONAL) : PdfTemplate {

    enum class Style { PROFESSIONAL, MODERN, MINIMALIST, CREATIVE }

    private data class Palette(
        val primary: Int,
        val secondary: Int,
        val accent: Int,
        val subtitle: Int,
        val chipBg: Int,
        val chipText: Int,
        val divider: Int,
    )

    private data class Typography(
        val h1: Float = 24f,
        val h2: Float = 16f,
        val body: Float = 11f,
        val small: Float = 9.5f,
    )

    override fun generate(resume: Resume, outputFile: File): String {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 48f
        val rightMargin = 48f
        val topMargin = 56f
        val bottomMargin = 56f

        val palette = when (style) {
            Style.PROFESSIONAL -> Palette(
                primary = Color.rgb(33, 33, 33),
                secondary = Color.DKGRAY,
                accent = Color.rgb(25, 118, 210),
                subtitle = Color.rgb(90, 90, 90),
                chipBg = Color.rgb(232, 240, 254),
                chipText = Color.rgb(25, 118, 210),
                divider = Color.rgb(210, 210, 210)
            )

            Style.MODERN -> Palette(
                primary = Color.BLACK,
                secondary = Color.DKGRAY,
                accent = Color.rgb(0, 102, 204),
                subtitle = Color.rgb(120, 120, 120),
                chipBg = Color.rgb(245, 247, 250),
                chipText = Color.rgb(0, 102, 204),
                divider = Color.rgb(200, 200, 200)
            )

            Style.MINIMALIST -> Palette(
                primary = Color.BLACK,
                secondary = Color.DKGRAY,
                accent = Color.rgb(66, 165, 245),
                subtitle = Color.GRAY,
                chipBg = Color.rgb(248, 249, 250),
                chipText = Color.DKGRAY,
                divider = Color.rgb(220, 220, 220)
            )

            Style.CREATIVE -> Palette(
                primary = Color.rgb(33, 33, 33),
                secondary = Color.DKGRAY,
                accent = Color.rgb(255, 193, 7),
                subtitle = Color.rgb(100, 100, 100),
                chipBg = Color.rgb(252, 252, 252),
                chipText = Color.rgb(33, 33, 33),
                divider = Color.rgb(210, 210, 210)
            )
        }

        val type = Typography()

        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primary
            textSize = type.body
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primary
            textSize = type.body
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val subtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.subtitle
            textSize = type.body
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val h1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.primary
            textSize = type.h1
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val h2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            textSize = type.h2
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.secondary
            textSize = type.small
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.divider
            strokeWidth = 1.2f
        }
        val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.chipBg
            style = Paint.Style.FILL
        }
        val chipText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.chipText
            textSize = type.body
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        var pageNumber = 1
        var page =
            doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var x = leftMargin
        var y = topMargin
        val contentWidth = pageWidth - leftMargin - rightMargin
        val maxY = pageHeight - bottomMargin

        fun newPage() {
            doc.finishPage(page)
            pageNumber += 1
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            x = leftMargin
            y = topMargin
        }

        fun ensureSpace(required: Float) {
            if (y + required > maxY) newPage()
        }

        fun drawDivider(spacingTop: Float = 12f, spacingBottom: Float = 16f) {
            ensureSpace(spacingTop + spacingBottom + 2f)
            y += spacingTop
            canvas.drawLine(x, y, x + contentWidth, y, dividerPaint)
            y += spacingBottom
        }

        fun drawHeading(text: String) {
            ensureSpace(h1.textSize + 8f)
            y += h1.textSize
            canvas.drawText(text, x, y, h1)
            y += 6f
        }

        fun drawSubheading(text: String) {
            ensureSpace(h2.textSize + 6f)
            y += h2.textSize
            canvas.drawText(text, x, y, h2)
            y += 4f
        }

        fun drawParagraph(text: String, paint: Paint = regular, lineSpacing: Float = 4f) {
            if (text.isBlank()) return
            val lines = wrapText(text, paint, contentWidth)
            lines.forEach { line ->
                ensureSpace(paint.textSize + lineSpacing)
                y += paint.textSize
                canvas.drawText(line, x, y, paint)
                y += lineSpacing
            }
        }

        fun drawBulletList(items: List<String>) {
            items.forEach { item ->
                val text = "• $item"
                drawParagraph(text, regular, 3f)
            }
        }

        fun drawChips(items: List<String>) {
            if (items.isEmpty()) return
            var cx = x
            val paddingH = 10f
            val paddingV = 6f
            val radius = 8f
            items.forEach { label ->
                val labelWidth = chipText.measureText(label)
                val chipWidth = labelWidth + paddingH * 2
                val chipHeight = chipText.textSize + paddingV * 2
                if (cx + chipWidth > x + contentWidth) {
                    ensureSpace(chipHeight + 6f)
                    cx = x
                    y += chipHeight + 6f
                }
                ensureSpace(chipHeight)
                val rect = RectF(cx, y, cx + chipWidth, y + chipHeight)
                canvas.drawRoundRect(rect, radius, radius, chipBg)
                val baseline = y + paddingV + chipText.textSize
                canvas.drawText(label, cx + paddingH, baseline, chipText)
                cx += chipWidth + 8f
            }
            y += chipText.textSize + paddingV * 2 + 6f
        }

        // Заголовок
        val fullName = resume.personalInfo.fullName.ifBlank { "Имя не указано" }
        drawHeading(fullName)
        if (resume.personalInfo.specialization.isNotBlank()) {
            drawParagraph(resume.personalInfo.specialization, subtitle, 6f)
        }
        // Контакты в одну строку
        val contacts = buildList {
            if (resume.personalInfo.email.isNotBlank()) add(resume.personalInfo.email)
            if (resume.personalInfo.phone.isNotBlank()) add(resume.personalInfo.phone)
            if (resume.personalInfo.location.isNotBlank()) add(resume.personalInfo.location)
        }.joinToString("  •  ")
        if (contacts.isNotBlank()) drawParagraph(contacts, subtitle, 8f)

		// Опыт (для импорта: ожидаются точные метки "Опыт:" и "Опыт по специализации:")
		val experienceLine = buildList {
			if (resume.personalInfo.totalExperience.isNotBlank()) {
				add("Опыт: ${resume.personalInfo.totalExperience}")
			}
			if (resume.personalInfo.specializationExperience.isNotBlank()) {
				add("Опыт по специализации: ${resume.personalInfo.specializationExperience}")
			}
		}.joinToString("  •  ")
		if (experienceLine.isNotBlank()) drawParagraph(experienceLine, subtitle, 8f)

        drawDivider()

        // Резюме (краткое описание)
        if (resume.summary.isNotBlank()) {
            drawSubheading("Профессиональное резюме")
            drawParagraph(resume.summary)
        }

        // О себе
        if (resume.personalInfo.aboutMe.isNotBlank()) {
            drawSubheading("О себе")
            drawParagraph(resume.personalInfo.aboutMe)
        }

        // Образование
        if (resume.personalInfo.education.isNotBlank()) {
            drawSubheading("Образование")
            drawParagraph(resume.personalInfo.education)
        }

        // Языки
        if (resume.personalInfo.languages.isNotEmpty()) {
            drawSubheading("Языки")
            resume.personalInfo.languages.forEach { lang ->
                drawParagraph("${lang.name} — ${lang.level}")
            }
        }

        // Технические навыки
        val skills = resume.professionalSkills
        if (
            skills.operatingSystems.isNotEmpty() ||
            skills.programmingLanguages.isNotEmpty() ||
            skills.frameworks.isNotEmpty() ||
            skills.libraries.isNotEmpty() ||
            skills.databases.isNotEmpty() ||
            skills.otherTechnologies.isNotEmpty() ||
            skills.softSkills.isNotEmpty() ||
            skills.certifications.isNotEmpty()
        ) {
            drawSubheading("Технические навыки")
            if (skills.operatingSystems.isNotEmpty()) {
                drawParagraph("Операционные системы")
                drawChips(skills.operatingSystems)
            }
            if (skills.programmingLanguages.isNotEmpty()) {
                drawParagraph("Языки программирования")
                drawChips(skills.programmingLanguages)
            }
            if (skills.frameworks.isNotEmpty()) {
                drawParagraph("Фреймворки")
                drawChips(skills.frameworks)
            }
            if (skills.libraries.isNotEmpty()) {
                drawParagraph("Библиотеки")
                drawChips(skills.libraries)
            }
            if (skills.databases.isNotEmpty()) {
                drawParagraph("Базы данных")
                drawChips(skills.databases)
            }
            if (skills.otherTechnologies.isNotEmpty()) {
                drawParagraph("Другие технологии")
                drawChips(skills.otherTechnologies)
            }
            if (skills.certifications.isNotEmpty()) {
                drawSubheading("Сертификации")
                drawBulletList(skills.certifications)
            }
            if (skills.softSkills.isNotEmpty()) {
                drawParagraph("Гибкие навыки")
                drawChips(skills.softSkills)
            }
        }

        // Достижения
        if (skills.professionalAchievements.isNotEmpty()) {
            drawSubheading("Ключевые достижения")
            drawBulletList(skills.professionalAchievements)
        }

        // Проекты
        if (resume.projects.isNotEmpty()) {
            drawSubheading("Проектный опыт")
            resume.projects.forEachIndexed { index, p ->
                drawParagraph("Проект ${index + 1}: ${p.name}", h2)
                val parts = mutableListOf<String>()
                if (p.role.isNotBlank()) parts.add("Роль: ${p.role}")
                if (p.duration.isNotBlank()) parts.add("Срок: ${p.duration}")
                if (p.teamSize.isNotBlank()) parts.add("Команда: ${p.teamSize}")
                if (p.technologies.isNotEmpty()) parts.add(
                    "Технологии: ${
                        p.technologies.joinToString(
                            ", "
                        )
                    }"
                )
                if (parts.isNotEmpty()) drawParagraph(parts.joinToString("  •  "), subtitle)
                if (p.description.isNotBlank()) drawParagraph(p.description)
                if (p.responsibilities.isNotEmpty()) drawBulletList(p.responsibilities)
                drawDivider(6f, 10f)
            }
        }

        // Нижний колонтитул
        ensureSpace(20f)
        val footerY = maxY
        canvas.drawText("AutCSV • ${java.time.LocalDate.now()}", x, footerY, small)

        // Встраивание невидимых JSON‑метаданных для повышения точности импорта
        try {
            val json = Gson().toJson(resume)
            val markerStart = "AUTCSV_JSON_START"
            val markerEnd = "AUTCSV_JSON_END"
            val metadata = markerStart + json + markerEnd
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE // невидимо на белом фоне
                textSize = 1.0f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            // Запись метаданных очень мелким белым текстом внизу страницы
            var metaY = maxY - 4f
            val metaChunkSize = 120
            var idx = 0
            while (idx < metadata.length) {
                val chunk =
                    metadata.substring(idx, kotlin.math.min(idx + metaChunkSize, metadata.length))
                canvas.drawText(chunk, x, metaY, metaPaint)
                metaY -= 2f
                idx += metaChunkSize
                if (metaY < topMargin + 4f) break
            }
        } catch (_: Exception) {
            // Встраивание по принципу «лучшее усилие»; ошибки игнорируются
        }

        doc.finishPage(page)
        FileOutputStream(outputFile).use { doc.writeTo(it) }
        doc.close()
        return outputFile.absolutePath
    }

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
            while (end <= text.length && paint.measureText(
                    text.substring(
                        start,
                        end
                    )
                ) <= maxWidth
            ) {
                end++
            }
            val line = text.substring(start, end - 1)
            lines.add(line)
            start = end - 1
        }
        return lines
    }
}


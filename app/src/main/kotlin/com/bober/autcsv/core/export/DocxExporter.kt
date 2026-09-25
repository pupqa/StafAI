package com.bober.autcsv.core.export

import android.content.Context
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.AppLocales
import com.bober.autcsv.domain.model.Resume
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Экспорт резюме в DOCX (№11): редактируемый формат, который просят рекрутеры.
 *
 * DOCX — это zip-архив из XML-частей (OOXML). Собираем минимальный валидный
 * пакет без внешних библиотек: [Content_Types].xml, связи, стили и document.xml
 * с заголовками/абзацами/списками. Полученный файл открывается в Word,
 * LibreOffice и Google Docs. Заголовки секций локализуются по языку приложения.
 */
object DocxExporter {

    /** Собирает DOCX для резюме и записывает в [outputFile]; возвращает путь. */
    fun export(context: Context, resume: Resume, outputFile: File): String {
        val ctx = AppLocales.localizedContext(context)
        val documentXml = buildDocumentXml(ctx, resume)
        val langTag =
            if (ctx.resources.configuration.locales[0].language == "ru") "ru-RU" else "en-US"
        writeOoxmlPackage(documentXml, langTag, outputFile)
        return outputFile.absolutePath
    }

    /**
     * DOCX сопроводительного письма: необязательный заголовок и абзацы
     * по строкам текста. Язык документа определяется по [langTag].
     */
    fun exportLetter(
        langTag: String,
        title: String?,
        body: String,
        outputFile: File,
    ): String {
        val bodyXml = StringBuilder().apply {
            title?.takeIf { it.isNotBlank() }?.let { heading(it, 26) }
            body.split('\n').forEach { line ->
                if (line.isBlank()) spacing() else paragraph(line.trim())
            }
        }
        writeOoxmlPackage(bodyXml.toString(), langTag, outputFile)
        return outputFile.absolutePath
    }

    /** Общий сборщик OOXML-пакета для резюме и письма. */
    private fun writeOoxmlPackage(
        documentXml: String,
        langTag: String,
        outputFile: File,
    ) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES.trim().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS.trim().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zip.write(DOCUMENT_RELS.trim().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/styles.xml"))
            zip.write(STYLES.trim().replace("__LANG__", langTag).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.trim().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    // ── Строители document.xml ────────────────────────────────────────────

    private fun buildDocumentXml(ctx: Context, resume: Resume): String {
        val p = resume.personalInfo
        val s = resume.professionalSkills

        fun str(resId: Int, vararg args: Any?): String = ctx.getString(resId, *args)

        val body = StringBuilder().apply {
            heading(p.fullName.ifBlank { str(R.string.preview_fallback_title) }, 28) // имя, 14pt
            if (p.specialization.isNotBlank()) paragraph(p.specialization, bold = true)

            metaLine(ctx, p)?.let { paragraph(it, size = 20, color = "595959") }

            socialsLine(p)?.let { paragraph(it, size = 20, color = "595959") }

            section(str(R.string.doc_prof_resume)) { line(resume.summary) }
            section(str(R.string.about_me)) { line(p.aboutMe) }

            if (p.languages.isNotEmpty()) {
                section(str(R.string.section_languages)) {
                    p.languages.filter { it.name.isNotBlank() }.forEach {
                        bullet("${it.name} — ${it.level}")
                    }
                }
            }

            val groups = listOf(
                str(R.string.operating_systems) to s.operatingSystems,
                str(R.string.programming_languages) to s.programmingLanguages,
                str(R.string.frameworks) to s.frameworks,
                str(R.string.libraries) to s.libraries,
                str(R.string.databases) to s.databases,
                str(R.string.other_technologies) to s.otherTechnologies,
            ).filter { it.second.isNotEmpty() }

            if (groups.isNotEmpty() || s.certifications.isNotEmpty() || s.softSkills.isNotEmpty()) {
                section(str(R.string.doc_technical_skills)) {
                    groups.forEach { (label, values) ->
                        paragraph("$label: ${values.joinToString(", ")}")
                    }
                    if (s.certifications.isNotEmpty()) {
                        paragraph(
                            str(
                                R.string.doc_certifications_fmt,
                                s.certifications.joinToString(", ")
                            )
                        )
                    }
                    if (s.softSkills.isNotEmpty()) {
                        paragraph(
                            str(
                                R.string.doc_soft_skills_fmt,
                                s.softSkills.joinToString(", ")
                            )
                        )
                    }
                }
            }

            section(str(R.string.education)) { line(p.education) }

            if (s.professionalAchievements.isNotEmpty()) {
                section(str(R.string.doc_key_achievements)) {
                    s.professionalAchievements.filter { it.isNotBlank() }.forEach { bullet(it) }
                }
            }

            if (resume.projects.isNotEmpty()) {
                section(str(R.string.doc_project_experience)) {
                    resume.projects.forEachIndexed { index, project ->
                        heading(str(R.string.doc_project_fmt, index + 1, project.name), 22)
                        val meta = listOf(
                            project.role.takeIf { it.isNotBlank() }
                                ?.let { str(R.string.doc_role_fmt, it) },
                            project.duration.takeIf { it.isNotBlank() }
                                ?.let { str(R.string.doc_duration_fmt, it) },
                            project.teamSize.takeIf { it.isNotBlank() }
                                ?.let { str(R.string.doc_team_fmt, it) },
                        ).filterNotNull().joinToString("  •  ")
                        if (meta.isNotEmpty()) paragraph(meta, color = "595959")
                        if (project.technologies.isNotEmpty()) {
                            paragraph(
                                str(
                                    R.string.doc_tech_fmt,
                                    project.technologies.joinToString(", ")
                                )
                            )
                        }
                        if (project.link.isNotBlank()) {
                            paragraph(
                                str(R.string.doc_link_fmt, project.link),
                                size = 20,
                                color = "595959"
                            )
                        }
                        line(project.description)
                        project.responsibilities.filter { it.isNotBlank() }.forEach { bullet(it) }
                        if (index < resume.projects.lastIndex) spacing()
                    }
                }
            }
        }

        return DOCUMENT_WRAPPER.trim().format(body)
    }

    /** Шапка с контактом/опытом/зарплатой — как в PDF-шаблонах. */
    private fun metaLine(ctx: Context, p: com.bober.autcsv.domain.model.PersonalInfo): String? {
        val parts = mutableListOf<String>()
        listOf(p.email, p.phone, p.location).filter { it.isNotBlank() }.let {
            if (it.isNotEmpty()) parts.add(it.joinToString("  •  "))
        }
        val experience = buildList {
            if (p.totalExperience.isNotBlank()) {
                add(ctx.getString(R.string.doc_experience_fmt, p.totalExperience))
            }
            if (p.specializationExperience.isNotBlank()) {
                add(ctx.getString(R.string.doc_spec_experience_fmt, p.specializationExperience))
            }
        }
        if (experience.isNotEmpty()) parts.add(experience.joinToString("  •  "))

        fun money(value: String): String? {
            val digits = value.filter { it.isDigit() }
            if (digits.isEmpty()) return null
            return digits.reversed().chunked(3).joinToString(" ").reversed()
        }

        val min = money(p.salaryMin)
        val max = money(p.salaryMax)
        when {
            min != null && max != null -> parts.add(
                ctx.getString(
                    R.string.doc_salary_line_full,
                    min,
                    max
                )
            )

            min != null -> parts.add(ctx.getString(R.string.doc_salary_line_from, min))
            max != null -> parts.add(ctx.getString(R.string.doc_salary_line_to, max))
        }
        if (p.readyToRelocate.isNotBlank()) {
            val cities = p.relocationCities.takeIf { it.isNotBlank() }
                ?.let { ctx.getString(R.string.doc_cities_suffix, it) }
                .orEmpty()
            parts.add(ctx.getString(R.string.doc_relocation_line, p.readyToRelocate) + cities)
        }
        val employment = listOf(
            p.employment.takeIf { it.isNotBlank() }
                ?.let { ctx.getString(R.string.doc_employment_line, it) },
            p.workSchedule.takeIf { it.isNotBlank() }
                ?.let { ctx.getString(R.string.doc_schedule_line, it) },
        ).filterNotNull()
        if (employment.isNotEmpty()) parts.add(employment.joinToString("  •  "))
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun socialsLine(p: com.bober.autcsv.domain.model.PersonalInfo): String? =
        p.socialLinks
            .filter { it.platform.isNotBlank() || it.url.isNotBlank() }
            .joinToString("  •  ") {
                when {
                    it.platform.isNotBlank() && it.url.isNotBlank() -> "${it.platform}: ${it.url}"
                    it.platform.isNotBlank() -> it.platform
                    else -> it.url
                }
            }
            .takeIf { it.isNotEmpty() }

    // ── Примитивы разметки ────────────────────────────────────────────────

    private fun StringBuilder.heading(text: String, halfPoints: Int) {
        append(
            """<w:p><w:pPr><w:spacing w:after="120"/>""" +
                    """<w:rPr><w:b/><w:sz w:val="$halfPoints"/></w:rPr></w:pPr>""" +
                    """<w:r><w:rPr><w:b/><w:sz w:val="$halfPoints"/></w:rPr>""" +
                    """<w:t xml:space="preserve">${escape(text)}</w:t></w:r></w:p>"""
        )
    }

    private fun StringBuilder.section(title: String, block: StringBuilder.() -> Unit) {
        append("""<w:p><w:pPr><w:spacing w:before="240" w:after="60"/>""")
        append("""<w:pBdr><w:bottom w:val="single" w:sz="6" w:space="2" w:color="D9D9D9"/></w:pBdr>""")
        append("""<w:rPr><w:b/><w:color w:val="1A202C"/><w:sz w:val="21"/></w:rPr></w:pPr>""")
        append("""<w:r><w:rPr><w:b/><w:color w:val="1A202C"/><w:sz w:val="21"/></w:rPr>""")
        append("""<w:t xml:space="preserve">${escape(title.uppercase())}</w:t></w:r></w:p>""")
        block()
    }

    private fun StringBuilder.paragraph(
        text: String,
        bold: Boolean = false,
        size: Int = 21, // half-points: 21 = 10.5pt
        color: String? = null,
    ) {
        // Многострочный текст (metaLine) разбиваем на отдельные абзацы
        text.split('\n').forEach { line ->
            val rPr = buildString {
                if (bold) append("<w:b/>")
                if (color != null) append("""<w:color w:val="$color"/>""")
                append("""<w:sz w:val="$size"/>""")
            }
            append("""<w:p><w:pPr><w:spacing w:after="80"/><w:rPr>$rPr</w:rPr></w:pPr>""")
            append("""<w:r><w:rPr>$rPr</w:rPr><w:t xml:space="preserve">${escape(line)}</w:t></w:r></w:p>""")
        }
    }

    private fun StringBuilder.bullet(text: String) {
        append("""<w:p><w:pPr><w:ind w:left="360" w:hanging="180"/><w:spacing w:after="60"/>""")
        append("""<w:rPr><w:sz w:val="21"/></w:rPr></w:pPr>""")
        append("""<w:r><w:rPr><w:sz w:val="21"/></w:rPr><w:t xml:space="preserve">• ${escape(text)}</w:t></w:r></w:p>""")
    }

    private fun StringBuilder.line(text: String) {
        if (text.isNotBlank()) paragraph(text)
    }

    private fun StringBuilder.spacing() {
        append("""<w:p><w:pPr><w:spacing w:after="120"/></w:pPr></w:p>""")
    }

    /** XML-экранирование текста. */
    internal fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    // ── Статические части пакета ──────────────────────────────────────────

    private const val CONTENT_TYPES = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>
"""

    private const val ROOT_RELS = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
"""

    private const val DOCUMENT_RELS = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
"""

    // __LANG__ заменяется на ru-RU / en-US по локали документа
    private const val STYLES = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults>
<w:rPrDefault><w:rPr>
<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
<w:sz w:val="21"/><w:szCs w:val="21"/>
<w:lang w:val="__LANG__"/>
</w:rPr></w:rPrDefault>
<w:pPrDefault><w:pPr><w:spacing w:after="80" w:line="252" w:lineRule="auto"/></w:pPr></w:pPrDefault>
</w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
</w:styles>
"""

    private const val DOCUMENT_WRAPPER = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>%s
<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="850" w:bottom="1134" w:left="850"/></w:sectPr>
</w:body></w:document>
"""
}

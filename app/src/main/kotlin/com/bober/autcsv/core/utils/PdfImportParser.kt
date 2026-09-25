package com.bober.autcsv.core.utils

import android.content.Context
import android.net.Uri
import com.bober.autcsv.core.utils.PdfImportParser.INFO_METADATA_KEY
import com.bober.autcsv.core.utils.PdfImportParser.normalizeHhHeadings
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.SocialLink
import com.google.gson.Gson
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.UUID

/**
 * Парсинг PDF-резюме и восстановление структуры [Resume].
 *
 * Приоритетный канал — встроенные метаданные AUTCSV_JSON_*…AUTCSV_JSON_END:
 * текущие экспорты пишут туда Gson-JSON резюме в base64 (переносы между
 * кусками отрисовки вычищаются перед декодированием); старые экспорты с
 * сырым JSON тоже поддержаны. Любой сбой канала даёт null и текстовый
 * fallback — никогда не исключение.
 *
 * Fallback — текстовый разбор по заголовкам секций собственного PDF-шаблона
 * плюс нормализованные заголовки hh.ru ([normalizeHhHeadings]).
 *
 * Общие эвристики токенизации вынесены в приватные функции файла и
 * переиспользуются разбором навыков и проектов (раньше дублировались).
 */
object PdfImportParser {

    /** Ключ пользовательского свойства в Info-словаре PDF. */
    private const val INFO_METADATA_KEY = "AUTCSV_JSON"

    /**
     * Парсит PDF, сгенерированный приложением, и восстанавливает структуру Resume.
     * Возвращает новый Resume с новым UUID, не зависящий от наличия старого в БД.
     *
     * Каналы восстановления, по приоритету:
     * 1. Info-словарь PDF ([INFO_METADATA_KEY]) — обычная строка в структуре файла,
     *    не зависит от шрифтов и от качества извлечения текста;
     * 2. Невидимый текстовый слой (фреймы base64 / маркеры);
     * 3. Текстовый разбор по заголовкам секций.
     */
    fun parseNewResumeFromPdf(context: Context, uri: Uri): Resume {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(uri)
            ?: return blankResume()
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                val metaJson =
                    document.documentInformation.getCustomMetadataValue(INFO_METADATA_KEY)
                if (!metaJson.isNullOrBlank()) {
                    val parsed = runCatching { Gson().fromJson(metaJson, Resume::class.java) }
                        .getOrNull()
                        ?.let { sanitizeParsedResume(it) }
                    if (parsed != null) {
                        return parsed.copy(id = UUID.randomUUID().toString())
                    }
                }
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                return parseResumeFromText(stripper.getText(document))
            }
        }
    }

    /** Пустое резюме для нечитаемого файла: форма откроется как черновик. */
    private fun blankResume(): Resume = Resume(
        personalInfo = PersonalInfo(fullName = "", specialization = "", totalExperience = ""),
        professionalSkills = ProfessionalSkills(professionalAchievements = emptyList()),
        projects = emptyList(),
    )

    /**
     * Разбор уже извлечённого текста (используется из PDF-пайплайна и в тестах).
     */
    fun parseResumeFromText(rawText: String): Resume {
        // Сначала пробуем извлечь встроенные метаданные без потерь
        extractEmbeddedJsonResume(rawText)?.let {
            return it.copy(
                id = UUID.randomUUID().toString()
            )
        }
        // Канал не сработал: убираем строки невидимого слоя метаданных,
        // иначе base64-куски попадают в контент секций (например в описание проектов)
        val text = normalizeHhHeadings(cleanPdfText(rawText))
            .lines()
            .filterNot { isMetadataLayerLine(it) }
            .joinToString("\n")
        val sections = splitIntoSections(text)

        val personalInfoBase = parsePersonalInfo(sections["header"].orEmpty())
        val summary = sections["summary"].orEmpty().trim()
        val educationSection = sections["education"].orEmpty()
        val education = educationSection.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val educations = parseEducationEntries(educationSection)
        val parsedLanguages = parseLanguages(sections["languages"].orEmpty())
        val skills = parseSkills(sections["skills"].orEmpty())
        val achievements = parseAchievements(sections["achievements"].orEmpty())
        val projects = parseProjects(sections["projects"].orEmpty())

        // Если опыт не распознан из шапки, пытаемся вытащить его из всего текста (включая summary)
        val (fixedTotalExp, fixedSpecExp) =
            if (personalInfoBase.totalExperience.isBlank() || personalInfoBase.specializationExperience.isBlank())
                extractExperienceFromText(text, personalInfoBase.specialization)
            else personalInfoBase.totalExperience to personalInfoBase.specializationExperience

        val personalInfo = personalInfoBase.copy(
            languages = parsedLanguages,
            education = education,
            educations = educations,
            aboutMe = sections["about"].orEmpty().trim(),
            totalExperience = fixedTotalExp.ifBlank { personalInfoBase.totalExperience },
            specializationExperience = fixedSpecExp.ifBlank { personalInfoBase.specializationExperience },
        )

        return Resume(
            id = UUID.randomUUID().toString(),
            personalInfo = personalInfo,
            professionalSkills = skills.copy(
                professionalAchievements = if (achievements.isNotEmpty()) achievements
                else skills.professionalAchievements
            ),
            projects = projects,
            summary = summary,
            aiAnalysis = AiAnalysis(),
        )
    }

    // ── Нормализация текста ────────────────────────────────────────────

    /** Строки невидимого слоя метаданных: тег фрейма или длинный base64-поток. */
    private fun isMetadataLayerLine(line: String): Boolean {
        if (line.contains("#A#")) return true
        return Regex("[A-Za-z0-9+/=]{40,}").containsMatchIn(line)
    }

    /**
     * Минимальная очистка извлеченного текста PDF от артефактов:
     * - удаление zero‑width символов
     * - нормализация «растянутых» маркеров AUTCSV_JSON_START/END и обрезка содержимого между ними
     */
    private fun cleanPdfText(input: String): String {
        val noZw = input.replace(Regex("[\u200B-\u200D\uFEFF]"), "")
        val startPattern = Regex(
            "A\\s*U\\s*T\\s*C\\s*S\\s*V\\s*_\\s*J\\s*S\\s*O\\s*N\\s*_\\s*S\\s*T\\s*A\\s*R\\s*T",
            RegexOption.IGNORE_CASE
        )
        val endPattern = Regex(
            "A\\s*U\\s*T\\s*C\\s*S\\s*V\\s*_\\s*J\\s*S\\s*O\\s*N\\s*_\\s*E\\s*N\\s*D",
            RegexOption.IGNORE_CASE
        )
        var normalized = noZw.replace(startPattern, "AUTCSV_JSON_START")
            .replace(endPattern, "AUTCSV_JSON_END")

        // Удаляем все сегменты метаданных между стартом и концом (если вдруг попались)
        while (true) {
            val s = normalized.indexOf("AUTCSV_JSON_START")
            val e = normalized.indexOf("AUTCSV_JSON_END")
            if (s >= 0 && e > s) {
                normalized = normalized.removeRange(s, e + "AUTCSV_JSON_END".length)
            } else break
        }
        return normalized
    }

    /**
     * Приводит заголовки секций hh.ru к заголовкам собственного шаблона,
     * чтобы текстовый fallback-разбор работал и для выгрузок hh.
     */
    private fun normalizeHhHeadings(text: String): String =
        text.lines().joinToString("\n") { line ->
            val t = line.trim()
            when {
                t.equals("Ключевые навыки", ignoreCase = true) -> "Технические навыки"
                t.equals("Знание языков", ignoreCase = true) -> "Языки"
                else -> {
                    val m = Regex("^Опыт работы\\s*[—–-]\\s*(.+)$").find(t)
                    if (m != null) "Опыт: ${m.groupValues[1].trim()}" else line
                }
            }
        }

    // ── Общие эвристики токенизации (переиспользуются навыками и проектами) ──

    private fun cleanToken(raw: String): String {
        val trimmed = raw
            .replace(Regex("[\u200B-\u200D\uFEFF]"), "")
            .replace(Regex("^[-•]\\s*"), "")
            .trim()
        val withoutQuotes = trimmed.trim('"', '\'', '[', ']', '{', '}', '“', '”')
        val normalizedSpaces = withoutQuotes.replace(Regex("\\s+"), " ")
        return collapseSpreadLetters(normalizedSpaces)
    }

    /** Сжимает «р а з р я ж е н н ы й» мелким шрифтом текст в обычные слова. */
    private fun collapseSpreadLetters(s: String): String {
        val spacedLetters = Regex("^(?:[\\p{L}]\\s+){3,}[\\p{L}](?:.*)?$")
        return if (spacedLetters.containsMatchIn(s)) {
            s.replace(Regex("(?<=[\\p{L}])\\s(?=[\\p{L}])"), "")
        } else s
    }

    private fun splitIfNoSeparators(part: String): List<String> {
        val p = part.trim()
        if (p.contains('•') || p.contains(',') || p.contains('·') || p.contains(';')) return listOf(
            p
        )
        val words = p.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size >= 3 && words.all {
                it.firstOrNull()?.isUpperCase() == true
            }) words else listOf(p)
    }

    private fun joinCompounds(parts: List<String>): List<String> {
        if (parts.isEmpty()) return parts
        val result = mutableListOf<String>()
        var i = 0
        while (i < parts.size) {
            val cur = parts[i]
            val next = parts.getOrNull(i + 1)
            if (cur.equals("REST", ignoreCase = true) && next?.equals(
                    "API",
                    ignoreCase = true
                ) == true
            ) {
                result.add("REST API")
                i += 2
                continue
            }
            if (cur.equals("Celery", ignoreCase = true) && next?.equals(
                    "Beat",
                    ignoreCase = true
                ) == true
            ) {
                result.add("Celery Beat")
                i += 2
                continue
            }
            result.add(cur)
            i += 1
        }
        return result
    }

    private fun isJsonArtifact(line: String): Boolean {
        if (line.contains("AUTCSV", ignoreCase = true)) return true
        if (line.contains("_JSON_", ignoreCase = true)) return true
        if (line.contains("\":")) return true
        val jsonChars = charArrayOf('{', '}', '[', ']', '"', ':', '_')
        return line.count { ch -> jsonChars.contains(ch) } >= 3
    }

    private fun isLikelyJsonNoise(s: String): Boolean {
        if (isJsonArtifact(s)) return true
        val toks = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val oneLetters = toks.count { it.length == 1 && it[0].isLetter() }
        return toks.size >= 8 && oneLetters >= toks.size / 2
    }

    private val allowedShort = setOf("C", "R", "Go", "C#", "C++")

    private fun isValidTechToken(token: String): Boolean {
        val t = token.trim()
        if (t.isEmpty()) return false
        if (t in allowedShort) return true
        if (t.length <= 2) return false
        if (t.any { it in charArrayOf('{', '}', '[', ']', '"', ':', '_') }) return false
        return true
    }

    /** «5 лет» → «5», «7» → «7»: форме нужно чистое число лет. */
    fun normalizeYearsValue(value: String): String {
        val v = value.trim()
        v.takeWhile { it.isDigit() }.takeIf { it.isNotEmpty() }?.let { return it }
        Regex("\\d{1,2}").find(v)?.value?.let { return it }
        return v
    }

    // ── Опыт ───────────────────────────────────────────────────────────

    private fun extractExperienceFromText(
        text: String,
        specialization: String,
    ): Pair<String, String> {
        // Метки распознаются на обоих языках интерфейса: PDF мог быть
        // сгенерирован до переключения языка
        val labeledTotal = Regex("(?:Опыт|Experience):\\s*([^|•\\n]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val labeledSpec = Regex(
            "(?:Опыт по специализации|Specialized experience):\\s*([^|•\\n]+)",
            RegexOption.IGNORE_CASE
        ).find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        var total = labeledTotal
        var spec = labeledSpec

        val token = Regex("(\\d{1,2})\\s+(год|года|лет|г\\.|year|years)", RegexOption.IGNORE_CASE)

        if (spec.isBlank() && specialization.isNotBlank()) {
            val specPhrase = Regex(
                pattern = "(^|\\n)\\s*(\\d{1,2})\\s+(год|года|лет|г\\.|year|years)\\s+(в|по|in|at)\\s+[^|•\\n]*" +
                        Regex.escape(specialization) + "[^|•\\n]*",
                option = RegexOption.IGNORE_CASE
            )
            specPhrase.find(text)?.let { spec = it.value.trim() }
        }

        if (total.isBlank()) {
            val tokens = text.lines().filterNot(::isDemographicLine)
                .flatMap { token.findAll(it) }.map { it.value.trim() }.toList()
            val first = tokens.firstOrNull()
            if (!first.isNullOrBlank()) total = first
            if (spec.isBlank() && tokens.size >= 2) spec = tokens[1]
        }

        return normalizeYearsValue(total) to normalizeYearsValue(spec)
    }

    // ── Секции ─────────────────────────────────────────────────────────

    /**
     * Разбивает текст на секции по заголовкам. Заголовок сопоставляется
     * точно (вся строка, без учёта регистра и необязательного двоеточия),
     * чтобы строка «Языки программирования» не открывала секцию «Языки».
     * Конец секции — следующий найденный заголовок, а не фиксированный
     * порядок: порядок секций в PDF настраивается пользователем.
     */
    private fun splitIntoSections(text: String): Map<String, String> {
        // Заголовки собственного шаблона на обоих языках плюс синонимы hh.ru
        val lines = text.lines()
        fun findIndex(vararg titles: String) = lines.indexOfFirst { line ->
            val heading = line.trim().trimEnd(':')
            titles.any { t -> heading.equals(t, ignoreCase = true) }
        }

        val idxSummary = findIndex(
            "Профессиональное резюме", "Professional resume"
        )
        val idxAbout = findIndex("О себе", "Обо мне", "About me")
        val idxEdu = findIndex("Образование", "Education")
        val idxLang = findIndex(
            "Языки", "Знание языков", "Иностранные языки",
            "Languages"
        )
        val idxSkills = findIndex(
            "Технические навыки", "Ключевые навыки",
            "Профессиональные навыки", "Навыки",
            "Technical skills", "Key skills", "Skills"
        )
        val idxAch = findIndex("Ключевые достижения", "Key achievements")
        val idxProjects = findIndex("Проектный опыт", "Проекты", "Project experience", "Projects")

        val headingIndices =
            listOf(idxSummary, idxAbout, idxEdu, idxLang, idxSkills, idxAch, idxProjects)
                .filter { it >= 0 }
        val firstHeading = headingIndices.minOrNull() ?: lines.size

        /** Конец секции — ближайший следующий заголовок любого типа. */
        fun nextHeadingAfter(start: Int): Int =
            headingIndices.filter { it > start }.minOrNull() ?: lines.size

        fun slice(start: Int): String =
            if (start >= 0) lines.subList(start + 1, nextHeadingAfter(start))
                .joinToString("\n") else ""

        return mapOf(
            "header" to lines.subList(0, firstHeading).joinToString("\n"),
            "summary" to slice(idxSummary),
            "about" to slice(idxAbout),
            "education" to slice(idxEdu),
            "languages" to slice(idxLang),
            "skills" to slice(idxSkills),
            "achievements" to slice(idxAch),
            "projects" to if (idxProjects >= 0) slice(idxProjects) else "",
        )
    }

    private fun extractEmbeddedJsonResume(text: String): Resume? {
        // Текущий формат: строки «#A#NNN#<base64>», собираемые по индексам —
        // устойчиво к вклиниванию видимого текста страницы при извлечении
        decodeFramedBase64Resume(text)?.let { return it }

        val startMarker = "AUTCSV_JSON_START"
        val endMarker = "AUTCSV_JSON_END"
        val start = text.indexOf(startMarker)
        val end = text.indexOf(endMarker)
        if (start < 0 || end <= start) return null
        val blob = text.substring(start + startMarker.length, end)
        // Промежуточный формат: base64 между маркерами — пробелы и переносы
        // между кусками отрисовки вычищаются перед декодированием
        decodeBase64Resume(blob)?.let { return it }
        // Старые экспорты: сырой JSON; извлекатель вставлял внутрь него
        // переводы строк между кусками отрисовки — вычищаем и пробуем так
        val compacted = blob.replace(Regex("[\\r\\n\\t]"), "")
        return runCatching { Gson().fromJson(compacted, Resume::class.java) }.getOrNull()
            ?.let { sanitizeParsedResume(it) }
    }

    /** Тег фрейма метаданных: «#A#000#<до 160 символов base64>». */
    private const val META_FRAME_CHUNK = 160

    /**
     * Собирает base64-поток по тегам с индексами: строки метаданных при
     * извлечении перемежаются видимым текстом страницы, поэтому каждая
     * строка ищется по тегу, берётся часть после тега, обрезается до
     * размера куска и складывается по индексу. Пропуск позиции или сбой
     * декодирования дают null — вызывающий код уйдёт в текстовый разбор.
     */
    private fun decodeFramedBase64Resume(text: String): Resume? {
        val frameRegex = Regex("#A#(\\d{3})#([A-Za-z0-9+/=]+)")
        val parts = HashMap<Int, String>()
        var maxIndex = -1
        frameRegex.findAll(text).forEach { match ->
            val index = match.groupValues[1].toInt()
            val segment = match.groupValues[2].take(META_FRAME_CHUNK)
            val existing = parts[index]
            if (existing == null || segment.length > existing.length) parts[index] = segment
            if (index > maxIndex) maxIndex = index
        }
        if (maxIndex < 0) return null
        val stream = StringBuilder()
        for (i in 0..maxIndex) {
            val segment = parts[i] ?: return null
            stream.append(segment)
        }
        return runCatching {
            val bytes = java.util.Base64.getDecoder().decode(stream.toString())
            metadataBytesToResume(bytes)
        }.getOrNull()
    }

    /** Декодирует base64-метаданные в [Resume]; любой сбой даёт null. */
    private fun decodeBase64Resume(blob: String): Resume? = runCatching {
        val cleaned = blob.replace(Regex("\\s+"), "")
        if (cleaned.isEmpty() || !cleaned.matches(Regex("[A-Za-z0-9+/=]+"))) return@runCatching null
        val json = String(java.util.Base64.getDecoder().decode(cleaned), Charsets.UTF_8)
        Gson().fromJson(json, Resume::class.java)
    }.getOrNull()?.let { sanitizeParsedResume(it) }

    /**
     * Полезная нагрузка текстового слоя: gzip(JSON) — компактность уменьшает
     * число строк и риск перемешивания; на всякий случай принимается и
     * несжатый JSON (первые экспорты промежуточной версии).
     */
    private fun metadataBytesToResume(bytes: ByteArray): Resume? {
        val json = runCatching {
            java.util.zip.GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
                .toString(Charsets.UTF_8)
        }.getOrElse { bytes.toString(Charsets.UTF_8) }
        return runCatching { Gson().fromJson(json, Resume::class.java) }
            .getOrNull()
            ?.let { sanitizeParsedResume(it) }
    }

    /**
     * Gson не применяет значения по умолчанию и создаёт объекты напрямую:
     * в данных, сохранённых до появления новых полей (или повреждённых),
     * поля приходят как null, хотя типы объявлены ненулевыми. Собираем
     * резюме заново с явными значениями по умолчанию; при любых проблемах
     * возвращаем null — вызывающий код уйдёт в текстовый разбор.
     */
    private fun sanitizeParsedResume(resume: Resume): Resume? {
        val nvl: (String?) -> String = { it ?: "" }
        return runCatching {
            val info = resume.personalInfo ?: return@runCatching null
            val skills = resume.professionalSkills ?: return@runCatching null
            Resume(
                id = resume.id.takeIf { !it.isNullOrBlank() } ?: UUID.randomUUID().toString(),
                personalInfo = PersonalInfo(
                    fullName = nvl(info.fullName),
                    specialization = nvl(info.specialization),
                    totalExperience = nvl(info.totalExperience),
                    specializationExperience = nvl(info.specializationExperience),
                    education = nvl(info.education),
                    educations = info.educations ?: emptyList(),
                    languages = info.languages ?: emptyList(),
                    email = nvl(info.email),
                    phone = nvl(info.phone),
                    location = nvl(info.location),
                    aboutMe = nvl(info.aboutMe),
                    salaryMin = nvl(info.salaryMin),
                    salaryMax = nvl(info.salaryMax),
                    readyToRelocate = nvl(info.readyToRelocate),
                    relocationCities = nvl(info.relocationCities),
                    photoUri = nvl(info.photoUri),
                    employment = nvl(info.employment),
                    workSchedule = nvl(info.workSchedule),
                    socialLinks = info.socialLinks ?: emptyList(),
                ),
                professionalSkills = ProfessionalSkills(
                    operatingSystems = skills.operatingSystems ?: emptyList(),
                    programmingLanguages = skills.programmingLanguages ?: emptyList(),
                    frameworks = skills.frameworks ?: emptyList(),
                    libraries = skills.libraries ?: emptyList(),
                    databases = skills.databases ?: emptyList(),
                    otherTechnologies = skills.otherTechnologies ?: emptyList(),
                    certifications = skills.certifications ?: emptyList(),
                    softSkills = skills.softSkills ?: emptyList(),
                    professionalAchievements = skills.professionalAchievements ?: emptyList(),
                ),
                projects = resume.projects ?: emptyList(),
                summary = nvl(resume.summary),
                aiAnalysis = resume.aiAnalysis ?: AiAnalysis(),
                lastModified = resume.lastModified,
                isDeleted = resume.isDeleted,
                deletedAt = resume.deletedAt,
                status = resume.status ?: com.bober.autcsv.domain.model.CandidateStatus.NONE,
                isFavorite = resume.isFavorite,
            )
        }.getOrNull()
    }

    private fun parsePersonalInfo(header: String): PersonalInfo {
        val lines = header.lines().filter { it.isNotBlank() }
        val fullName = lines.firstOrNull()?.trim().orEmpty()
        val specialization = lines.getOrNull(1).orEmpty().trim()
        val contacts = lines.drop(2).joinToString(" ")

        val email =
            Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(contacts)?.value.orEmpty()
        val phone = findPhoneNumber(contacts)
        val contactParts =
            Regex("[•|]|\\s{2,}").split(contacts).map { it.trim() }.filter { it.isNotEmpty() }
        val guessedLocation = contactParts.firstOrNull { part ->
            part != email && part != phone &&
                    !part.contains("@") &&
                    part.count { it.isLetter() } >= 2 &&
                    !part.startsWith("Релокация") &&
                    !part.startsWith("Relocation") &&
                    !part.startsWith("Желаемая зарплата") &&
                    !part.startsWith("Desired salary") &&
                    !isDemographicLine(part) &&
                    !part.contains(":")
        }.orEmpty()

        var totalExp = Regex("(?:Опыт|Experience):\\s*([^|•\\n]+)")
            .find(header)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        var specializationExp =
            Regex("(?:Опыт по специализации|Specialized experience):\\s*([^|•\\n]+)")
                .find(header)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        // Фоллбек: распознаем русские и английские форматы "1 год"/"5 years"
        val yearToken = Regex(
            "\\b(\\d{1,2})\\s+(год|года|лет|г\\.|year|years)\\b",
            RegexOption.IGNORE_CASE
        )

        if (specializationExp.isBlank()) {
            val specRegex = if (specialization.isNotBlank()) {
                Regex(
                    "\\b(\\d{1,2})\\s+(год|года|лет|г\\.|year|years)\\s+(в|по|in|at)\\s+[^|•\\n]*" +
                            Regex.escape(specialization) + "[^|•\\n]*",
                    RegexOption.IGNORE_CASE
                )
            } else {
                Regex(
                    "\\b(\\d{1,2})\\s+(год|года|лет|г\\.|year|years)\\s+(в|по|in|at)\\b[^|•\\n]*",
                    RegexOption.IGNORE_CASE
                )
            }
            specRegex.find(header)?.let { specializationExp = it.value.trim() }
        }

        if (totalExp.isBlank()) {
            // Возраст («Мужчина, 29 лет…») — не опыт: демографические строки пропускаем
            val allMatches = header.lines().filterNot(::isDemographicLine)
                .flatMap { yearToken.findAll(it) }.map { it.value.trim() }.toList()
            val candidate = allMatches.firstOrNull {
                specializationExp.isBlank() || !specializationExp.contains(it)
            }
            if (!candidate.isNullOrBlank()) totalExp = candidate
        }

        if (totalExp.isBlank() || specializationExp.isBlank()) {
            header.lines().filterNot(::isDemographicLine).forEach { line ->
                val matches = yearToken.findAll(line).map { it.value.trim() }.toList()
                if (matches.size >= 2) {
                    if (totalExp.isBlank()) totalExp = matches[0]
                    if (specializationExp.isBlank()) specializationExp = matches[1]
                    return@forEach
                }
            }
        }

        return PersonalInfo(
            fullName = fullName,
            specialization = specialization,
            totalExperience = normalizeYearsValue(totalExp),
            specializationExperience = normalizeYearsValue(specializationExp),
            education = "",
            languages = emptyList(),
            email = email,
            phone = phone,
            location = guessedLocation,
            aboutMe = "",
            salaryMin = extractSalary(header, from = true),
            salaryMax = extractSalary(header, from = false),
            readyToRelocate = labeledHeaderValueBilingual(header, "Релокация:", "Relocation:")
                .substringBefore("·").substringBefore("· cities:").trim(),
            relocationCities = labeledHeaderValueBilingual(header, "города:", "cities:"),
            employment = labeledHeaderValueBilingual(header, "Занятость:", "Employment:")
                .substringBefore("График:").substringBefore("Schedule:").substringBefore("•")
                .trim(),
            workSchedule = labeledHeaderValueBilingual(header, "График:", "Schedule:"),
            socialLinks = extractSocialLinks(header),
        )
    }

    /** Значение подписанной строки шапки: пробует метку на обоих языках. */
    private fun labeledHeaderValueBilingual(
        header: String,
        labelRu: String,
        labelEn: String,
    ): String =
        labeledHeaderValue(header, labelRu).ifBlank { labeledHeaderValue(header, labelEn) }

    /** Значение подписанной строки шапки («Релокация: …», «График: …»). */
    private fun labeledHeaderValue(header: String, label: String): String =
        header.lines().firstOrNull { it.contains(label, ignoreCase = true) }
            ?.substringAfter(label, "")?.trim().orEmpty()

    /** Зарплатная вилка из строки «Желаемая зарплата: от N до M» / «Desired salary: …». */
    private fun extractSalary(header: String, from: Boolean): String {
        val line = labeledHeaderValueBilingual(header, "Желаемая зарплата:", "Desired salary:")
        val marker = if (from) {
            if (line.contains("от")) "от" else "from"
        } else {
            when {
                line.contains("до") -> "до"
                line.contains("up to") -> "up to"
                else -> "to"
            }
        }
        val segment = if (from) line.substringAfter(marker, "") else line.substringAfter(marker, "")
        return segment.takeWhile { it.isDigit() || it == ' ' || it == '\u00A0' }
            .filter { it.isDigit() }
    }

    /**
     * Телефон: ищем последовательности, похожие на номер, и принимаем только
     * правдоподобные — 10–12 цифр с ведущим «+»/7/8. Иначе в номер попадают
     * зарплатные суммы («от 200 000») и диапазоны дат («2019 - 2022»).
     */
    private fun findPhoneNumber(text: String): String =
        Regex("[+]?\\d[\\d\\s().-]{7,}").findAll(text)
            .map { it.value.trim() }
            .firstOrNull { candidate ->
                val digits = candidate.count { it.isDigit() }
                digits in 10..12 && (
                        candidate.contains('+') ||
                                candidate.firstOrNull { it.isDigit() }
                                    ?.let { it == '7' || it == '8' } == true
                        )
            }
            .orEmpty()

    /** Строки о возрасте/поле/дате рождения: возраст из них не должен стать опытом. */
    private fun isDemographicLine(line: String): Boolean =
        Regex("родил|мужчин|женщин|возраст|лет,|years old", RegexOption.IGNORE_CASE)
            .containsMatchIn(line)

    /**
     * Соцсети и профили в шапке. Собственный шаблон рисует их парами
     * «Платформа: адрес» через «•»; во внешних резюме ищем полные URL
     * и telegram-ники («@nick», но не локальную часть email).
     */
    private fun extractSocialLinks(text: String): List<SocialLink> {
        val links = mutableListOf<SocialLink>()
        text.lines().forEach { line ->
            line.split('•').forEach { segment ->
                val match = Regex("^\\s*([A-Za-zА-Яа-яЁё .&]+?)\\s*:\\s*(\\S.*)$")
                    .find(segment.trim()) ?: return@forEach
                val label = match.groupValues[1].trim()
                val platform = knownPlatforms.firstOrNull { it.equals(label, ignoreCase = true) }
                    ?: return@forEach
                val value = match.groupValues[2].trim()
                if (links.none { it.url.equals(value, ignoreCase = true) }) {
                    links.add(SocialLink(platform = platform, url = value))
                }
            }
        }
        Regex("(?:https?://|www\\.)[A-Za-z0-9._/?#&=-]+").findAll(text).forEach { match ->
            val url = match.value.removePrefix("http://").removePrefix("https://")
            if (!url.contains('@') && links.none {
                    it.url.contains(
                        url.removePrefix("www."),
                        ignoreCase = true
                    )
                }) {
                links.add(SocialLink(platform = guessPlatform(url), url = url))
            }
        }
        // Ник не внутри email: перед «@» не должно быть буквы/цифры/точки
        Regex("(?<![A-Za-z0-9.@])@[A-Za-z0-9_]{3,32}").findAll(text).forEach { match ->
            if (links.none { it.url.equals(match.value, ignoreCase = true) }) {
                links.add(SocialLink(platform = "Telegram", url = match.value))
            }
        }
        return links.distinctBy { it.url.lowercase() }
    }

    /** Платформы, которыми подписываются профили в собственном шаблоне. */
    private val knownPlatforms = listOf(
        "Telegram", "LinkedIn", "GitHub", "GitLab", "Habr Career",
        "Behance", "Dribbble", "YouTube", "VK", "Личный сайт"
    )

    /** Название платформы профиля по адресу ссылки. */
    private fun guessPlatform(url: String): String {
        val u = url.lowercase()
        return when {
            "t.me" in u || "telegram" in u -> "Telegram"
            "github" in u -> "GitHub"
            "gitlab" in u -> "GitLab"
            "linkedin" in u -> "LinkedIn"
            "vk.com" in u -> "VK"
            "habr" in u -> "Habr Career"
            "behance" in u -> "Behance"
            "dribbble" in u -> "Dribbble"
            "youtube" in u -> "YouTube"
            else -> "Личный сайт"
        }
    }

    // ── Навыки ─────────────────────────────────────────────────────────

    private fun parseSkills(block: String): ProfessionalSkills {
        // Подкатегории на обоих языках: PDF мог быть сгенерирован в другой локали
        val titles = listOf(
            "Операционные системы", "Operating systems",
            "Языки программирования", "Programming languages",
            "Фреймворки", "Frameworks",
            "Библиотеки", "Libraries",
            "Базы данных", "Databases",
            "Другие технологии", "Other technologies",
            "Сертификации", "Certifications",
            "Гибкие навыки", "Soft skills"
        )

        /**
         * Начало — любой из переданных заголовков (ру/en); конец — ближайший
         * следующий заголовок ЛЮБОЙ подкатегории из общего списка [titles]:
         * иначе блок проглатывает все последующие секции.
         */
        fun subsectionBetween(vararg headings: String): String {
            val lines = block.lines()
            val start = lines.indexOfFirst { line ->
                headings.any { t -> line.contains(t, ignoreCase = true) }
            }
            if (start == -1) return ""
            val end = lines.drop(start + 1)
                .indexOfFirst { line -> titles.any { t -> line.contains(t, ignoreCase = true) } }
            val realEnd = if (end == -1) lines.size else start + 1 + end
            return lines.subList(start + 1, realEnd).joinToString("\n")
        }

        fun parseChips(text: String): List<String> {
            if (text.isBlank()) return emptyList()

            fun expandParentheses(token: String): List<String> {
                val m = Regex("^(.*)\\((.+)\\)$").find(token)
                return if (m != null) {
                    listOf(
                        m.groupValues[1].trim(),
                        m.groupValues[2].trim()
                    ).filter { it.isNotBlank() }
                } else listOf(token)
            }

            val rawParts = text.lines().flatMap { line ->
                line.replace("  •  ", " • ")
                    .split('•', ',', '·', ';')
                    .flatMap { splitIfNoSeparators(it) }
            }

            val normalized = joinCompounds(rawParts)
                .map { cleanToken(it) }
                .flatMap { expandParentheses(it) }
                .filter { it.isNotBlank() }

            // Дедупликация без учета регистра
            val seen = HashSet<String>()
            return normalized.filter { token -> seen.add(token.lowercase()) }
        }

        fun parseBullets(text: String): List<String> =
            text.lines().map { it.replace(Regex("^[-•]\\s*"), "").trim() }
                .filter { it.isNotBlank() }

        val operatingSystems =
            parseChips(subsectionBetween("Операционные системы", "Operating systems"))
        val programmingLanguages =
            parseChips(subsectionBetween("Языки программирования", "Programming languages"))
        val frameworks = parseChips(subsectionBetween("Фреймворки", "Frameworks"))
        val libraries = parseChips(subsectionBetween("Библиотеки", "Libraries"))
        val databases = parseChips(subsectionBetween("Базы данных", "Databases"))
        var otherTechnologies =
            parseChips(subsectionBetween("Другие технологии", "Other technologies"))
        val certifications = parseBullets(subsectionBetween("Сертификации", "Certifications"))
        val softSkills = parseChips(subsectionBetween("Гибкие навыки", "Soft skills"))

        // Внешние резюме (например hh.ru) дают плоский список чипов без
        // подкатегорий — не теряем его, складываем в «другие технологии»
        val noCategories = operatingSystems.isEmpty() && programmingLanguages.isEmpty() &&
                frameworks.isEmpty() && libraries.isEmpty() && databases.isEmpty() &&
                otherTechnologies.isEmpty() && certifications.isEmpty() && softSkills.isEmpty()
        if (noCategories && block.isNotBlank()) {
            otherTechnologies = parseChips(block)
                .filter { token -> token.length <= 32 && !token.contains('.') && !token.contains(':') }
        }

        return ProfessionalSkills(
            operatingSystems = operatingSystems,
            programmingLanguages = programmingLanguages,
            frameworks = frameworks,
            libraries = libraries,
            databases = databases,
            otherTechnologies = otherTechnologies,
            professionalAchievements = emptyList(),
            certifications = certifications,
            softSkills = softSkills
        )
    }

    private fun parseAchievements(block: String): List<String> =
        block.lines().map { it.removePrefix("•").trim() }.filter { it.isNotBlank() }

    // ── Образование ────────────────────────────────────────────────────

    /**
     * Уровни образования — те же подписи, что в SuggestionDictionary.EducationLevel,
     * на обоих языках, чтобы форма после импорта подсветила выбранный чип.
     */
    private val educationLevelLabels = listOf(
        "СПО", "Secondary vocational",
        "Высшее (бакалавриат)", "Higher education (Bachelor’s)",
        "Высшее (специалитет)", "Higher education (Specialist)",
        "Высшее (магистратура)", "Higher education (Master’s)",
        "Послевузовское", "Postgraduate",
        "Дополнительное", "Additional"
    )

    /**
     * Каждая непустая строка секции «Образование» — отдельная запись
     * формата «заведение, специальность, уровень» (часть с узнаваемым
     * уровнем уходит в level, остальные — в institution/specialty).
     * Колонтитулы и служебные строки отбрасываются, голый год
     * («МГУ, 2019») не становится специальностью.
     */
    private fun parseEducationEntries(block: String): List<EducationEntry> =
        block.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !isJsonArtifact(it) && !isJunkLine(it) }
            .map { line ->
                val parts = line.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val level = parts.firstOrNull { p ->
                    educationLevelLabels.any { it.equals(p, ignoreCase = true) }
                }.orEmpty()
                val rest = parts.filter { it != level }
                    .filterNot { it.length == 4 && it.all(Char::isDigit) } // годы не специальность
                EducationEntry(
                    level = level,
                    institution = rest.getOrNull(0).orEmpty(),
                    specialty = rest.getOrNull(1).orEmpty()
                )
            }

    /** Колонтитулы и служебные строки, случайно попавшие в секцию. */
    private fun isJunkLine(line: String): Boolean {
        if (line.startsWith("AutCSV", ignoreCase = true)) return true
        if (line.startsWith("СтафИИ") || line.startsWith("StaffAI")) return true
        if (line.contains('•')) return true
        return Regex("страниц|page \\d|сформирован", RegexOption.IGNORE_CASE).containsMatchIn(line)
    }

    /**
     * Строка вида «Русский — Родной» / «English — B2 — Средний».
     * В секцию языков при текстовом разборе может попасть чужой контент
     * (сайдбар двухколоночного шаблона, произвольный порядок секций),
     * поэтому каждая строка проверяется: имя должно быть похожим на язык,
     * а уровень — на шкалу владения (A1–C2, «Родной», Intermediate и т.п.).
     */
    private fun parseLanguages(block: String): List<Language> {
        if (block.isBlank()) return emptyList()
        return block.lines().mapNotNull { raw ->
            val line = raw.trim()
            if (line.isEmpty() || isJsonArtifact(line)) return@mapNotNull null
            // Формат «Русский — Родной» / «English — B2 — Средний»
            val dashParts = line.split(Regex("\\s*[—–]\\s*|\\s+-\\s+")).map { it.trim() }
            val pair = if (dashParts.size >= 2) {
                dashParts[0] to dashParts[1]
            } else {
                // Формат «Русский (Родной)» / «Английский (B2)»
                val m = Regex("^(.{1,24}?)\\s*[(](.+)[)]$").find(line)?.groupValues
                    ?: return@mapNotNull null
                m[1].trim() to m[2].trim()
            }
            val name = pair.first
            val level = pair.second
            if (looksLikeLanguageName(name) && looksLikeLanguageLevel(level)) {
                Language(name = name, level = level)
            } else null
        }
    }

    /** Известные языки (ru/en) — распознаются сразу, без эвристик. */
    private val knownLanguageNames = setOf(
        "русский", "russian", "английский", "english", "немецкий", "german",
        "французский", "french", "испанский", "spanish", "итальянский", "italian",
        "китайский", "chinese", "японский", "japanese", "корейский", "korean",
        "турецкий", "turkish", "арабский", "arabic", "португальский", "portuguese",
        "украинский", "ukrainian", "белорусский", "belarusian", "казахский", "kazakh",
        "польский", "polish", "чешский", "czech", "болгарский", "венгерский",
        "голландский", "dutch", "норвежский", "norwegian", "шведский", "swedish",
        "финский", "finnish", "датский", "danish", "иврит", "hebrew", "хинди", "hindi",
        "вьетнамский", "фарси"
    )

    /** Имя языка: известный язык либо короткое слово из букв (без цифр, адресов и пр.). */
    private fun looksLikeLanguageName(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || n.length > 24) return false
        if (knownLanguageNames.any { it.equals(n, ignoreCase = true) }) return true
        val words = n.split(Regex("\\s+"))
        return words.size <= 2 && words.all { word ->
            word.isNotEmpty() && word.all { it.isLetter() || it == '-' }
        }
    }

    /** Уровень владения: A1–C2, «Родной», «Свободно», Intermediate, Fluent и т.п. */
    private fun looksLikeLanguageLevel(level: String): Boolean {
        val v = level.trim()
        if (v.isEmpty() || v.length > 40) return false
        // «B2 — Средне-продвинутый»: проверяем часть до тире и всё вместе
        val head = v.split(Regex("\\s*[—–-]\\s*")).first()
        val candidates = listOf(v, head)
        return candidates.any { candidate ->
            Regex("^[ABC][12]$", RegexOption.IGNORE_CASE).matches(candidate) ||
                    Regex("^(родной|native)$", RegexOption.IGNORE_CASE).matches(candidate) ||
                    setOf(
                        "начальный", "элементарный", "средний", "продвинутый", "в совершенстве",
                        "свободно", "свободное владение", "хорошо", "базовый", "базовый уровень",
                        "basic", "beginner", "elementary", "intermediate", "advanced",
                        "fluent", "proficient", "upper-intermediate", "pre-intermediate",
                        "conversational", "professional"
                    ).any { it.equals(candidate, ignoreCase = true) }
        }
    }

    // ── Проекты ────────────────────────────────────────────────────────

    private fun parseProjects(block: String): List<Project> {
        if (block.isBlank()) return emptyList()
        val lines = block.lines()
        val projects = mutableListOf<Project>()
        var current = mutableListOf<String>()
        fun flush() {
            if (current.isEmpty()) return
            projects.add(parseSingleProject(current))
            current = mutableListOf()
        }
        lines.forEach { line ->
            if (line.startsWith("Проект ") || line.startsWith("Project ")) {
                flush()
            }
            current.add(line)
        }
        flush()
        return projects
    }

    private fun parseSingleProject(lines: List<String>): Project {
        var name = ""
        var role = ""
        var duration = ""
        var team = ""
        var link = ""
        val resp = mutableListOf<String>()
        val techs = mutableListOf<String>()
        val descBuilder = StringBuilder()
        var headerParsed = false

        fun looksLikeTechList(text: String): Boolean {
            val parts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 3) return false
            val alphaParts = parts.count { it.any { ch -> ch.isLetter() } }
            val sentenceEnders = listOf('.', '!', '?')
            val hasSentenceEnd = text.any { it in sentenceEnders }
            return alphaParts >= parts.size - 1 && !hasSentenceEnd
        }

        lines.forEach { raw ->
            val line = raw.trim()
            if (line.isBlank()) return@forEach
            // Пропускаем колонтитулы вида «AutCSV • …» / «СтафИИ • …»
            if (line.startsWith("AutCSV") || line.startsWith("СтафИИ") || line.startsWith("StaffAI")) return@forEach
            // Пропускаем строки статусов и желаемых условий, попавшие в хвост
            if (line.startsWith("Статус:") || line.startsWith("Status:") || line.startsWith("Занятость:") || line.startsWith(
                    "Employment:"
                )
            ) return@forEach

            if (line.startsWith("Проект ") || line.startsWith("Project ")) {
                name = line.substringAfter(":").trim()
                headerParsed = false
                return@forEach
            }

            // Разбиваем составную строку заголовка по маркерам «•»
            val segments = line
                .replace("  •  ", " • ")
                .split('•')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            var anyHeaderKey = false
            segments.forEach { seg ->
                when {
                    seg.startsWith("Роль:") || seg.startsWith("Role:") -> {
                        role = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    seg.startsWith("Срок:") || seg.startsWith("Сроки:") ||
                            seg.startsWith("Сроки участия:") || seg.startsWith("Период:") ||
                            seg.startsWith("Duration:") -> {
                        duration = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    seg.startsWith("Команда:") || seg.startsWith("Размер команды:") ||
                            seg.startsWith("Team size:") -> {
                        team = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    seg.startsWith("Технологии:") || seg.startsWith("Стек:") ||
                            seg.startsWith("Стек технологий:") || seg.startsWith("Tech stack:") -> {
                        val rawStack = seg.substringAfter(":")
                        val parts = rawStack.split(',', '•', '·', ';')
                        val collected = mutableListOf<String>()
                        for (p in parts) {
                            if (isLikelyJsonNoise(p)) break
                            collected.addAll(splitIfNoSeparators(p))
                        }
                        techs.addAll(
                            joinCompounds(collected.map { cleanToken(it) }).filter {
                                isValidTechToken(
                                    it
                                )
                            }
                        )
                        anyHeaderKey = true
                    }

                    // Ссылка на проект подписывается на обоих языках шаблона
                    seg.startsWith("Ссылка:") || seg.startsWith("Ссылка на проект:") ||
                            seg.startsWith("Link:") || seg.startsWith("Project link:") ||
                            seg.startsWith("URL:") -> {
                        link = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    // Эвристика: первый «простой» сегмент — вероятно роль
                    !seg.contains(":") && role.isEmpty() && (line.contains("Срок") || line.contains(
                        "Команда"
                    ) || line.contains("Технологии") || !headerParsed) -> {
                        role = seg
                        anyHeaderKey = true
                    }
                }
            }

            if (anyHeaderKey) {
                headerParsed = true
                return@forEach
            }

            // Голая ссылка без метки — не описание: забираем в link
            if (link.isEmpty() &&
                (line.startsWith("http://") || line.startsWith("https://")) &&
                !line.contains(' ')
            ) {
                link = line
                return@forEach
            }

            // Контент, не относящийся к заголовку
            when {
                line.startsWith("•") -> if (!isJsonArtifact(line)) resp.add(
                    cleanToken(
                        line.removePrefix(
                            "•"
                        )
                    )
                )

                looksLikeTechList(line) -> {
                    if (!isLikelyJsonNoise(line)) {
                        val parts =
                            line.split(',').map { cleanToken(it) }.filter { isValidTechToken(it) }
                        techs.addAll(joinCompounds(parts))
                    }
                }

                else -> if (!isJsonArtifact(line)) descBuilder.appendLine(cleanToken(line))
            }
        }

        val seen = HashSet<String>()
        val uniqueTechs = mutableListOf<String>()
        joinCompounds(techs.map { cleanToken(it) })
            .filter { it.isNotBlank() }
            .forEach { t ->
                if (seen.add(t.lowercase())) uniqueTechs.add(t)
            }

        return Project(
            name = name,
            role = role,
            duration = duration,
            description = descBuilder.toString().trim(),
            technologies = uniqueTechs,
            responsibilities = resp,
            teamSize = team,
            link = link
        )
    }
}

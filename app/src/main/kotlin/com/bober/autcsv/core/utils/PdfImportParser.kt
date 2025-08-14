package com.bober.autcsv.core.utils

import android.content.Context
import android.net.Uri
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.UUID

object PdfImportParser {

    /**
     * Парсит PDF, сгенерированный приложением, и восстанавливает структуру Resume.
     * Возвращает новый Resume с новым UUID, не зависящий от наличия старого в БД.
     */
    fun parseNewResumeFromPdf(context: Context, uri: Uri): Resume {
        val rawText = readPdfText(context, uri)
        // Сначала пробуем извлечь встроенные JSON‑метаданные для без потерь
        extractEmbeddedJsonResume(rawText)?.let { return it.copy(id = UUID.randomUUID().toString()) }
        val text = cleanPdfText(rawText)
        val sections = splitIntoSections(text)

        val personalInfoBase = parsePersonalInfo(sections["header"].orEmpty())
        val summary = sections["summary"].orEmpty().trim()
        val education = sections["education"].orEmpty().lines().firstOrNull()?.trim().orEmpty()
        val parsedLanguages = parseLanguages(sections["languages"].orEmpty())
        val skills = parseSkills(sections["skills"].orEmpty())
        val achievements = parseAchievements(sections["achievements"].orEmpty())
        val projects = parseProjects(sections["projects"].orEmpty())

        val resumeId = UUID.randomUUID().toString()

        // Если опыт не распознан из шапки, пытаемся вытащить его из всего текста (включая summary)
        val combinedTextForExp = text
        val (fixedTotalExp, fixedSpecExp) =
            if (personalInfoBase.totalExperience.isBlank() || personalInfoBase.specializationExperience.isBlank())
                extractExperienceFromText(
                    combinedTextForExp,
                    personalInfoBase.specialization
                )
            else personalInfoBase.totalExperience to personalInfoBase.specializationExperience

        val personalInfo = personalInfoBase.copy(
            languages = parsedLanguages,
            education = education,
            aboutMe = sections["about"].orEmpty().trim(),
            totalExperience = fixedTotalExp.ifBlank { personalInfoBase.totalExperience },
            specializationExperience = fixedSpecExp.ifBlank { personalInfoBase.specializationExperience },
        )

        return Resume(
            id = resumeId,
            personalInfo = personalInfo,
            professionalSkills = skills.copy(
                professionalAchievements = if (achievements.isNotEmpty()) achievements else skills.professionalAchievements
            ),
            projects = projects,
            summary = summary
        )
    }

    /**
     * Минимальная очистка извлеченного текста PDF от артефактов:
     * - удаление zero‑width символов
     * - нормализация «растянутых» маркеров AUTCSV_JSON_START/END и обрезка содержимого между ними
     */
    private fun cleanPdfText(input: String): String {
        val noZw = input.replace(Regex("[\u200B-\u200D\uFEFF]"), "")
        // Нормализуем размазанные по пробелам маркеры AUTCSV_JSON_START/END
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

    private fun extractExperienceFromText(text: String, specialization: String): Pair<String, String> {
        // 1) Метки
        val labeledTotal = Regex("Опыт:\\s*([^|•\\n]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val labeledSpec = Regex("Опыт по специализации:\\s*([^|•\\n]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        var total = labeledTotal
        var spec = labeledSpec

        // 2) Русские форматы числительных
        val token = Regex("(\\d{1,2})\\s+(год|года|лет|г\\.)", RegexOption.IGNORE_CASE)

        if (spec.isBlank() && specialization.isNotBlank()) {
            val specPhrase = Regex(
                pattern = "(^|\\n)\\s*(\\d{1,2})\\s+(год|года|лет|г\\.)\\s+(в|по)\\s+[^|•\\n]*" + Regex.escape(specialization) + "[^|•\\n]*",
                option = RegexOption.IGNORE_CASE
            )
            specPhrase.find(text)?.let { spec = it.value.trim() }
        }

        if (total.isBlank()) {
            val tokens = token.findAll(text).map { it.value.trim() }.toList()
            val first = tokens.firstOrNull()
            if (!first.isNullOrBlank()) total = first
            if (spec.isBlank() && tokens.size >= 2) spec = tokens[1]
        }

        return total to spec
    }

    private fun readPdfText(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input: InputStream = context.contentResolver.openInputStream(uri) ?: return ""
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                return stripper.getText(document)
            }
        }
    }

    private fun splitIntoSections(text: String): Map<String, String> {
        // Ровно те же русские заголовки, что рисуются в AndroidPdfTemplate
        val lines = text.lines()
        val indices = mutableMapOf<String, Int>()
        fun findIndex(vararg titles: String) =
            lines.indexOfFirst { line -> titles.any { t -> line.contains(t, ignoreCase = true) } }

        val idxSummary = findIndex("Профессиональное резюме")
        val idxAbout = findIndex("О себе")
        val idxEdu = findIndex("Образование")
        val idxLang = findIndex("Языки")
        val idxSkills = findIndex("Технические навыки")
        val idxAch = findIndex("Ключевые достижения")
        val idxProjects = findIndex("Проектный опыт")

        fun slice(start: Int, end: Int): String =
            if (start >= 0 && end > start) lines.subList(start + 1, end).joinToString("\n") else ""

        val headerEnd =
            listOf(idxSummary, idxAbout, idxEdu, idxLang, idxSkills, idxAch, idxProjects)
                .filter { it >= 0 }
                .minOrNull() ?: lines.size

        val summaryEnd = listOf(idxAbout, idxEdu, idxLang, idxSkills, idxAch, idxProjects)
            .filter { it >= 0 }
            .minOrNull() ?: lines.size

        val aboutEnd =
            listOf(idxEdu, idxLang, idxSkills, idxAch, idxProjects).filter { it >= 0 }.minOrNull()
                ?: lines.size
        val eduEnd = listOf(idxLang, idxSkills, idxAch, idxProjects).filter { it >= 0 }.minOrNull()
            ?: lines.size
        val langEnd =
            listOf(idxSkills, idxAch, idxProjects).filter { it >= 0 }.minOrNull() ?: lines.size
        val skillsEnd = listOf(idxAch, idxProjects).filter { it >= 0 }.minOrNull() ?: lines.size
        val achEnd = listOf(idxProjects).filter { it >= 0 }.minOrNull() ?: lines.size

        return mapOf(
            "header" to lines.subList(0, headerEnd).joinToString("\n"),
            "summary" to slice(idxSummary, summaryEnd),
            "about" to slice(idxAbout, aboutEnd),
            "education" to slice(idxEdu, eduEnd),
            "languages" to slice(idxLang, langEnd),
            "skills" to slice(idxSkills, skillsEnd),
            "achievements" to slice(idxAch, achEnd),
            "projects" to if (idxProjects >= 0) lines.subList(idxProjects + 1, lines.size)
                .joinToString("\n") else "",
        )
    }

    private fun extractEmbeddedJsonResume(text: String): Resume? {
        val startMarker = "AUTCSV_JSON_START"
        val endMarker = "AUTCSV_JSON_END"
        val start = text.indexOf(startMarker)
        val end = text.indexOf(endMarker)
        if (start >= 0 && end > start) {
            val json = text.substring(start + startMarker.length, end)
            return runCatching {
                com.google.gson.Gson().fromJson(json, Resume::class.java)
            }.getOrNull()
        }
        return null
    }

    private fun parsePersonalInfo(header: String): PersonalInfo {
        val lines = header.lines().filter { it.isNotBlank() }
        val fullName = lines.firstOrNull()?.trim().orEmpty()
        val secondLine = lines.getOrNull(1).orEmpty()
        val specialization = secondLine.trim()
        val contacts = lines.drop(2).joinToString(" ")

        val email =
            Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(contacts)?.value.orEmpty()
        val phone = Regex("[+]?\\d[\\d\\s().-]{7,}").find(contacts)?.value.orEmpty()
        val contactParts =
            Regex("[•|]|\\s{2,}").split(contacts).map { it.trim() }.filter { it.isNotEmpty() }
        val guessedLocation = contactParts.firstOrNull { part ->
            part != email && part != phone &&
                    !part.contains("@") &&
                    part.count { it.isLetter() } >= 2
        }.orEmpty()

        // Попытка вытащить опыт, если он печатается в заголовке ("Опыт:")
        var totalExp =
            Regex("Опыт:\\s*([^|•\n]+)").find(header)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        var specializationExp =
            Regex("Опыт по специализации:\\s*([^|•\n]+)").find(header)?.groupValues?.getOrNull(1)
                ?.trim().orEmpty()

        // Фоллбек: распознаем русские форматы "1 год", "2 года", "5 лет"
        val yearToken = Regex("\\b(\\\\d{1,2})\\s+(год|года|лет|г\\\\.)\\b", RegexOption.IGNORE_CASE)

        // Попробуем найти опыт по специализации как фразу вида "X год(а/лет) в <специализации>"
        if (specializationExp.isBlank()) {
            val specRegex = if (specialization.isNotBlank()) {
                Regex("\\b(\\\\d{1,2})\\s+(год|года|лет|г\\\\.)\\s+(в|по)\\s+[^|•\\n]*" + Regex.escape(specialization) + "[^|•\\n]*", RegexOption.IGNORE_CASE)
            } else {
                Regex("\\b(\\\\d{1,2})\\s+(год|года|лет|г\\\\.)\\s+(в|по)\\b[^|•\\n]*", RegexOption.IGNORE_CASE)
            }
            specRegex.find(header)?.let { m -> specializationExp = m.value.trim() }
        }

        // Если общий опыт пуст — берем первое вхождение "X год/года/лет", исключая то, что попало в specializationExp
        if (totalExp.isBlank()) {
            val allMatches = yearToken.findAll(header).map { it.value.trim() }.toList()
            val candidate = allMatches.firstOrNull { m -> specializationExp.isBlank() || !specializationExp.contains(m) }
            if (!candidate.isNullOrBlank()) totalExp = candidate
        }

        // Если всё ещё пусты — попробуем увидеть две цифры в одной строке (первую в общий, вторую в специализацию)
        if (totalExp.isBlank() || specializationExp.isBlank()) {
            header.lines().forEach { line ->
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
            totalExperience = totalExp,
            specializationExperience = specializationExp,
            education = "",
            languages = emptyList(),
            email = email,
            phone = phone,
            location = guessedLocation,
            aboutMe = ""
        )
    }

    private fun parseSkills(block: String): ProfessionalSkills {
        val titles = listOf(
            "Операционные системы",
            "Языки программирования",
            "Фреймворки",
            "Библиотеки",
            "Базы данных",
            "Другие технологии",
            "Сертификации",
            "Гибкие навыки"
        )

        fun subsectionBetween(title: String): String {
            val lines = block.lines()
            val start = lines.indexOfFirst { it.contains(title, ignoreCase = true) }
            if (start == -1) return ""
            val end = lines.drop(start + 1)
                .indexOfFirst { line -> titles.any { t -> line.contains(t, ignoreCase = true) } }
            val realEnd = if (end == -1) lines.size else start + 1 + end
            return lines.subList(start + 1, realEnd).joinToString("\n")
        }

        fun parseChips(text: String): List<String> {
            if (text.isBlank()) return emptyList()

            fun cleanToken(raw: String): String {
                val trimmed = raw
                    .replace(Regex("[\u200B-\u200D\uFEFF]"), "") // zero-width
                    .replace(Regex("^[-•]\\s*"), "")
                    .trim()
                val withoutQuotes = trimmed.trim('"', '\'', '[', ']', '{', '}', '“', '”')
                val normalizedSpaces = withoutQuotes.replace(Regex("\\s+"), " ")
                val spacedLetters = Regex("^(?:[\\p{L}]\\s+){3,}[\\p{L}](?:.*)?$")
                return if (spacedLetters.containsMatchIn(normalizedSpaces))
                    normalizedSpaces.replace(Regex("(?<=[\\p{L}])\\s(?=[\\p{L}])"), "")
                else normalizedSpaces
            }

            fun splitIfNoSeparators(part: String): List<String> {
                val p = part.trim()
                if (p.contains('•') || p.contains(',') || p.contains('·') || p.contains(';')) return listOf(p)
                val words = p.split(Regex("\\s+")).filter { it.isNotBlank() }
                return if (words.size >= 3 && words.all { it.firstOrNull()?.isUpperCase() == true }) words else listOf(p)
            }

            fun expandParentheses(token: String): List<String> {
                val m = Regex("^(.*)\\((.+)\\)$").find(token)
                return if (m != null) listOf(m.groupValues[1].trim(), m.groupValues[2].trim()).filter { it.isNotBlank() } else listOf(token)
            }

            fun joinCompounds(parts: List<String>): List<String> {
                if (parts.isEmpty()) return parts
                val result = mutableListOf<String>()
                var i = 0
                while (i < parts.size) {
                    val cur = parts[i]
                    val next = parts.getOrNull(i + 1)
                    if (cur.equals("REST", ignoreCase = true) && next?.equals("API", ignoreCase = true) == true) {
                        result.add("REST API")
                        i += 2
                        continue
                    }
                    if (cur.equals("Celery", ignoreCase = true) && next?.equals("Beat", ignoreCase = true) == true) {
                        result.add("Celery Beat")
                        i += 2
                        continue
                    }
                    result.add(cur)
                    i += 1
                }
                return result
            }

            val rawParts = text.lines().flatMap { line ->
                line.replace(Regex("[\u200B-\u200D\uFEFF]"), "")
                    .replace("  •  ", " • ")
                    .split('•', ',', '·', ';')
                    .flatMap { splitIfNoSeparators(it) }
            }

            val normalized = joinCompounds(rawParts)
                .map { cleanToken(it) }
                .flatMap { expandParentheses(it) }
                .filter { it.isNotBlank() }

            // Дедупликация без учета регистра и лишних пробелов
            val seen = HashSet<String>()
            val unique = mutableListOf<String>()
            normalized.forEach { token ->
                val key = token.lowercase()
                if (key.isNotBlank() && seen.add(key)) unique.add(token)
            }
            return unique
        }

        fun parseBullets(text: String): List<String> =
            text.lines().map { it.replace(Regex("^[-•]\\s*"), "").trim() }
                .filter { it.isNotBlank() }

        val operatingSystems = parseChips(subsectionBetween("Операционные системы"))
        val programmingLanguages = parseChips(subsectionBetween("Языки программирования"))
        val frameworks = parseChips(subsectionBetween("Фреймворки"))
        val libraries = parseChips(subsectionBetween("Библиотеки"))
        val databases = parseChips(subsectionBetween("Базы данных"))
        val otherTechnologies = parseChips(subsectionBetween("Другие технологии"))
        val certifications = parseBullets(subsectionBetween("Сертификации"))
        val softSkills = parseChips(subsectionBetween("Гибкие навыки"))

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

    private fun parseLanguages(block: String): List<Language> {
        if (block.isBlank()) return emptyList()
        return block.lines().mapNotNull { line ->
            val parts = line.split("—", "-").map { it.trim() }
            if (parts.size >= 2) Language(name = parts[0], level = parts[1]) else null
        }
    }

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
            if (line.startsWith("Проект ")) {
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

        fun cleanToken(raw: String): String {
            val trimmed = raw
                .replace(Regex("[\u200B-\u200D\uFEFF]"), "")
                .replace(Regex("^[-•]\\s*"), "")
                .trim()
            val withoutQuotes = trimmed.trim('"', '\'', '[', ']', '{', '}', '“', '”')
            val normalized = withoutQuotes.replace(Regex("\\s+"), " ")
            return normalized
        }

        fun splitIfNoSeparators(part: String): List<String> {
            val p = part.trim()
            if (p.contains('•') || p.contains(',') || p.contains('·') || p.contains(';')) return listOf(p)
            val words = p.split(Regex("\\s+")).filter { it.isNotBlank() }
            return if (words.size >= 3 && words.all { it.firstOrNull()?.isUpperCase() == true }) words else listOf(p)
        }

        fun joinCompounds(parts: List<String>): List<String> {
            if (parts.isEmpty()) return parts
            val result = mutableListOf<String>()
            var i = 0
            while (i < parts.size) {
                val cur = parts[i]
                val next = parts.getOrNull(i + 1)
                if (cur.equals("REST", ignoreCase = true) && next?.equals("API", ignoreCase = true) == true) {
                    result.add("REST API")
                    i += 2
                    continue
                }
                if (cur.equals("Celery", ignoreCase = true) && next?.equals("Beat", ignoreCase = true) == true) {
                    result.add("Celery Beat")
                    i += 2
                    continue
                }
                result.add(cur)
                i += 1
            }
            return result
        }

        fun isJsonArtifact(line: String): Boolean {
            if (line.contains("AUTCSV", ignoreCase = true)) return true
            if (line.contains("_JSON_", ignoreCase = true)) return true
            if (line.contains("\":")) return true
            val jsonChars = charArrayOf('{', '}', '[', ']', '"', ':', '_')
            val jsonCharCount = line.count { ch -> jsonChars.contains(ch) }
            return jsonCharCount >= 3
        }

        fun isLikelyJsonNoise(s: String): Boolean {
            if (isJsonArtifact(s)) return true
            val toks = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val oneLetters = toks.count { it.length == 1 && it[0].isLetter() }
            return toks.size >= 8 && oneLetters >= toks.size / 2
        }

        val allowedShort = setOf("C", "R", "Go", "C#", "C++")
        fun isValidTechToken(token: String): Boolean {
            val t = token.trim()
            if (t.isEmpty()) return false
            if (t in allowedShort) return true
            if (t.length <= 2) return false
            // запретим явный JSON-мусор
            if (t.any { it in charArrayOf('{','}','[',']','"',':','_') }) return false
            return true
        }

        fun cleanLineForDescription(raw: String): String {
            val noZw = raw.replace(Regex("[\u200B-\u200D\uFEFF]"), "").trim()
            val tokens = noZw.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val oneLetterCount = tokens.count { it.length == 1 && it[0].isLetter() }
            val shouldCollapse = tokens.size >= 8 && oneLetterCount >= tokens.size * 0.5
            return if (shouldCollapse)
                noZw.replace(Regex("(?<=[\\p{L}])\\s(?=[\\p{L}])"), "")
            else noZw
        }

        lines.forEach { raw ->
            val line = raw.trim()
            if (line.isBlank()) return@forEach
            // Пропускаем футер вида "AutCSV • 2025-08-12"
            if (line.startsWith("AutCSV")) return@forEach

            if (line.startsWith("Проект ")) {
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

                    seg.startsWith("Срок:") || seg.startsWith("Сроки:") || seg.startsWith("Сроки участия:") || seg.startsWith(
                        "Период:"
                    ) || seg.startsWith("Duration:") -> {
                        duration = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    seg.startsWith("Команда:") || seg.startsWith("Размер команды:") || seg.startsWith(
                        "Team size:"
                    ) -> {
                        team = seg.substringAfter(":").trim()
                        anyHeaderKey = true
                    }

                    seg.startsWith("Технологии:") || seg.startsWith("Стек:") || seg.startsWith("Стек технологий:") || seg.startsWith(
                        "Tech stack:"
                    ) -> {
                        val raw = seg.substringAfter(":")
                        val parts = raw.split(',', '•', '·', ';')
                        val collected = mutableListOf<String>()
                        for (p in parts) {
                            if (isLikelyJsonNoise(p)) break
                            collected.addAll(splitIfNoSeparators(p))
                        }
                        val items = joinCompounds(collected.map { cleanToken(it) })
                            .filter { isValidTechToken(it) }
                        techs.addAll(items)
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

            // Контент, не относящийся к заголовку
            when {
                line.startsWith("•") -> if (!isJsonArtifact(line)) resp.add(cleanLineForDescription(line.removePrefix("•")))
                looksLikeTechList(line) -> {
                    if (!isLikelyJsonNoise(line)) {
                        val parts = line.split(',').map { cleanToken(it) }.filter { isValidTechToken(it) }
                        techs.addAll(joinCompounds(parts))
                    }
                }
                else -> if (!isJsonArtifact(line)) descBuilder.appendLine(cleanLineForDescription(line))
            }
        }
        // Итоговая нормализация и дедупликация технологий
        val uniqueTechs = mutableListOf<String>()
        val seen = HashSet<String>()
        joinCompounds(techs.map { cleanToken(it) })
            .filter { it.isNotBlank() }
            .forEach { t ->
                val key = t.lowercase()
                if (seen.add(key)) uniqueTechs.add(t)
            }

        return Project(
            name = name,
            role = role,
            duration = duration,
            description = descBuilder.toString().trim(),
            technologies = uniqueTechs,
            responsibilities = resp,
            teamSize = team
        )
    }
}


package com.bober.autcsv.presentation.screens.form

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.bober.autcsv.R

/**
 * Справочник подсказок для автодополнения: учебные заведения, специальности
 * по уровням образования, компании, должности, навыки.
 *
 * Локализуемые словари (должности, города, языки, уровни, гибкие навыки,
 * занятость, график, платформы, специальности) читаются из строковых
 * ресурсов values / values-en; неизменяемые бренды и имена собственные
 * (вузы, компании, технологии) остаются в коде.
 *
 * Фильтрация кэшируется в [LruCache] по паре «словарь + запрос», чтобы
 * не пересчитывать список на каждую перерисовку.
 */
object SuggestionDictionary {

    /**
     * Уровни образования в порядке убывания.
     * В БД хранится текстовая метка ([knownLabels] объединяет подписи всех
     * локалей), поэтому старые записи распознаются после смены языка.
     */
    enum class EducationLevel(val labelRes: Int, val knownLabels: Set<String>) {
        SECONDARY_VOCATIONAL(R.string.edu_level_svo, setOf("СПО", "Secondary vocational")),
        BACHELOR(
            R.string.edu_level_bachelor,
            setOf("Высшее (бакалавриат)", "Higher education (Bachelor\u2019s)")
        ),
        SPECIALIST(
            R.string.edu_level_specialist,
            setOf("Высшее (специалитет)", "Higher education (Specialist)")
        ),
        MASTER(
            R.string.edu_level_master,
            setOf("Высшее (магистратура)", "Higher education (Master\u2019s)")
        ),
        POSTGRADUATE(R.string.edu_level_postgraduate, setOf("Послевузовское", "Postgraduate")),
        ADDITIONAL(R.string.edu_level_additional, setOf("Дополнительное", "Additional"));

        /** Подпись на языке текущего интерфейса. */
        val label: String
            @Composable get() = stringResource(labelRes)

        companion object {
            fun fromLabel(label: String): EducationLevel? =
                entries.firstOrNull { label in it.knownLabels }
        }
    }

    /**
     * Готовность к релокации. Значение хранится в домене как текстовая метка;
     * [ready] отмечает варианты, при которых показываются города релокации.
     */
    enum class RelocationOption(
        val labelRes: Int,
        val ready: Boolean,
        val knownLabels: Set<String>,
    ) {
        NOT_READY(
            R.string.reloc_not_ready,
            false,
            setOf("Не готов к переезду", "Not ready to relocate")
        ),
        READY(
            R.string.reloc_ready,
            true,
            setOf("Готов к переезду", "Ready to relocate")
        ),
        CONDITIONAL(
            R.string.reloc_conditional,
            true,
            setOf("Готов при определенных условиях", "Ready under certain conditions")
        );

        /** Подпись на языке текущего интерфейса. */
        val label: String
            @Composable get() = stringResource(labelRes)

        companion object {
            fun fromLabel(label: String): RelocationOption? =
                entries.firstOrNull { label in it.knownLabels }
        }
    }

    // ── Локализуемые словари ────────────────────────────────────────────

    /** Города. */
    val cities: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_cities).toList()

    /** Языки. */
    val languages: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_languages).toList()

    /** Уровни владения языком (шкала как в hh). */
    val languageLevels: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_language_levels).toList()

    /** Должности для автодополнения. */
    val positions: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_positions).toList()

    /** Гибкие навыки. */
    val softSkills: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_soft_skills).toList()

    /** Варианты желаемой занятости (мультивыбор, хранится строкой через запятую). */
    val employmentOptions: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_employment).toList()

    /** Варианты желаемого графика (мультивыбор, хранится строкой через запятую). */
    val workScheduleOptions: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_work_schedule).toList()

    /** Платформы для ссылок на профили кандидата. */
    val socialPlatforms: List<String>
        @Composable get() = stringArrayResource(R.array.suggestion_social_platforms).toList()

    /** Специальности, доступные для выбранного уровня образования (в композиции). */
    @Composable
    fun specialtiesFor(level: EducationLevel): List<String> =
        stringArrayResource(specialtiesArrayFor(level)).toList()

    /** Специальности вне композиции (ViewModel, импорт): локаль контекста. */
    fun specialtiesFor(context: Context, level: EducationLevel): List<String> =
        context.resources.getStringArray(specialtiesArrayFor(level)).toList()

    private fun specialtiesArrayFor(level: EducationLevel): Int = when (level) {
        EducationLevel.SECONDARY_VOCATIONAL -> R.array.suggestion_specialties_svo
        EducationLevel.BACHELOR -> R.array.suggestion_specialties_bachelor
        EducationLevel.SPECIALIST -> R.array.suggestion_specialties_specialist
        EducationLevel.MASTER -> R.array.suggestion_specialties_master
        EducationLevel.POSTGRADUATE -> R.array.suggestion_specialties_postgraduate
        EducationLevel.ADDITIONAL -> R.array.suggestion_specialties_additional
    }

    // ── Неизменяемые словари (имена собственные, бренды) ────────────────

    /** Учебные заведения для автодополнения. */
    val institutions: List<String> = listOf(
        "МГУ имени М.В. Ломоносова",
        "СПбГУ",
        "МФТИ (ГУ)",
        "НИУ ВШЭ",
        "МГТУ им. Н.Э. Баумана",
        "МИФИ (НИЯУ МИФИ)",
        "ИТМО",
        "МИРЭА — Российский технологический университет",
        "МЭИ",
        "СПбГЭТУ «ЛЭТИ»",
        "Новосибирский государственный университет (НГУ)",
        "УрФУ имени Б.Н. Ельцина",
        "КФУ (Казанский федеральный университет)",
        "ЮФУ (Южный федеральный университет)",
        "Томский политехнический университет (ТПУ)",
        "МАИ",
        "МТУСИ",
        "РЭУ имени Г.В. Плеханова",
        "Финансовый университет при Правительстве РФ",
        "Колледж связи № 54",
        "Политехнический колледж им. Н.Н. Годовикова"
    )

    /** Компании для автодополнения. */
    val companies: List<String> = listOf(
        "Яндекс", "Сбер", "VK", "Ozon", "Wildberries", "Т-Банк", "Альфа-Банк",
        "Газпром нефть", "Ростелеком", "X5 Tech", "Авито", "2ГИС", "Lamoda",
        "Самокат", "Kaspersky", "Positive Technologies", "Naumen", "Bell Integrator",
        "EPAM", "Luxoft", "Иннотех", "СберТех", "SberDevices", "Miro", "JetBrains",
        "Huawei", "Intel", "Google", "Microsoft"
    )

    /** Типовые сертификации для автодополнения. */
    val certifications: List<String> = listOf(
        "Google Associate Android Developer", "Kotlin Certified Professional",
        "AWS Certified Solutions Architect", "Microsoft Certified: Azure Developer",
        "Oracle Certified Professional: Java SE", "Cisco CCNA", "ISTQB Foundation",
        "Google Analytics Certification", "Scrum Master (PSM I)", "Docker Certified Associate"
    )

    /** Технологии и навыки по категориям. */
    val operatingSystems: List<String> =
        listOf(
            "Linux",
            "Windows",
            "macOS",
            "Android",
            "iOS",
            "Ubuntu",
            "CentOS",
            "Debian",
            "Fedora"
        )
    val programmingLanguages: List<String> = listOf(
        "Kotlin", "Java", "Python", "C++", "C#", "Go", "Swift", "JavaScript",
        "TypeScript", "PHP", "Ruby", "Rust", "Scala", "Dart", "SQL", "1C"
    )
    val frameworks: List<String> = listOf(
        "Jetpack Compose", "Spring", "Spring Boot", "Django", "Flask", "FastAPI",
        "React", "Vue.js", "Angular", "Flutter", "Ktor", "Qt", "ASP.NET Core", "Ruby on Rails"
    )
    val libraries: List<String> = listOf(
        "Room", "Retrofit", "OkHttp", "Glide", "Coil", "Dagger 2", "Hilt",
        "Koin", "Coroutines", "RxJava", "TensorFlow", "PyTorch", "NumPy", "Pandas"
    )
    val databases: List<String> = listOf(
        "PostgreSQL", "MySQL", "SQLite", "MongoDB", "Redis", "Oracle DB",
        "Microsoft SQL Server", "Elasticsearch", "ClickHouse", "Cassandra"
    )
    val otherTechnologies: List<String> = listOf(
        "Docker", "Kubernetes", "Git", "CI/CD", "Jenkins", "GitLab CI",
        "GitHub Actions", "Gradle", "Maven", "Nginx", "Kafka", "RabbitMQ", "gRPC"
    )

    /** Кэш результатов фильтрации: ключ — словарь + нормализованный запрос. */
    private val filterCache = LruCache<String, List<String>>(128)

    private fun normalize(s: String): String =
        s.trim().lowercase().replace("ё", "е")

    /**
     * Фильтрует словарь по префиксу/подстроке с кэшированием результата.
     * Пустой запрос возвращает первые [limit] элементов словаря.
     */
    fun filter(dictionary: List<String>, query: String, limit: Int = 6): List<String> {
        val key = "${System.identityHashCode(dictionary)}|${normalize(query)}"
        filterCache.get(key)?.let { return it }
        val result = if (query.isBlank()) {
            dictionary.take(limit)
        } else {
            val q = normalize(query)
            dictionary.filter { normalize(it).contains(q) }.take(limit)
        }
        filterCache.put(key, result)
        return result
    }
}

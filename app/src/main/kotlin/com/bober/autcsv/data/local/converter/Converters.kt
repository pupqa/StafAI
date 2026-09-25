package com.bober.autcsv.data.local.converter

import androidx.room.TypeConverter
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.SocialLink
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/**
 * TypeConverter'ы Room для списков и сложных объектов (AiAnalysis, Project,
 * Language, SocialLink). Реализованы через Gson.
 *
 * Важно: Gson обходит null-safety Kotlin — битая или легаси-строка в колонке
 * иначе вернула бы объект с null в non-null полях или бросила исключение прямо
 * в запросе. Поэтому каждый разбор обёрнут в try/catch с безопасным дефолтом:
 * испорченное поле даёт пустой список/дефолтный объект, а не краш списка резюме.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
            /** Сериализация списка языков в JSON. */
    fun fromLanguageList(value: List<Language>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список языков. */
    fun toLanguageList(value: String): List<Language> {
        return safeParse<List<Language>>(
            value,
            object : TypeToken<List<Language>>() {}.type,
            emptyList()
        )
            .map { it.sanitized() }
    }

    @TypeConverter
            /** Сериализация списка ссылок на соцсети в JSON. */
    fun fromSocialLinkList(value: List<SocialLink>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список ссылок на соцсети. */
    fun toSocialLinkList(value: String): List<SocialLink> {
        return safeParse<List<SocialLink>>(
            value,
            object : TypeToken<List<SocialLink>>() {}.type,
            emptyList()
        )
            .map { it.sanitized() }
    }

    @TypeConverter
            /** Сериализация списка строк в JSON. */
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список строк. */
    fun toStringList(value: String): List<String> {
        return safeParse(value, object : TypeToken<List<String>>() {}.type, emptyList())
    }

    @TypeConverter
            /** Сериализация списка записей об образовании в JSON. */
    fun fromEducationEntryList(value: List<EducationEntry>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список записей об образовании. */
    fun toEducationEntryList(value: String): List<EducationEntry> {
        return safeParse<List<EducationEntry>>(
            value,
            object : TypeToken<List<EducationEntry>>() {}.type,
            emptyList()
        )
            .map { it.sanitized() }
    }

    @TypeConverter
            /** Сериализация списка проектов в JSON. */
    fun fromProjectList(value: List<Project>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список проектов. */
    fun toProjectList(value: String): List<Project> {
        return safeParse<List<Project>>(
            value,
            object : TypeToken<List<Project>>() {}.type,
            emptyList()
        )
            .map { it.sanitized() }
    }

    @TypeConverter
            /** Сериализация анализа ИИ в JSON. */
    fun fromAiAnalysis(value: AiAnalysis): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в анализ ИИ. */
    fun toAiAnalysis(value: String): AiAnalysis {
        return safeParse(value, AiAnalysis::class.java, AiAnalysis())
    }

    /** Разбор с защитой от битых/легаси-данных: ошибка или null -> [fallback]. */
    private fun <T> safeParse(value: String, type: java.lang.reflect.Type, fallback: T): T {
        if (value.isBlank()) return fallback
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<T>(value, type) ?: fallback
        } catch (_: JsonSyntaxException) {
            fallback
        } catch (_: IllegalStateException) {
            // Gson бросает его при неожиданной структуре JSON-дерева
            fallback
        }
    }
}

/**
 * Gson создаёт объекты без вызова конструктора (Unsafe.allocateInstance),
 * поэтому в полях non-null Kotlin-типов могут оказаться null — например,
 * когда поле появилось в модели позже, чем строка записана в базу
 * (краш «parameter specified as non-null is null» при открытии резюме).
 * Санитайзеры заполняют такие поля дефолтами сразу на границе БД.
 */
private fun Project.sanitized(): Project = copy(
    name = name.orNullSafe(),
    role = role.orNullSafe(),
    duration = duration.orNullSafe(),
    description = description.orNullSafe(),
    teamSize = teamSize.orNullSafe(),
    link = link.orNullSafe(),
    technologies = technologies.orNullSafeList(),
    responsibilities = responsibilities.orNullSafeList(),
)

private fun SocialLink.sanitized(): SocialLink = copy(
    platform = platform.orNullSafe(),
    url = url.orNullSafe(),
)

private fun Language.sanitized(): Language = Language(
    name = name.orNullSafe(),
    level = level.orNullSafe(),
)

private fun EducationEntry.sanitized(): EducationEntry = EducationEntry(
    level = level.orNullSafe(),
    specialty = specialty.orNullSafe(),
    institution = institution.orNullSafe(),
)

/** Расширение на nullable-приёмнике: не вставляет intrinsic null-check Kotlin. */
private fun String?.orNullSafe(): String = this ?: ""

@Suppress("UNCHECKED_CAST")
private fun List<String>?.orNullSafeList(): List<String> =
    (this as? List<String>)?.filterNotNull() ?: emptyList()

package com.bober.autcsv.data.local.converter

import androidx.room.TypeConverter
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.Project
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TypeConverter'ы Room для списков, карт и сложных объектов (AiAnalysis, Project, Language).
 * Реализованы через Gson.
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
        val type = object : TypeToken<List<Language>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
            /** Сериализация списка строк в JSON. */
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список строк. */
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
            /** Сериализация списка проектов в JSON. */
    fun fromProjectList(value: List<Project>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в список проектов. */
    fun toProjectList(value: String): List<Project> {
        val type = object : TypeToken<List<Project>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
            /** Сериализация анализа ИИ в JSON. */
    fun fromAiAnalysis(value: AiAnalysis): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в анализ ИИ. */
    fun toAiAnalysis(value: String): AiAnalysis {
        return gson.fromJson(value, AiAnalysis::class.java)
    }

    @TypeConverter
            /** Сериализация карты строк в JSON. */
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
            /** Десериализация JSON в карту строк. */
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
} 
package com.bober.autcsv.data.local.converter

import androidx.room.TypeConverter
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.Project
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLanguageList(value: List<Language>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLanguageList(value: String): List<Language> {
        val type = object : TypeToken<List<Language>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromProjectList(value: List<Project>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProjectList(value: String): List<Project> {
        val type = object : TypeToken<List<Project>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromAiAnalysis(value: AiAnalysis): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAiAnalysis(value: String): AiAnalysis {
        return gson.fromJson(value, AiAnalysis::class.java)
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
} 
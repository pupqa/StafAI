package com.bober.autcsv.core.utils

import com.bober.autcsv.domain.model.Resume

/**
 * Подготовка резюме к машинному переводу: извлекает переводимые поля
 * плоским списком «путь → значение», применяет переводы обратно.
 *
 * Не переводится (намеренно): имя кандидата, контакты, ссылки, стек
 * технологий (бренды), зарплаты, фото, служебные поля. Уровень образования
 * и варианты релокации/занятости идут как свободный текст — их переведённые
 * значения остаются читаемыми, а обратное распознавание меток работает
 * на обоих языках (knownLabels в SuggestionDictionary).
 */
object ResumeTranslator {

    /** Переводимое поле: путь внутри JSON-структуры и исходное значение. */
    data class Field(val path: String, val value: String)

    /** Извлекает непустые переводимые поля. Порядок стабилен. */
    fun extractTranslatable(resume: Resume): List<ResumeTranslator.Field> {
        val fields = mutableListOf<Field>()
        fun add(path: String, value: String?) {
            if (!value.isNullOrBlank()) fields.add(Field(path, value))
        }

        add("summary", resume.summary)

        val p = resume.personalInfo
        add("personalInfo.specialization", p.specialization)
        add("personalInfo.aboutMe", p.aboutMe)
        add("personalInfo.education", p.education)
        add("personalInfo.location", p.location)
        add("personalInfo.readyToRelocate", p.readyToRelocate)
        add("personalInfo.relocationCities", p.relocationCities)
        add("personalInfo.employment", p.employment)
        add("personalInfo.workSchedule", p.workSchedule)

        p.educations.forEachIndexed { i, e ->
            add("educations[$i].level", e.level)
            add("educations[$i].specialty", e.specialty)
            add("educations[$i].institution", e.institution)
        }

        resume.projects.forEachIndexed { i, project ->
            add("projects[$i].name", project.name)
            add("projects[$i].role", project.role)
            add("projects[$i].duration", project.duration)
            add("projects[$i].description", project.description)
            add("projects[$i].teamSize", project.teamSize)
            project.responsibilities.forEachIndexed { j, line ->
                add("projects[$i].responsibilities[$j]", line)
            }
        }

        resume.professionalSkills.softSkills.forEachIndexed { k, skill ->
            add("professionalSkills.softSkills[$k]", skill)
        }

        return fields
    }

    /**
     * Возвращает копию резюме с применёнными переводами по путям.
     * Пустые и неизвестные переводы игнорируются — структура не меняется.
     */
    fun applyTranslations(resume: Resume, translations: Map<String, String>): Resume {
        if (translations.isEmpty()) return resume

        fun tr(path: String, original: String): String =
            translations[path]?.takeIf { it.isNotBlank() } ?: original

        val personal = resume.personalInfo
        val educations = personal.educations.mapIndexed { i, entry ->
            entry.copy(
                level = tr("educations[$i].level", entry.level),
                specialty = tr("educations[$i].specialty", entry.specialty),
                institution = tr("educations[$i].institution", entry.institution),
            )
        }
        val projects = resume.projects.mapIndexed { i, project ->
            project.copy(
                name = tr("projects[$i].name", project.name),
                role = tr("projects[$i].role", project.role),
                duration = tr("projects[$i].duration", project.duration),
                description = tr("projects[$i].description", project.description),
                teamSize = tr("projects[$i].teamSize", project.teamSize),
                responsibilities = project.responsibilities.mapIndexed { j, line ->
                    tr("projects[$i].responsibilities[$j]", line)
                },
            )
        }
        val skills = resume.professionalSkills
        val softSkills = skills.softSkills.mapIndexed { k, skill ->
            tr("professionalSkills.softSkills[$k]", skill)
        }

        return resume.copy(
            summary = tr("summary", resume.summary),
            personalInfo = personal.copy(
                specialization = tr("personalInfo.specialization", personal.specialization),
                aboutMe = tr("personalInfo.aboutMe", personal.aboutMe),
                education = tr("personalInfo.education", personal.education),
                location = tr("personalInfo.location", personal.location),
                readyToRelocate = tr("personalInfo.readyToRelocate", personal.readyToRelocate),
                relocationCities = tr("personalInfo.relocationCities", personal.relocationCities),
                employment = tr("personalInfo.employment", personal.employment),
                workSchedule = tr("personalInfo.workSchedule", personal.workSchedule),
                educations = educations,
            ),
            professionalSkills = skills.copy(softSkills = softSkills),
            projects = projects,
        )
    }
}

package com.bober.autcsv.presentation.screens.form

import android.util.Patterns
import androidx.annotation.StringRes
import com.bober.autcsv.R

/**
 * Вынесенная из ViewModel валидация полевого уровня и расчёт прогресса
 * заполнения. Чистые функции — легко тестировать и мемоизировать.
 *
 * Ошибки возвращаются как @StringRes-идентификаторы: локаль применяется
 * в точке отображения (stringResource), поэтому тексты всегда на языке UI.
 */
object FormValidators {

    /** Идентификаторы полей с валидацией в реальном времени. */
    enum class Field {
        FULL_NAME, SPECIALIZATION, TOTAL_EXPERIENCE, EMAIL, PHONE,
        SALARY_MIN, SALARY_MAX, SOCIAL_URL
    }

    /**
     * Возвращает ресурс с текстом ошибки для поля или null, если значение корректно.
     * Обязательные поля ругаются только после касания (пустое значение —
     * не ошибка, чтобы не пугать пользователя на старте).
     */
    @StringRes
    fun validateField(field: Field, value: String, touched: Boolean = true): Int? {
        if (!touched) return null
        return when (field) {
            Field.FULL_NAME ->
                if (value.isBlank()) R.string.validation_full_name else null

            Field.SPECIALIZATION ->
                if (value.isBlank()) R.string.validation_specialization else null

            Field.TOTAL_EXPERIENCE ->
                when {
                    value.isBlank() -> R.string.validation_total_experience
                    value.toIntOrNull() == null -> R.string.validation_experience_number
                    else -> null
                }

            Field.EMAIL ->
                if (value.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(value).matches())
                    R.string.validation_email_invalid
                else null

            Field.PHONE ->
                if (value.isNotEmpty() && value.filter { it.isDigit() }.length < 10)
                    R.string.validation_phone_short
                else null

            Field.SALARY_MIN ->
                when {
                    value.isBlank() -> R.string.validation_salary_from_required
                    !isValidSalary(value) -> R.string.validation_salary_number
                    else -> null
                }

            Field.SALARY_MAX ->
                if (value.isNotEmpty() && !isValidSalary(value))
                    R.string.validation_salary_number
                else null

            Field.SOCIAL_URL ->
                if (value.isNotEmpty() && !isValidSocialUrl(value))
                    R.string.validation_social_link_invalid
                else null
        }
    }

    /**
     * Ссылка на профиль: полный URL, домен без схемы или telegram-@ник.
     * Пустое значение допустимо — поле необязательное.
     */
    fun isValidSocialUrl(value: String): Boolean {
        val v = value.trim()
        if (v.isBlank()) return true
        if (v.startsWith("@")) return v.length > 1
        val withScheme = if (v.startsWith("http://") || v.startsWith("https://")) v
        else "https://$v"
        return Patterns.WEB_URL.matcher(withScheme).matches()
    }

    /**
     * Зарплата: цифры, допускающие разделители разрядов (узкий пробел из
     * NumberFormatting.groupThousands). «150 000» — валидно, «150 тыс» — нет.
     */
    fun isValidSalary(value: String): Boolean {
        val normalized = value.replace('\u202F', ' ')
        if (normalized.any { !it.isDigit() && it != ' ' }) return false
        return normalized.filter { it.isDigit() }.toLongOrNull() != null
    }

    /**
     * Согласованность зарплатной вилки: максимум не меньше минимума.
     * Ошибку показываем у поля «до».
     */
    @StringRes
    fun validateSalaryRange(min: String, max: String): Int? {
        val minNum = min.filter { it.isDigit() }.toLongOrNull() ?: return null
        val maxNum = max.filter { it.isDigit() }.toLongOrNull() ?: return null
        return if (maxNum < minNum) R.string.validation_salary_range else null
    }

    /** Процент заполненности формы (0..100) по ключевым блокам. */
    fun progressPercent(state: ResumeFormState): Int {
        var filled = 0
        val total = 18
        fun check(condition: Boolean) {
            if (condition) filled++
        }
        check(state.fullName.isNotBlank())                       // 1
        check(state.specialization.isNotBlank())                 // 2
        check(state.totalExperience.isNotBlank())                // 3
        check(state.specializationExperience.isNotBlank())       // 4
        check(state.educations.any { !it.isBlankEntry() })          // 5
        check(state.email.isNotBlank())                          // 6
        check(state.phone.isNotBlank())                          // 7
        check(state.location.isNotBlank())                       // 8
        check(state.aboutMe.isNotBlank())                        // 9
        check(state.languages.any { it.name.isNotBlank() })      // 10
        check(
            state.programmingLanguages.isNotBlank() ||
                    state.frameworks.isNotBlank() ||
                    state.libraries.isNotBlank() ||
                    state.databases.isNotBlank() ||
                    state.otherTechnologies.isNotBlank()
        )                                                        // 11 — hard skills
        check(state.softSkills.isNotBlank())                     // 12 — soft skills
        check(state.projects.any { it.name.isNotBlank() })        // 13
        check(state.professionalAchievements.any { it.isNotBlank() }) // 14
        check(state.salaryMin.isNotBlank())                      // 15 — зарплатная вилка
        check(state.readyToRelocate.isNotBlank())                // 16 — релокация
        check(state.socialLinks.any { it.url.isNotBlank() })     // 17 — соцсети
        check(state.employment.isNotBlank() || state.workSchedule.isNotBlank() || state.photoUri.isNotBlank()) // 18 — условия
        return (filled * 100 / total).coerceIn(0, 100)
    }

    /**
     * Прогресс заполнения по вкладкам формы (№23): возвращает пару
     * «заполнено / всего» для вкладки с индексом [tabIndex].
     * Порядок индексов совпадает с FormTab: 0 — личное, 1 — образование,
     * 2 — языки, 3 — навыки, 4 — проекты.
     */
    fun tabProgress(tabIndex: Int, state: ResumeFormState): Pair<Int, Int> {
        val checks: List<Boolean> = when (tabIndex) {
            0 -> listOf(
                state.fullName.isNotBlank(),
                state.specialization.isNotBlank(),
                state.totalExperience.isNotBlank(),
                state.email.isNotBlank(),
                state.phone.isNotBlank(),
                state.location.isNotBlank(),
                state.aboutMe.isNotBlank(),
            )

            1 -> listOf(
                state.educations.any { it.level.isNotBlank() },
                state.educations.any { it.specialty.isNotBlank() },
                state.educations.any { it.institution.isNotBlank() },
            )

            2 -> listOf(
                state.languages.any { it.name.isNotBlank() },
                state.languages.any { it.level.isNotBlank() },
            )

            3 -> listOf(
                state.operatingSystems.isNotBlank() ||
                        state.programmingLanguages.isNotBlank() ||
                        state.frameworks.isNotBlank() ||
                        state.libraries.isNotBlank() ||
                        state.databases.isNotBlank() ||
                        state.otherTechnologies.isNotBlank(),
                state.certifications.isNotBlank(),
                state.softSkills.isNotBlank(),
                state.professionalAchievements.any { it.isNotBlank() },
            )

            4 -> listOf(
                state.projects.any { it.name.isNotBlank() },
                state.projects.any {
                    it.description.isNotBlank() || it.responsibilitiesText.isNotBlank()
                },
            )

            else -> return 0 to 0
        }
        return checks.count { it } to checks.size
    }
}

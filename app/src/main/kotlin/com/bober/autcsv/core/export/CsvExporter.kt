package com.bober.autcsv.core.export

import android.content.Context
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.AppLocales
import com.bober.autcsv.domain.model.Resume

/**
 * Экспорт резюме в CSV (№12): одна строка = одно резюме.
 *
 * Формат — RFC 4180 (разделитель «;» и BOM UTF-8, чтобы файл корректно
 * открывался в Excel с русской локалью). Колонки плоские: списковые поля
 * (языки, навыки, проекты) сворачиваются в одну ячейку через « | ».
 *
 * Заголовки колонок локализуются по языку приложения.
 */
object CsvExporter {

    /** CSV для одного резюме: строка заголовка + строка данных. */
    fun export(context: Context, resume: Resume): String {
        val ctx = AppLocales.localizedContext(context)
        return buildString {
            append('\uFEFF') // BOM: Excel понимает, что файл в UTF-8
            appendHeader(ctx)
            appendRow(row(ctx, resume))
        }
    }

    /** CSV для всей базы: заголовок + по строке на резюме. */
    fun exportAll(context: Context, resumes: List<Resume>): String {
        val ctx = AppLocales.localizedContext(context)
        return buildString {
            append('\uFEFF')
            appendHeader(ctx)
            resumes.forEach { appendRow(row(ctx, it)) }
        }
    }

    private fun columns(ctx: Context): List<String> = listOf(
        ctx.getString(R.string.csv_col_full_name),
        ctx.getString(R.string.csv_col_position),
        ctx.getString(R.string.csv_col_total_experience),
        ctx.getString(R.string.csv_col_spec_experience),
        ctx.getString(R.string.csv_col_email),
        ctx.getString(R.string.csv_col_phone),
        ctx.getString(R.string.csv_col_city),
        ctx.getString(R.string.csv_col_salary_from),
        ctx.getString(R.string.csv_col_salary_to),
        ctx.getString(R.string.csv_col_relocation),
        ctx.getString(R.string.csv_col_relocation_cities),
        ctx.getString(R.string.csv_col_employment),
        ctx.getString(R.string.csv_col_schedule),
        ctx.getString(R.string.csv_col_education),
        ctx.getString(R.string.csv_col_languages),
        ctx.getString(R.string.csv_col_os),
        ctx.getString(R.string.csv_col_prog_langs),
        ctx.getString(R.string.csv_col_frameworks),
        ctx.getString(R.string.csv_col_libraries),
        ctx.getString(R.string.csv_col_databases),
        ctx.getString(R.string.csv_col_other_tech),
        ctx.getString(R.string.csv_col_certifications),
        ctx.getString(R.string.csv_col_soft_skills),
        ctx.getString(R.string.csv_col_achievements),
        ctx.getString(R.string.csv_col_projects),
        ctx.getString(R.string.csv_col_project_links),
        ctx.getString(R.string.csv_col_about),
        ctx.getString(R.string.csv_col_status),
        ctx.getString(R.string.csv_col_favorite),
        ctx.getString(R.string.csv_col_modified),
    )

    private fun StringBuilder.appendHeader(ctx: Context) {
        append(columns(ctx).joinToString(";") { escape(it) })
        append("\r\n")
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        append(values.joinToString(";") { escape(it) })
        append("\r\n")
    }

    /**
     * Экранирование по RFC 4180: кавычки удваиваются, обрамляем при спецсимволах.
     * Дополнительно нейтрализуем formula injection: Excel/LibreOffice исполняют
     * ячейки, начинающиеся с =,+,-,@, поэтому префиксим их апострофом.
     */
    internal fun escape(value: String): String {
        val safe = if (value.isNotEmpty() && value.first() in "=+-@") "'$value" else value
        val needsQuoting = safe.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"${safe.replace("\"", "\"\"")}\"" else safe
    }

    private fun row(ctx: Context, resume: Resume): List<String> {
        val p = resume.personalInfo
        val s = resume.professionalSkills
        return listOf(
            p.fullName,
            p.specialization,
            p.totalExperience,
            p.specializationExperience,
            p.email,
            p.phone,
            p.location,
            p.salaryMin,
            p.salaryMax,
            p.readyToRelocate,
            p.relocationCities,
            p.employment,
            p.workSchedule,
            p.education,
            p.languages.filter { it.name.isNotBlank() }
                .joinToString(" | ") { "${it.name}: ${it.level}" },
            s.operatingSystems.joinToString(" | "),
            s.programmingLanguages.joinToString(" | "),
            s.frameworks.joinToString(" | "),
            s.libraries.joinToString(" | "),
            s.databases.joinToString(" | "),
            s.otherTechnologies.joinToString(" | "),
            s.certifications.joinToString(" | "),
            s.softSkills.joinToString(" | "),
            s.professionalAchievements.joinToString(" | "),
            resume.projects.joinToString(" | ") { project ->
                listOf(project.name, project.role, project.duration)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
            },
            resume.projects.map { it.link }
                .filter { it.isNotBlank() }
                .joinToString(" | "),
            p.aboutMe,
            ctx.getString(resume.status.labelRes),
            if (resume.isFavorite) ctx.getString(R.string.csv_yes) else ctx.getString(R.string.csv_no),
            java.time.Instant.ofEpochMilli(resume.lastModified)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString(),
        )
    }
}

package com.bober.autcsv.domain.model

import androidx.annotation.StringRes
import com.bober.autcsv.R

/**
 * Статус кандидата в пайплайне рекрутера.
 * NONE — новый кандидат без статуса.
 * В БД хранится [name], поэтому подпись ([labelRes]) можно свободно локализовать.
 */
enum class CandidateStatus(@StringRes val labelRes: Int) {
    NONE(R.string.status_none),
    SCREENING(R.string.status_screening),
    INTERVIEW(R.string.status_interview),
    OFFER(R.string.status_offer),
    REJECTED(R.string.status_rejected);

    companion object {
        fun fromName(name: String?): CandidateStatus =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

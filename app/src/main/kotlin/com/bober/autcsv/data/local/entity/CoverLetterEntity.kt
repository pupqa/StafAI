package com.bober.autcsv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bober.autcsv.domain.model.CoverLetter

/** Таблица сопроводительных писем; письма живут отдельно от резюме. */
@Entity(
    tableName = "cover_letters",
    // Индекс обязан совпадать с тем, что создаёт MIGRATION_8_9,
    // иначе валидация схемы Room падает после миграции
    indices = [Index(value = ["resumeId"])],
)
data class CoverLetterEntity(
    @PrimaryKey val id: String,
    val resumeId: String,
    val vacancyTitle: String,
    val vacancyText: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomainModel(): CoverLetter = CoverLetter(
        id = id,
        resumeId = resumeId,
        vacancyTitle = vacancyTitle,
        vacancyText = vacancyText,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomainModel(letter: CoverLetter): CoverLetterEntity = CoverLetterEntity(
            id = letter.id,
            resumeId = letter.resumeId,
            vacancyTitle = letter.vacancyTitle,
            vacancyText = letter.vacancyText,
            content = letter.content,
            createdAt = letter.createdAt,
            updatedAt = letter.updatedAt,
        )
    }
}

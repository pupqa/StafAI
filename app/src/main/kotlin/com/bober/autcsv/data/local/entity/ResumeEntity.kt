package com.bober.autcsv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bober.autcsv.domain.model.AiAnalysis
import com.bober.autcsv.domain.model.CandidateStatus
import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.PersonalInfo
import com.bober.autcsv.domain.model.ProfessionalSkills
import com.bober.autcsv.domain.model.Project
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.domain.model.SocialLink

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val specialization: String,
    val totalExperience: String,
    val specializationExperience: String,
    val education: String,
    val educations: List<EducationEntry>,
    val email: String,
    val phone: String,
    val location: String,
    val aboutMe: String,
    val salaryMin: String,
    val salaryMax: String,
    val readyToRelocate: String,
    val relocationCities: String,
    val socialLinks: List<SocialLink>,
    val employment: String,
    val workSchedule: String,
    val photoUri: String,
    val status: String,
    val isFavorite: Boolean,
    val languages: List<Language>,
    val operatingSystems: List<String>,
    val programmingLanguages: List<String>,
    val frameworks: List<String>,
    val libraries: List<String>,
    val databases: List<String>,
    val otherTechnologies: List<String>,
    val professionalAchievements: List<String>,
    val certifications: List<String>,
    val softSkills: List<String>,
    val projects: List<Project>,
    val summary: String,
    val lastModified: Long,
    val aiAnalysis: AiAnalysis,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
) {
    fun toDomainModel(): Resume {
        return Resume(
            id = id,
            personalInfo = PersonalInfo(
                fullName = fullName,
                specialization = specialization,
                totalExperience = totalExperience,
                specializationExperience = specializationExperience,
                education = education,
                educations = educations,
                languages = languages,
                email = email,
                phone = phone,
                location = location,
                aboutMe = aboutMe,
                salaryMin = salaryMin,
                salaryMax = salaryMax,
                readyToRelocate = readyToRelocate,
                relocationCities = relocationCities,
                socialLinks = socialLinks,
                employment = employment,
                workSchedule = workSchedule,
                photoUri = photoUri
            ),
            professionalSkills = ProfessionalSkills(
                operatingSystems = operatingSystems,
                programmingLanguages = programmingLanguages,
                frameworks = frameworks,
                libraries = libraries,
                databases = databases,
                otherTechnologies = otherTechnologies,
                professionalAchievements = professionalAchievements,
                certifications = certifications,
                softSkills = softSkills
            ),
            projects = projects,
            summary = summary,
            aiAnalysis = aiAnalysis,
            lastModified = lastModified,
            isDeleted = isDeleted,
            deletedAt = deletedAt,
            status = CandidateStatus.fromName(status),
            isFavorite = isFavorite
        )
    }

    companion object {
        /**
         * Создает сущность Room из доменной модели [Resume] для сохранения в БД.
         */
        fun fromDomainModel(resume: Resume): ResumeEntity {
            return ResumeEntity(
                id = resume.id,
                fullName = resume.personalInfo.fullName,
                specialization = resume.personalInfo.specialization,
                totalExperience = resume.personalInfo.totalExperience,
                specializationExperience = resume.personalInfo.specializationExperience,
                education = resume.personalInfo.education,
                educations = resume.personalInfo.educations,
                email = resume.personalInfo.email,
                phone = resume.personalInfo.phone,
                location = resume.personalInfo.location,
                aboutMe = resume.personalInfo.aboutMe,
                salaryMin = resume.personalInfo.salaryMin,
                salaryMax = resume.personalInfo.salaryMax,
                readyToRelocate = resume.personalInfo.readyToRelocate,
                relocationCities = resume.personalInfo.relocationCities,
                socialLinks = resume.personalInfo.socialLinks,
                employment = resume.personalInfo.employment,
                workSchedule = resume.personalInfo.workSchedule,
                photoUri = resume.personalInfo.photoUri,
                languages = resume.personalInfo.languages,
                operatingSystems = resume.professionalSkills.operatingSystems,
                programmingLanguages = resume.professionalSkills.programmingLanguages,
                frameworks = resume.professionalSkills.frameworks,
                libraries = resume.professionalSkills.libraries,
                databases = resume.professionalSkills.databases,
                otherTechnologies = resume.professionalSkills.otherTechnologies,
                professionalAchievements = resume.professionalSkills.professionalAchievements,
                certifications = resume.professionalSkills.certifications,
                softSkills = resume.professionalSkills.softSkills,
                projects = resume.projects,
                summary = resume.summary,
                lastModified = resume.lastModified,
                aiAnalysis = resume.aiAnalysis,
                isDeleted = resume.isDeleted,
                deletedAt = resume.deletedAt,
                status = resume.status.name,
                isFavorite = resume.isFavorite
            )
        }
    }
}
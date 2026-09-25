package com.bober.autcsv.presentation.screens.form

import com.bober.autcsv.domain.model.EducationEntry
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.domain.model.SocialLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты валидации полей формы, зарплатной вилки и расчёта прогресса
 * заполнения (общего и по вкладкам).
 */
class FormValidatorsTest {

    // ── validateField ───────────────────────────────────────────────────

    @Test
    fun `untouched field never reports error`() {
        assertNull(
            FormValidators.validateField(
                FormValidators.Field.FULL_NAME,
                value = "",
                touched = false
            )
        )
        assertNull(
            FormValidators.validateField(
                FormValidators.Field.EMAIL,
                value = "не-email",
                touched = false
            )
        )
    }

    @Test
    fun `required fields reject blank values`() {
        assertNotNull(FormValidators.validateField(FormValidators.Field.FULL_NAME, "  "))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SPECIALIZATION, ""))
        assertNotNull(FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, ""))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SALARY_MIN, " "))
    }

    @Test
    fun `experience must be a number`() {
        assertNotNull(FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, "пять"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, "5лет"))
        assertNull(FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, "5"))
        assertNull(FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, "12"))
    }

    @Test
    fun `email optional but must be valid when filled`() {
        assertNull(FormValidators.validateField(FormValidators.Field.EMAIL, ""))
        assertNull(FormValidators.validateField(FormValidators.Field.EMAIL, "ivan.petrov@mail.ru"))
        assertNull(FormValidators.validateField(FormValidators.Field.EMAIL, "a@b.io"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.EMAIL, "ivan@"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.EMAIL, "ivan собака mail.ru"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.EMAIL, "@mail.ru"))
    }

    @Test
    fun `phone needs at least 10 digits`() {
        assertNull(FormValidators.validateField(FormValidators.Field.PHONE, ""))
        assertNull(FormValidators.validateField(FormValidators.Field.PHONE, "+7 921 123-45-67"))
        assertNull(FormValidators.validateField(FormValidators.Field.PHONE, "89211234567"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.PHONE, "12345"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.PHONE, "+7 921"))
    }

    @Test
    fun `salary fields must be numeric`() {
        assertNull(FormValidators.validateField(FormValidators.Field.SALARY_MIN, "100000"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SALARY_MIN, "100к"))
        assertNull(FormValidators.validateField(FormValidators.Field.SALARY_MAX, ""))
        assertNull(FormValidators.validateField(FormValidators.Field.SALARY_MAX, "200000"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SALARY_MAX, "двести"))
    }

    @Test
    fun `social url accepts url host-only and nick`() {
        assertNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, ""))
        assertNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, "@ivan_dev"))
        assertNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, "github.com/ivan"))
        assertNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, "https://t.me/ivan"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, "не ссылка"))
        assertNotNull(FormValidators.validateField(FormValidators.Field.SOCIAL_URL, "@"))
    }

    // ── isValidSocialUrl ────────────────────────────────────────────────

    @Test
    fun `social url direct checks`() {
        assertTrue(FormValidators.isValidSocialUrl(""))
        assertTrue(FormValidators.isValidSocialUrl("@nick"))
        assertTrue(!FormValidators.isValidSocialUrl("@"))
        assertTrue(FormValidators.isValidSocialUrl("linkedin.com/in/ivan"))
        assertTrue(FormValidators.isValidSocialUrl("http://linkedin.com/in/ivan"))
        assertTrue(!FormValidators.isValidSocialUrl("просто слова"))
    }

    // ── validateSalaryRange ─────────────────────────────────────────────

    @Test
    fun `salary range consistency`() {
        assertNull(FormValidators.validateSalaryRange("100000", "200000"))
        assertNull(FormValidators.validateSalaryRange("100000", "100000"))
        assertNotNull(FormValidators.validateSalaryRange("200000", "100000"))
        // Не-числа не блокируют форму — их ловит поле-валидатор
        assertNull(FormValidators.validateSalaryRange("abc", "100000"))
        assertNull(FormValidators.validateSalaryRange("100000", "abc"))
    }

    // ── progressPercent ─────────────────────────────────────────────────

    @Test
    fun `empty form has zero progress`() {
        assertEquals(0, FormValidators.progressPercent(ResumeFormState()))
    }

    @Test
    fun `fully filled form reaches hundred percent`() {
        val state = ResumeFormState(
            fullName = "Иванова Анна Сергеевна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            specializationExperience = "5",
            educations = listOf(EducationEntry(level = "Высшее (бакалавриат)")),
            email = "anna@example.com",
            phone = "+7 921 123-45-67",
            location = "Санкт-Петербург",
            aboutMe = "Люблю чистый код",
            languages = listOf(Language("Русский", "Родной")),
            programmingLanguages = "Kotlin, Java",
            softSkills = "Коммуникабельность",
            projects = listOf(ProjectFormState(name = "Кошелёк")),
            professionalAchievements = listOf("Снизила время старта"),
            salaryMin = "180000",
            readyToRelocate = "Не готова к переезду",
            socialLinks = listOf(SocialLink("Telegram", "@anna")),
            employment = "Полная занятость",
        )
        assertEquals(100, FormValidators.progressPercent(state))
    }

    @Test
    fun `half filled form shows fifty percent`() {
        val state = ResumeFormState(
            fullName = "Иванова Анна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            specializationExperience = "5",
            educations = listOf(EducationEntry(level = "Высшее (магистратура)")),
            email = "anna@example.com",
            phone = "+7 921 123-45-67",
            location = "Санкт-Петербург",
            aboutMe = "Люблю чистый код",
        )
        assertEquals(50, FormValidators.progressPercent(state))
    }

    // ── tabProgress ─────────────────────────────────────────────────────

    @Test
    fun `tab sizes for empty form`() {
        assertEquals(0 to 7, FormValidators.tabProgress(0, ResumeFormState()))
        assertEquals(0 to 3, FormValidators.tabProgress(1, ResumeFormState()))
        assertEquals(0 to 2, FormValidators.tabProgress(2, ResumeFormState()))
        assertEquals(0 to 4, FormValidators.tabProgress(3, ResumeFormState()))
        assertEquals(0 to 2, FormValidators.tabProgress(4, ResumeFormState()))
    }

    @Test
    fun `unknown tab index yields zero pair`() {
        assertEquals(0 to 0, FormValidators.tabProgress(99, ResumeFormState()))
    }

    @Test
    fun `personal tab counts each field`() {
        val state = ResumeFormState(
            fullName = "Иванова Анна",
            specialization = "Android-разработчик",
            totalExperience = "7",
            email = "anna@example.com",
        )
        assertEquals(4 to 7, FormValidators.tabProgress(0, state))
    }

    @Test
    fun `skills tab accepts any hard skill group`() {
        assertEquals(1 to 4, FormValidators.tabProgress(3, ResumeFormState(frameworks = "Ktor")))
        assertEquals(1 to 4, FormValidators.tabProgress(3, ResumeFormState(softSkills = "Эмпатия")))
    }

    @Test
    fun `projects tab needs name or description`() {
        assertEquals(0 to 2, FormValidators.tabProgress(4, ResumeFormState()))
        assertEquals(
            1 to 2,
            FormValidators.tabProgress(4, ResumeFormState(projects = listOf(ProjectFormState(name = "X"))))
        )
        assertEquals(
            2 to 2,
            FormValidators.tabProgress(
                4,
                ResumeFormState(
                    projects = listOf(
                        ProjectFormState(name = "X", description = "Важный проект")
                    )
                )
            )
        )
    }
}

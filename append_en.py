# -*- coding: utf-8 -*-
"""Append the EN localization block to values-en/strings.xml."""
import io

BLOCK = '''
    <!-- ==================== Локализация RU/EN: документы (PDF/DOCX) ==================== -->
    <string name="resume_name_not_specified">Name not specified</string>
    <string name="doc_prof_resume">Professional Resume</string>
    <string name="doc_key_achievements">Key Achievements</string>
    <string name="doc_project_experience">Project Experience</string>
    <string name="doc_technical_skills">Technical Skills</string>
    <string name="doc_certifications_fmt">Certifications: %1$s</string>
    <string name="doc_soft_skills_fmt">Soft skills: %1$s</string>
    <string name="doc_project_fmt">Project %1$d: %2$s</string>
    <string name="doc_role_fmt">Role: %1$s</string>
    <string name="doc_duration_fmt">Duration: %1$s</string>
    <string name="doc_team_fmt">Team: %1$s</string>
    <string name="doc_tech_fmt">Technologies: %1$s</string>
    <string name="doc_experience_fmt">Experience: %1$s</string>
    <string name="doc_spec_experience_fmt">Specialized experience: %1$s</string>
    <string name="doc_salary_line_full">Desired salary: %1$s to %2$s RUB/month</string>
    <string name="doc_salary_line_from">Desired salary: from %1$s RUB/month</string>
    <string name="doc_salary_line_to">Desired salary: up to %1$s RUB/month</string>
    <string name="doc_relocation_line">Relocation: %1$s</string>
    <string name="doc_cities_suffix">\u00A0· cities: %1$s</string>
    <string name="doc_employment_line">Employment: %1$s</string>
    <string name="doc_schedule_line">Schedule: %1$s</string>
    <string name="pdf_page_footer_fmt">%1$s • %2$s • p.\u00A0</string>
    <string name="pdf_certifications">Certifications</string>
    <string name="pdf_soft_skills">Soft Skills</string>
    <string name="duplicate_suffix">" (copy)"</string>

    <!-- ==================== Статусы кандидата ==================== -->
    <string name="status_none">No status</string>
    <string name="status_screening">Screening</string>
    <string name="status_interview">Interview</string>
    <string name="status_offer">Offer</string>
    <string name="status_rejected">Rejected</string>

    <!-- ==================== Сортировка дашборда ==================== -->
    <string name="sort_by_modified">By last modified</string>
    <string name="sort_by_salary">By salary</string>
    <string name="sort_by_experience">By experience</string>
    <string name="sort_by_name">By name</string>
    <string name="sort_ascending">Ascending ↑</string>
    <string name="sort_descending">Descending ↓</string>

    <!-- ==================== Разные подписи и contentDescription ==================== -->
    <string name="filter_salary_k_fmt">%1$dk+</string>
    <string name="cd_add_resume">Add new resume</string>
    <string name="cd_import_pdf">Import PDF</string>
    <string name="cd_edit_resume">Edit resume</string>
    <string name="cd_rating">Rating</string>
    <string name="error_generic">Error</string>
    <string name="no_data">No data</string>
    <string name="action_ok">OK</string>
    <string name="analysis_rating_scale">/ 5.0</string>
    <string name="splash_logo_letter">S</string>
    <string name="settings_api_key_placeholder">sk-…</string>
    <string name="error_pdf_generate_fmt">Failed to generate PDF: %1$s</string>

    <!-- ==================== Уровни образования (SuggestionDictionary) ==================== -->
    <string name="edu_level_svo">Secondary vocational</string>
    <string name="edu_level_bachelor">Higher education (Bachelor\u2019s)</string>
    <string name="edu_level_specialist">Higher education (Specialist)</string>
    <string name="edu_level_master">Higher education (Master\u2019s)</string>
    <string name="edu_level_postgraduate">Postgraduate</string>
    <string name="edu_level_additional">Additional</string>

    <!-- ==================== Варианты релокации ==================== -->
    <string name="reloc_not_ready">Not ready to relocate</string>
    <string name="reloc_ready">Ready to relocate</string>
    <string name="reloc_conditional">Ready under certain conditions</string>

    <!-- ==================== Мок-анализ (AnalyzeCvUseCase) ==================== -->
    <string name="mock_completeness_fmt">The resume is %1$d%% complete: all key sections are present — personal details, skills and experience. Text length is %2$d characters. The structure reads well, but some sections could be expanded to showcase qualifications more fully.</string>
    <string name="mock_logic">The narrative is generally consistent: sections do not contradict each other and the experience matches the declared tech stack. Generic wording without measurable results makes it hard to assess real contributions to projects.</string>
    <string name="mock_param_tech_relevance">Technology relevance</string>
    <string name="mock_param_specialization_match">Specialization match</string>
    <string name="mock_param_project_quality">Project description quality</string>
    <string name="mock_param_structure_logic">Structure logic</string>
    <string name="mock_param_achievement_specificity">Specificity of achievements</string>
    <string name="mock_value_high">high</string>
    <string name="mock_value_strong">strong</string>
    <string name="mock_value_moderate">moderate</string>
    <string name="mock_value_needs_improvement">needs improvement</string>

    <!-- ==================== Плейсхолдеры соцсетей ==================== -->
    <string name="social_ph_vk">https://vk.com/your_id</string>
    <string name="social_ph_telegram">https://t.me/your_username</string>
    <string name="social_ph_github">https://github.com/your_login</string>
    <string name="social_ph_gitlab">https://gitlab.com/your_login</string>
    <string name="social_ph_linkedin">https://linkedin.com/in/your_username</string>
    <string name="social_ph_habr">https://career.habr.com/your_username</string>
    <string name="social_ph_behance">https://behance.net/your_username</string>
    <string name="social_ph_dribbble">https://dribbble.com/your_username</string>
    <string name="social_ph_youtube">https://youtube.com/@your_channel</string>
    <string name="social_ph_site">https://your-site.com</string>
    <string name="social_ph_generic">https://… or @username</string>

    <!-- ==================== Массивы подсказок автодополнения ==================== -->
    <string-array name="suggestion_cities">
        <item>Moscow</item>
        <item>St. Petersburg</item>
        <item>Novosibirsk</item>
        <item>Yekaterinburg</item>
        <item>Kazan</item>
        <item>Nizhny Novgorod</item>
        <item>Chelyabinsk</item>
        <item>Samara</item>
        <item>Rostov-on-Don</item>
        <item>Krasnodar</item>
        <item>Voronezh</item>
        <item>Perm</item>
        <item>Volgograd</item>
        <item>Ufa</item>
        <item>Krasnoyarsk</item>
        <item>Tyumen</item>
        <item>Sochi</item>
    </string-array>
    <string-array name="suggestion_languages">
        <item>Russian</item>
        <item>English</item>
        <item>German</item>
        <item>French</item>
        <item>Spanish</item>
        <item>Italian</item>
        <item>Chinese</item>
        <item>Japanese</item>
        <item>Korean</item>
        <item>Turkish</item>
        <item>Arabic</item>
        <item>Portuguese</item>
    </string-array>
    <string-array name="suggestion_language_levels">
        <item>A1 — Beginner</item>
        <item>A2 — Elementary</item>
        <item>B1 — Intermediate</item>
        <item>B2 — Upper-intermediate</item>
        <item>C1 — Advanced</item>
        <item>C2 — Proficient</item>
        <item>Native</item>
    </string-array>
    <string-array name="suggestion_positions">
        <item>Junior Android Developer</item>
        <item>Android Developer</item>
        <item>Senior Android Developer</item>
        <item>iOS Developer</item>
        <item>Flutter Developer</item>
        <item>Frontend Developer</item>
        <item>Backend Developer</item>
        <item>Java Developer</item>
        <item>Kotlin Developer</item>
        <item>Python Developer</item>
        <item>Go Developer</item>
        <item>Fullstack Developer</item>
        <item>QA Engineer</item>
        <item>Automation QA Engineer</item>
        <item>DevOps Engineer</item>
        <item>SRE Engineer</item>
        <item>Data Scientist</item>
        <item>Data Engineer</item>
        <item>ML Engineer</item>
        <item>Data Analyst</item>
        <item>Systems Analyst</item>
        <item>Business Analyst</item>
        <item>Product Manager</item>
        <item>Project Manager</item>
        <item>Team Lead</item>
        <item>Tech Lead</item>
        <item>Software Architect</item>
        <item>UX/UI Designer</item>
        <item>Web Designer</item>
        <item>1C Developer</item>
    </string-array>
    <string-array name="suggestion_soft_skills">
        <item>Communication</item>
        <item>Teamwork</item>
        <item>Time management</item>
        <item>Critical thinking</item>
        <item>Problem solving</item>
        <item>Adaptability</item>
        <item>Mentorship</item>
        <item>Public speaking</item>
        <item>Negotiation</item>
        <item>Conflict management</item>
        <item>Empathy</item>
        <item>Multitasking</item>
    </string-array>
    <string-array name="suggestion_employment">
        <item>Full-time</item>
        <item>Part-time</item>
        <item>Side job</item>
        <item>Internship</item>
    </string-array>
    <string-array name="suggestion_work_schedule">
        <item>Remote work</item>
        <item>Hybrid format</item>
        <item>Office work</item>
    </string-array>
    <string-array name="suggestion_social_platforms">
        <item>Telegram</item>
        <item>LinkedIn</item>
        <item>GitHub</item>
        <item>GitLab</item>
        <item>Habr Career</item>
        <item>Behance</item>
        <item>Dribbble</item>
        <item>YouTube</item>
        <item>VK</item>
        <item>Personal website</item>
    </string-array>
    <string-array name="suggestion_specialties_svo">
        <item>Information systems and programming</item>
        <item>Computer systems and complexes</item>
        <item>Network and system administration</item>
        <item>Programming in computer systems</item>
        <item>Multi-channel telecommunication systems</item>
        <item>Maintenance and repair of radio-electronic equipment</item>
        <item>Economics and accounting</item>
        <item>Law and social security organization</item>
        <item>Design (by industry)</item>
        <item>Operational activities in logistics</item>
    </string-array>
    <string-array name="suggestion_specialties_bachelor">
        <item>Software engineering</item>
        <item>Informatics and computer engineering</item>
        <item>Applied mathematics and informatics</item>
        <item>Information security</item>
        <item>Information systems and technologies</item>
        <item>Business informatics</item>
        <item>Applied informatics</item>
        <item>Radio engineering</item>
        <item>Economics</item>
        <item>Management</item>
        <item>Design</item>
    </string-array>
    <string-array name="suggestion_specialties_specialist">
        <item>Applied mathematics and informatics</item>
        <item>Comprehensive information security of automated systems</item>
        <item>Software engineering</item>
        <item>Informatics and computer engineering</item>
        <item>Automated information processing and control systems</item>
        <item>Computers, complexes, systems and networks</item>
        <item>Economic security</item>
        <item>General medicine</item>
        <item>Jurisprudence</item>
    </string-array>
    <string-array name="suggestion_specialties_master">
        <item>Software engineering</item>
        <item>Applied mathematics and informatics</item>
        <item>Information security</item>
        <item>Intelligent data analysis</item>
        <item>Artificial intelligence and machine learning</item>
        <item>Software development management</item>
        <item>Business informatics</item>
    </string-array>
    <string-array name="suggestion_specialties_postgraduate">
        <item>Informatics and computer engineering</item>
        <item>Mathematical modeling, numerical methods and software complexes</item>
        <item>Theoretical foundations of computer science</item>
        <item>Methods and systems of information protection</item>
        <item>Economic theory</item>
    </string-array>
    <string-array name="suggestion_specialties_additional">
        <item>Professional retraining</item>
        <item>Advanced training</item>
        <item>Data Science and data analytics</item>
        <item>Software testing</item>
        <item>DevOps engineering</item>
        <item>Project management</item>
    </string-array>

    <!-- Списки мок-анализа (порядок элементов значим) -->
    <string-array name="mock_strengths">
        <item>Clear structure: all key resume sections are present</item>
        <item>Relevant technology stack backed by projects</item>
        <item>Experience listed in chronological order without gaps</item>
        <item>Contacts and location provided — easy for employers to reach out</item>
    </string-array>
    <string-array name="mock_improvements">
        <item>Add quantitative results to project descriptions</item>
        <item>Describe your role in the team for recent projects</item>
        <item>Specify proficiency levels for foreign languages</item>
        <item>Expand the About me section with key achievements</item>
    </string-array>
    <string-array name="mock_recommendations">
        <item>Rephrase 2–3 responsibilities as achievements using the scheme “action → result → metric”</item>
        <item>Move your strongest technologies to the top of the skills block</item>
        <item>Add links to your portfolio or repositories</item>
        <item>Double-check consistency of date and title formatting</item>
    </string-array>

    <!-- Годы опыта: год/года/лет (для PDF и карточек) -->
    <plurals name="experience_years">
        <item quantity="one">%1$d year</item>
        <item quantity="other">%1$d years</item>
    </plurals>
</resources>'''


def main():
    path = 'values-en/strings.xml'
    with io.open(path, encoding='utf-8') as f:
        content = f.read()
    stripped = content.rstrip()
    assert stripped.endswith('</resources>'), 'unexpected file ending'
    content = stripped[: -len('</resources>')] + BLOCK.lstrip('\n')
    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    print('OK')


if __name__ == '__main__':
    main()

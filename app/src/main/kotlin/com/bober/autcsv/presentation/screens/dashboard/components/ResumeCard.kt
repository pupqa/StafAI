package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bober.autcsv.domain.model.Resume

/**
 * Карточка резюме в списке дашборда.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeCard(
    resume: Resume,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = resume.personalInfo.fullName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${resume.personalInfo.specialization} • ${resume.personalInfo.specializationExperience}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = resume.professionalSkills.getAllTechnologies().take(3).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (resume.summary.isNotBlank()) {
                Text(
                    text = resume.summary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
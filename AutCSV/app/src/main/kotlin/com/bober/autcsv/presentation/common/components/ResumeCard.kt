package com.bober.autcsv.presentation.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bober.autcsv.domain.model.Resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeCard(
    resume: Resume,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = resume.personalInfo.fullName,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = resume.personalInfo.specialization,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Total Experience: ${resume.personalInfo.totalExperience}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = resume.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
} 
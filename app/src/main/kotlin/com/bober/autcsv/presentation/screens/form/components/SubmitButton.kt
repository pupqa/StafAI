package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.ResumeFormViewModel

@Composable
fun SubmitButton(
    viewModel: ResumeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Button(
        onClick = {
            LlmLogger.logUserAction("Submit form", "Form", "Resume ID: ${state.resumeId}")
            viewModel.onEvent(ResumeFormEvent.Submit)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        enabled = state.isValid && !state.isLoading,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(stringResource(R.string.save))
        }
    }
}
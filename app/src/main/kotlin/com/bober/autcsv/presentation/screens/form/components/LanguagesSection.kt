package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.Language
import com.bober.autcsv.presentation.common.components.RoundedCorner
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.ResumeFormViewModel

@Composable
fun LanguagesSection(
    languages: List<Language>,
    viewModel: ResumeFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.languages),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            state.languages.forEachIndexed { index, language ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundedCorner(
                        value = language.name,
                        onValueChange = {
                            LlmLogger.logFormEvent(
                                "LanguageNameChanged",
                                "language[$index].name",
                                it
                            )
                            viewModel.onEvent(
                                ResumeFormEvent.LanguageNameChanged(
                                    index,
                                    it
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        labelText = stringResource(R.string.language_name),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RoundedCorner(
                        value = language.level,
                        onValueChange = {
                            LlmLogger.logFormEvent(
                                "LanguageLevelChanged",
                                "language[$index].level",
                                it
                            )
                            viewModel.onEvent(
                                ResumeFormEvent.LanguageLevelChanged(
                                    index,
                                    it
                                )
                            )
                        },
                        labelText = "Уровень",
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    IconButton(
                        onClick = {
                            LlmLogger.logUserAction(
                                "Delete language",
                                "Form",
                                "index: $index"
                            )
                            viewModel.onEvent(
                                ResumeFormEvent.DeleteLanguage(
                                    index
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    LlmLogger.logUserAction("Add language", "Form", null)
                    viewModel.onEvent(ResumeFormEvent.AddLanguage)
                },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)

            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.language_name))
            }
        }
    }
}
package com.bober.autcsv.presentation.screens.coverletter

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.MD
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Экран сопроводительного письма: генерация LLM по тексту вакансии,
 * редактирование, сохранение писем резюме, повторное открытие и удаление.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterScreen(
    onNavigateBack: () -> Unit,
    viewModel: CoverLetterViewModel = hiltViewModel(),
) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val letters by viewModel.letters.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editor.message) {
        editor.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cover_letter_title),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BackgroundSoft,
                    titleContentColor = AppColors.TextPrimary,
                    navigationIconContentColor = AppColors.TextPrimary
                )
            )
        }
    ) { padding ->
        AdaptiveContent(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // ── Вакансия ────────────────────────────────────────────
                OutlinedTextField(
                    value = editor.vacancyText,
                    onValueChange = viewModel::onVacancyChange,
                    label = { Text(stringResource(R.string.cover_letter_vacancy_label)) },
                    placeholder = { Text(stringResource(R.string.cover_letter_vacancy_hint)) },
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = viewModel::generate,
                    enabled = !editor.isGenerating && editor.vacancyText.isNotBlank(),
                    shape = RoundedCornerShape(MD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Accent,
                        contentColor = AppColors.AccentInk
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (editor.isGenerating) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = AppColors.AccentInk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.cover_letter_generating))
                    } else {
                        Text(stringResource(R.string.cover_letter_generate))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Редактор письма ─────────────────────────────────────
                Text(
                    text = stringResource(R.string.cover_letter_content_label),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = AppColors.TextFaint
                )
                Spacer(modifier = Modifier.height(8.dp))

                val contentFieldShape = RoundedCornerShape(MD)
                if (editor.content.isBlank() && !editor.isGenerating) {
                    Card(
                        shape = contentFieldShape,
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface2),
                        border = BorderStroke(1.dp, AppColors.BorderSoft)
                    ) {
                        Text(
                            text = stringResource(R.string.cover_letter_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextDim,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = editor.content,
                        onValueChange = viewModel::onContentChange,
                        singleLine = false,
                        maxLines = 18,
                        shape = contentFieldShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, editor.content)
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(send, null)
                                )
                            }
                        },
                        enabled = editor.content.isNotBlank(),
                        shape = RoundedCornerShape(MD),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.share), fontSize = 13.5.sp)
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = editor.content.isNotBlank(),
                        shape = RoundedCornerShape(MD),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.AccentInk
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save), fontSize = 13.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Сохранённые письма ──────────────────────────────────
                if (letters.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.cover_letter_saved_list),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = AppColors.TextFaint
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                letters.forEach { letter ->
                    SavedLetterCard(
                        letter = letter,
                        onOpen = { viewModel.edit(letter) },
                        onRequestDelete = { pendingDeleteId = letter.id }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Диалог подтверждения удаления
    pendingDeleteId?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.delete(id)
                        pendingDeleteId = null
                    }
                ) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
            title = { Text(stringResource(R.string.cover_letter_delete_confirm_title)) },
            text = { Text(stringResource(R.string.cover_letter_delete_confirm_message)) }
        )
    }
}

@Composable
private fun SavedLetterCard(
    letter: com.bober.autcsv.domain.model.CoverLetter,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val locale = java.util.Locale.getDefault()
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", locale) }

    Card(
        shape = RoundedCornerShape(MD),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(1.dp, AppColors.BorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = letter.vacancyTitle.ifBlank { letter.content.take(48) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = AppColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(letter.updatedAt)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = AppColors.TextFaint
                )
            }
            IconButton(onClick = onRequestDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = AppColors.TextDim
                )
            }
        }
    }
}

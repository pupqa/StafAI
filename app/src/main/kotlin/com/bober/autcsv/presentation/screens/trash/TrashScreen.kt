package com.bober.autcsv.presentation.screens.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bober.autcsv.core.utils.AppLocales
import androidx.hilt.navigation.compose.hiltViewModel
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.presentation.screens.form.components.StaffAiIconButton
import com.bober.autcsv.presentation.screens.form.components.StaffAiOutlineButton
import com.bober.autcsv.presentation.screens.form.components.StaffAiSectionCard
import com.bober.autcsv.presentation.screens.form.components.StaffAiTextAction
import com.bober.autcsv.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран корзины: список мягко удалённых резюме с возможностью восстановить
 * каждое, удалить окончательно или очистить корзину целиком.
 * Оформлен в общей тёмной палитре «СтафИИ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Сообщение об успешном восстановлении (параметризовано именем резюме,
    // поэтому строка собирается через context.getString внутри эффекта)
    LaunchedEffect(state.lastRestoredName) {
        state.lastRestoredName?.let { name ->
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.trash_restored, name)
            )
            viewModel.consumeRestoredMessage()
        }
    }

    LlmLogger.logUiEvent("Trash", "Screen rendered", "Count: ${state.resumes.size}")

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(AppColors.BackgroundSoft)
                    .statusBarsPadding()
            ) {
                AdaptiveContent {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StaffAiIconButton(
                            onClick = { onNavigateBack() },
                            contentDescription = stringResource(R.string.back),
                            icon = Icons.AutoMirrored.Filled.ArrowBack
                        )
                        Text(
                            text = stringResource(R.string.nav_trash),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        )
                        if (state.resumes.isNotEmpty()) {
                            StaffAiTextAction(
                                text = stringResource(R.string.trash_empty_all),
                                onClick = { viewModel.requestEmptyTrash() }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Accent)
                }

                state.resumes.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AppColors.TextFaint,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.trash_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.trash_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextDim,
                        textAlign = TextAlign.Center
                    )
                }

                else -> AdaptiveContent(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            vertical = 8.dp
                        )
                    ) {
                        items(state.resumes, key = { it.id }) { resume ->
                            key(resume.id) {
                                TrashResumeCard(
                                    resume = resume,
                                    onRestore = { viewModel.restoreResume(resume) },
                                    onDeletePermanently = { viewModel.requestPermanentDelete(resume.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Подтверждение окончательного удаления одного резюме
    state.pendingPermanentDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermanentDelete() },
            title = { Text(stringResource(R.string.trash_delete_forever_title)) },
            text = { Text(stringResource(R.string.trash_delete_forever_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePermanently(id) }) {
                    Text(
                        text = stringResource(R.string.delete_confirm),
                        color = AppColors.Danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermanentDelete() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = com.bober.autcsv.ui.theme.Surface
        )
    }

    // Подтверждение очистки корзины
    if (state.showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEmptyTrashDialog() },
            title = { Text(stringResource(R.string.trash_empty_confirm_title)) },
            text = { Text(stringResource(R.string.trash_empty_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash() }) {
                    Text(
                        text = stringResource(R.string.trash_empty_all),
                        color = AppColors.Danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEmptyTrashDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = com.bober.autcsv.ui.theme.Surface
        )
    }

    // Баннер ошибки
    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(message = error)
            viewModel.clearError()
        }
    }
}

/** Карточка удалённого резюме: ФИО, должность, дата удаления, действия. */
@Composable
private fun TrashResumeCard(
    resume: Resume,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit,
) {
    StaffAiSectionCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resume.personalInfo.fullName.ifBlank { stringResource(R.string.untitled) },
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                    if (resume.personalInfo.specialization.isNotBlank()) {
                        Text(
                            text = resume.personalInfo.specialization,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextDim
                        )
                    }
                    Text(
                        text = formatDeletedAt(resume.deletedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextFaint,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                StaffAiIconButton(
                    onClick = onDeletePermanently,
                    contentDescription = stringResource(R.string.trash_delete_forever_title),
                    icon = Icons.Default.Delete,
                    tint = AppColors.Danger,
                    background = AppColors.DangerSoft
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StaffAiOutlineButton(
                text = stringResource(R.string.trash_restore),
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun formatDeletedAt(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    // Дата форматировалась жёстко под русскую локаль — берём локаль приложения
    val locale = AppLocales.localizedContext(LocalContext.current)
        .resources.configuration.locales[0]
    val format = SimpleDateFormat("d MMMM yyyy, HH:mm", locale)
    return stringResource(R.string.trash_deleted_date, format.format(Date(timestamp)))
}

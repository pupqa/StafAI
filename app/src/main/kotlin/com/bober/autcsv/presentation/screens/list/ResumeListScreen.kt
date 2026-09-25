package com.bober.autcsv.presentation.screens.list

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.core.utils.PdfImportParser
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.presentation.screens.form.components.StaffAiIconButton
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.LG
import com.bober.autcsv.ui.theme.LocalAppPalette
import com.bober.autcsv.ui.theme.MD
import com.bober.autcsv.ui.theme.SM
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Экран списка резюме: загрузка, пустое состояние, список карточек,
 * импорт PDF, переходы к формам/предпросмотру/дашборду/настройкам.
 * Оформлен в стиле макета «СтафИИ»: тёмная поверхность, золотой FAB
 * со скруглённым квадратом, моно-акценты для специализации и метаданных.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeListScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToFormEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ResumeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Показываем короткий прогресс перед отображением контента
    var initialLoading by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(250)
        initialLoading = false
    }
    val isUiLoading by remember(state.isLoading, initialLoading) {
        derivedStateOf { state.isLoading || initialLoading }
    }

    // Строки снекбара резолвим заранее: лямбды кнопок не являются composable
    val movedToTrashText = stringResource(R.string.moved_to_trash)
    val undoActionText = stringResource(R.string.action_undo)
    val untitledText = stringResource(R.string.untitled)

    // Мягкое удаление со снекбаром «Отменить» вместо блокирующего диалога:
    // резюме и так остаётся в корзине — подтверждение избыточно
    fun deleteWithUndo(resumeId: String, title: String) {
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        viewModel.deleteResume(resumeId)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "$title — $movedToTrashText",
                actionLabel = undoActionText,
                withDismissAction = true,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.restoreResume(resumeId)
            }
        }
    }

    LlmLogger.logUiEvent("List", "Screen rendered", "Resumes count: ${state.resumes.size}")

    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val resolver = context.contentResolver
                    resolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                // Создаем новый Resume, распарсив PDF, и сохраняем
                try {
                    val resume = PdfImportParser.parseNewResumeFromPdf(context, uri)
                    LlmLogger.logUiEvent("Import", "Parsed resume from PDF", resume.id)
                    // Сохраняем импортированное резюме и переходим в форму
                    viewModel.saveImportedResume(resume) { newId ->
                        onNavigateToFormEdit(newId)
                    }
                } catch (e: Exception) {
                    LlmLogger.logError("Failed to import PDF: ${e.message}", e)
                }
            }
        }
    )

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_list),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BackgroundSoft,
                    titleContentColor = AppColors.TextPrimary,
                ),
                actions = {
                    // Import PDF action
                    StaffAiIconButton(
                        onClick = {
                            LlmLogger.logUserAction("Import PDF button pressed", "List", null)
                            importLauncher.launch(arrayOf("application/pdf"))
                        },
                        contentDescription = stringResource(R.string.cd_import_pdf),
                        icon = Icons.Outlined.FileUpload,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    StaffAiIconButton(
                        onClick = {
                            LlmLogger.logUserAction("Dashboard button pressed", "List", null)
                            onNavigateToDashboard()
                        },
                        contentDescription = stringResource(R.string.nav_dashboard),
                        icon = Icons.Outlined.Dashboard,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    StaffAiIconButton(
                        onClick = {
                            LlmLogger.logUserAction("Settings button pressed", "List", null)
                            onNavigateToSettings()
                        },
                        contentDescription = stringResource(R.string.nav_settings),
                        icon = Icons.Default.Settings,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    LlmLogger.logUserAction("Add resume button pressed", "List", null)
                    onNavigateToForm()
                },
                shape = RoundedCornerShape(MD),
                containerColor = AppColors.Accent,
                contentColor = AppColors.AccentInk,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        },
        snackbarHost = {
            androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = AppColors.Surface3,
                    contentColor = AppColors.TextPrimary,
                    actionColor = AppColors.Accent,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundSoft)
                .padding(padding)
        ) {
            if (isUiLoading) {
                LlmLogger.logUiEvent("List", "Loading state", "UI loading spinner")
                CircularProgressIndicator(
                    color = AppColors.Accent,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (state.resumes.isEmpty()) {
                    LlmLogger.logUiEvent("List", "Empty state", "No resumes found")
                    EmptyResumeState(
                        onCreateClick = {
                            LlmLogger.logUserAction("Empty state CTA", "List", null)
                            onNavigateToForm()
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Черновики (незаполненные резюме) показываются отдельной секцией
                    val completedResumes = state.resumes.filter { it.isValid() }
                    val draftResumes = state.resumes.filter { !it.isValid() }

                    AdaptiveContent(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (completedResumes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "${stringResource(R.string.nav_list)} \u00B7 ${completedResumes.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                                    )
                                }
                            }
                            items(completedResumes, key = { it.id }) { resume ->
                                SwipeableResumeCard(
                                    resume = resume,
                                    onCardClick = {
                                        LlmLogger.logUserAction(
                                            "Resume card clicked",
                                            "List",
                                            resume.id
                                        )
                                        onNavigateToPreview(resume.id)
                                    },
                                    onDeleteClick = {
                                        LlmLogger.logUserAction(
                                            "Delete resume",
                                            "List",
                                            resume.id
                                        )
                                        deleteWithUndo(
                                            resume.id,
                                            resume.personalInfo.fullName.ifBlank { untitledText })
                                    },
                                    onEditClick = { onNavigateToFormEdit(resume.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(resume.id) },
                                    onDuplicate = {
                                        viewModel.duplicateResume(resume.id) { /* список обновится из Flow */ }
                                    },
                                    onSharePdf = { viewModel.sharePdf(resume.id) },
                                )
                            }

                            if (draftResumes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "${stringResource(R.string.drafts_section)} \u00B7 ${draftResumes.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AppColors.TextFaint,
                                        modifier = Modifier.padding(
                                            top = 10.dp,
                                            bottom = 2.dp,
                                            start = 2.dp
                                        )
                                    )
                                }
                            }
                            items(draftResumes, key = { "draft_${it.id}" }) { resume ->
                                // Черновик: тап сразу открывает редактирование, удаление —
                                // долгим удержанием или через меню карточки
                                SwipeableResumeCard(
                                    resume = resume,
                                    onCardClick = {
                                        LlmLogger.logUserAction(
                                            "Draft card clicked",
                                            "List",
                                            resume.id
                                        )
                                        onNavigateToFormEdit(resume.id)
                                    },
                                    onDeleteClick = {
                                        LlmLogger.logUserAction(
                                            "Delete draft",
                                            "List",
                                            resume.id
                                        )
                                        deleteWithUndo(
                                            resume.id,
                                            resume.personalInfo.fullName.ifBlank { untitledText })
                                    },
                                    isDraft = true,
                                    onDuplicate = {
                                        viewModel.duplicateResume(resume.id) { }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Ошибки операций показываются снекбаром без действия
    state.error?.let { error ->
        LlmLogger.logError("List screen error: $error")
        val dismissLabel = stringResource(R.string.action_ok)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(message = error, actionLabel = dismissLabel)
            viewModel.clearError()
        }
    }
}


// Импорт по PDF теперь создает новый Resume, парсинг id по имени файла больше не используется

/** Пустое состояние списка: иконка, объяснение и CTA создания первого резюме. */
@Composable
private fun EmptyResumeState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.FileUpload,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.empty_resume_list),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_resume_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextDim,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        androidx.compose.material3.Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(MD),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent,
                contentColor = AppColors.AccentInk,
            ),
        ) {
            Text(
                text = stringResource(R.string.cd_add_resume),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Карточка резюме: обычный тап открывает карточку (у готовых резюме —
 * предпросмотр, у черновиков — сразу форму редактирования), долгое
 * удержание запускает удаление; меню «⋮» дублирует удаление и даёт
 * быстрые действия — избранное, дублировать, поделиться PDF.
 */
@Composable
fun SwipeableResumeCard(
    resume: Resume,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    isDraft: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    val longPressDurationMs = 1500
    // Индикация удаления не появляется сразу: первые 500 мс прогресс держится на нуле
    val longPressIndicatorDelayMs = 500
    val pressProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Палитра читается в композабельном контексте, derivedStateOf берёт снапшот значений
    val palette = LocalAppPalette.current
    val baseContainer = if (isDraft) palette.Surface.copy(alpha = 0.55f) else palette.Surface
    val containerColor by remember(palette, isDraft) {
        derivedStateOf { lerp(baseContainer, palette.DangerSoft, pressProgress.value) }
    }
    val borderColor by remember(palette) {
        derivedStateOf { lerp(palette.BorderSoft, palette.Danger, pressProgress.value) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(resume.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var deleted = false
                    val holdJob = scope.launch {
                        pressProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = longPressDurationMs
                                0f at longPressIndicatorDelayMs
                            }
                        )
                        deleted = true
                        LlmLogger.logUserAction("Long press delete", "List", resume.id)
                        onDeleteClick()
                    }
                    val released = waitForUpOrCancellation()
                    holdJob.cancel()
                    scope.launch {
                        pressProgress.animateTo(0f, animationSpec = tween(150))
                    }
                    // Открываем карточку только по настоящему тапу:
                    // если жест съеден (скролл) — не срабатываем
                    if (released != null && !deleted) {
                        onCardClick()
                    }
                }
            },
        shape = RoundedCornerShape(LG),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDraft) {
                        resume.personalInfo.fullName.ifBlank { stringResource(R.string.untitled) }
                    } else {
                        resume.personalInfo.fullName
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                        .let { style ->
                            if (isDraft) style.copy(color = AppColors.TextDim) else style
                        },
                    modifier = Modifier.weight(1f)
                )
                if (isDraft) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(SM),
                        color = AppColors.BorderSoft
                    ) {
                        Text(
                            text = stringResource(R.string.draft_badge),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                            color = AppColors.TextDim,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    StaffAiIconButton(
                        onClick = {
                            LlmLogger.logUserAction("Edit resume button", "List", resume.id)
                            onEditClick()
                        },
                        contentDescription = stringResource(R.string.edit_resume),
                        icon = Icons.Default.Edit
                    )
                }
                // Меню быстрых действий: избранное/дублировать/поделиться/удалить
                Box {
                    StaffAiIconButton(
                        onClick = {
                            haptics.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                            menuExpanded = true
                        },
                        contentDescription = stringResource(R.string.card_more_actions),
                        icon = Icons.Default.MoreVert
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (!isDraft && onToggleFavorite != null) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (resume.isFavorite) R.string.card_remove_favorite
                                            else R.string.card_add_favorite
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (resume.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = AppColors.Accent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleFavorite()
                                }
                            )
                        }
                        if (onDuplicate != null) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_duplicate)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = AppColors.TextDim,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                        }
                        if (!isDraft && onSharePdf != null) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_share_pdf)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Share,
                                        contentDescription = null,
                                        tint = AppColors.TextDim,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSharePdf()
                                }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.delete),
                                    color = AppColors.Danger,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = AppColors.Danger,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
            if (resume.personalInfo.specialization.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\u203A ${resume.personalInfo.specialization}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (isDraft) AppColors.TextDim else AppColors.Accent
                )
            }
            if (isDraft) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.draft_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${stringResource(R.string.specialization_experience)}: ${resume.personalInfo.specializationExperience}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextFaint
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = resume.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextDim,
                    maxLines = 2
                )
            }
            if (pressProgress.value > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = pressProgress.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(SM)),
                    color = AppColors.Danger,
                    trackColor = AppColors.DangerSoft
                )
            }
        }
    }
}

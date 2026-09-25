package com.bober.autcsv.presentation.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.AppLocales
import com.bober.autcsv.core.utils.LanguageStore
import com.bober.autcsv.core.utils.LlmLogger
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.LG
import com.bober.autcsv.ui.theme.MD
import kotlinx.coroutines.launch

/**
 * Экран настроек приложения — стиль экрана «06 · Настройки» из макета:
 * mono-заголовки секций, карточки .set-card, поле API-ключа со скрытыми
 * точками, инфо-плашка вместо модалки, переключатель темы с пружиной.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    // TODO: прокинуть реальные BuildConfig.VERSION_NAME / VERSION_CODE с сайта вызова
    appVersionName: String = "1.0.0",
    appVersionCode: Int = 1,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    LlmLogger.logUiEvent("Settings", "Screen rendered", null)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // --- Состояние API-ключа ---
    var apiKey by rememberSaveable { mutableStateOf("") }
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
    var apiKeySaved by rememberSaveable { mutableStateOf(false) }

    // Показываем уже сохранённый ключ (или ключ сборки) при первом входе
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (apiKey.isBlank()) apiKey = viewModel.initialKey()
    }

    val apiKeySavedMessage = stringResource(R.string.settings_api_key_saved_snackbar)

    // --- Состояние операций с данными (№12/№33/№34) ---
    val dataOps by viewModel.dataOps.collectAsStateWithLifecycle()

    // Некоторые провайдеры отдают JSON как octet-stream/text-plain — берём все три типа
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::restoreBackup) }

    androidx.compose.runtime.LaunchedEffect(dataOps.message) {
        dataOps.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // --- Состояние темы ---
    var isDarkTheme by rememberSaveable { mutableStateOf(true) }

    // --- Состояние языка интерфейса: тег текущей локали приложения ---
    val appContextForLocale = LocalContext.current
    var selectedLanguageTag by rememberSaveable {
        mutableStateOf(AppLocales.currentTag(appContextForLocale))
    }

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        letterSpacing = (-0.2).sp,
                        color = AppColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        LlmLogger.logUserAction("Back from Settings", "Settings", null)
                        onNavigateBack()
                    }) {
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
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BackgroundSoft)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // ── Секция: доступ к ИИ ─────────────────────────────────────────
                //  SecLabel(text = stringResource(R.string.settings_section_api))
                Spacer(Modifier.height(10.dp))

                SettingsCard {
                    Text(
                        text = stringResource(R.string.settings_api_key_label).uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        color = AppColors.TextFaint,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ApiKeyField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            apiKeySaved = false
                        },
                        visible = apiKeyVisible,
                        onToggleVisible = { apiKeyVisible = !apiKeyVisible },
                        onDone = { focusManager.clearFocus() }
                    )

                    Spacer(Modifier.height(12.dp))

                    SaveApiKeyButton(
                        enabled = apiKey.isNotBlank(),
                        saved = apiKeySaved,
                        onClick = {
                            focusManager.clearFocus()
                            LlmLogger.logUserAction("Save API key", "Settings", null)
                            viewModel.saveApiKey(apiKey)
                            apiKeySaved = true
                            scope.launch {
                                snackbarHostState.showSnackbar(message = apiKeySavedMessage)
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    InfoBanner(text = stringResource(R.string.settings_api_key_description))
                }

                Spacer(Modifier.height(28.dp))

                // ── Секция: данные (бэкап/восстановление/CSV) ───────────────────
                SecLabel(text = stringResource(R.string.settings_section_data))
                Spacer(Modifier.height(10.dp))

                SettingsCard {
                    DataOpRow(
                        icon = Icons.Outlined.SaveAlt,
                        title = stringResource(R.string.settings_backup_export),
                        isWorking = dataOps.isWorking,
                        onClick = {
                            LlmLogger.logUserAction("Backup export", "Settings", null)
                            viewModel.createBackup()
                        }
                    )
                    SettingsDivider()
                    DataOpRow(
                        icon = Icons.Outlined.Restore,
                        title = stringResource(R.string.settings_backup_import),
                        isWorking = dataOps.isWorking,
                        onClick = {
                            LlmLogger.logUserAction("Backup import picker", "Settings", null)
                            backupPicker.launch(
                                arrayOf(
                                    "application/json",
                                    "application/octet-stream",
                                    "text/plain"
                                )
                            )
                        }
                    )
                    SettingsDivider()
                    DataOpRow(
                        icon = Icons.Outlined.TableChart,
                        title = stringResource(R.string.settings_export_all_csv),
                        isWorking = dataOps.isWorking,
                        onClick = {
                            LlmLogger.logUserAction("Export all CSV", "Settings", null)
                            viewModel.exportAllCsv()
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoBanner(text = stringResource(R.string.settings_backup_hint))
                }

                Spacer(Modifier.height(28.dp))

                // ── Секция: безопасность (биометрия, №36) ───────────────────────
                SecLabel(text = stringResource(R.string.settings_section_security))
                Spacer(Modifier.height(10.dp))

                SettingsCard {
                    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = AppColors.TextDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_biometric_title),
                                fontSize = 14.sp,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_biometric_hint),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = AppColors.TextFaint,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                LlmLogger.logUserAction(
                                    "Biometric toggled",
                                    "Settings",
                                    it.toString()
                                )
                                viewModel.setBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AppColors.Accent,
                                checkedThumbColor = AppColors.AccentInk,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Секция: экспорт PDF ─────────────────────────────────────
                SecLabel(text = stringResource(R.string.export_section_title))
                Spacer(Modifier.height(10.dp))

                SettingsCard {
                    val embedResumeData by viewModel.embedResumeData.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = AppColors.TextDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_pdf_embed_title),
                                fontSize = 14.sp,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_pdf_embed_hint),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = AppColors.TextFaint,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Switch(
                            checked = embedResumeData,
                            onCheckedChange = {
                                LlmLogger.logUserAction(
                                    "PDF data embedding toggled",
                                    "Settings",
                                    it.toString()
                                )
                                viewModel.setEmbedResumeData(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AppColors.Accent,
                                checkedThumbColor = AppColors.AccentInk,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Секция: приложение ──────────────────────────────────────────
                SecLabel(text = stringResource(R.string.settings_section_app))
                Spacer(Modifier.height(10.dp))

                SettingsCard {
                    LanguageRow(
                        selectedTag = selectedLanguageTag,
                        onLanguageChange = { tag ->
                            if (tag != selectedLanguageTag) {
                                selectedLanguageTag = tag
                                LlmLogger.logUserAction("Language changed", "Settings", tag)
                                onLanguageSelected(tag)
                                // Меняет локаль приложения: Activity пересоздастся
                                viewModel.setLanguage(tag)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = stringResource(R.string.nav_trash),
                        onClick = {
                            LlmLogger.logUserAction("Trash opened", "Settings", null)
                            onNavigateToTrash()
                        }
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = stringResource(R.string.settings_help),
                        onClick = {
                            LlmLogger.logUserAction("Help opened", "Settings", null)
                            onNavigateToHelp()
                        }
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_about),
                        onClick = {
                            LlmLogger.logUserAction("About opened", "Settings", null)
                            onNavigateToAbout()
                        }
                    )
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.settings_privacy_policy),
                        onClick = {
                            LlmLogger.logUserAction("Privacy policy opened", "Settings", null)
                            onNavigateToPrivacyPolicy()
                        }
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── Подпись версии приложения ───────────────────────────────────
                VersionFooter(versionName = appVersionName, versionCode = appVersionCode)

                Spacer(Modifier.height(12.dp))
                /*
                            // ── Секция: оформление ──────────────────────────────────────────
                            SecLabel(text = stringResource(R.string.settings_section_appearance))
                            Spacer(Modifier.height(10.dp))

                            SettingsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_theme_dark),
                                        fontSize = 14.sp,
                                        color = .TextPrimary
                                    )
                                    AutSwitch(
                                        checked = isDarkTheme,
                                        onCheckedChange = {
                                            isDarkTheme = it
                                            LlmLogger.logUserAction(
                                                "Theme toggled",
                                                "Settings",
                                                if (it) "dark" else "light"
                                            )
                                            // TODO: передать выбор темы через ViewModel / AppTheme state
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                 */
            }
        }
    }
}

// ─── Вспомогательные компоненты ──────────────────────────────────────────────

/** Моно-лейбл секции в стиле .sec-label из макета. */
@Composable
private fun SecLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.7.sp,
        color = AppColors.TextFaint
    )
}

/** Карточка секции в стиле .set-card из макета. */
@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LG))
            .background(
                AppColors.Surface
            )
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(LG))
            .padding(16.dp),
        content = content
    )
}

/** Поле API-ключа в стиле .pw-field: скрытые точки моноширинным шрифтом + переключатель видимости. */
@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MD))
            .background(
                AppColors.Surface2
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_api_key_placeholder),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = AppColors.TextFaint
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    letterSpacing = if (!visible && value.isNotEmpty()) 2.sp else 0.sp,
                    color = AppColors.TextPrimary
                ),
                visualTransformation =
                    if (visible) VisualTransformation.None else PasswordVisualTransformation(
                        '•'
                    ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp)
            )
        }

        IconButton(onClick = onToggleVisible) {
            Icon(
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible)
                    stringResource(R.string.settings_hide_key)
                else
                    stringResource(R.string.settings_show_key),
                tint = AppColors.TextDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Кнопка сохранения в стиле .btn-fill из макета. */
@Composable
private fun SaveApiKeyButton(
    enabled: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(MD),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Accent,
            contentColor = AppColors.AccentInk,
            disabledContainerColor = AppColors.Surface3,
            disabledContentColor = AppColors.TextFaint
        )
    ) {
        if (saved) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 6.dp)
            )
        }
        Text(
            text = if (saved)
                stringResource(R.string.settings_api_key_saved)
            else
                stringResource(R.string.settings_api_key_save),
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
        )
    }
}

/** Информационная плашка в стиле .info-banner из макета — вместо модального окна. */
@Composable
private fun InfoBanner(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MD))
            .background(
                AppColors.AmberSoft
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = AppColors.Accent.copy(alpha = 0.85f)
        )
    }
}

/** Тонкий разделитель между строками внутри .set-card. */
@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                AppColors.BorderSoft
            )
            .padding(vertical = 6.dp)
    )
}

/** Обычная кликабельная строка настройки: иконка, заголовок, шеврон справа. */
@Composable
private fun SettingsLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextDim,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Строка длительной операции с данными: спиннер вместо шеврона на время работы. */
@Composable
private fun DataOpRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isWorking: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isWorking, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextDim,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isWorking) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = AppColors.Accent,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Строка выбора языка интерфейса: текущее значение + выпадающее меню. */
@Composable
private fun LanguageRow(
    selectedTag: String,
    onLanguageChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // Пары «тег локали → подпись»: названия языков всегда показываются
    // на самом языке (Русский / English), как принято в настройках
    val languages = listOf(
        LanguageStore.LANG_RU to stringResource(R.string.settings_language_ru),
        LanguageStore.LANG_EN to stringResource(R.string.settings_language_en),
    )
    val selectedName = languages.firstOrNull { it.first == selectedTag }?.second
        ?: languages.firstOrNull { selectedTag.startsWith(it.first) }?.second
        ?: selectedTag

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = AppColors.TextDim,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.settings_language_row),
                fontSize = 14.sp,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedName,
                fontSize = 12.5.sp,
                color = AppColors.TextFaint,
                modifier = Modifier.padding(end = 2.dp)
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextFaint,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                AppColors.Surface2
            )
        ) {
            languages.forEach { (tag, name) ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = name,
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp
                            )
                            if (tag == selectedTag) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppColors.Accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onLanguageChange(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Подпись версии приложения внизу экрана — моно-шрифт, приглушённый цвет. */
@Composable
private fun VersionFooter(
    versionName: String,
    versionCode: Int,
) {
    Text(
        text = stringResource(R.string.version_footer, versionName, versionCode),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = AppColors.TextFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/*
/**
 * Кастомный переключатель в стиле .switch из макета: пилюля 46x27dp,
 * заливка акцентом при включении, белый бегунок с пружинным ходом.
 */
@Composable
private fun AutSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switchThumbOffset"
    )

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(27.dp)
            .clip(RoundedCornerShape(Pill))
            .background(if (checked) .Accent else .Surface3)
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .offset(x = thumbOffset - 3.dp)
                .size(21.dp)
                .background(androidx.compose.ui.graphics.Color.White, CircleShape)
        )
    }
}*/
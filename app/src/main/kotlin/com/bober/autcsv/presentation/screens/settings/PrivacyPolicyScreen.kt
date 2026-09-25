package com.bober.autcsv.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.ui.theme.AppColors

/**
 * Экран «Политика конфиденциальности»: скроллируемые разделы с заголовками,
 * дата редакции внизу — стандартный паттерн текстовых экранов настроек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_privacy_policy),
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
                PolicySection(
                    titleRes = R.string.privacy_intro_title,
                    bodyRes = R.string.privacy_intro_body
                )
                PolicySection(
                    titleRes = R.string.privacy_storage_title,
                    bodyRes = R.string.privacy_storage_body
                )
                PolicySection(
                    titleRes = R.string.privacy_llm_title,
                    bodyRes = R.string.privacy_llm_body
                )
                PolicySection(
                    titleRes = R.string.privacy_permissions_title,
                    bodyRes = R.string.privacy_permissions_body
                )
                PolicySection(
                    titleRes = R.string.privacy_rights_title,
                    bodyRes = R.string.privacy_rights_body
                )
                PolicySection(
                    titleRes = R.string.privacy_changes_title,
                    bodyRes = R.string.privacy_changes_body
                )

                // Контакты: адрес подставляется в локализованный шаблон строки
                Text(
                    text = stringResource(R.string.privacy_contact_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.privacy_contact_body, SUPPORT_EMAIL),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextDim
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.privacy_effective_date),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = AppColors.TextFaint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun PolicySection(titleRes: Int, bodyRes: Int) {
    Text(
        text = stringResource(titleRes),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = AppColors.TextPrimary,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.TextDim
    )
}

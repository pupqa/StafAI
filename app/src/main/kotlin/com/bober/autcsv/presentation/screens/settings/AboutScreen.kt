package com.bober.autcsv.presentation.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.BuildConfig
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.AdaptiveContent
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.MD

/**
 * Экран «О приложении»: логотип, название, теглайн, версия, описание
 * и действия — написать в поддержку, поделиться приложением.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = AppColors.BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_about),
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
                // ── Шапка: логотип, имя, теглайн ────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(AppColors.Accent, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.splash_logo_letter),
                            color = AppColors.AccentInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.create_prof_resume),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextDim
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MD),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface2),
                    border = BorderStroke(1.dp, AppColors.BorderSoft)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.about_version_label),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
                            color = AppColors.TextFaint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.version_footer,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextDim
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AboutActionRow(
                    icon = { Icon(Icons.Outlined.MailOutline, null, tint = AppColors.TextDim) },
                    title = stringResource(R.string.about_contact_title),
                    subtitle = SUPPORT_EMAIL,
                    onClick = { openSupportEmail(context) }
                )
                AboutActionRow(
                    icon = { Icon(Icons.Outlined.Share, null, tint = AppColors.TextDim) },
                    title = stringResource(R.string.about_share_title),
                    subtitle = null,
                    onClick = { shareApp(context) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = AppColors.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.TextDim
                )
            }
        }
    }
}

/** Письмо в поддержку: тема подставляется заранее, ошибки запуска молча игнорируются. */
private fun openSupportEmail(context: android.content.Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_contact_subject))
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

/** Поделиться ссылкой на приложение системным шеринговым листом. */
private fun shareApp(context: android.content.Context) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.about_share_text))
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_resume_chooser))
        )
    }
}

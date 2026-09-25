package com.bober.autcsv.presentation.screens.form.components

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.LG

/**
 * Развёрнутые подсказки по заполнению: кнопка «?» рядом с лейблом поля
 * открывает нижний лист с вводным абзацем и списком советов.
 *
 * Контент хранится парой ресурсов: [tipsIntroRes] — интро-абзац,
 * [tipsBulletsRes] — string-array строк-советов. Заголовком листа служит
 * сам лейбл поля, поэтому отдельный ресурс заголовка не нужен.
 */

/** Лейбл поля в стиле формы; при заданных ресурсах советов — с кнопкой «?». */
@Composable
fun FieldLabelWithTips(
    labelText: String,
    @StringRes tipsIntroRes: Int,
    @ArrayRes tipsBulletsRes: Int,
    modifier: Modifier = Modifier,
) {
    var showTips by remember { mutableStateOf(false) }
    val hasTips = tipsIntroRes != 0 && tipsBulletsRes != 0

    Row(
        modifier = modifier.padding(bottom = 7.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = labelText.uppercase(),
            style = MaterialTheme.typography.labelSmall
        )
        if (hasTips) {
            Spacer(modifier = Modifier.width(7.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(AppColors.Surface2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.tips_icon_description),
                    tint = AppColors.Accent,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = { showTips = true })
                )
            }
        }
    }

    if (showTips && hasTips) {
        ResumeTipsSheet(
            title = labelText,
            introRes = tipsIntroRes,
            bulletsRes = tipsBulletsRes,
            onDismiss = { showTips = false }
        )
    }
}

/** Нижний лист с развёрнутыми советами: интро-абзац и маркированный список. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeTipsSheet(
    title: String,
    @StringRes introRes: Int,
    @ArrayRes bulletsRes: Int,
    onDismiss: () -> Unit,
) {
    val bullets = stringArrayResource(bulletsRes)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = LG, topEnd = LG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 30.dp)
        ) {
            Text(
                text = stringResource(R.string.tips_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Accent,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(introRes),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextDim
            )
            Spacer(modifier = Modifier.height(12.dp))
            bullets.forEach { bullet ->
                Row {
                    Text(
                        text = "\u2022",
                        color = AppColors.Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
            }
            // Отступ под жестовую навигационную панель
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

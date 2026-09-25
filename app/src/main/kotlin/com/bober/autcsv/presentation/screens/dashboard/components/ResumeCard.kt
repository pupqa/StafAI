package com.bober.autcsv.presentation.screens.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.CandidateStatus
import com.bober.autcsv.domain.model.Resume
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.LG
import com.bober.autcsv.ui.theme.Pill
import com.bober.autcsv.ui.theme.SM

/**
 * Карточка резюме в списке дашборда — стиль .rcard из макета:
 * имя, роль моноширинным шрифтом с префиксом «>», чип технологий, теги стека,
 * зарплата/релокация, статус пайплайна, избранное и дублирование.
 */
@Composable
fun ResumeCard(
    resume: Resume,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    onCycleStatus: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    compareSelected: Boolean = false,
    onCompareToggle: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label = "resumeCardScale"
    )

    val technologies = resume.professionalSkills.getAllTechnologies()

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(LG))
            .background(if (pressed) AppColors.Surface2 else AppColors.Surface)
            .border(
                1.dp,
                if (compareSelected) AppColors.Accent else AppColors.BorderSoft,
                RoundedCornerShape(LG)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onCompareToggle != null) {
                        Text(
                            text = if (compareSelected) "◉" else "○",
                            fontSize = 16.sp,
                            color = AppColors.Accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(SM))
                                .clickable(onClick = onCompareToggle)
                                // Расширенная зона нажатия: символ сам по себе
                                // слишком мал для комфортного тапа
                                .padding(start = 2.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                        )
                    }
                    Text(
                        text = resume.personalInfo.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (resume.isFavorite && onToggleFavorite == null) {
                        Text(text = " ★", color = AppColors.Accent, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (resume.personalInfo.specialization.isNotBlank()) {
                    Text(
                        text = "> ${resume.personalInfo.specialization}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = AppColors.Accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onToggleFavorite != null) {
                    Icon(
                        imageVector = if (resume.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (resume.isFavorite) {
                            stringResource(R.string.card_remove_favorite)
                        } else {
                            stringResource(R.string.card_add_favorite)
                        },
                        tint = if (resume.isFavorite) AppColors.Accent else AppColors.TextFaint,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(SM))
                            .clickable(onClick = {
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm
                                )
                                onToggleFavorite()
                            })
                            .padding(2.dp)
                    )
                }
                if (onDuplicate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.card_duplicate),
                        tint = AppColors.TextFaint,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(SM))
                            .clickable(onClick = onDuplicate)
                            .padding(2.dp)
                    )
                }
                if (technologies.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Pill))
                            .background(AppColors.AmberSoft)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = stringResource(
                                R.string.card_technologies_count,
                                technologies.size
                            ),
                            tint = AppColors.Accent,
                            modifier = Modifier
                                .height(11.dp)
                                .width(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${technologies.size}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = AppColors.Accent
                        )
                    }
                }
            }
        }

        // Зарплата · релокация · статус пайплайна
        val metaLine = buildList {
            if (resume.personalInfo.salaryMin.isNotBlank()) {
                add(
                    stringResource(R.string.card_salary_from_val, resume.personalInfo.salaryMin) +
                            if (resume.personalInfo.salaryMax.isNotBlank())
                                stringResource(
                                    R.string.card_salary_to_val,
                                    resume.personalInfo.salaryMax
                                )
                            else ""
                )
            }
            // Значение — свободный текст, выбранный при текущей локали
            if (resume.personalInfo.readyToRelocate.startsWith("Готов") ||
                resume.personalInfo.readyToRelocate.startsWith("Ready")
            ) add(stringResource(R.string.card_relocation_short))
            if (resume.personalInfo.location.isNotBlank()) add(resume.personalInfo.location)
        }.joinToString(" · ")
        if (metaLine.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = metaLine,
                fontSize = 12.sp,
                color = AppColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onCycleStatus != null && resume.status != CandidateStatus.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(resume.status.labelRes),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AppColors.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(Pill))
                    .background(AppColors.AmberSoft)
                    .clickable(onClick = onCycleStatus)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        } else if (onCycleStatus != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.card_status_none),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AppColors.TextFaint,
                modifier = Modifier
                    .clip(RoundedCornerShape(Pill))
                    .background(AppColors.Surface2)
                    .clickable(onClick = onCycleStatus)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }

        if (resume.personalInfo.specializationExperience.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.card_experience_val,
                    resume.personalInfo.specializationExperience
                ),
                fontSize = 12.sp,
                color = AppColors.TextFaint
            )
        }

        val techPreview = technologies.take(3)
        if (techPreview.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                techPreview.forEach { tech ->
                    Text(
                        text = tech,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AppColors.TextDim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(SM))
                            .background(AppColors.Surface2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (resume.summary.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = resume.summary,
                fontSize = 12.5.sp,
                color = AppColors.TextDim,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

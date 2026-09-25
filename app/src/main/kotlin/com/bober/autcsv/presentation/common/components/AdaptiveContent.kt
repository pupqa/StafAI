package com.bober.autcsv.presentation.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Максимальная ширина контента на планшетах в портретной ориентации. */
val ADAPTIVE_CONTENT_MAX_WIDTH: Dp = 720.dp

/**
 * Адаптивный контейнер контента: на телефоне занимает всю ширину,
 * на широких экранах (планшеты в портрете) ограничен [maxWidth]
 * и центрирован — форма не растягивается на всю ширину.
 *
 * Фоновые заливки остаются full-bleed, ограничивается только контент.
 */
@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ADAPTIVE_CONTENT_MAX_WIDTH,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)) {
            content()
        }
    }
}

package com.bober.autcsv.presentation.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.ui.theme.CYellow
import com.bober.autcsv.ui.theme.Gray
import com.bober.autcsv.ui.theme.White

/**
 * Универсальное текстовое поле с закруглёнными углами и поддержкой лейбла,
 * многострочного ввода и настроек клавиатуры.
 */
@Composable
fun RoundedCorner(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: Int? = null,
    labelText: String? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester = FocusRequester(),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (maxLines == 1) Modifier.height(52.dp) else Modifier)

    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedTextColor = Gray.copy(alpha = 0.8f),
                focusedTextColor = White,
                unfocusedPlaceholderColor = Gray.copy(alpha = 0.6f),
                focusedPlaceholderColor = CYellow.copy(alpha = 0.8f),
                cursorColor = CYellow,
                focusedLabelColor = CYellow,
                unfocusedLabelColor = Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (maxLines == 1) Modifier.height(52.dp) else Modifier)
                .focusRequester(focusRequester),
            label = {
                when {
                    label != null -> Text(text = stringResource(label))
                    labelText != null -> Text(text = labelText)
                }
            },
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}
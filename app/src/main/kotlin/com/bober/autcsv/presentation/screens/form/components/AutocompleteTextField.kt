package com.bober.autcsv.presentation.screens.form.components

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.bober.autcsv.R
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary
import com.bober.autcsv.ui.theme.MD

/**
 * Поле ввода с автодополнением (как в hh / LinkedIn).
 *
 * Ключевая деталь реализации: выпадающий список открывается в попапе с
 * focusable = false — иначе попап перехватывает фокус у текстового поля,
 * IME закрывается и ручной ввод становится невозможен. Фильтрация
 * справочника выполняется синхронно по текущему значению поля (словари
 * маленькие, результат кэшируется в [SuggestionDictionary.filter]),
 * поэтому подсказки не отстают от ввода.
 */
@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    hint: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onSuggestionPicked: ((String) -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    @StringRes tipsIntroRes: Int = 0,
    @ArrayRes tipsBulletsRes: Int = 0,
) {
    var expanded by remember { mutableStateOf(false) }

    // Строки доступности выносятся из лямбд: stringResource — composable-вызов
    val toggleDescription = stringResource(
        if (expanded) R.string.form_hide_suggestions else R.string.form_show_suggestions
    )
    val autocompleteDescription = stringResource(R.string.form_autocomplete_field, labelText)

    // derivedStateOf исключает повторную фильтрацию при несвязанных перерисовках
    val visibleSuggestions by remember(suggestions) {
        derivedStateOf { SuggestionDictionary.filter(suggestions, value) }
    }

    Column(modifier = modifier) {
        StaffAiTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                expanded = true
            },
            labelText = labelText,
            hint = hint,
            isError = isError,
            errorText = errorText,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardType = keyboardType,
            imeAction = imeAction,
            keyboardActions = keyboardActions,
            onFocusChanged = onFocusChanged,
            tipsIntroRes = tipsIntroRes,
            tipsBulletsRes = tipsBulletsRes,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = toggleDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = autocompleteDescription }
        )

        androidx.compose.foundation.layout.Box {
            DropdownMenu(
                expanded = expanded && visibleSuggestions.isNotEmpty(),
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(MD),
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .fillMaxWidth(0.95f)
            ) {
                visibleSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                            onSuggestionPicked?.invoke(suggestion)
                        }
                    )
                }
            }
        }
    }
}

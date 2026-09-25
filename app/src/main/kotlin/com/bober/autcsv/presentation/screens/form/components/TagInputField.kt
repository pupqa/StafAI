package com.bober.autcsv.presentation.screens.form.components

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.Pill

/**
 * Поле ввода тегов: выбранные значения показываются снимаемыми чипами,
 * новые добавляются выбором из списка автодополнения, клавишей «Готово»
 * на клавиатуре или вводом произвольного текста.
 *
 * Значение хранится снаружи как строка тегов через запятую — формат
 * совпадает с сохранением скиллов в БД, состояние не меняется.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInputField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    hint: String? = null,
    @StringRes tipsIntroRes: Int = 0,
    @ArrayRes tipsBulletsRes: Int = 0,
) {
    val tags = remember(value) {
        value.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    var input by remember { mutableStateOf("") }

    // Уже выбранные теги не предлагаются повторно
    val availableSuggestions = remember(suggestions, tags) {
        suggestions.filter { suggestion -> tags.none { it.equals(suggestion, ignoreCase = true) } }
    }

    fun commitTag(raw: String) {
        val tag = raw.trim()
        if (tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }) {
            onValueChange((tags + tag).joinToString(", "))
        }
        input = ""
    }

    fun removeTag(tag: String) {
        onValueChange(tags.filter { !it.equals(tag, ignoreCase = true) }.joinToString(", "))
    }

    Column(modifier = modifier) {
        FieldLabelWithTips(
            labelText = labelText,
            tipsIntroRes = tipsIntroRes,
            tipsBulletsRes = tipsBulletsRes
        )

        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                tags.forEach { tag ->
                    TagChip(tag = tag, onRemove = { removeTag(tag) })
                }
            }
        }

        AutocompleteTextField(
            value = input,
            onValueChange = { input = it },
            labelText = stringResource(R.string.add_skill),
            hint = hint,
            suggestions = availableSuggestions,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { commitTag(input) }),
            onSuggestionPicked = { commitTag(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Снимаемый чип выбранного тега. */
@Composable
private fun TagChip(tag: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .padding(end = 6.dp, bottom = 6.dp)
            .background(AppColors.Surface2, RoundedCornerShape(Pill))
            .border(1.dp, AppColors.BorderSoft, RoundedCornerShape(Pill))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.form_remove_tag, tag),
            tint = AppColors.TextDim,
            modifier = Modifier
                // Увеличенная зона нажатия: сам крестик 18 dp слишком мал
                .size(26.dp)
                .clickable(onClick = onRemove)
                .padding(5.dp)
        )
    }
}


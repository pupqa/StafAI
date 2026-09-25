package com.bober.autcsv.presentation.screens.form.components

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.LG
import com.bober.autcsv.ui.theme.MD
import com.bober.autcsv.ui.theme.Pill

/** Карточка-раздел формы: тёмная поверхность, мягкая рамка, скруглённые углы. */
@Composable
fun StaffAiSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: ColumnScopeContent,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(LG),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.BorderSoft)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            content()
        }
    }
}

typealias ColumnScopeContent = @Composable () -> Unit

/** Общие цвета «пилюльного» поля — общие для всех вариантов полей формы. */
@Composable
private fun staffAiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppColors.Surface2,
    unfocusedContainerColor = AppColors.Surface2,
    disabledContainerColor = AppColors.Surface2,
    focusedBorderColor = AppColors.Accent,
    unfocusedBorderColor = AppColors.BorderSoft,
    errorBorderColor = AppColors.Danger,
    cursorColor = AppColors.Accent,
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedPlaceholderColor = AppColors.TextFaint,
    unfocusedPlaceholderColor = AppColors.TextFaint,
)

/** Текст под полем: ошибка имеет приоритет, иначе — подсказка. */
@Composable
private fun FieldFooter(errorText: String?, hint: String?) {
    when {
        errorText != null -> Text(
            text = errorText,
            color = AppColors.Danger,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, start = 6.dp)
        )

        hint != null -> Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, start = 6.dp)
        )
    }
}

/** Поле ввода в стиле макета: моно-лейбл капслоком над "пилюльным" полем. */
@Composable
fun StaffAiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    @StringRes tipsIntroRes: Int = 0,
    @ArrayRes tipsBulletsRes: Int = 0,
) {
    Column(modifier = modifier) {
        FieldLabelWithTips(
            labelText = labelText,
            tipsIntroRes = tipsIntroRes,
            tipsBulletsRes = tipsBulletsRes
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onFocusChanged != null) {
                        Modifier.onFocusChanged { onFocusChanged(it.hasFocus) }
                    } else {
                        Modifier
                    }
                ),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else maxLines,
            isError = isError,
            trailingIcon = trailingIcon,
            placeholder = if (placeholder != null) {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium) }
            } else null,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(MD),
            colors = staffAiFieldColors()
        )
        FieldFooter(
            errorText = if (isError) errorText else null,
            hint = hint,
        )
    }
}

/**
 * Поле с автоформатированием (маска телефона, группировка разрядов):
 * курсор после каждого изменения ставится в конец текста, пока фокус
 * не ушёл на другое поле.
 *
 * Проблема обычного [StaffAiTextField]: при трансформации в onValueChange
 * длина текста меняется, а выделение остаётся на старом смещении — курсор
 * «уезжает» в середину и следующие символы вставляются не туда. Здесь
 * значение держится как [TextFieldValue] и синхронизируется с внешним
 * состоянием всегда с выделением «в конец».
 */
@Composable
fun StaffAiTextFieldValue(
    value: String,
    onTextChange: (String) -> Unit,
    labelText: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    @StringRes tipsIntroRes: Int = 0,
    @ArrayRes tipsBulletsRes: Int = 0,
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }

    // Внешнее изменение (загрузка резюме, очистка, формат сверху):
    // принимаем новый текст, курсор — в конец
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }

    Column(modifier = modifier) {
        FieldLabelWithTips(
            labelText = labelText,
            tipsIntroRes = tipsIntroRes,
            tipsBulletsRes = tipsBulletsRes
        )
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { typed ->
                onTextChange(typed.text)
                // Курсор всегда в конец введённого текста
                fieldValue = typed.copy(selection = TextRange(typed.text.length))
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onFocusChanged != null) {
                        Modifier.onFocusChanged { onFocusChanged(it.hasFocus) }
                    } else {
                        Modifier
                    }
                ),
            singleLine = true,
            isError = isError,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            shape = RoundedCornerShape(MD),
            colors = staffAiFieldColors()
        )
        FieldFooter(
            errorText = if (isError) errorText else null,
            hint = hint,
        )
    }
}

/** Моно-лейбл раздела, например "РЕЗЮМЕ · 3" в макете. */
@Composable
fun StaffAiSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, modifier = modifier)
}

/** Круглая кнопка-иконка на поверхности, как iconbtn в макете. */
@Composable
fun StaffAiIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    background: Color = AppColors.Surface2,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

/** Иконка удаления в приглушённо-розовом (danger) тоне. */
@Composable
fun StaffAiDeleteIconButton(onClick: () -> Unit, contentDescription: String?) {
    StaffAiIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        icon = Icons.Default.Delete,
        tint = AppColors.Danger,
        background = AppColors.DangerSoft
    )
}

/** Кнопка добавления элемента — золотая пилюля с плюсом, как в макете. */
@Composable
fun StaffAiAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Accent,
            contentColor = AppColors.AccentInk
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(
            text,
            modifier = Modifier.padding(start = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/** Пилюльный сегментированный ряд табов (как горизонтальные вкладки формы в макете). */
@Composable
fun StaffAiTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) AppColors.Accent else AppColors.Surface2,
                label = "tabBg"
            )
            val fg by animateColorAsState(
                if (selected) AppColors.AccentInk else AppColors.TextDim,
                label = "tabFg"
            )
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .background(bg, RoundedCornerShape(Pill))
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else AppColors.BorderSoft,
                        RoundedCornerShape(Pill)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = fg,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/** Плоская текстовая кнопка золотого акцента (например "Сохранить" в шапке). */
@Composable
fun StaffAiTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

/** Селектируемый чип уровня (используется для выбора уровня образования). */
@Composable
fun StaffAiLevelChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        if (selected) AppColors.Accent else AppColors.Surface2,
        label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) AppColors.AccentInk else AppColors.TextDim,
        label = "chipFg"
    )
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(Pill))
            .border(
                1.dp,
                if (selected) Color.Transparent else AppColors.BorderSoft,
                RoundedCornerShape(Pill)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

/** Мягкая outline-кнопка, как btn-outline в макете. */
@Composable
fun StaffAiOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(MD),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}
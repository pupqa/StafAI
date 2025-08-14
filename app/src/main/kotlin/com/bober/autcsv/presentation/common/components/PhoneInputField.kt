package com.bober.autcsv.presentation.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.PhoneFormatter

/**
 * Поле ввода телефона с маской и валидацией.
 * Хранит только цифры во внешнем состоянии, форматирует отображение.
 */
@Composable
fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    val isPhoneValid = PhoneFormatter.isValidPhoneNumber(value)
    val showError = isError || (value.isNotEmpty() && !isPhoneValid)
    val errorText = errorMessage ?: if (value.isNotEmpty() && !isPhoneValid) {
        stringResource(R.string.error_invalid_phone)
    } else null

    // Отображаемое значение (отформатированное)
    val displayValue = PhoneFormatter.formatPhoneNumber(value)

    // Состояние для TextFieldValue с курсором в конце
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = displayValue,
                selection = TextRange(displayValue.length)
            )
        )
    }

    // Обновляем TextFieldValue при изменении значения
    LaunchedEffect(displayValue) {
        textFieldValue = TextFieldValue(
            text = displayValue,
            selection = TextRange(displayValue.length)
        )
    }

    // Обработка ввода пользователя
    val onPhoneValueChange: (TextFieldValue) -> Unit = { newTextFieldValue ->
        // Извлекаем только цифры из введенного значения
        val newDigits = PhoneFormatter.extractDigits(newTextFieldValue.text)

        // Ограничиваем длину до 15 цифр
        val limitedDigits = if (newDigits.length > 15) {
            newDigits.substring(0, 15)
        } else {
            newDigits
        }

        // Передаем только цифры в родительский компонент
        onValueChange(limitedDigits)

        // Обновляем TextFieldValue с курсором в конце
        val formattedValue = PhoneFormatter.formatPhoneNumber(limitedDigits)
        textFieldValue = TextFieldValue(
            text = formattedValue,
            selection = TextRange(formattedValue.length)
        )
    }

    TextField(
        value = textFieldValue,
        onValueChange = onPhoneValueChange,
        label = { Text(text = labelText) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone
        ),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        singleLine = true,
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
            fontSize = 14.sp
        )
    )

    /*
    if (showError && errorText != null) {
        Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    if (value.isEmpty()) {
        Text(
            text = stringResource(R.string.phone_format_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )
    }
     */
} 
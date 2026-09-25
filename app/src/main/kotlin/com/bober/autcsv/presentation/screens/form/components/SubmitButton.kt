package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bober.autcsv.R
import com.bober.autcsv.ui.theme.AppColors
import com.bober.autcsv.ui.theme.Pill

/**
 * Кнопка сохранения формы: stateless — получает состояние снаружи.
 */
@Composable
fun SubmitButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val saveDescription = stringResource(R.string.form_save_resume)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(52.dp)
            .semantics { contentDescription = saveDescription },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.Accent,
            contentColor = AppColors.AccentInk,
            disabledContainerColor = AppColors.Surface2,
            disabledContentColor = AppColors.TextFaint,
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = AppColors.AccentInk,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = stringResource(R.string.save),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

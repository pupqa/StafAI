package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.presentation.common.components.PhoneInputField
import com.bober.autcsv.presentation.common.components.RoundedCorner
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent

@Composable
fun PersonalInfoSection(
    fullName: String,
    specialization: String,
    totalExperience: String,
    specializationExperience: String,
    education: String,
    email: String,
    phone: String,
    aboutMe: String = "",
    location: String = "",
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.personal_info),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            RoundedCorner(
                value = fullName,
                onValueChange = { onEvent(ResumeFormEvent.FullNameChanged(it)) },
                labelText = stringResource(R.string.full_name),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = specialization,
                onValueChange = { onEvent(ResumeFormEvent.SpecializationChanged(it)) },
                labelText = stringResource(R.string.specialization),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = totalExperience,
                onValueChange = { onEvent(ResumeFormEvent.TotalExperienceChanged(it)) },
                labelText = stringResource(R.string.total_experience),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = specializationExperience,
                onValueChange = { onEvent(ResumeFormEvent.SpecializationExperienceChanged(it)) },
                labelText = stringResource(R.string.specialization_experience),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = education,
                onValueChange = { onEvent(ResumeFormEvent.EducationChanged(it)) },
                labelText = stringResource(R.string.education),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = email,
                onValueChange = { onEvent(ResumeFormEvent.EmailChanged(it)) },
                labelText = stringResource(R.string.email),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            if (!isValidEmail(email)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.error_invalid_email),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PhoneInputField(
                value = phone,
                onValueChange = { onEvent(ResumeFormEvent.PhoneChanged(it)) },
                labelText = stringResource(R.string.phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                value = location,
                onValueChange = { onEvent(ResumeFormEvent.LocationChanged(it)) },
                labelText = stringResource(R.string.location),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoundedCorner(
                singleLine = false,
                maxLines = 3,
                value = aboutMe,
                onValueChange = { onEvent(ResumeFormEvent.AboutMeChanged(it)) },
                labelText = stringResource(R.string.about_me),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
} 
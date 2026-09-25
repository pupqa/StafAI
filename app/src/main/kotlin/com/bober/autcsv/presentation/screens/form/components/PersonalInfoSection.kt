package com.bober.autcsv.presentation.screens.form.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.core.utils.PhoneFormatter
import com.bober.autcsv.presentation.screens.form.FormValidators
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Раздел «Личное»: ФИО, желаемая должность (с автодополнением), опыт, контакты,
 * город, о себе. Зарплата, релокация и соцсети вынесены в отдельные секции.
 * Полностью stateless: получает значения и шлёт события наверх — лёгкая
 * мемоизация и никакого обращения к ViewModel изнутри.
 * Валидация полей — в реальном времени через [FormValidators].
 */
@Composable
fun PersonalInfoSection(
    fullName: String,
    specialization: String,
    totalExperience: String,
    specializationExperience: String,
    email: String,
    phone: String,
    aboutMe: String = "",
    location: String = "",
    photoUri: String = "",
    resumeId: String = "",
    aiBusy: Boolean = false,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ошибки пересчитываются только при изменении соответствующего значения
    val fullNameError = remember(fullName) {
        FormValidators.validateField(FormValidators.Field.FULL_NAME, fullName)
    }
    val specializationError = remember(specialization) {
        FormValidators.validateField(FormValidators.Field.SPECIALIZATION, specialization)
    }
    val experienceError = remember(totalExperience) {
        FormValidators.validateField(FormValidators.Field.TOTAL_EXPERIENCE, totalExperience)
    }
    val emailError = remember(email) {
        FormValidators.validateField(FormValidators.Field.EMAIL, email)
    }
    val phoneError = remember(phone) {
        FormValidators.validateField(FormValidators.Field.PHONE, phone)
    }

    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.personal_info)
    ) {
        Column {
            PhotoPickerRow(
                photoUri = photoUri,
                resumeId = resumeId,
                onPhotoPicked = { onEvent(ResumeFormEvent.PhotoChanged(it)) },
                onPhotoRemoved = { onEvent(ResumeFormEvent.PhotoChanged("")) },
            )
            Spacer(modifier = Modifier.height(14.dp))
            StaffAiTextField(
                value = fullName,
                onValueChange = { onEvent(ResumeFormEvent.FullNameChanged(it)) },
                labelText = stringResource(R.string.full_name),
                hint = stringResource(R.string.full_name),
                isError = fullNameError != null,
                errorText = fullNameError?.let { stringResource(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            AutocompleteTextField(
                value = specialization,
                onValueChange = { onEvent(ResumeFormEvent.SpecializationChanged(it)) },
                labelText = stringResource(R.string.specialization),
                suggestions = SuggestionDictionary.positions,
                tipsIntroRes = R.string.tips_position_intro,
                tipsBulletsRes = R.array.tips_position,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            StaffAiTextField(
                value = totalExperience,
                onValueChange = { onEvent(ResumeFormEvent.TotalExperienceChanged(it)) },
                labelText = stringResource(R.string.total_experience),
                keyboardType = KeyboardType.Number,
                isError = experienceError != null,
                errorText = experienceError?.let { stringResource(it) },
                tipsIntroRes = R.string.tips_experience_intro,
                tipsBulletsRes = R.array.tips_experience,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            StaffAiTextField(
                value = specializationExperience,
                onValueChange = { onEvent(ResumeFormEvent.SpecializationExperienceChanged(it)) },
                labelText = stringResource(R.string.specialization_experience),
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            StaffAiTextField(
                value = email,
                onValueChange = { onEvent(ResumeFormEvent.EmailChanged(it)) },
                labelText = stringResource(R.string.email),
                keyboardType = KeyboardType.Email,
                isError = emailError != null,
                errorText = emailError?.let { stringResource(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Маска телефона с автоформатированием: курсор всегда в конце
            // ввода (StaffAiTextFieldValue), иначе после вставки скобок он
            // оставался на старой позиции и цифры вставлялись не туда
            StaffAiTextFieldValue(
                value = PhoneFormatter.formatPhoneNumber(phone),
                onTextChange = { text ->
                    val digits = PhoneFormatter.extractDigits(text).take(11)
                    onEvent(ResumeFormEvent.PhoneChanged(PhoneFormatter.formatPhoneNumber(digits)))
                },
                labelText = stringResource(R.string.phone),
                hint = stringResource(R.string.phone_format_hint),
                keyboardType = KeyboardType.Phone,
                isError = phoneError != null,
                errorText = phoneError?.let { stringResource(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            AutocompleteTextField(
                value = location,
                onValueChange = { onEvent(ResumeFormEvent.LocationChanged(it)) },
                labelText = stringResource(R.string.location),
                suggestions = SuggestionDictionary.cities,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            StaffAiTextField(
                value = aboutMe,
                onValueChange = { onEvent(ResumeFormEvent.AboutMeChanged(it)) },
                labelText = stringResource(R.string.about_me),
                hint = stringResource(R.string.about_me),
                singleLine = false,
                maxLines = 3,
                tipsIntroRes = R.string.tips_about_intro,
                tipsBulletsRes = R.array.tips_about,
                modifier = Modifier.fillMaxWidth()
            )

            // ИИ-помощники для «О себе»: улучшить написанное или собрать из данных
            Row(modifier = Modifier.fillMaxWidth()) {
                StaffAiOutlineButton(
                    text = stringResource(R.string.ai_improve),
                    onClick = { onEvent(ResumeFormEvent.ImproveAboutMe) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StaffAiOutlineButton(
                    text = stringResource(R.string.ai_generate),
                    onClick = { onEvent(ResumeFormEvent.GenerateAboutMe) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (aiBusy) {
                Text(
                    text = stringResource(R.string.ai_busy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                )
            }
        }
    }
}

/**
 * Строка выбора фото кандидата: круглое превью + кнопки заменить/убрать.
 * Выбранное изображение копируется в filesDir/photos/{resumeId}.jpg —
 * переживает перезапуск приложения и попадает в PDF.
 */
@Composable
private fun PhotoPickerRow(
    photoUri: String,
    resumeId: String,
    onPhotoPicked: (String) -> Unit,
    onPhotoRemoved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching {
                val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
                val target = File(photosDir, "$resumeId.jpg")
                // Центр-кроп до квадрата + ограничение размера: без этого
                // вертикальное фото «сплющивалось» круглой маской в PDF
                saveSquareThumbnail(context.contentResolver, uri, target)
                withContext(Dispatchers.Main) { onPhotoPicked(target.absolutePath) }
            }.onFailure {
                com.bober.autcsv.core.utils.LlmLogger.logWarning("Не удалось сохранить фото: ${it.message}")
            }
        }
    }

    fun launchPicker() = pickerLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val photoFile = remember(photoUri) { photoUri.takeIf { it.isNotBlank() }?.let(::File) }
        if (photoFile != null && photoFile.exists()) {
            // Декод и даунскейл — в IO-потоке: чтение файла из композиции
            // подвешивало бы main thread при открытии формы
            val decoded by produceState<android.graphics.Bitmap?>(null, photoFile.absolutePath) {
                value = withContext(Dispatchers.IO) { decodeScaledBitmap(photoFile, sizePx = 256) }
            }
            val bitmap = decoded
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.change_photo),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable { launchPicker() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    StaffAiOutlineButton(
                        text = stringResource(R.string.change_photo),
                        onClick = { launchPicker() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.remove_photo),
                        color = com.bober.autcsv.ui.theme.AppColors.Danger,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clickable(onClick = onPhotoRemoved)
                            .padding(top = 2.dp)
                    )
                }
            }
        } else {
            StaffAiOutlineButton(
                text = stringResource(R.string.add_photo),
                onClick = { launchPicker() }
            )
        }
    }
}

/** Декодирует фото с понижением разрешения для превью. */
private fun decodeScaledBitmap(file: File, sizePx: Int): android.graphics.Bitmap? {
    if (!file.exists()) return null
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) {
        sample *= 2
    }
    return android.graphics.BitmapFactory.decodeFile(
        file.absolutePath,
        android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    )
}

/** Максимальная сторона сохранённого фото, px. */
private const val PHOTO_MAX_SIDE_PX = 1024

/**
 * Декодирует выбранное изображение (два прохода по потоку: границы,
 * затем с inSampleSize), обрезает центральный квадрат по минимальной
 * стороне с ограничением [PHOTO_MAX_SIDE_PX] и пишет JPEG в [target].
 */
private fun saveSquareThumbnail(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri,
    target: File,
) {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)
        ?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Не удалось декодировать изображение" }

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= PHOTO_MAX_SIDE_PX &&
        bounds.outHeight / (sample * 2) >= PHOTO_MAX_SIDE_PX
    ) sample *= 2

    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, options)
    } ?: throw IllegalArgumentException("Пустой поток изображения")

    val side = minOf(decoded.width, decoded.height)
    val left = (decoded.width - side) / 2
    val top = (decoded.height - side) / 2
    val square = android.graphics.Bitmap.createBitmap(decoded, left, top, side, side)
    java.io.FileOutputStream(target).use { output ->
        square.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, output)
    }
    if (square !== decoded) decoded.recycle()
}

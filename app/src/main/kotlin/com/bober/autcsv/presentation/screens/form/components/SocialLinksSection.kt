package com.bober.autcsv.presentation.screens.form.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bober.autcsv.R
import com.bober.autcsv.domain.model.SocialLink
import com.bober.autcsv.presentation.screens.form.FormValidators
import com.bober.autcsv.presentation.screens.form.ResumeFormEvent
import com.bober.autcsv.presentation.screens.form.SuggestionDictionary

/**
 * Раздел «Соцсети и профили»: опциональный список ссылок
 * (платформа с автодополнением + адрес). key() сохраняет состояние
 * полей при удалении элементов.
 */
@Composable
fun SocialLinksSection(
    socialLinks: List<SocialLink>,
    onEvent: (ResumeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StaffAiSectionCard(
        modifier = modifier,
        title = stringResource(R.string.social_links)
    ) {
        Column {
            socialLinks.forEachIndexed { index, link ->
                key(index) {
                    // Валидация ссылки: полный URL, домен без схемы или @ник
                    val urlError = remember(link.url) {
                        FormValidators.validateField(FormValidators.Field.SOCIAL_URL, link.url)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AutocompleteTextField(
                            value = link.platform,
                            onValueChange = {
                                onEvent(ResumeFormEvent.SocialLinkPlatformChanged(index, it))
                            },
                            labelText = stringResource(R.string.social_link_platform),
                            suggestions = SuggestionDictionary.socialPlatforms,
                            modifier = Modifier.weight(0.9f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StaffAiTextField(
                            value = link.url,
                            onValueChange = {
                                onEvent(ResumeFormEvent.SocialLinkUrlChanged(index, it))
                            },
                            labelText = stringResource(R.string.social_link_url),
                            placeholder = placeholderForPlatform(link.platform),
                            isError = urlError != null,
                            errorText = urlError?.let { stringResource(it) },
                            tipsIntroRes = R.string.tips_social_links_intro,
                            tipsBulletsRes = R.array.tips_social_links,
                            modifier = Modifier.weight(1.3f)
                        )
                        StaffAiDeleteIconButton(
                            onClick = { onEvent(ResumeFormEvent.DeleteSocialLink(index)) },
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            StaffAiAddButton(
                text = stringResource(R.string.add_social_link),
                onClick = { onEvent(ResumeFormEvent.AddSocialLink) },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * Шаблон ссылки-подсказки внутри поля (placeholder): подставляется
 * под выбранную платформу, пока поле адреса пустое. Ключи платформ
 * включают алиасы на обоих языках интерфейса.
 */
@Composable
internal fun placeholderForPlatform(platform: String): String {
    val resId = when (platform.trim().lowercase()) {
        "vk", "вк", "вконтакте" -> R.string.social_ph_vk
        "telegram", "телеграм", "tg", "t.me" -> R.string.social_ph_telegram
        "github", "гитхаб" -> R.string.social_ph_github
        "gitlab", "гитлаб" -> R.string.social_ph_gitlab
        "linkedin", "линкедин" -> R.string.social_ph_linkedin
        "habr career", "habr", "хабр", "хабр карьер" -> R.string.social_ph_habr
        "behance", "беханс" -> R.string.social_ph_behance
        "dribbble", "дріббл", "дриббл" -> R.string.social_ph_dribbble
        "youtube", "ютуб" -> R.string.social_ph_youtube
        "личный сайт", "сайт", "personal website", "website", "site" -> R.string.social_ph_site
        else -> R.string.social_ph_generic
    }
    return stringResource(resId)
}

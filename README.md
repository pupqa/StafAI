# СтафИИ (AutCSV / StaffAI)

Android-приложение для создания профессиональных резюме с ИИ-помощником: умная форма с автодополнением, пять PDF-шаблонов с настраиваемым оформлением, экспорт в **PDF / DOCX / CSV**, обратимый импорт из собственного PDF, ИИ-анализ и улучшение текстов через OpenRouter, панель рекрутера с фильтрами и сравнением кандидатов. Полная локализация **русский / английский** со сменой языка на лету.

Все пользовательские данные хранятся **только на устройстве**.

---

## Возможности

- **Конструктор резюме**: 5 вкладок формы (личные данные, образование, языки, навыки, проекты), автодополнение по словарям (специальности, вузы, города, технологии), теги навыков, многострочные задачи проектов, прогресс заполнения по вкладкам, дебаунс-валидация и автосохранение черновиков.
- **ИИ-помощник** (через личный API-ключ OpenRouter): структурированный анализ резюме (оценка, сильные стороны, зоны роста), улучшение текста «О себе», генерация раздела по данным резюме, сопоставление резюме с текстом вакансии.
- **Сопроводительные письма**: LLM-генерация под конкретную вакансию с редактированием, хранением писем в БД и шерингом текста.
- **Автоперевод резюме RU ↔ EN**: создаёт отдельную языковую версию — свободные тексты переводит LLM, структура и нетекстовые данные сохраняются.
- **PDF-движок**: 5 шаблонов (Профессиональный, Современный, Минималистичный, Креативный, Сайдбар), выбор акцентного цвета, настройка порядка секций, фото профиля, печать и шеринг.
- **Экспорт**: PDF, редактируемый DOCX (OOXML собирается вручную, без внешних библиотек), таблица CSV (RFC 4180, BOM, разделитель «;», 30 колонок).
- **Импорт**: открытие ранее экспортированного PDF восстанавливает резюме целиком — по невидимым метаданным внутри файла; есть fallback-разбор видимого текста (в т.ч. выгрузок hh.ru).
- **Панель управления (дашборд)**: поиск, сортировка, фильтры (опыт, технологии, языки, специализация, зарплата, статус пайплайна, релокация, избранное), сравнение двух кандидатов, дублирование карточек, статусы найма.
- **Корзина** с восстановлением; **бэкап** всех резюме с фото в JSON и восстановление.
- **Безопасность**: биометрическая блокировка входа, API-ключ в EncryptedSharedPreferences.
- **Локализация RU/EN** с переключателем в настройках (перезапуск не требуется); локализуются и интерфейс, и генерируемые документы.

---

## Технологический стек

| Компонент | Технология | Версия |
|---|---|---|
| Язык | Kotlin | 2.2.10 |
| Сборка | AGP / Gradle Wrapper | 8.12.0 |
| UI | Jetpack Compose (BOM), Material 3 | 2025.08.00 |
| DI | Hilt (`KSP`) + hilt-navigation-compose | 2.57.2 / 1.3.0 |
| БД | Room (`KSP`) + TypeConverters на Gson | 2.8.2, схема v8 |
| Сеть | Retrofit + OkHttp + Gson-converter | 3.0.0 / 5.2.1 |
| Навигация | Navigation Compose, типобезопасные маршруты (kotlinx.serialization) | 2.9.5 |
| PDF | `android.graphics.pdf.PdfDocument` + pdfbox-android (метаданные) | 2.0.27.0 |
| Шифрование | androidx.security (EncryptedSharedPreferences) | 1.1.0-alpha06 |
| Биометрия | androidx.biometric | 1.1.0 |
| Локали | appcompat (`AppCompatDelegate.setApplicationLocales`) | 1.7.1 |
| Корутины | kotlinx-coroutines | 1.10.2 |
| Прочее | core-splashscreen, desugar_jdk_libs | — |

**SDK**: minSdk 32 (Android 12L), targetSdk 35, compileSdk 36, JVM target 17.

В каталоге зависимостей также объявлены Ktor и OpenRouter-клиент — это экспериментальный запасной стек, активный путь работы с LLM построен на Retrofit.

### Тестовые зависимости
JUnit 4, MockK, kotlinx-coroutines-test, Turbine; для юнит-тестов PDF-экспорта подключены стабы `android.graphics.*` (см. [Тестирование](#тестирование)).

---

## Сборка и запуск

1. Android Studio (Narwhal или новее), JDK 17.
2. Склонировать репозиторий и открыть его как Android-проект.
3. Создать/проверить `local.properties` (путь к SDK подставит студия). Опционально добавить ключ OpenRouter по умолчанию:

   ```properties
   # local.properties
   OPENROUTER_API_KEY=sk-or-v1-...
   ```

   Значение попадает в `BuildConfig.OPENROUTER_API_KEY`. Это **необязательно**: без ключа приложение полностью работает, а ИИ-функции включатся после ввода собственного ключа в «Настройки → API-ключ» (пользовательский ключ всегда приоритетнее сборочного).
4. Собрать и запустить:

   ```bash
   ./gradlew :app:assembleDebug      # APK: app/build/outputs/apk/debug/
   ./gradlew :app:testDebugUnitTest  # юнит-тесты
   ```

Минификация release-сборки выключена; proguard-rules.pro подключён.

---

## Архитектура

Один модуль `:app`, слоистая организация пакетов, MVVM с однонаправленным потоком данных (UDF). Слой домена не зависит от Android-фреймворка (кроме точек, где нужен контекст для строк — см. «Локализация»).

```
┌────────────────────────────────────────────────────────────┐
│ presentation                                               │
│   screens/*  + ViewModel'и (StateFlow → Compose)           │
│   navigation (типобезопасные маршруты)                     │
│   common/components                                        │
├────────────────────────────────────────────────────────────┤
│ domain                                                     │
│   model (Resume, Project, CvAnalysis, CandidateStatus…)    │
│   repository (интерфейсы ResumeRepository, LlmRepository)  │
│   usecase (AnalyzeCvUseCase)                               │
├────────────────────────────────────────────────────────────┤
│ data                                                       │
│   local (Room: ResumeDatabase, DAO, Entity, Converters)    │
│   api/llm (OpenRouterService, Retrofit API, DTO)           │
│   repository (реализации)                                  │
├────────────────────────────────────────────────────────────┤
│ core                                                       │
│   pdf (AndroidPdfTemplate, PdfTemplateType, PdfStyleStore) │
│   export (DocxExporter, CsvExporter)                       │
│   utils (PdfImportParser, LanguageStore, ApiKeyStore…)     │
├────────────────────────────────────────────────────────────┤
│ di (AppModule) + core/di (NetworkModule, LlmModule)        │
└────────────────────────────────────────────────────────────┘
```

### Поток данных на примере формы

```
UI (Compose) ──ResumeFormEvent──▶ ResumeFormViewModel
                                    │ updateState → ProjectFormState… (StateFlow)
                                    │ дебаунс 300 мс → FormValidators.revalidate()
                                    │ автосохранение черновика
                                    ▼
                            buildResume(): ResumeFormState → domain.Resume
                                    ▼
                          ResumeRepositoryImpl → Room (DAO ↔ Entity ↔ Gson)
```

Экраны подписаны на `StateFlow` через `collectAsState`; события пользователя идут вверх sealed-классами (`ResumeFormEvent`). Вью-модели получают зависимости через Hilt-конструктор (`@HiltViewModel`), сообщения об ошибках формируют через `Context.localizedString(...)` с учётом выбранного языка.

### DI-граф (основное)

- `AppModule`: база данных Room (с миграциями), DAO, `Gson`, `ResumeRepository`.
- `NetworkModule`: OkHttp (+логгер), Retrofit `OpenRouterApi`, значение ключа сборки, `OpenRouterService`.
- `LlmModule`: связывает `LlmRepository` с реализацией.
- Хранилища настроек (`ApiKeyStore`, `LanguageStore`, `BiometricLockStore`, `PdfStyleStore`) — `@Singleton` c инъекцией `@ApplicationContext`.

---

## Структура пакетов

```
app/src/main/kotlin/com/bober/autcsv/
├── AutCsvApplication.kt        # @HiltAndroidApp
├── MainActivity.kt             # AppCompatActivity: сплэш, биометрия, NavHost
├── core/
│   ├── constants/LlmConstants.kt
│   ├── di/{LlmModule, NetworkModule}.kt
│   ├── export/{CsvExporter, DocxExporter}.kt
│   ├── pdf/{AndroidPdfTemplate, PdfTemplate, PdfTemplateType,
│   │        PdfStyleStore}.kt
│   └── utils/                  # AppLocales, ApiKeyStore, BiometricLockStore,
│                               # LanguageStore, LlmLogger, LlmResponseProcessor,
│                               # PdfImportParser, PdfPrintHelper, PhoneFormatter
├── data/
│   ├── api/llm/{OpenRouterApi, OpenRouterService, OpenRouterConfig,
│   │            dto/LlmModels}
│   ├── local/{ResumeDatabase, dao/ResumeDao, entity/ResumeEntity,
│   │          converter/Converters}
│   └── repository/{ResumeRepositoryImpl, LlmRepositoryImpl}
├── di/AppModule.kt
├── domain/
│   ├── model/                  # Resume, PersonalInfo, EducationEntry, Project,
│   │                           # ProfessionalSkills, CvAnalysis, VacancyMatch…
│   ├── repository/             # интерфейсы
│   └── usecase/AnalyzeCvUseCase.kt
├── presentation/
│   ├── common/components/      # AdaptiveContent, LoadingState, RoundedCorner…
│   ├── navigation/{NavRoutes, Navigation}.kt
│   └── screens/
│       ├── analysis/  dashboard/  form/ (+components, SuggestionDictionary)
│       ├── list/      preview/    settings/(HelpScreen, AboutScreen,
│       │                                       PrivacyPolicyScreen)
│       └── splash/    trash/
└── ui/theme/                   # AppColors, шрифты, радиусы
app/schemas/…/ResumeDatabase/N.json   # экспортируемые схемы Room
```

Ресурсы локализации: `res/values/strings.xml` (русский — default), `res/values-en/`, `res/values-ru/`; наборы ключей идентичны (503 строки, 17 массивов, плюралы лет).

---

## Ключевые подсистемы

### 1. Данные и хранение

- **Room**, версия схемы 8, экспорт схем включён (`app/schemas/`), применяются явные `Migration` + `fallbackToDestructiveMigration(false)`.
- Плоская таблица резюме; вложенные структуры (`personalInfo`, `professionalSkills`, `projects`, `languages`, `socialLinks`, `aiAnalysis`) сериализуются в JSON-колонки через `Converters` на Gson. Благодаря этому добавление необязательных полей модели (например, `Project.link`) не требует миграции: старые JSON читаются со значениями по умолчанию.
- Фото профиля хранятся файлами во внутреннем хранилище; в бэкап встраиваются base64.

### 2. ИИ-интеграция (OpenRouter)

- `OpenRouterService` — единая точка: чат с фолбэком по цепочке моделей (бесплатные первыми), извлечение JSON из ответа (устойчиво к ```json-обёрткам).
- Сценарии: `analyzeResume`, `improveText`, `generateAboutMe`, `matchVacancy`. Язык ответа модели подбирается под язык интерфейса (`responseLanguage()`).
- Приоритет ключей: пользовательский (EncryptedSharedPreferences, «Настройки → API-ключ») → ключ из `BuildConfig`.
- ⚠️ **Известное ограничение:** в `AnalyzeCvUseCase` реальный вызов LLM временно отключён — активен мок-блок с правдоподобным результатом (тексты локализованы). Реальный путь сохранён в закомментированном блоке внутри `invoke()`.
- `LlmLogger` пишет подробные лог-события (запросы, тайминги, разбор ответов).

### 3. PDF-движок и обратимый импорт

`AndroidPdfTemplate` рисует документ на `android.graphics.pdf.PdfDocument` по декларативной спецификации стиля (`StyleSpec`: палитра, правила секций, чипы, баннеры, сайдбар). Поддержаны: акцентный цвет пользователя (`PdfStyleStore`), порядок секций (`PdfSection`), круглое фото, колонтитул с брендом и страницей, перенос по словам с жёстким фолбэком.

**Round-trip импорта** (`PdfImportParser`) — два канала, по приоритету:

1. **Метаданные**: JSON резюме дублируется в Info-словарь PDF (custom key `AUTCVS_JSON`, записывается pdfbox-android) и невидимыми ASCII-кусками `#A#NNN#<gzip+base64>` прямо на холсте. Каналы не зависят от шрифтов/локали и восстанавливают резюме один-в-один.
2. **Текстовый fallback**: разбор видимого текста по заголовкам секций, меткам опыта («Опыт:», «Experience:»), проектам, чипам навыков, буллетам задач; нормализация заголовков hh.ru; эвристики телефона/email/демографии. Все маркеры распознаются **на обоих языках интерфейса** — PDF, созданный до смены языка, корректно импортируется.

### 4. Экспорт DOCX / CSV

- `DocxExporter` собирает минимально валидный OOXML-пакет (zip на `java.util.zip`): `[Content_Types].xml`, связи, стили (язык документа ru-RU/en-US по локали) и `document.xml` с заголовками, абзацами и буллетами. Открывается в Word/LibreOffice/Google Docs.
- `CsvExporter` — RFC 4180: BOM UTF-8, разделитель «;» (Excel с русской локалью), экранирование кавычек, 30 локализованных колонок; списковые поля сворачиваются через « | ».

### 5. Локализация RU/EN

- Ресурсы: строки, массивы автодополнения (`suggestion_*`), плюралы лет (`experience_years`), переводы всех документов и писем об ошибках. Паритет ключей между локалями проверяется при сборке AAPT.
- Переключение: `LanguageStore` → `AppCompatDelegate.setApplicationLocales()`; `MainActivity` — `AppCompatActivity` (тема `Theme.AppCompat.DayNight.NoActionBar`), поэтому Activity пересоздаётся мгновенно на всех поддерживаемых API; персистентность на API < 33 — сервис `AppLocalesMetadataHolderService` (`autoStoreLocales`), на API 33+ — системные per-app locales (`res/xml/locales_config.xml`).
- Не-Compose код (экспортёры, парсер, ViewModel) берёт строки через `Context.localizedString(resId, …)` из `core/utils/AppLocales.kt` — он оборачивает контекст в выбранную локаль приложения.
- Значения, хранимые в БД текстом (уровень образования, вариант релокации, платформы соцсетей), распознаются по подписям обеих локалей (`knownLabels`) — старые записи не «теряются» после смены языка.
- Соглашение: **никаких пользовательских строк литералами в Kotlin** — только `stringResource` (Compose) или `localizedString` (остальное). Валидаторы возвращают `@StringRes Int?`, локаль применяется в точке отображения.

### 6. Безопасность

- API-ключ пользователя — `EncryptedSharedPreferences` (AES256_GCM/SIV, MasterKey из Android Keystore).
- Биометрическая блокировка (`BiometricLockStore`): экран блокировки при старте и возврате из фона; fail-open, если биометрия отвязана после включения (приложение нельзя сделать неоткрываемым).
- Ключ OpenRouter никогда не пишется в логи.

### 7. Корзина и бэкапы

- Мягкое удаление: резюме помечается `deletedAt` и исчезает из списков; экран «Корзина» позволяет восстановить или удалить окончательно.
- Бэкап: полный JSON всех резюме (фото встроены base64) в загрузки; восстановление через системный файловый пикер. Дополнительно — экспорт всей базы в один CSV.

---

## Навигация

Типобезопасные маршруты на kotlinx.serialization (`NavRoutes.kt`):

`SplashRoute → ResumeListRoute ⇄ { ResumeFormRoute, ResumeFormEditRoute(id), ResumePreviewRoute(id) → AnalysisRoute(id) }`,
`ResumeListRoute → DashboardRoute`, `→ SettingsRoute → { TrashRoute, HelpRoute, AboutRoute, PrivacyPolicyRoute }`.

---

## Тестирование

Юнит-тесты (`app/src/test`): `CsvExporterTest`, `DocxExporterTest`, `PdfImportParserTest`, `FormValidatorsTest`, `SuggestionDictionaryTest`, плюс `PdfPreviewHarness`. Для тестирования PDF-экспорта вне устройства в test-classpath подложены рукописные стабы `android.graphics.*` и `android.util.LruCache` (см. `test/kotlin/android/...`).

```bash
./gradlew :app:testDebugUnitTest
```

Инструментальные тесты Compose подключены (`androidTest`), содержательной нагрузки пока не несут.

---

## Соглашения кода

- Комментарии и документация — на русском; комментарии объясняют «почему», а не «что».
- UI-строки — только через ресурсы (см. «Локализация»). Ключи именуются по экрану/подсистеме: `dashboard_*`, `filter_*`, `validation_*`, `doc_*` (документы), `csv_col_*` (колонки CSV), `privacy_*`, `help_*`.
- Экран: stateless-Composable + `@HiltViewModel` + `StateFlow`; события — sealed-классы.
- Чистые функции валидации/парсинга живут отдельно от ViewModel (`FormValidators`, `PdfImportParser`) и покрыты тестами.
- Новые поля моделей делайте optional-with-default: конвертеры Gson допишут их старым записям без миграции.

---

## Известные ограничения и планы

1. Gson использует reflection — кандидат на замену на kotlinx.serialization (важно для будущей кроссплатформенности, см. ниже).
2. R8-минификация для release включена (keep-правила для Gson/pdfbox — в `proguard-rules.pro`); перед публикацией прогнать смоук-тест экспорта/импорта на реальном устройстве.
3. Сборка: `kapt` переведён на **KSP**, включены parallel/caching/configuration-cache (`gradle.properties`); тёплая no-op сборка ≈ 1–2 с, release-APK ≈ 12 МБ (фильтр локалей en/ru + R8 + shrinkResources).
4. Рассматривается миграция на **Kotlin Multiplatform** (Android/iOS): переносимы домен, CSV-экспорт, парсер, словари; потребуются замены Gson → kotlinx.serialization, Retrofit → Ktor, `java.util.zip` → okio, PDF-движка на expect/actual. Детальный анализ уже выполнен в истории обсуждений проекта.

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.hilt.android)
    kotlin("plugin.serialization") version "2.0.21"
}

// API-ключ читается из gitignore-ного local.properties (OPENROUTER_API_KEY=…),
// чтобы секрет не попадал в коммиты; ключ задаётся через экран настроек приложения
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val openRouterApiKey: String = localProperties.getProperty("OPENROUTER_API_KEY").orEmpty()

android {
    namespace = "com.bober.autcsv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bober.autcsv"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Приложение локализовано только на en/ru; фильтр отбрасывает ~80 локалей
        // из библиотек (appcompat и др.), сокращая APK и обработку ресурсов
        resourceConfigurations += listOf("en", "ru")
    }

    buildTypes {
        release {
            // R8: минификация кода и удаление неиспользуемых ресурсов;
            // keep-правила для reflection-стеков (Gson) — в proguard-rules.pro
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        }
        debug {
            buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

// ── AWT-заглушки для JVM-превью PDF-шаблонов (PdfPreviewHarness) ──────────
// SDK android.jar содержит урезанный java/awt, перекрывающий JDK при
// компиляции unit-тестов. Таска извлекает полный java.awt + javax.imageio
// из текущего JDK в jar; на рантайме используется настоящий java.desktop.
val awtStubJar = layout.buildDirectory.file("awt-stubs/awt-desktop.jar")

val generateAwtStubJar by tasks.registering {
    val output = awtStubJar.get().asFile
    outputs.file(output)
    outputs.upToDateWhen { output.exists() }
    doLast {
        val javaHome = File(System.getProperty("java.home") ?: return@doLast)
        val jmodFile = File(javaHome, "jmods${File.separator}java.desktop.jmod")
        if (!jmodFile.exists()) {
            logger.lifecycle("java.desktop.jmod не найден в $javaHome — генерация awt-desktop.jar пропущена")
            return@doLast
        }
        val isWindows = System.getProperty("os.name").contains("Windows")
        val jmodTool = File(javaHome, "bin${File.separator}jmod" + if (isWindows) ".exe" else "")
        val jarTool = File(javaHome, "bin${File.separator}jar" + if (isWindows) ".exe" else "")
        val tmp = File.createTempFile("awt-stub", "").let {
            it.delete()
            it.mkdirs()
            it
        }
        try {
            // ProcessBuilder вместо Gradle exec: таска должна быть совместима
            // с configuration cache (никаких ссылок на объекты build-скрипта)
            fun runProcess(workingDir: File?, vararg command: String) {
                val process = ProcessBuilder(*command).apply {
                    if (workingDir != null) directory(workingDir)
                    inheritIO()
                }.start()
                val code = process.waitFor()
                check(code == 0) { "Команда завершилась с кодом $code: ${command.joinToString(" ")}" }
            }
            output.parentFile.mkdirs()
            runProcess(
                null,
                jmodTool.absolutePath, "extract",
                "--dir", tmp.absolutePath, jmodFile.absolutePath
            )
            runProcess(
                File(tmp, "classes"),
                jarTool.absolutePath, "cf", output.absolutePath, "java/awt", "javax/imageio"
            )
            logger.lifecycle("Сгенерирован ${output.absolutePath}")
        } finally {
            tmp.deleteRecursively()
        }
    }
}

// KSP и компилятор unit-тестов читают classpath тестов (jar подключён через testImplementation)
tasks.matching {
    (it.name.startsWith("compile") || it.name.startsWith("ksp")) && it.name.contains("UnitTest")
}
    .configureEach { dependsOn(generateAwtStubJar) }

// Конфигурация KSP-процессоров (Room, Hilt)
ksp {
    // Экспорт схем Room для миграций
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    //noinspection KaptUsageInsteadOfKsp
    ksp(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // PDF Generation via Android PdfDocument (no external PDF libs required)
    // PDF Parsing for import (PDFBox-Android)
    implementation(libs.pdfbox.android)
    // Шифрованное хранилище API-ключа (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)
    // Биометрическая блокировка приложения (№36)
    implementation(libs.androidx.biometric)
    // AppCompat для per-app language (AppCompatDelegate.setApplicationLocales)
    implementation(libs.androidx.appcompat)
    // Расширенный набор иконок Material (StarBorder, ContentCopy и т.д.)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Полный java.awt для JVM-превью PDF-шаблонов: android.jar из SDK содержит
    // урезанный java/awt, перекрывающий JDK при компиляции unit-тестов.
    // Jar генерируется таской generateAwtStubJar из текущего JDK.
    testImplementation(files(awtStubJar))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.mockk) {
        exclude(module = "objenesis")
    }
    androidTestImplementation(libs.kotlinx.coroutines.test)

}
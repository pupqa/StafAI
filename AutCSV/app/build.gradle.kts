import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.bober.autcsv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bober.autcsv"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "OPENROUTER_API_KEY", "\"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\"")
        }
        debug {
            buildConfigField("String", "OPENROUTER_API_KEY", "\"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\"")
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

    // Add KAPT configuration
    kapt {
        correctErrorTypes = true
        useBuildCache = true
        // Add JVM arguments for KAPT to fix Java module access
        javacOptions {
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
            option("--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
        }
        // Room schema export
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
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
    kapt(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // PDF Generation
    implementation(libs.itext7.core)
    implementation(libs.kernel)
    implementation(libs.layout)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.mockk) {
        exclude(module = "objenesis")
    }
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // OpenRouter Integration
    implementation(libs.openai)
    implementation(libs.ktor)
}
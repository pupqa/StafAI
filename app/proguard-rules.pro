# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ── Отладка: сохраняем имена строк и номера линий в стектрейсах ──────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Gson (reflection-сериализация без аннотаций) ─────────────────────────
# Модели домена сериализуются по именам полей: конвертеры Room, бэкап JSON,
# встраивание AUTCVS_JSON в PDF. Переименование полей R8 ломает round-trip.
-keep class com.bober.autcsv.domain.model.** { <fields>; }
-keep class com.bober.autcsv.data.api.llm.dto.** { <fields>; }
# Обобщённые TypeToken'ы (TypeToken<List<Project>> и т.п.) читают Signature
-keepattributes Signature, *Annotation*
-keep class * extends com.google.gson.reflect.TypeToken
# Синтезированные accessor'ы Gson (sun.misc.Unsafe не используется на Android)
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Retrofit / OkHttp ──────────────────────────────────────────────────────
# Современные версии несут consumer rules; дублируем базовые для устойчивости
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlinx.serialization.**

# ── pdfbox-android: пишет метаданные AUTCSV_JSON через reflection API ─────
-keep class com.tom_roush.** { *; }
-dontwarn com.tom_roush.**

# ── Корутины / OkHttp / BouncyCastle-транзитивы pdfbox ────────────────────
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

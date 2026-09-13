# ProGuard rules for CodexBar Android

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# Keep serializable domain model classes
-keep class com.steipete.codexbar.domain.model.** { *; }
-keep class com.steipete.codexbar.data.model.** { *; }

# OkHttp 3 & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# AndroidX Security Crypto (KeyStore & Tink)
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# Jetpack Compose
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-dontwarn kotlinx.coroutines.**

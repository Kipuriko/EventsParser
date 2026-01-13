# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve source file and line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ru.purebytestudio.eventparser.**$$serializer { *; }
-keepclassmembers class ru.purebytestudio.eventparser.** {
    *** Companion;
}
-keepclasseswithmembers class ru.purebytestudio.eventparser.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used for serialization
-keep class ru.purebytestudio.eventparser.domain.model.** { *; }
-keep class ru.purebytestudio.eventparser.data.local.entity.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# JSoup
-keep class org.jsoup.nodes.** { *; }
-keep class org.jsoup.parser.** { *; }
-keep class org.jsoup.select.** { *; }
-keeppackagenames org.jsoup.nodes
# JSoup использует Re2j опционально - если его нет, используется стандартный java.util.regex
-dontwarn com.google.re2j.**
-dontwarn org.jspecify.annotations.**

# Koin
-keep class org.koin.core.** { *; }
-keep class org.koin.android.** { *; }
-keep class kotlin.Metadata { *; }

# Orbit MVI
-keep class org.orbitmvi.orbit.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**

# Jetpack Compose
-keep class androidx.compose.runtime.Composer { *; }
-keep class androidx.compose.runtime.ComposerImpl { *; }
-keep class androidx.compose.runtime.ComposerKt { *; }
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt { *; }
-keep class androidx.compose.ui.node.** { *; }
-dontwarn androidx.compose.animation.core.VectorizedAnimationSpecKt

# Crashlytics/Timber
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Ignore warnings for known issues
-ignorewarnings
-dontwarn java.lang.invoke.StringConcatFactory

# Keep all services
-keep class * extends android.app.Service

# Additional resource packaging
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
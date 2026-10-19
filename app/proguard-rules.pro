# Tink (шифрование в androidx.security) использует protobuf-lite через рефлексию.
-keep class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { <fields>; }
-keepclassmembers class com.google.crypto.tink.** { <fields>; }

# Необязательные зависимости Tink, отсутствующие на Android.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign*

# Убираем отладочные логи из релиза.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

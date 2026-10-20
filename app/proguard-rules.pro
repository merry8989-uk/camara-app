# Keep ML Kit and Play Services entry points: they are loaded reflectively by the
# unbundled (Play Services) model loader.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# CameraX and Media3 reference implementations reflectively.
-keep class androidx.camera.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn androidx.camera.**
-dontwarn androidx.media3.**

# Kotlin metadata is needed by the coroutines/reflect paths used by Compose.
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-dontwarn org.jetbrains.annotations.**

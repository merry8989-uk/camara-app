# Keep CameraX and Media3 entry points that are referenced reflectively.
-keep class androidx.camera.** { *; }
-keep class androidx.media3.** { *; }
-dontwarn androidx.camera.**

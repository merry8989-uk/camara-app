plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.camara.app"
    // Media3 1.10+ requires compiling against API 36.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.camara.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.1"

        vectorDrawables { useSupportLibrary = true }

        ndk {
            // Ship the two ABIs real phones actually use. The x86/x86_64 emulator
            // libraries roughly double the APK (ML models are ~20 MB per ABI) for no
            // benefit on hardware.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("camara") {
            // A THROWAWAY development key, committed on purpose so that every build
            // (local and CI) is signed with the SAME certificate. Without this, each
            // CI run generated a fresh debug key and Android refused to install an
            // update over an existing install — you had to uninstall first.
            //
            // It is not a secret and must NEVER be used to publish a real app.
            // Replace it with your own keystore and keep that one private.
            storeFile = rootProject.file("keystore/camara-dev.p12")
            storeType = "PKCS12"
            storePassword = "camara123"
            keyAlias = "camara"
            keyPassword = "camara123"
            // v1 signing is forced on so the signing certificate is inspectable
            // (META-INF/CERT.RSA) and can be verified in CI.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        // Minified on purpose: R8 cuts the (unminified) dex from ~68 MB to a few MB,
        // which is the single biggest lever on the download size.
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("camara")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("camara")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val camerax = "1.3.4"
    val media3 = "1.11.1"

    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    // Core + lifecycle + activity
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // CameraX
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.camera:camera-extensions:$camerax")

    // Media3 — video playback, editing and export
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-transformer:$media3")
    implementation("androidx.media3:media3-effect:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // ML Kit, unbundled ("thin") variants: the models live in Google Play Services
    // instead of inside the APK. Same API, but face detection drops from ~6.9 MB to
    // ~0.8 MB of app size and subject segmentation from tens of MB of native code to
    // ~200 KB. The model downloads on first use (or at install time, see the manifest).
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    // EXIF orientation handling
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

// FFmpeg is bundled in every NusaMusic APK. The AAR was built from the
// official AndroidX Media3 1.9.3 decoder using FFmpeg 6.0 and is tracked in
// app/libs/ so Android Studio builds work after an ordinary Git Pull.
val bundledFfmpegAar = file("libs/media3-decoder-ffmpeg.aar")
check(bundledFfmpegAar.isFile) {
    "Missing bundled FFmpeg library: app/libs/media3-decoder-ffmpeg.aar. " +
        "Pull the full repository (including binary assets) from GitHub."
}

// Fail rather than accidentally shipping an ordinary APK without native
// FFmpeg, or an AAR containing libraries for only the build host ABI.
ZipFile(bundledFfmpegAar).use { archive ->
    val contents = archive.entries().asSequence().map { it.name }.toSet()
    val requiredAbis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    requiredAbis.forEach { abi ->
        check("jni/$abi/libffmpegJNI.so" in contents) {
            "FFmpeg AAR is missing $abi/libffmpegJNI.so. " +
                "A complete native AAR is required for every normal build."
        }
    }
}

val lastFmApiKey = sequenceOf(
    localProperties.getProperty("LASTFM_API_KEY"),
    providers.gradleProperty("LASTFM_API_KEY").orNull,
    providers.environmentVariable("LASTFM_API_KEY").orNull
)
    .filterNotNull()
    .map { it.trim() }
    .firstOrNull { it.isNotEmpty() && it != "YOUR_LASTFM_API_KEY" }
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")


android {
    namespace = "com.yohandeku32.nusamusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yohandeku32.nusamusic"
        minSdk = 29
        targetSdk = 36
        versionCode = 46
        versionName = "3.13.4"

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.github.mwiede:jsch:0.2.23")
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.3")
    implementation("androidx.media3:media3-session:1.9.3")
    implementation("androidx.media3:media3-ui:1.9.3")
    implementation("androidx.media3:media3-inspector:1.9.3")
    // Required direct dependency of AndroidX's native FFmpeg extension.
    implementation("androidx.media3:media3-decoder:1.9.3")
    implementation(files(bundledFfmpegAar))
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

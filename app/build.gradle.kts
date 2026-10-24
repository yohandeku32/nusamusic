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

// Media3's FFmpeg renderer is a native Android extension, not a Maven
// dependency. The AAR is optional so the app remains buildable without it.
// Build it from the matching AndroidX Media3 v1.9.3 source (see docs).
val localFfmpegAar = file("libs/media3-decoder-ffmpeg.aar")
val requireFfmpegDecoder = providers.gradleProperty("requireFfmpegDecoder")
    .map { it.toBoolean() }
    .getOrElse(false)

if (requireFfmpegDecoder && !localFfmpegAar.isFile) {
    error(
        "FFmpeg was required, but app/libs/media3-decoder-ffmpeg.aar is missing. " +
            "See docs/FFMPEG_ANDROID.md to build the native extension."
    )
}

if (localFfmpegAar.isFile) {
    // A Java-only stub AAR is not functional. Require the native JNI shared
    // library to be present so builds do not silently claim FFmpeg support.
    val hasNativeJni = ZipFile(localFfmpegAar).use { zip ->
        zip.entries().asSequence().any {
            it.name.startsWith("jni/") && it.name.endsWith("/libffmpegJNI.so")
        }
    }
    check(hasNativeJni) {
        "The FFmpeg AAR is missing libffmpegJNI.so. " +
            "Build its Android NDK libraries before exporting the AAR."
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    // Actual Lucide ImageVector pack, loaded lazily per used icon.
    implementation("com.composables:icons-lucide-cmp:2.2.1")
    // Hardware-backed, source-aware backdrop blur for the floating
    // Immersive control pills. Pin Haze 1.6.x for Kotlin 2.2.10 compatibility;
    // Haze 2.x requires a newer Kotlin/Compose toolchain.
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    // Persistent, bounded video cache for Spotify Canvas (Media3 SimpleCache).
    implementation("androidx.media3:media3-datasource:1.9.3")
    implementation("androidx.media3:media3-database:1.9.3")
    implementation("androidx.media3:media3-exoplayer-hls:1.9.3")
    implementation("androidx.media3:media3-session:1.9.3")
    implementation("androidx.media3:media3-ui:1.9.3")
    implementation("androidx.media3:media3-inspector:1.9.3")
    // Last stable UI release on Compose 1.9; newer releases require Compose 1.10.
    implementation("com.mocharealm.accompanist:lyrics-core:0.4.2")
    implementation("com.mocharealm.accompanist:lyrics-ui:1.0.15")
    if (localFfmpegAar.isFile) {
        // The extension depends on Media3's decoder base module; keep all
        // Media3 dependencies at exactly the same version.
        implementation("androidx.media3:media3-decoder:1.9.3")
        implementation(files(localFfmpegAar))
    }
    testImplementation("junit:junit:4.13.2")
    // Real JSONObject for pure JVM online-lyrics contract tests.
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

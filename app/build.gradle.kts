import java.io.File
import java.net.URI
import java.util.Properties

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

val lastFmApiKey = localProperties.getProperty("LASTFM_API_KEY", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val decentAudioEngineVersion = "v0.1.0-libs"
val decentAudioEngineBaseUrl =
    "https://github.com/Ma145/decent-player/releases/download/$decentAudioEngineVersion"

private val decentAudioEngineArtifacts = listOf(
    "decent-usb-audio-driver-release.aar",
    "decent-usb-audio-wrapper-media3-release.aar"
)

val decentAudioEngineDir = layout.buildDirectory.dir("decent-audio-engine").get().asFile

val downloadDecentAudioEngine by tasks.registering {
    outputs.files(
        decentAudioEngineArtifacts.map { File(decentAudioEngineDir, it) }
    )

    doLast {
        decentAudioEngineDir.mkdirs()

        decentAudioEngineArtifacts.forEach { artifactName ->
            val destination = File(decentAudioEngineDir, artifactName)
            if (destination.exists() && destination.length() > 0L) {
                logger.lifecycle("Decent Audio Engine: using cached $artifactName")
                return@forEach
            }

            val artifactUrl = "$decentAudioEngineBaseUrl/$artifactName"
            logger.lifecycle("Decent Audio Engine: downloading $artifactName")

            val connection = URI(artifactUrl).toURL().openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 60_000
            }

            connection.getInputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            check(destination.length() > 0L) {
                "Downloaded Decent Audio Engine artifact is empty: $artifactName"
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadDecentAudioEngine)
}


android {
    namespace = "com.yohandeku32.nusamusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yohandeku32.nusamusic"
        // The Decent USB Audio engine requires Android 10+ (API 29).
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    implementation(
        files(
            File(decentAudioEngineDir, "decent-usb-audio-driver-release.aar"),
            File(decentAudioEngineDir, "decent-usb-audio-wrapper-media3-release.aar")
        )
    )
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.github.mwiede:jsch:0.2.23")
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.media3:media3-session:1.9.3")
    implementation("androidx.media3:media3-ui:1.9.3")
    implementation("androidx.media3:media3-inspector:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

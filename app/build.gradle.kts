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

val canvasApiBaseUrl = sequenceOf(
    localProperties.getProperty("CANVAS_API_BASE_URL"),
    providers.gradleProperty("CANVAS_API_BASE_URL").orNull,
    providers.environmentVariable("CANVAS_API_BASE_URL").orNull
)
    .filterNotNull()
    .map { it.trim().trimEnd('/') }
    .firstOrNull { it.isNotEmpty() && it != "YOUR_CANVAS_API_BASE_URL" }
    .orEmpty()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

val canvasApiKey = sequenceOf(
    localProperties.getProperty("CANVAS_API_KEY"),
    providers.gradleProperty("CANVAS_API_KEY").orNull,
    providers.environmentVariable("CANVAS_API_KEY").orNull
)
    .filterNotNull()
    .map { it.trim() }
    .firstOrNull { it.isNotEmpty() && it != "YOUR_CANVAS_API_KEY" }
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
        buildConfigField("String", "CANVAS_API_BASE_URL", "\"$canvasApiBaseUrl\"")
        buildConfigField("String", "CANVAS_API_KEY", "\"$canvasApiKey\"")
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
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.9.3")
    implementation("androidx.media3:media3-session:1.9.3")
    implementation("androidx.media3:media3-ui:1.9.3")
    implementation("androidx.media3:media3-inspector:1.9.3")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

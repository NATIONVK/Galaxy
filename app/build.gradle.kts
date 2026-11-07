import java.util.Properties

val galaxyLocalProperties = Properties().apply {
    val secretsFile = rootProject.file("local.properties")
    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
}
val galaxyApiKey = galaxyLocalProperties.getProperty("GALAXY_API_KEY") ?: System.getenv("GALAXY_API_KEY").orEmpty()
val escapedGalaxyApiKey = galaxyApiKey.replace("\\", "\\\\").replace("\"", "\\\"")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nationvk.galaxy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nationvk.galaxy"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GALAXY_API_KEY", "\"${escapedGalaxyApiKey}\"")
        buildConfigField("String", "GALAXY_API_BASE_URL", "\"https://generativelanguage.googleapis.com\"")
        buildConfigField("String", "GALAXY_API_MODEL", "\"gemini-2.5-flash\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

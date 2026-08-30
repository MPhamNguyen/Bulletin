import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

fun parseEnvLine(line: String): Pair<String, String>? {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
        return null
    }
    val parts = trimmed.split("=", limit = 2)
    val key = parts[0].trim()
    val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
    return key to value
}

fun readPropertyFromEnvFile(key: String): String? {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) {
        return null
    }
    for (line in envFile.readLines()) {
        val entry = parseEnvLine(line) ?: continue
        if (entry.first == key) {
            return entry.second
        }
    }
    return null
}

fun readEnvProperty(key: String, defaultValue: String = ""): String {
    return readPropertyFromEnvFile(key)
        ?: System.getenv(key)
        ?: (project.findProperty(key) as? String)
        ?: defaultValue
}

android {
    namespace = "com.jdrms.bulletin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jdrms.bulletin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val supabaseUrl = readEnvProperty("SUPABASE_URL", "")
        val supabaseApiKey = readEnvProperty(
            "SUPABASE_API_KEY",
            readEnvProperty("SUPABASE_ANON_KEY", "")
        )
        val supabaseIsConnected = readEnvProperty("SUPABASE_IS_CONNECTED", "false").toBoolean()

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_API_KEY", "\"$supabaseApiKey\"")
        buildConfigField("boolean", "SUPABASE_IS_CONNECTED", "$supabaseIsConnected")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

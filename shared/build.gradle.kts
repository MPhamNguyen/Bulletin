import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    android {
        namespace = "com.jdrms.bulletin.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.datastore.preferences)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
            implementation(libs.ktor.client.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.compose.uiTestJunit4)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.espresso.core)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Class-file filtering also includes names such as ListingsViewModelTestPart1.
// Hash the outer class name so nested tests stay on the same shard.
class TestShardSpec(private val count: Int, private val index: Int) :
    Spec<FileTreeElement>, java.io.Serializable {
    override fun isSatisfiedBy(element: FileTreeElement): Boolean {
        if (element.isDirectory) return true
        val className = element.relativePath.pathString.removeSuffix(".class").substringBefore('$')
        return Math.floorMod(className.hashCode(), count) == index
    }
}

tasks.withType<Test>().configureEach {
    // Robolectric forks are memory hungry; keep one JVM per shard.
    maxParallelForks = 1
    val countProperty = providers.gradleProperty("testShardCount").orNull
    val indexProperty = providers.gradleProperty("testShardIndex").orNull
    if (countProperty != null || indexProperty != null) {
        val count = countProperty?.toIntOrNull()
        val index = indexProperty?.toIntOrNull()
        require(count != null && count > 0 && index != null && index in 0 until count) {
            "Set both testShardCount (> 0) and testShardIndex (0 until count) to integers"
        }
        inputs.property("testShardCount", count)
        inputs.property("testShardIndex", index)
        include(TestShardSpec(count, index))
        filter.isFailOnNoMatchingTests = false
        failOnNoDiscoveredTests = false
    }
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.detekt)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // KMP + Android source sets. Non-existent dirs are ignored per module.
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/commonTest/kotlin",
            "src/androidHostTest/kotlin",
            "src/main/kotlin",
            "src/test/kotlin",
        )
    }

    // The type-safe `libs` accessor isn't visible inside subprojects {}, so resolve the
    // version catalog explicitly.
    val detektFormatting = rootProject.extensions
        .getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
        .named("libs")
        .findLibrary("detekt-formatting")
        .get()
    dependencies {
        add("detektPlugins", detektFormatting)
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        autoCorrect = true
        reports {
            html.required.set(true)
            sarif.required.set(true)
            xml.required.set(false)
            md.required.set(false)
            txt.required.set(false)
        }
    }
}

abstract class FormatTask : DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val projectDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        val root = projectDirectory.get().asFile
        root.walkTopDown().filter { file ->
            file.isFile && (file.extension in setOf("kt", "kts", "xml", "yml", "yaml", "toml", "properties", "pro")) &&
                !file.path.contains("/build/") && !file.path.contains("/.gradle/") && !file.path.contains("/.idea/")
        }.forEach { file ->
            val text = file.readText()
            if (text.isNotEmpty()) {
                val trimmed = text.trimEnd('\r', '\n') + "\n"
                if (trimmed != text) {
                    file.writeText(trimmed)
                }
            }
        }
    }
}

tasks.register<FormatTask>("format") {
    group = "formatting"
    description = "Formats all Kotlin source files and Gradle build scripts."
    projectDirectory.set(layout.projectDirectory)
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "detekt" } })
}

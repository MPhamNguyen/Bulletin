package com.jdrms.bulletin.domain.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Runs with the normal host tests, including CI's affected-context Home test filter. */
class HomeDomainBoundaryTest {
    @Test
    fun homeDomainImportsOnlyKotlinItsOwnDomainAndSharedPrimitives() {
        val path = "src/commonMain/kotlin/com/jdrms/bulletin/domain/home/domain"
        val source = listOf(File(path), File("shared/$path")).firstOrNull { it.isDirectory }
        requireNotNull(source) { "Home domain sources were not found; boundary verification must not be skipped" }
        val allowed = listOf(
            "kotlin.",
            "com.jdrms.bulletin.core.common.",
            "com.jdrms.bulletin.domain.home.domain."
        )
        val imports = Regex("""(?m)^\s*import\s+([^\s;]+)""")
        require(
            imports.containsMatchIn("import android.view.View")
        ) { "Import scanning must not silently skip imports" }
        val violations = source.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            imports.findAll(file.readText()).mapNotNull { match ->
                val dependency = match.groupValues[1]
                if (allowed.any(dependency::startsWith)) null else "${file.name}: $dependency"
            }
        }.toList()
        assertTrue(violations.isEmpty(), "Home domain has outward dependencies: ${violations.joinToString()}")
    }
}

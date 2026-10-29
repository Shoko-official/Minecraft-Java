plugins {
    id("minecraft.java-conventions")
}

// Every code module: all projects but this one and the parents without code (:engine, :tools).
val codeModules = rootProject.subprojects.filter { it != project && it.childProjects.isEmpty() }

// Package suffix of a module, as in ModuleRules.DIRECT: :engine:core is core, :tools:oracle is tools.oracle.
fun moduleName(module: Project): String = module.path.removePrefix(":").removePrefix("engine:").replace(':', '.')

fun hasCode(module: Project): Boolean =
    !module.fileTree("src/main/java") {
        include("**/*.java")
        exclude("**/package-info.java")
    }.isEmpty

dependencies {
    codeModules.forEach { testImplementation(project(it.path)) }
    testImplementation(libs.archunit.junit5)
    // The fixtures of the Gson rules (ADR 0006) need Gson types to break them.
    testImplementation(libs.gson)
}

tasks.test {
    systemProperty("architecture.modules", codeModules.joinToString(",") { moduleName(it) })
    systemProperty("architecture.modulesWithCode", codeModules.filter(::hasCode).joinToString(",") { moduleName(it) })
    // ModuleGraphTest reads the module table; without this input a cached result would hide a doc change.
    inputs.file(rootProject.file("docs/ARCHITECTURE.md"))
        .withPropertyName("architectureDoc")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// No test here talks to the reference game, and its engine needs the system properties set on test.
tasks.named("oracleTest") {
    enabled = false
}

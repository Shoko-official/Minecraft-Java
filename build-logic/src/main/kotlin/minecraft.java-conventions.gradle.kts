import java.time.Duration

plugins {
    `java-library`
    checkstyle
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
}

val libs = the<VersionCatalogsExtension>().named("libs")

group = "dev.shoko.minecraft"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    errorprone(libs.findLibrary("errorprone-core").get())
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

// A looping test must kill the forked test JVM instead of hanging the build: JUnit @Timeout runs
// the test in a separate thread that it cannot stop.
tasks.withType<Test>().configureEach {
    timeout = Duration.ofMinutes(5)
}

// Tests against the reference game need a local oracle (ADR 0002): they run in oracleTest only.
tasks.test {
    useJUnitPlatform {
        excludeTags("oracle")
    }
}

val oracleTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the differential tests that need the local reference game."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("oracle")
    }

    // Oracle tests skip themselves when the cache is absent: running this task without it would
    // report success with nothing proved. A cached or up-to-date result would hide the same thing.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
    val cacheDirVariable = providers.environmentVariable("MINECRAFT_CACHE_DIR")
    val defaultCacheDir = rootProject.layout.projectDirectory.dir(".cache")
    doFirst {
        val cacheDir = cacheDirVariable.orNull?.takeIf { it.isNotBlank() }?.let(::File)
            ?: defaultCacheDir.asFile
        val reference = cacheDir.resolve("reference")
        if (reference.list().isNullOrEmpty()) {
            throw GradleException(
                "oracleTest needs the reference data, but $reference is missing or empty. " +
                    "Run ./gradlew :tools:bootstrap:run first " +
                    "(or set MINECRAFT_CACHE_DIR to an existing cache)."
            )
        }
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.findVersion("googleJavaFormat").get().requiredVersion)
    }
}

checkstyle {
    toolVersion = libs.findVersion("checkstyle").get().requiredVersion
    configDirectory = rootProject.layout.projectDirectory.dir("config/checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

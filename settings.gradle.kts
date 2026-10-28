pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "minecraft-java"

include(
    ":engine:core",
    ":engine:data",
    ":engine:world",
    ":engine:item",
    ":engine:loot",
    ":engine:worldgen",
    ":engine:entity",
    ":engine:game",
    ":net",
    ":server",
    ":client",
    ":tools:bootstrap",
    ":tools:oracle",
    ":tools:diff",
    ":architecture-tests",
)

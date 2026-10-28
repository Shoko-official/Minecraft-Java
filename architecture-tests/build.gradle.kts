plugins {
    id("minecraft.java-conventions")
}

dependencies {
    testImplementation(project(":engine:core"))
    testImplementation(project(":engine:data"))
    testImplementation(project(":engine:world"))
    testImplementation(project(":engine:item"))
    testImplementation(project(":engine:loot"))
    testImplementation(project(":engine:worldgen"))
    testImplementation(project(":engine:entity"))
    testImplementation(project(":engine:game"))
    testImplementation(project(":net"))
    testImplementation(project(":server"))
    testImplementation(project(":client"))
    testImplementation(project(":tools:bootstrap"))
    testImplementation(project(":tools:oracle"))
    testImplementation(project(":tools:diff"))
    testImplementation(libs.archunit.junit5)
}

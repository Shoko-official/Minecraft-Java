plugins {
    id("minecraft.java-conventions")
}

dependencies {
    api(project(":engine:world"))
    api(project(":engine:worldgen"))
    api(project(":engine:entity"))
    api(project(":engine:loot"))
    api(project(":engine:game"))
    api(project(":tools:oracle"))
    implementation(libs.gson)
}

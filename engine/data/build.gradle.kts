plugins {
    id("minecraft.java-conventions")
}

dependencies {
    api(project(":engine:core"))
    // ADR 0006: confined to dev.shoko.minecraft.data.json, absent from every public signature.
    implementation(libs.gson)
}

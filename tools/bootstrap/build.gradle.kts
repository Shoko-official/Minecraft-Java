plugins {
    id("minecraft.java-conventions")
    application
}

dependencies {
    implementation(libs.gson)
}

application {
    mainClass = "dev.shoko.minecraft.tools.bootstrap.Bootstrap"
}

tasks.named<JavaExec>("run") {
    // The cache lives at the repository root, not inside the module directory.
    workingDir = rootProject.projectDir
}

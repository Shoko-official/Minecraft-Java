plugins {
    id("minecraft.java-conventions")
    application
}

application {
    mainClass = "dev.shoko.minecraft.tools.oracle.OracleMain"
}

tasks.named<JavaExec>("run") {
    // The cache lives at the repository root, not inside the module directory.
    workingDir = rootProject.projectDir
}

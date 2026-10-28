plugins {
    id("minecraft.java-conventions")
}

dependencies {
    api(project(":engine:worldgen"))
    api(project(":engine:entity"))
    api(project(":engine:loot"))
}
